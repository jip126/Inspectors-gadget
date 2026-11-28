package com.inspectorsgadget.app;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
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
public class UpdaterTest {

    @Before
    public void signedIn() { Auth.setUnlockedForTest(true); }

    private static final String RELEASE = "{\"tag_name\":\"v0.4\",\"body\":\"Settings screen\\nUpdate checks\","
            + "\"assets\":[{\"name\":\"notes.txt\",\"browser_download_url\":\"https://x/notes.txt\",\"size\":10},"
            + "{\"name\":\"InspectorsGadget-0.4-4.apk\",\"browser_download_url\":\"https://x/InspectorsGadget-0.4-4.apk\",\"size\":731000}]}";

    @Test
    public void parsesReleaseAndComparesVersionCode() throws Exception {
        Updater.Release r = Updater.parse(RELEASE);
        assertNotNull(r);
        assertEquals("0.4", r.versionName);
        assertEquals(4, r.versionCode);
        assertEquals("https://x/InspectorsGadget-0.4-4.apk", r.apkUrl);
        assertEquals(731000, r.apkSize);
        assertEquals("Settings screen\nUpdate checks", r.notes);
        assertTrue(Updater.isNewer(r, 3, "0.3"));
        assertFalse(Updater.isNewer(r, 4, "0.4"));
        assertFalse(Updater.isNewer(r, 5, "0.5"));
        assertFalse(Updater.isNewer(null, 1, "0.1"));
    }

    @Test
    public void fallsBackToVersionNameWithoutCodeInAssetName() throws Exception {
        Updater.Release r = Updater.parse("{\"tag_name\":\"v0.10\",\"assets\":[{\"name\":\"app.apk\","
                + "\"browser_download_url\":\"https://x/app.apk\"}]}");
        assertEquals(-1, r.versionCode);
        assertTrue(Updater.isNewer(r, 99, "0.9"));
        assertFalse(Updater.isNewer(r, 1, "0.10"));
        assertEquals(0, Updater.compareNames("0.2-demo", "0.2"));
        assertTrue(Updater.compareNames("1.0", "0.9.9") > 0);
        assertNull(Updater.parse("{\"tag_name\":\"v1\",\"assets\":[]}"));
    }

    @Test
    public void settingsShowsVersionAndCheckForUpdates() throws Exception {
        Activity a = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        assertNotNull(find(a.getWindow().getDecorView(), "Check for updates"));
        assertNotNull(find(a.getWindow().getDecorView(), "Version"));
        shot(a, "screen_27_settings.png");
    }

    @Test
    public void homeGearOpensSettings() {
        Activity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        View gear = findDesc(a.getWindow().getDecorView(), "Settings");
        try { shot(a, "screen_28_home_settings.png"); } catch (Exception e) { throw new RuntimeException(e); }
        assertNotNull(gear);
        gear.performClick();
        assertEquals(SettingsActivity.class.getName(),
                Shadows.shadowOf(a).getNextStartedActivity().getComponent().getClassName());
    }

    @Test
    public void downloadedApkIsServedToTheInstaller() {
        File f = Updater.apkFile(RuntimeEnvironment.getApplication());
        android.net.Uri u = FileShareProvider.uriFor(RuntimeEnvironment.getApplication(), f);
        assertEquals("content://com.inspectorsgadget.app.files/cache/updates/update.apk", u.toString());
        assertEquals("application/vnd.android.package-archive", new FileShareProvider().getType(u));
    }

    private static View find(View v, String text) {
        if (v instanceof TextView && ((TextView) v).getText().toString().equals(text)) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View r = find(g.getChildAt(i), text);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static View findDesc(View v, String desc) {
        if (desc.contentEquals(v.getContentDescription() == null ? "" : v.getContentDescription())) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View r = findDesc(g.getChildAt(i), desc);
                if (r != null) return r;
            }
        }
        return null;
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
}
