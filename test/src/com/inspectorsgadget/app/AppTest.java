package com.inspectorsgadget.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w400dp-h860dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AppTest {

    @org.junit.Before
    public void signedIn() { Auth.setUnlockedForTest(true); }

    private static List<String> texts(View v) {
        List<String> out = new ArrayList<>();
        collect(v, out);
        return out;
    }

    private static void collect(View v, List<String> out) {
        if (v instanceof TextView) out.add(((TextView) v).getText().toString());
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) collect(g.getChildAt(i), out);
        }
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

    private Activity start(Class<? extends Activity> cls, Intent i) {
        ActivityController<? extends Activity> c = Robolectric.buildActivity(cls, i).setup();
        ShadowLooper.idleMainLooper();
        return c.get();
    }

    @Test
    public void demoFlowPrefillsReportsAndScreens() throws Exception {
        Activity main = start(MainActivity.class, new Intent());
        View demo = findText(main.getWindow().getDecorView(), "Load demo");
        assertNotNull("Load demo button", demo);
        demo.performClick();
        ShadowLooper.idleMainLooper();
        Intent next = Shadows.shadowOf(main).getNextStartedActivity();
        assertEquals(InspectionActivity.class.getName(), next.getComponent().getClassName());
        String id = next.getStringExtra(InspectionActivity.EXTRA_INSPECTION);
        Inspection in = Store.get(main, id);
        assertNotNull(in);

        // ---- 4-Point prefill
        assertEquals("150", in.str("fp.elec.mainAmps"));
        assertEquals("Circuit breaker", in.str("fp.elec.mainType"));
        assertTrue(in.list("fp.elec.hazards").contains("Double taps"));
        assertEquals("Unsatisfactory", in.str("fp.elec.cond"));
        assertEquals("[\"Copper\",\"NM, BX or conduit\"]", in.raw("fp.elec.wiring").toString());
        assertTrue(in.list("fp.elec.presence").isEmpty());
        assertEquals("Yes", in.str("fp.plumb.tpr"));
        assertEquals("Unsatisfactory", in.str("fp.plumb.fx.sinks"));
        assertEquals("12", in.str("fp.roof.age"));
        assertEquals("Garage", in.str("fp.plumb.whLocation"));
        assertEquals("Justin Huffman", in.str("fp.cert.inspector"));
        assertEquals("HI-12683", in.str("fp.cert.license"));
        assertEquals("Florida DBPR", in.str("fp.cert.issuer"));

        // ---- company profile, roster and agreement
        assertEquals("360 Home Inspections LLC", in.str("info.company"));
        assertEquals("HI-12683", in.str("info.license"));
        assertEquals("Florida DBPR", in.str("info.issuer"));
        assertEquals("JH", in.str("info.initials"));
        assertEquals(Agreement.SIGNED, Agreement.status(in));
        assertEquals("425.00", in.str("pa.feeTotal"));
        assertTrue(in.list("pa.attend").contains("Real estate agent"));
        // Intake in the agreement step shares Property & Client Details fields
        Forms.Section intake = Forms.section("pa.intake");
        assertNotNull(intake);
        assertEquals(Forms.AGREEMENT, intake.formId);
        assertEquals("Citizens Property Insurance", in.str("info.insurer"));
        assertEquals(String.valueOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - 1998), in.str("pa.age")); // from year built
        List<Inspection> roster = Company.inspectors(main);
        assertEquals(1, roster.size());
        assertTrue(in.isPrefilled("fp.elec.mainAmps"));

        // ---- Wind mitigation suggestions
        assertTrue(in.str("wm.building.answer"), in.str("wm.building.answer").startsWith("C"));
        assertTrue(in.str("wm.cover.answer").startsWith("A"));
        assertTrue(in.str("wm.deck.answer"), in.str("wm.deck.answer").startsWith("B"));
        assertTrue(in.str("wm.r2w.answer").startsWith("B - Clips"));
        assertTrue(in.str("wm.geom.answer").startsWith("A - Hip"));
        assertTrue(in.str("wm.swr.answer").startsWith("B"));
        assertTrue(in.str("wm.open.answer"), in.str("wm.open.answer").startsWith("B.2"));
        assertTrue(in.str("wm.cover.answer"), in.str("wm.cover.answer").startsWith("A"));
        assertEquals("03/18/2014", in.str("wm.cover.t1.permit"));
        assertEquals("Jordan & Casey Rivera", in.str("wm.owner.name"));
        assertEquals(java.util.Arrays.asList(Forms.OPENING[2]), in.list("wm.open.windows"));
        assertEquals("1998", in.str("wm.building.year"));

        // ---- user edit overrides prefill and is not overwritten
        in.setUser("fp.elec.mainAmps", "200");
        Logic.prefillForm(in, Forms.FOUR_POINT);
        assertEquals("200", in.str("fp.elec.mainAmps"));
        assertFalse(in.isPrefilled("fp.elec.mainAmps"));
        // changes in general flow through to untouched prefilled fields
        in.setUser("g.elec.mainBrand", "Eaton BR");
        Logic.prefillForm(in, Forms.FOUR_POINT);
        assertEquals("Eaton BR", in.str("fp.elec.mainBrand"));

        // ---- photo reuse
        List<String> importable = Logic.importablePhotos(in, Forms.section("fp.elec"));
        assertEquals(2, importable.size());
        assertEquals(9, Logic.photoCount(in, Forms.GENERAL));
        assertEquals(1, in.sectionPhotos("wm.open").size());
        System.out.println("General findings: " + Logic.findings(in, Forms.GENERAL).size());

        // ---- a second employee: their details replace the first one's everywhere they were prefilled
        Inspection emp = Company.addInspector(main);
        emp.setUser("ip.name", "Riley Chen");
        emp.setUser("ip.license", "HI-99881");
        emp.setUser("ip.expires", "07/31/2027");
        Activity esig = start(AnnotateActivity.class, new Intent().putExtra(AnnotateActivity.EXTRA_INSPECTION, emp.id)
                .putExtra(AnnotateActivity.EXTRA_SIGNATURE, "ip.signature"));
        DrawingView ev = findDrawing(esig.getWindow().getDecorView());
        ev.layout(0, 0, 1000, 400);
        long t0 = SystemClock.uptimeMillis();
        ev.dispatchTouchEvent(MotionEvent.obtain(t0, t0, MotionEvent.ACTION_DOWN, 100, 250, 0));
        ev.dispatchTouchEvent(MotionEvent.obtain(t0, t0 + 10, MotionEvent.ACTION_MOVE, 350, 120, 0));
        ev.dispatchTouchEvent(MotionEvent.obtain(t0, t0 + 20, MotionEvent.ACTION_UP, 700, 260, 0));
        findText(esig.getWindow().getDecorView(), "Save signature").performClick();
        assertTrue(emp.str("ip.signature").endsWith(".png"));
        Store.save(emp);
        assertEquals(2, Company.inspectors(main).size());
        Company.applyInspector(main, in, emp.id);
        Logic.prefillForm(in, Forms.FOUR_POINT);
        Logic.prefillForm(in, Forms.WIND);
        assertEquals("Riley Chen", in.str("info.inspector"));
        assertEquals("Riley Chen", in.str("fp.cert.inspector"));
        assertEquals("HI-99881", in.str("fp.cert.license"));
        assertEquals("RC", in.str("info.initials"));
        assertTrue(in.str("info.inspectorSig").startsWith("sig_inspector_"));
        assertTrue(new File(new File(in.dir, "signatures"), in.str("info.inspectorSig")).exists());
        assertEquals(in.str("info.inspectorSig"), in.str("fp.cert.signature"));
        // switch back to the owner for the rest of the demo
        Company.applyInspector(main, in, roster.get(0).id);
        Logic.prefillForm(in, Forms.FOUR_POINT);
        assertEquals("Justin Huffman", in.str("fp.cert.inspector"));
        assertFalse(in.has("fp.cert.signature"));

        // ---- unsigned agreement gates the General inspection
        Inspection fresh = Store.create(main);
        fresh.setUser("info.address", "12 Test St");
        Company.applyDefault(main, fresh);
        assertEquals(Agreement.NOT_SENT, Agreement.status(fresh));
        Activity hub = start(InspectionActivity.class, new Intent().putExtra(InspectionActivity.EXTRA_INSPECTION, fresh.id));
        View gv = findText(hub.getWindow().getDecorView(), "General Home Inspection");
        while (gv != null && !gv.hasOnClickListeners()) gv = (View) gv.getParent();
        assertNotNull(gv);
        gv.performClick();
        android.app.AlertDialog gateDlg = (android.app.AlertDialog) org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull("agreement gate", gateDlg);
        assertTrue(gateDlg.isShowing());
        gateDlg.dismiss();
        fresh.setUser("pa.signed", "Signed");
        assertEquals(Agreement.SIGNED, Agreement.status(fresh));
        Store.delete(main, fresh.id);

        // ---- photos and comments on individual General items
        assertEquals(1, in.sectionPhotos("g.ext.siding").size());
        assertEquals(0, in.sectionPhotos("g.ext").size());
        Activity ext = start(SectionActivity.class, new Intent().putExtra(SectionActivity.EXTRA_INSPECTION, id)
                .putExtra(SectionActivity.EXTRA_SECTION, "g.ext"));
        View photoPill = findText(ext.getWindow().getDecorView(), "📷  Photo"); // first item = Wall cladding
        assertNotNull("item photo button", photoPill);
        photoPill.performClick();
        org.robolectric.shadows.ShadowActivity.IntentForResult camReq = Shadows.shadowOf(ext).getNextStartedActivityForResult();
        assertEquals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE, camReq.intent.getAction());
        assertEquals(0, camReq.intent.getIntExtra("android.intent.extras.CAMERA_FACING", -1)); // rear camera
        assertNull("no phone gallery in General", findText(ext.getWindow().getDecorView(), "🖼"));
        String newPid = null;
        for (java.util.Iterator<String> it = in.photoMeta().keys(); it.hasNext(); ) {
            String p = it.next();
            if (!in.photoExists(p)) newPid = p;
        }
        assertNotNull(newPid);
        Images.writeJpeg(Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888), in.originalFile(newPid), 80);
        Shadows.shadowOf(ext).receiveResult(camReq.intent, Activity.RESULT_OK, null);
        assertEquals(2, in.sectionPhotos("g.ext.siding").size());
        assertEquals("Wall cladding", in.caption(newPid));
        View micPill = findText(ext.getWindow().getDecorView(), "🎤  Comment");
        micPill.performClick();
        org.robolectric.shadows.ShadowActivity.IntentForResult voiceReq = Shadows.shadowOf(ext).getNextStartedActivityForResult();
        while (voiceReq != null && !android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH.equals(voiceReq.intent.getAction()))
            voiceReq = Shadows.shadowOf(ext).getNextStartedActivityForResult();
        assertNotNull("dictation started", voiceReq);
        java.util.ArrayList<String> heard = new java.util.ArrayList<>();
        heard.add("efflorescence at the base of the front wall");
        Shadows.shadowOf(ext).receiveResult(voiceReq.intent, Activity.RESULT_OK,
                new Intent().putStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS, heard));
        assertEquals("Efflorescence at the base of the front wall.", in.str(Forms.commentKey("g.ext.siding")));
        assertEquals(10, Logic.photoCount(in, Forms.GENERAL));
        in.deletePhotoEverywhere(newPid); // blank test image; keep the demo previews clean
        assertEquals(9, Logic.photoCount(in, Forms.GENERAL));

        // ---- every screen renders
        start(InspectionActivity.class, new Intent().putExtra(InspectionActivity.EXTRA_INSPECTION, id));
        for (Forms.Form f : Forms.all().values()) {
            start(FormActivity.class, new Intent().putExtra(FormActivity.EXTRA_INSPECTION, id)
                    .putExtra(FormActivity.EXTRA_FORM, f.id));
            for (Forms.Section s : f.sections) {
                Activity a = start(SectionActivity.class, new Intent().putExtra(SectionActivity.EXTRA_INSPECTION, id)
                        .putExtra(SectionActivity.EXTRA_SECTION, s.key));
                assertFalse(s.key, a.isFinishing());
            }
        }

        // 4-Point and Wind pick only from this inspection's photos
        Activity fpRoof = start(SectionActivity.class, new Intent().putExtra(SectionActivity.EXTRA_INSPECTION, id)
                .putExtra(SectionActivity.EXTRA_SECTION, "fp.roof"));
        assertNull(findText(fpRoof.getWindow().getDecorView(), "🖼"));
        findText(fpRoof.getWindow().getDecorView(), "♻  Inspection photos").performClick();
        android.app.AlertDialog picker = (android.app.AlertDialog) org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(picker.isShowing());
        View pickerView = picker.getWindow().getDecorView();
        assertNotNull("photo labeled with its item", findText(pickerView, "Roof › Signs of damage"));
        assertNotNull(findText(pickerView, "Attic & Roof Structure › Roof-to-wall attachment"));
        // render the picker's photo list for the screenshots folder
        View list = findText(pickerView, "Roof › Signs of damage");
        while (!(list.getParent() instanceof android.widget.ScrollView)) list = (View) list.getParent();
        int pw = main.getResources().getDisplayMetrics().widthPixels;
        ShadowLooper.idleMainLooper();
        list.measure(View.MeasureSpec.makeMeasureSpec(pw, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(8000, View.MeasureSpec.AT_MOST));
        list.layout(0, 0, list.getMeasuredWidth(), list.getMeasuredHeight());
        Bitmap pb = Bitmap.createBitmap(pw, Math.min(list.getMeasuredHeight(), 8000), Bitmap.Config.ARGB_8888);
        pb.eraseColor(0xFFFFFFFF);
        list.draw(new android.graphics.Canvas(pb));
        png(pb, new File(System.getProperty("pdfOut", "/tmp"), "screen_12_inspection_photo_picker.png"));
        picker.dismiss();

        // Import all in 4-Point electrical
        Activity sec = start(SectionActivity.class, new Intent().putExtra(SectionActivity.EXTRA_INSPECTION, id)
                .putExtra(SectionActivity.EXTRA_SECTION, "fp.elec"));
        View imp = findText(sec.getWindow().getDecorView(), "Import all");
        assertNotNull("import banner", imp);
        imp.performClick();
        assertEquals(2, in.sectionPhotos("fp.elec").size());

        // Chip click changes a value
        Activity gen = start(SectionActivity.class, new Intent().putExtra(SectionActivity.EXTRA_INSPECTION, id)
                .putExtra(SectionActivity.EXTRA_SECTION, "g.roof"));
        View metal = findText(gen.getWindow().getDecorView(), "Metal");
        metal.performClick();
        assertEquals("Metal", in.str("g.roof.covering"));
        Logic.prefillForm(in, Forms.WIND);
        assertTrue(in.list("wm.cover.types").contains("Metal"));

        // ---- markup a photo
        String pid = in.sectionPhotos("g.ext.siding").get(0);
        Activity ann = start(AnnotateActivity.class, new Intent().putExtra(AnnotateActivity.EXTRA_INSPECTION, id)
                .putExtra(AnnotateActivity.EXTRA_PHOTO, pid));
        DrawingView dv = findDrawing(ann.getWindow().getDecorView());
        assertNotNull(dv);
        dv.layout(0, 0, 1000, 700);
        long t = SystemClock.uptimeMillis();
        dv.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, 300, 300, 0));
        dv.dispatchTouchEvent(MotionEvent.obtain(t, t + 10, MotionEvent.ACTION_MOVE, 400, 350, 0));
        dv.dispatchTouchEvent(MotionEvent.obtain(t, t + 20, MotionEvent.ACTION_UP, 500, 380, 0));
        assertFalse(dv.isEmpty());
        findText(ann.getWindow().getDecorView(), "Save").performClick();
        String annotated = in.meta(pid).optString("annotated");
        assertTrue(annotated.length() > 0);
        assertTrue(new File(in.photoDir(), annotated).length() > 1000);

        // ---- signature
        Activity sig = start(AnnotateActivity.class, new Intent().putExtra(AnnotateActivity.EXTRA_INSPECTION, id)
                .putExtra(AnnotateActivity.EXTRA_SIGNATURE, "fp.cert.signature"));
        DrawingView sv = findDrawing(sig.getWindow().getDecorView());
        sv.layout(0, 0, 1000, 400);
        sv.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, 100, 200, 0));
        sv.dispatchTouchEvent(MotionEvent.obtain(t, t + 10, MotionEvent.ACTION_MOVE, 300, 100, 0));
        sv.dispatchTouchEvent(MotionEvent.obtain(t, t + 20, MotionEvent.ACTION_UP, 600, 250, 0));
        findText(sig.getWindow().getDecorView(), "Save signature").performClick();
        assertTrue(in.str("fp.cert.signature").endsWith(".png"));

        // ---- Report pages (PdfDocument is native-only, so render the same layout to bitmaps)
        File outDir = new File(System.getProperty("pdfOut", "/tmp"));
        for (String f : new String[]{Forms.GENERAL, Forms.FOUR_POINT, Forms.WIND}) {
            List<Bitmap> pages = ReportGenerator.preview(in, f);
            System.out.println(f + " report pages: " + pages.size());
            assertTrue(pages.size() >= 1);
            for (int i = 0; i < pages.size(); i++) png(pages.get(i), new File(outDir, "report_" + f + "_" + (i + 1) + ".png"));
        }

        List<Bitmap> ag = Agreement.preview(main, in);
        System.out.println("agreement pages: " + ag.size());
        assertEquals(2, ag.size());
        for (int i = 0; i < ag.size(); i++) png(ag.get(i), new File(outDir, "agreement_" + (i + 1) + ".png"));

        // ---- Screenshots of key screens
        shot(start(MainActivity.class, new Intent()), new File(outDir, "screen_1_home.png"), false);
        shot(start(InspectionActivity.class, new Intent().putExtra(InspectionActivity.EXTRA_INSPECTION, id)),
                new File(outDir, "screen_2_hub.png"), true);
        shot(start(FormActivity.class, new Intent().putExtra(FormActivity.EXTRA_INSPECTION, id)
                .putExtra(FormActivity.EXTRA_FORM, Forms.GENERAL)), new File(outDir, "screen_3_general.png"), true);
        shot(start(SectionActivity.class, new Intent().putExtra(SectionActivity.EXTRA_INSPECTION, id)
                .putExtra(SectionActivity.EXTRA_SECTION, "g.elec")), new File(outDir, "screen_4_general_electrical.png"), true);
        shot(start(SectionActivity.class, new Intent().putExtra(SectionActivity.EXTRA_INSPECTION, id)
                .putExtra(SectionActivity.EXTRA_SECTION, "fp.roof")), new File(outDir, "screen_5_4pt_roof.png"), true);
        shot(start(SectionActivity.class, new Intent().putExtra(SectionActivity.EXTRA_INSPECTION, id)
                .putExtra(SectionActivity.EXTRA_SECTION, "wm.deck")), new File(outDir, "screen_6_wind_deck.png"), true);
        shot(start(FormActivity.class, new Intent().putExtra(FormActivity.EXTRA_INSPECTION, id)
                .putExtra(FormActivity.EXTRA_FORM, Forms.AGREEMENT)), new File(outDir, "screen_8_agreement.png"), true);
        shot(start(SectionActivity.class, new Intent().putExtra(SectionActivity.EXTRA_INSPECTION, id)
                .putExtra(SectionActivity.EXTRA_SECTION, "pa.details")), new File(outDir, "screen_9_agreement_details.png"), true);
        shot(start(CompanyActivity.class, new Intent()), new File(outDir, "screen_10_company.png"), true);
        shot(start(SectionActivity.class, new Intent().putExtra(SectionActivity.EXTRA_INSPECTION, roster.get(0).id)
                .putExtra(SectionActivity.EXTRA_SECTION, "ip")), new File(outDir, "screen_11_inspector.png"), true);
        String elecPhoto = in.sectionPhotos("g.elec.hazards").get(0);
        shot(start(AnnotateActivity.class, new Intent().putExtra(AnnotateActivity.EXTRA_INSPECTION, id)
                .putExtra(AnnotateActivity.EXTRA_PHOTO, elecPhoto)), new File(outDir, "screen_7_markup.png"), false);
        Store.save(in);
    }

    private static void png(Bitmap b, File f) throws Exception {
        java.io.FileOutputStream os = new java.io.FileOutputStream(f);
        b.compress(Bitmap.CompressFormat.PNG, 100, os);
        os.close();
    }

    /** Renders an activity. full = whole scrollable content, otherwise the visible window. */
    private static void shot(Activity a, File f, boolean full) throws Exception {
        ShadowLooper.idleMainLooper();
        Thread.sleep(300);
        ShadowLooper.idleMainLooper();
        View decor = a.getWindow().getDecorView();
        int w = a.getResources().getDisplayMetrics().widthPixels;
        int h = a.getResources().getDisplayMetrics().heightPixels;
        View target = decor;
        if (full) {
            ViewGroup content = a.findViewById(android.R.id.content);
            View scroll = content.getChildAt(0);
            target = ((ViewGroup) scroll).getChildAt(0);
            target.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            target.layout(0, 0, target.getMeasuredWidth(), target.getMeasuredHeight());
            h = Math.min(target.getMeasuredHeight(), 12000);
        } else {
            decor.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
            decor.layout(0, 0, w, h);
        }
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        b.eraseColor(0xFFF3F5F8);
        android.graphics.Canvas c = new android.graphics.Canvas(b);
        target.draw(c);
        png(b, f);
    }

    private static DrawingView findDrawing(View v) {
        if (v instanceof DrawingView) return (DrawingView) v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                DrawingView r = findDrawing(g.getChildAt(i));
                if (r != null) return r;
            }
        }
        return null;
    }

    @Test
    public void newInspectionStartsEmptyAndSectionsRender() {
        Inspection in = Store.create(RuntimeEnvironment.getApplication());
        for (Forms.Form f : Forms.all().values()) {
            for (Forms.Section s : f.sections) {
                start(SectionActivity.class, new Intent().putExtra(SectionActivity.EXTRA_INSPECTION, in.id)
                        .putExtra(SectionActivity.EXTRA_SECTION, s.key));
            }
        }
        assertEquals(0, Logic.prefilledCount(in, Forms.FOUR_POINT));
    }
}
