package com.inspectorsgadget.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Sign-in state for the app.
 *
 * Credentials are checked by a {@link Provider}. Today that is {@link LocalProvider}, which keeps
 * accounts on this device (PBKDF2 password hashes) because there is no account backend yet. When
 * the relay gets account endpoints, add a provider that calls them and return it from
 * {@link #provider}: the login screen, the session and biometric login stay the same, because a
 * provider only hands back an email, a name and an opaque refresh token.
 *
 * Planned relay endpoints: POST /v1/auth/register, POST /v1/auth/login, POST /v1/auth/refresh
 * (refresh token to a new session) and POST /v1/auth/reset (emails a reset link).
 *
 * Session rules: the refresh token is kept in private storage so the inspector stays signed in.
 * With biometric login on, the token is kept only encrypted by {@link Biometrics}, so every cold
 * start and every sign-in after logging out needs a fingerprint, face or the password.
 */
public final class Auth {
    private Auth() {}

    static final String PREFS = "auth";
    public static final int MIN_PASSWORD = 8;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** Unlocked for this process: set after a successful sign-in, cleared on logout. */
    private static volatile boolean unlocked;
    /** Refresh token for the signed-in user, held in memory while unlocked. */
    private static volatile String sessionToken = "";

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------ provider

    /** Result of a provider call. */
    public static final class Result {
        public final boolean ok;
        public final String error, email, name, refreshToken;

        Result(boolean ok, String error, String email, String name, String refreshToken) {
            this.ok = ok;
            this.error = error;
            this.email = email;
            this.name = name;
            this.refreshToken = refreshToken;
        }

        static Result fail(String error) { return new Result(false, error, "", "", ""); }

        static Result ok(String email, String name, String token) { return new Result(true, "", email, name, token); }
    }

    /** Checks credentials. Calls may block (network or hashing); {@link #run} keeps them off the UI thread. */
    public interface Provider {
        Result signIn(String email, String password);

        Result register(String name, String email, String password);

        /** Exchanges a refresh token saved by an earlier sign-in for a session (biometric login). */
        Result refresh(String email, String refreshToken);

        boolean exists(String email);

        /** True when the password is reset here after the device screen lock, false when a link is emailed. */
        boolean resetsOnDevice();

        /** Emails a reset link (online provider) or sets the new password (on-device provider). */
        Result resetPassword(String email, String newPassword);
    }

    private static Provider provider;

    public static synchronized Provider provider(Context c) {
        if (provider == null) provider = new LocalProvider(c.getApplicationContext());
        return provider;
    }

    /** For tests. */
    static synchronized void setProvider(Provider p) { provider = p; }

    public interface Callback { void done(Result r); }

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Call { Result call(); }

    /** Runs a provider call on a worker thread and delivers the result on the main thread. */
    public static void run(final Call call, final Callback cb) {
        WORKER.execute(new Runnable() {
            @Override
            public void run() {
                Result r;
                try {
                    r = call.call();
                } catch (Exception e) {
                    r = Result.fail("Sign in failed. Try again.");
                }
                final Result out = r;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() { cb.done(out); }
                });
            }
        });
    }

    // ------------------------------------------------------------ validation

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.US);
    }

    public static boolean validEmail(String email) { return EMAIL.matcher(normalizeEmail(email)).matches(); }

    // ------------------------------------------------------------ session

    public static boolean isUnlocked() { return unlocked; }

    /** Signed in on this device: a token is stored in the clear or behind biometrics. */
    public static boolean hasSession(Context c) {
        return email(c).length() > 0 && (storedToken(c).length() > 0 || Biometrics.isEnabled(c));
    }

    public static String email(Context c) { return prefs(c).getString("session.email", ""); }

    public static String name(Context c) { return prefs(c).getString("session.name", ""); }

    /** Email shown on the login screen: the last one used. */
    public static String lastEmail(Context c) { return prefs(c).getString("last.email", ""); }

    static String storedToken(Context c) { return prefs(c).getString("session.token", ""); }

    public static String token() { return sessionToken; }

    /**
     * Called on app start. Without biometric login a stored session unlocks the app, so the
     * inspector is not asked to sign in every time.
     */
    public static void restore(Context c) {
        if (unlocked) return;
        String t = storedToken(c);
        if (email(c).length() > 0 && t.length() > 0 && !Biometrics.isEnabled(c)) {
            sessionToken = t;
            unlocked = true;
        }
    }

    /** Records a successful sign-in. */
    public static void begin(Context c, Result r) {
        sessionToken = r.refreshToken;
        unlocked = true;
        SharedPreferences.Editor e = prefs(c).edit().putString("session.email", r.email)
                .putString("session.name", r.name).putString("last.email", r.email);
        if (Biometrics.isEnabled(c) && Biometrics.enabledEmail(c).equals(r.email)) {
            e.remove("session.token");
        } else {
            e.putString("session.token", r.refreshToken);
        }
        e.apply();
        if (Biometrics.isEnabled(c) && !Biometrics.enabledEmail(c).equals(r.email)) Biometrics.disable(c);
    }

    /** Persists the token in the clear again, after biometric login is turned off. */
    static void storeTokenInClear(Context c) {
        if (sessionToken.length() > 0) prefs(c).edit().putString("session.token", sessionToken).apply();
    }

    /** Called once the token is stored behind biometrics. */
    static void dropClearToken(Context c) { prefs(c).edit().remove("session.token").apply(); }

    /** Logs out. Biometric login stays set up, so the next sign-in can use it. */
    public static void logout(Context c) {
        unlocked = false;
        sessionToken = "";
        prefs(c).edit().remove("session.token").apply();
    }

    /** For tests: unlocked as a signed-in inspector, or locked. */
    static void setUnlockedForTest(boolean on) {
        unlocked = on;
        sessionToken = on ? "test" : "";
    }

    // ------------------------------------------------------------ local provider

    /** Accounts stored on this device. Passwords are PBKDF2-SHA256 hashed with a per-account salt. */
    static final class LocalProvider implements Provider {
        private static final int ITERATIONS = 120000, MAX_TOKENS = 5;
        private final Context c;
        private final SecureRandom random = new SecureRandom();

        LocalProvider(Context c) { this.c = c; }

        private SharedPreferences users() {
            return c.getSharedPreferences("auth_users", Context.MODE_PRIVATE);
        }

        private static boolean isDemoAdmin(String email) { return Config.DEMO_ADMIN_EMAIL.equals(email); }

        /** Creates the built-in demo admin the first time it is looked up (see {@link Config#DEMO_ADMIN}). */
        private void seedDemoAdmin() {
            if (users().contains(Config.DEMO_ADMIN_EMAIL)) return;
            try {
                JSONObject u = new JSONObject();
                u.put("name", Config.DEMO_ADMIN_NAME);
                setPassword(u, Config.DEMO_ADMIN_PASSWORD);
                u.put("created", System.currentTimeMillis());
                u.put("demoAdmin", true);
                users().edit().putString(Config.DEMO_ADMIN_EMAIL, u.toString()).commit();
            } catch (Exception ignored) {
            }
        }

        private JSONObject user(String email) {
            if (isDemoAdmin(email)) {
                if (!Config.DEMO_ADMIN) return null;
                seedDemoAdmin();
            }
            String s = users().getString(email, "");
            if (s.length() == 0) return null;
            try {
                return new JSONObject(s);
            } catch (Exception e) {
                return null;
            }
        }

        private void save(String email, JSONObject u) { users().edit().putString(email, u.toString()).apply(); }

        @Override
        public boolean exists(String email) { return user(normalizeEmail(email)) != null; }

        @Override
        public Result register(String name, String email, String password) {
            email = normalizeEmail(email);
            if (user(email) != null) return Result.fail("An account with this email already exists");
            try {
                JSONObject u = new JSONObject();
                u.put("name", name.trim());
                setPassword(u, password);
                u.put("created", System.currentTimeMillis());
                String token = newToken(u);
                save(email, u);
                return Result.ok(email, name.trim(), token);
            } catch (Exception e) {
                return Result.fail("Could not create the account");
            }
        }

        @Override
        public Result signIn(String email, String password) {
            email = normalizeEmail(email);
            JSONObject u = user(email);
            if (u == null || !checkPassword(u, password)) return Result.fail("Incorrect email or password");
            try {
                String token = newToken(u);
                save(email, u);
                return Result.ok(email, u.optString("name"), token);
            } catch (Exception e) {
                return Result.fail("Sign in failed. Try again.");
            }
        }

        @Override
        public Result refresh(String email, String refreshToken) {
            email = normalizeEmail(email);
            JSONObject u = user(email);
            if (u == null || refreshToken == null || refreshToken.length() == 0) return Result.fail("Log in with your password");
            org.json.JSONArray tokens = u.optJSONArray("tokens");
            byte[] h = sha256(refreshToken).getBytes();
            for (int i = 0; tokens != null && i < tokens.length(); i++) {
                if (MessageDigest.isEqual(h, tokens.optString(i).getBytes())) return Result.ok(email, u.optString("name"), refreshToken);
            }
            return Result.fail("Log in with your password");
        }

        @Override
        public boolean resetsOnDevice() { return true; }

        @Override
        public Result resetPassword(String email, String newPassword) {
            email = normalizeEmail(email);
            JSONObject u = user(email);
            if (u == null) return Result.fail("No account found for this email");
            try {
                setPassword(u, newPassword);
                u.remove("tokens");
                String token = newToken(u);
                save(email, u);
                return Result.ok(email, u.optString("name"), token);
            } catch (Exception e) {
                return Result.fail("Could not reset the password");
            }
        }

        /**
         * A new refresh token. Only hashes are stored, the newest few per account, so a token held
         * behind biometrics stays valid after a password sign-in. A password reset revokes them all.
         */
        private String newToken(JSONObject u) throws Exception {
            byte[] b = new byte[32];
            random.nextBytes(b);
            String token = Base64.encodeToString(b, Base64.NO_WRAP | Base64.URL_SAFE);
            org.json.JSONArray old = u.optJSONArray("tokens"), keep = new org.json.JSONArray();
            int from = old == null ? 0 : Math.max(0, old.length() - (MAX_TOKENS - 1));
            for (int i = from; old != null && i < old.length(); i++) keep.put(old.getString(i));
            keep.put(sha256(token));
            u.put("tokens", keep);
            return token;
        }

        private void setPassword(JSONObject u, String password) throws Exception {
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            u.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP));
            u.put("iter", ITERATIONS);
            u.put("hash", Base64.encodeToString(pbkdf2(password, salt, ITERATIONS), Base64.NO_WRAP));
        }

        private boolean checkPassword(JSONObject u, String password) {
            try {
                byte[] salt = Base64.decode(u.getString("salt"), Base64.NO_WRAP);
                byte[] want = Base64.decode(u.getString("hash"), Base64.NO_WRAP);
                return MessageDigest.isEqual(want, pbkdf2(password, salt, u.optInt("iter", ITERATIONS)));
            } catch (Exception e) {
                return false;
            }
        }

        private static byte[] pbkdf2(String password, byte[] salt, int iterations) throws Exception {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        }

        private static String sha256(String s) {
            try {
                byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));
                return Base64.encodeToString(d, Base64.NO_WRAP);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
