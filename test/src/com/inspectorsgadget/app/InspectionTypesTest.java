package com.inspectorsgadget.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w400dp-h860dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class InspectionTypesTest {

    @org.junit.Before
    public void signedIn() { Auth.setUnlockedForTest(true); }

    private static final File OUT = new File(System.getProperty("pdfOut", "/tmp"));

    private Activity start(Class<? extends Activity> cls, Intent i) {
        Activity a = Robolectric.buildActivity(cls, i).setup().get();
        ShadowLooper.idleMainLooper();
        return a;
    }

    private static View findText(View v, String startsWith) {
        if (v instanceof TextView && ((TextView) v).getText().toString().startsWith(startsWith)) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View r = findText(g.getChildAt(i), startsWith);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static List<String> texts(View v) {
        List<String> out = new ArrayList<>();
        collect(v, out);
        return out;
    }

    private static void collect(View v, List<String> out) {
        if (v instanceof TextView) out.add(((TextView) v).getText().toString());
        if (v instanceof ViewGroup)
            for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) collect(((ViewGroup) v).getChildAt(i), out);
    }

    private static void noDashes(String where, String s) {
        if (s == null) return;
        assertFalse("em dash in " + where + ": " + s, s.contains("—"));
        assertFalse("en dash in " + where + ": " + s, s.contains("–"));
    }

    @Test
    public void everyTypeHasAChecklistWithItemPhotosAndNoDashes() {
        assertEquals(18, Forms.INSPECTION_TYPES.length);
        for (String id : Forms.INSPECTION_TYPES) {
            Forms.Form f = Forms.form(id);
            assertNotNull(id, f);
            noDashes(id, f.title);
            noDashes(id, f.description);
            noDashes(id, f.reportNote);
            noDashes(id, Forms.typeTitle(id));
            if (Forms.FOUR_POINT.equals(id) || Forms.WIND.equals(id)) continue;
            assertTrue(id + " is a checklist", f.checklist);
            int items = 0;
            for (Forms.Section s : f.sections) {
                noDashes(s.key, s.title);
                noDashes(s.key, s.subtitle);
                for (Forms.Field fl : s.fields) {
                    noDashes(fl.key, fl.label);
                    if (fl.options != null) for (String o : fl.options) noDashes(fl.key, o);
                    if (fl.type == Forms.CHOICE || fl.type == Forms.MULTI) assertTrue(fl.key, fl.takesPhotos());
                    if (fl.inputs()) items++;
                    if (fl.flags != null && fl.options != null)
                        for (String flag : fl.flags)
                            assertTrue(fl.key + " flag " + flag, "*".equals(flag) || Arrays.asList(fl.options).contains(flag));
                }
            }
            assertTrue(id + " items " + items, items >= 20);
            System.out.println(id + ": " + f.sections.size() + " sections, " + items + " items");
        }
        // the inspector profile offers every type
        assertEquals(Forms.INSPECTION_TYPES.length, Forms.field("ip.types").options.length);
    }

    @Test
    public void licensedTypesDriveNewInspectionAndHub() throws Exception {
        Activity main = start(MainActivity.class, new Intent());
        Inspection ip = Company.inspectors(main).get(0);
        // seeded inspector: the original three
        assertEquals(Arrays.asList(Forms.DEFAULT_TYPES), Company.licensedTypes(ip));
        Forms.setLicensedTypes(ip, Arrays.asList(Forms.WDO, Forms.MOLD, Forms.ROOF, Forms.GENERAL));
        Store.save(ip);
        assertEquals(Arrays.asList(Forms.GENERAL, Forms.WDO, Forms.MOLD, Forms.ROOF), Company.licensedTypes(ip));

        shot(start(SectionActivity.class, new Intent().putExtra(SectionActivity.EXTRA_INSPECTION, ip.id)
                .putExtra(SectionActivity.EXTRA_SECTION, "ip")), new File(OUT, "screen_17_inspector_types.png"));

        main = start(MainActivity.class, new Intent());
        String tag = String.join("|", texts(main.getWindow().getDecorView()));
        assertTrue(tag, tag.contains("General · WDO · Mold · Roof"));
        findText(main.getWindow().getDecorView(), "+  New inspection").performClick();
        ShadowLooper.idleMainLooper();
        AlertDialog dlg = (AlertDialog) ShadowAlertDialog.getLatestAlertDialog();
        View dv = dlg.getWindow().getDecorView();
        List<String> shown = texts(dv);
        assertTrue(shown.contains("Wood-Destroying Organism (WDO) Inspection"));
        assertTrue(shown.contains("Mold Assessment"));
        assertTrue(shown.contains("Roof Inspection"));
        assertTrue(shown.contains("General Home Inspection"));
        assertFalse(shown.contains("4-Point Inspection"));
        assertFalse(shown.contains("Asbestos Inspection"));
        // General is preselected; add WDO and Mold
        findText(dv, "Wood-Destroying").performClick();
        findText(dv, "Mold Assessment").performClick();
        dialogShot(dlg, new File(OUT, "screen_18_new_inspection.png"));
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();
        Intent next = Shadows.shadowOf(main).getNextStartedActivity();
        Inspection in = Store.get(main, next.getStringExtra(InspectionActivity.EXTRA_INSPECTION));
        assertEquals(Arrays.asList(Forms.GENERAL, Forms.WDO, Forms.MOLD), Forms.types(in));
        // agreement lists the services
        assertTrue(in.list("pa.services").contains(Forms.SERVICES[0]));
        assertTrue(in.list("pa.services").contains(Forms.SERVICES[1]));
        assertTrue(in.str("pa.otherDesc"), in.str("pa.otherDesc").contains("Mold Assessment"));

        Activity hub = start(InspectionActivity.class, next);
        List<String> h = texts(hub.getWindow().getDecorView());
        assertTrue(h.contains("Mold Assessment"));
        assertTrue(h.contains("Wood-Destroying Organism (WDO) Inspection"));
        assertFalse(h.contains("4-Point Inspection"));
        assertFalse(h.contains("Roof Inspection"));
        assertTrue(h.contains("+  Add inspection type"));
        assertTrue(h.contains("📄  Mold Assessment"));

        // unsigned agreement gates every checklist inspection
        View mv = findText(hub.getWindow().getDecorView(), "Mold Assessment");
        while (mv != null && !mv.hasOnClickListeners()) mv = (View) mv.getParent();
        mv.performClick();
        AlertDialog gate = (AlertDialog) ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(gate.isShowing());
        gate.dismiss();

        // add the remaining licensed type from the hub
        findText(hub.getWindow().getDecorView(), "+  Add inspection type").performClick();
        AlertDialog add = (AlertDialog) ShadowAlertDialog.getLatestAlertDialog();
        assertEquals(1, add.getListView().getAdapter().getCount());
        assertEquals("Roof Inspection", add.getListView().getAdapter().getItem(0).toString());
        Shadows.shadowOf(add).clickOnItem(0);
        add.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();
        assertEquals(Arrays.asList(Forms.GENERAL, Forms.WDO, Forms.MOLD, Forms.ROOF), Forms.types(in));

        // item photos and comments are offered on specialty checklists
        Activity sec = start(SectionActivity.class, new Intent().putExtra(SectionActivity.EXTRA_INSPECTION, in.id)
                .putExtra(SectionActivity.EXTRA_SECTION, "mold.moisture"));
        assertNotNull(findText(sec.getWindow().getDecorView(), "📷  Photo"));
        assertNotNull(findText(sec.getWindow().getDecorView(), "🎤  Comment"));
        assertNotNull(findText(sec.getWindow().getDecorView(), "Other photos"));
    }

    @Test
    public void demoAndEveryTypeRenderReports() throws Exception {
        Activity main = start(MainActivity.class, new Intent());
        Inspection demo = SampleData.create(main);
        assertTrue(Forms.types(demo).contains(Forms.WDO));
        // roof checklist prefilled from the general inspection
        assertEquals("Asphalt/fiberglass shingle", demo.str("roof.covering"));
        assertTrue(demo.isPrefilled("roof.covering"));
        assertFalse(Logic.findings(demo, Forms.WDO).isEmpty());

        Activity hub = start(InspectionActivity.class, new Intent().putExtra(InspectionActivity.EXTRA_INSPECTION, demo.id));
        shot(hub, new File(OUT, "screen_19_hub_types.png"));
        shot(start(SectionActivity.class, new Intent().putExtra(SectionActivity.EXTRA_INSPECTION, demo.id)
                .putExtra(SectionActivity.EXTRA_SECTION, "wdo.findings")), new File(OUT, "screen_20_wdo_findings.png"));
        for (String f : new String[]{Forms.WDO, Forms.ROOF}) {
            List<Bitmap> pages = ReportGenerator.preview(demo, f);
            for (int i = 0; i < pages.size(); i++) png(pages.get(i), new File(OUT, "report_" + f + "_" + (i + 1) + ".png"));
        }

        // every specialty form renders when fully answered
        Inspection all = Store.create(main);
        all.setUser("info.address", "1 Test Way");
        Company.applyDefault(main, all);
        for (String id : Forms.INSPECTION_TYPES) {
            Forms.Form f = Forms.form(id);
            if (!f.checklist) continue;
            for (Forms.Section s : f.sections)
                for (Forms.Field fl : s.fields) {
                    if (fl.type == Forms.CHOICE || fl.type == Forms.MULTI) all.setUser(fl.key, fl.options[fl.options.length - 1]);
                    else if (fl.type == Forms.TEXT || fl.type == Forms.NOTES) all.setUser(fl.key, "Sample " + fl.label);
                    else if (fl.type == Forms.NUMBER) all.setUser(fl.key, "12");
                    else if (fl.type == Forms.DATE) all.setUser(fl.key, "11/26/2026");
                }
            List<Bitmap> pages = ReportGenerator.preview(all, id);
            assertTrue(id, pages.size() >= 1);
            System.out.println(id + " report pages: " + pages.size());
        }
    }

    private static void png(Bitmap b, File f) throws Exception {
        java.io.FileOutputStream os = new java.io.FileOutputStream(f);
        b.compress(Bitmap.CompressFormat.PNG, 100, os);
        os.close();
    }

    private static void dialogShot(AlertDialog d, File f) throws Exception {
        ShadowLooper.idleMainLooper();
        View decor = d.getWindow().getDecorView();
        int w = decor.getResources().getDisplayMetrics().widthPixels - 80;
        decor.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        decor.layout(0, 0, decor.getMeasuredWidth(), decor.getMeasuredHeight());
        Bitmap b = Bitmap.createBitmap(decor.getMeasuredWidth(), Math.max(1, decor.getMeasuredHeight()), Bitmap.Config.ARGB_8888);
        b.eraseColor(0xFFFFFFFF);
        decor.draw(new android.graphics.Canvas(b));
        png(b, f);
    }

    /** Whole scrollable content of an activity. */
    private static void shot(Activity a, File f) throws Exception {
        ShadowLooper.idleMainLooper();
        int w = a.getResources().getDisplayMetrics().widthPixels;
        ViewGroup content = a.findViewById(android.R.id.content);
        View target = ((ViewGroup) content.getChildAt(0)).getChildAt(0);
        target.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        target.layout(0, 0, target.getMeasuredWidth(), target.getMeasuredHeight());
        int h = Math.min(target.getMeasuredHeight(), 12000);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        b.eraseColor(0xFFF3F5F8);
        target.draw(new android.graphics.Canvas(b));
        png(b, f);
    }
}
