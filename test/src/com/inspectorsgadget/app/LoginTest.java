package com.inspectorsgadget.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w393dp-h852dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LoginTest {

    @Before
    public void locked() {
        Auth.setUnlockedForTest(false);
        Auth.setProvider(null);
    }

    private static Context ctx() { return RuntimeEnvironment.getApplication(); }

    private static void collect(View v, Class<?> cls, List<View> out) {
        if (cls.isInstance(v)) out.add(v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) collect(g.getChildAt(i), cls, out);
        }
    }

    private static List<EditText> fields(Activity a) {
        List<View> vs = new ArrayList<>();
        collect(a.getWindow().getDecorView(), EditText.class, vs);
        List<EditText> out = new ArrayList<>();
        for (View v : vs) out.add((EditText) v);
        return out;
    }

    private static View text(Activity a, String s) {
        List<View> vs = new ArrayList<>();
        collect(a.getWindow().getDecorView(), TextView.class, vs);
        for (View v : vs) if (((TextView) v).getText().toString().equals(s)) return v;
        return null;
    }

    private static void waitFor(java.util.concurrent.Callable<Boolean> cond) throws Exception {
        for (int i = 0; i < 400 && !cond.call(); i++) {
            Thread.sleep(25);
            ShadowLooper.idleMainLooper();
        }
        assertTrue("condition", cond.call());
    }

    private static void shot(Activity a, String name) throws Exception {
        ShadowLooper.idleMainLooper();
        View decor = a.getWindow().getDecorView();
        int w = a.getResources().getDisplayMetrics().widthPixels;
        int h = a.getResources().getDisplayMetrics().heightPixels;
        decor.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, w, h);
        android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
        decor.draw(new android.graphics.Canvas(b));
        File out = new File(System.getProperty("pdfOut", "out"), name);
        out.getParentFile().mkdirs();
        java.io.FileOutputStream os = new java.io.FileOutputStream(out);
        b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, os);
        os.close();
    }

    @Test
    public void localProviderHashesAndRotatesTokens() {
        Auth.Provider p = new Auth.LocalProvider(ctx());
        Auth.Result r = p.register("Justin Huffman", "Justin@Example.com ", "correct horse");
        assertTrue(r.ok);
        assertEquals("justin@example.com", r.email);
        assertFalse(p.register("x", "justin@example.com", "another one").ok);
        String stored = ctx().getSharedPreferences("auth_users", Context.MODE_PRIVATE).getString("justin@example.com", "");
        assertFalse("password not stored", stored.contains("correct horse"));
        assertFalse("token not stored", stored.contains(r.refreshToken));

        assertFalse(p.signIn("justin@example.com", "wrong password").ok);
        Auth.Result s = p.signIn("JUSTIN@example.com", "correct horse");
        assertTrue(s.ok);
        assertEquals("Justin Huffman", s.name);
        // An earlier token (for example one held behind biometrics) stays valid after a password sign-in.
        assertTrue(p.refresh("justin@example.com", r.refreshToken).ok);
        assertTrue(p.refresh("justin@example.com", s.refreshToken).ok);
        assertFalse(p.refresh("justin@example.com", "forged").ok);

        Auth.Result reset = p.resetPassword("justin@example.com", "new password 1");
        assertTrue(reset.ok);
        assertFalse("reset revokes old tokens", p.refresh("justin@example.com", r.refreshToken).ok);
        assertFalse(p.signIn("justin@example.com", "correct horse").ok);
        assertTrue(p.signIn("justin@example.com", "new password 1").ok);
    }

    @Test
    public void gateSendsScreensToLoginUntilSignedIn() {
        Activity main = Robolectric.buildActivity(MainActivity.class, new Intent()).setup().get();
        assertTrue(main.isFinishing());
        Intent next = Shadows.shadowOf(main).getNextStartedActivity();
        assertEquals(LoginActivity.class.getName(), next.getComponent().getClassName());
    }

    @Test
    public void createAccountLogOutAndLogBackIn() throws Exception {
        final Activity login = Robolectric.buildActivity(LoginActivity.class, new Intent()).setup().get();
        shot(login, "screen_23_login.png");
        assertNotNull(text(login, "Forgot Password?"));

        text(login, "Create Account").performClick();
        List<EditText> f = fields(login);
        assertEquals(4, f.size());
        f.get(0).setText("Justin Huffman");
        f.get(1).setText("justin@360homeinspections.com");
        f.get(2).setText("inspect123");
        f.get(3).setText("inspect12");
        shot(login, "screen_24_create_account.png");
        List<View> buttons = new ArrayList<>();
        collect(login.getWindow().getDecorView(), android.widget.Button.class, buttons);
        View create = null;
        for (View b : buttons) if (((TextView) b).getText().toString().equals("Create Account")) create = b;
        create.performClick();
        assertNotNull(text(login, "Passwords do not match"));
        f.get(3).setText("inspect123");
        create.performClick();
        waitFor(() -> Auth.isUnlocked());
        assertEquals("justin@360homeinspections.com", Auth.email(ctx()));
        assertTrue(Auth.hasSession(ctx()));

        // Stays signed in across a restart without biometrics.
        Auth.setUnlockedForTest(false);
        Auth.restore(ctx());
        assertTrue(Auth.isUnlocked());

        Auth.logout(ctx());
        assertFalse(Auth.hasSession(ctx()));
        Auth.restore(ctx());
        assertFalse(Auth.isUnlocked());

        Intent again = new Intent().putExtra(LoginActivity.EXTRA_LOGGED_OUT, true);
        final Activity back = Robolectric.buildActivity(LoginActivity.class, again).setup().get();
        List<EditText> g = fields(back);
        assertEquals(2, g.size());
        assertEquals(com.inspectorsgadget.app.Config.DEMO_ADMIN ? com.inspectorsgadget.app.Config.DEMO_ADMIN_EMAIL
                : "justin@360homeinspections.com", g.get(0).getText().toString());
        g.get(0).setText("justin@360homeinspections.com");
        g.get(1).setText("wrong pass");
        text(back, "Log In").performClick();
        waitFor(() -> text(back, "Incorrect email or password") != null);
        shot(back, "screen_25_login_error.png");
        g.get(1).setText("inspect123");
        text(back, "Log In").performClick();
        waitFor(() -> Auth.isUnlocked());
    }

    @Test
    public void biometricButtonShowsWhenEnabled() throws Exception {
        Auth.prefs(ctx()).edit().putString("bio.blob", "x").putString("bio.iv", "x").putString("bio.mode", "strong")
                .putString("bio.email", "justin@360homeinspections.com").putString("last.email", "justin@360homeinspections.com")
                .apply();
        android.hardware.biometrics.BiometricManager bm = ctx().getSystemService(android.hardware.biometrics.BiometricManager.class);
        org.robolectric.shadows.ShadowBiometricManager sbm = org.robolectric.shadow.api.Shadow.extract(bm);
        sbm.setCanAuthenticate(true);
        sbm.setAuthenticatorType(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG);
        assertEquals(Biometrics.STRONG, Biometrics.available(ctx()));
        Intent i = new Intent().putExtra(LoginActivity.EXTRA_LOGGED_OUT, true);
        Activity login = Robolectric.buildActivity(LoginActivity.class, i).setup().get();
        List<View> imgs = new ArrayList<>();
        collect(login.getWindow().getDecorView(), android.widget.ImageView.class, imgs);
        boolean found = false;
        for (View v : imgs) if ("Biometric login".contentEquals(v.getContentDescription() == null ? "" : v.getContentDescription())) found = true;
        assertTrue("fingerprint button", found);
        shot(login, "screen_26_login_biometric.png");
    }

    @Test
    public void signedInInspectorFollowsLoginEmailThenChoice() {
        Inspection first = Company.inspectors(ctx()).get(0);
        Inspection second = Company.addInspector(ctx());
        second.setUser("ip.name", "Pat Owner");
        second.setUser("ip.email", "Owner@Example.com");
        Store.save(second);
        assertEquals(first.id, Company.signedInInspector(ctx()).id);
        Auth.prefs(ctx()).edit().putString("session.email", "owner@example.com").apply();
        assertEquals(second.id, Company.signedInInspector(ctx()).id);
        Company.setSignedInInspector(ctx(), first.id);
        assertEquals(first.id, Company.signedInInspector(ctx()).id);
    }

    @Test
    public void demoAdminSignsInOnFreshInstall() {
        Auth.Provider p = Auth.provider(ctx());
        Auth.Result r = p.signIn(com.inspectorsgadget.app.Config.DEMO_ADMIN_EMAIL, com.inspectorsgadget.app.Config.DEMO_ADMIN_PASSWORD);
        assertTrue(r.error, r.ok);
        assertEquals(com.inspectorsgadget.app.Config.DEMO_ADMIN_NAME, r.name);
        assertTrue(p.exists("ADMIN@ig.app "));
        assertFalse(p.signIn(com.inspectorsgadget.app.Config.DEMO_ADMIN_EMAIL, "wrongpass").ok);
        assertFalse(p.register("X", com.inspectorsgadget.app.Config.DEMO_ADMIN_EMAIL, "something1").ok);
    }

    @Test
    public void demoAdminPrefilledSoLogInIsOneTap() throws Exception {
        final Activity a = Robolectric.buildActivity(LoginActivity.class).setup().get();
        List<EditText> f = fields(a);
        assertEquals(com.inspectorsgadget.app.Config.DEMO_ADMIN_EMAIL, f.get(0).getText().toString());
        assertEquals(com.inspectorsgadget.app.Config.DEMO_ADMIN_PASSWORD, f.get(1).getText().toString());
        shot(a, "screen_28_login_demo_admin.png");
        text(a, "Log In").performClick();
        waitFor(() -> Auth.isUnlocked());
        assertEquals(com.inspectorsgadget.app.Config.DEMO_ADMIN_EMAIL, Auth.email(ctx()));
    }
}
