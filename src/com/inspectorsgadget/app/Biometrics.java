package com.inspectorsgadget.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Fingerprint and face login with the framework BiometricPrompt (Android 9 and later).
 *
 * The session's refresh token is encrypted with an Android Keystore key. On devices with a strong
 * (Class 3) biometric the key itself requires a biometric for every use and is invalidated when a
 * new fingerprint or face is enrolled, so the token can only be decrypted inside the prompt. Devices
 * whose only biometric is weak (Class 2, for example many face unlocks) cannot unlock keys, so there
 * the prompt gates a key without that requirement.
 */
public final class Biometrics {
    private Biometrics() {}

    public static final int NONE = 0, WEAK = 1, STRONG = 2;

    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_STRONG = "ig_bio_strong", KEY_WEAK = "ig_bio_weak";
    private static final String TRANSFORM = "AES/GCM/NoPadding";

    private static SharedPreferences prefs(Context c) { return Auth.prefs(c); }

    /** What this device can do right now: NONE, WEAK or STRONG. */
    @SuppressWarnings("deprecation")
    public static int available(Context c) {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                BiometricManager m = c.getSystemService(BiometricManager.class);
                if (m == null) return NONE;
                if (m.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) return STRONG;
                if (m.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) return WEAK;
                return NONE;
            }
            if (Build.VERSION.SDK_INT == 29) {
                BiometricManager m = c.getSystemService(BiometricManager.class);
                return m != null && m.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS ? STRONG : NONE;
            }
            if (Build.VERSION.SDK_INT == 28) {
                FingerprintManager f = c.getSystemService(FingerprintManager.class);
                return f != null && f.isHardwareDetected() && f.hasEnrolledFingerprints() ? STRONG : NONE;
            }
        } catch (Exception ignored) {
        }
        return NONE;
    }

    public static boolean isEnabled(Context c) { return prefs(c).getString("bio.blob", "").length() > 0; }

    /** Account that biometric login signs in to. */
    public static String enabledEmail(Context c) { return prefs(c).getString("bio.email", ""); }

    public static boolean declined(Context c, String email) { return prefs(c).getBoolean("bio.declined." + email, false); }

    public static void setDeclined(Context c, String email) { prefs(c).edit().putBoolean("bio.declined." + email, true).apply(); }

    /** Turns biometric login off and deletes its key. The session token goes back to plain private storage. */
    public static void disable(Context c) {
        prefs(c).edit().remove("bio.blob").remove("bio.iv").remove("bio.email").remove("bio.mode").apply();
        try {
            KeyStore ks = KeyStore.getInstance(KEYSTORE);
            ks.load(null);
            ks.deleteEntry(KEY_STRONG);
            ks.deleteEntry(KEY_WEAK);
        } catch (Exception ignored) {
        }
        if (Auth.isUnlocked()) Auth.storeTokenInClear(c);
    }

    public interface Listener {
        /** Token decrypted (login) or stored (enable). */
        void success(String token);

        /** Cancelled or "Use password". */
        void cancelled();

        /** Biometric login can't be used any more (key invalidated, data damaged); it has been turned off. */
        void failed(String message);
    }

    /** Asks for a biometric and stores the signed-in session's token behind it. */
    public static void enable(final Activity a, final Listener l) {
        final String email = Auth.email(a), token = Auth.token();
        final int level = available(a);
        if (level == NONE || token.length() == 0) {
            l.failed("Biometric login is not available on this device");
            return;
        }
        final Cipher cipher;
        try {
            SecretKey key = createKey(level == STRONG);
            cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (Exception e) {
            l.failed("Biometric login is not available on this device");
            return;
        }
        prompt(a, level, "Enable biometric login", email, "Cancel", level == STRONG ? cipher : null, new Done() {
            @Override
            public void ok(Cipher authed) {
                try {
                    Cipher use = authed != null ? authed : cipher;
                    byte[] blob = use.doFinal(token.getBytes("UTF-8"));
                    prefs(a).edit().putString("bio.blob", Base64.encodeToString(blob, Base64.NO_WRAP))
                            .putString("bio.iv", Base64.encodeToString(use.getIV(), Base64.NO_WRAP))
                            .putString("bio.email", email).putString("bio.mode", level == STRONG ? "strong" : "weak")
                            .remove("bio.declined." + email).apply();
                    Auth.dropClearToken(a);
                    l.success(token);
                } catch (Exception e) {
                    disable(a);
                    l.failed("Could not enable biometric login");
                }
            }

            @Override
            public void cancel() { l.cancelled(); }
        });
    }

    /** Asks for a biometric and returns the stored token for {@link #enabledEmail}. */
    public static void unlock(final Activity a, final Listener l) {
        final boolean strong = "strong".equals(prefs(a).getString("bio.mode", ""));
        final int level = available(a);
        if (level == NONE || (strong && level != STRONG)) {
            l.failed("Biometric login is not available. Log in with your password.");
            return;
        }
        final Cipher cipher;
        final byte[] blob;
        try {
            blob = Base64.decode(prefs(a).getString("bio.blob", ""), Base64.NO_WRAP);
            byte[] iv = Base64.decode(prefs(a).getString("bio.iv", ""), Base64.NO_WRAP);
            KeyStore ks = KeyStore.getInstance(KEYSTORE);
            ks.load(null);
            SecretKey key = (SecretKey) ks.getKey(strong ? KEY_STRONG : KEY_WEAK, null);
            if (key == null) throw new IllegalStateException("no key");
            cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        } catch (KeyPermanentlyInvalidatedException e) {
            disable(a);
            l.failed("Biometrics changed on this device. Log in with your password.");
            return;
        } catch (Exception e) {
            disable(a);
            l.failed("Log in with your password");
            return;
        }
        prompt(a, strong ? STRONG : level, "Log in", enabledEmail(a), "Use password", strong ? cipher : null, new Done() {
            @Override
            public void ok(Cipher authed) {
                try {
                    l.success(new String((authed != null ? authed : cipher).doFinal(blob), "UTF-8"));
                } catch (Exception e) {
                    disable(a);
                    l.failed("Log in with your password");
                }
            }

            @Override
            public void cancel() { l.cancelled(); }
        });
    }

    private interface Done {
        void ok(Cipher authed);

        void cancel();
    }

    private static void prompt(Activity a, int level, String title, String subtitle, String negative, Cipher cipher,
                               final Done once) {
        final Done done = new Done() {
            boolean fired;

            @Override
            public void ok(Cipher authed) {
                if (fired) return;
                fired = true;
                once.ok(authed);
            }

            @Override
            public void cancel() {
                if (fired) return;
                fired = true;
                once.cancel();
            }
        };
        if (Build.VERSION.SDK_INT < 28) {
            done.cancel();
            return;
        }
        final CancellationSignal cancel = new CancellationSignal();
        BiometricPrompt.Builder b = new BiometricPrompt.Builder(a).setTitle(title).setSubtitle(subtitle)
                .setNegativeButton(negative, a.getMainExecutor(), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) { done.cancel(); }
                });
        if (Build.VERSION.SDK_INT >= 29) b.setConfirmationRequired(false);
        if (Build.VERSION.SDK_INT >= 30) {
            b.setAllowedAuthenticators(level == STRONG ? BiometricManager.Authenticators.BIOMETRIC_STRONG
                    : BiometricManager.Authenticators.BIOMETRIC_WEAK);
        }
        BiometricPrompt p = b.build();
        BiometricPrompt.AuthenticationCallback cb = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) {
                BiometricPrompt.CryptoObject co = r.getCryptoObject();
                done.ok(co != null ? co.getCipher() : null);
            }

            @Override
            public void onAuthenticationError(int code, CharSequence msg) { done.cancel(); }
        };
        if (cipher != null) p.authenticate(new BiometricPrompt.CryptoObject(cipher), cancel, a.getMainExecutor(), cb);
        else p.authenticate(cancel, a.getMainExecutor(), cb);
    }

    private static SecretKey createKey(boolean strong) throws Exception {
        KeyGenParameterSpec.Builder spec = new KeyGenParameterSpec.Builder(strong ? KEY_STRONG : KEY_WEAK,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256);
        if (strong) {
            spec.setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(true);
            if (Build.VERSION.SDK_INT >= 30) spec.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG);
        }
        KeyGenerator g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        g.init(spec.build());
        return g.generateKey();
    }
}
