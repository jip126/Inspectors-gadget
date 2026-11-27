package com.inspectorsgadget.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONObject;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w400dp-h860dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SubscriptionTest {

    @org.junit.Before
    public void signedIn() { Auth.setUnlockedForTest(true); }

    private static Context ctx() { return RuntimeEnvironment.getApplication(); }

    private static Inspection newInspection(Context c) {
        Inspection in = Store.create(c);
        in.setUser("info.address", "12 Oak St");
        in.setUser("info.client", "Pat Client");
        in.setUser("info.email", "pat@example.com");
        Store.save(in);
        Account.onInspectionCreated(c, in);
        return in;
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

    private static File fakePdf(Context c) throws Exception {
        File f = new File(c.getCacheDir(), "agreement.pdf");
        ESign.write(f, "%PDF-1.4 test agreement".getBytes(StandardCharsets.UTF_8));
        return f;
    }

    @Test
    public void oneFreeTrialInspectionThenPaywall() {
        Context c = ctx();
        assertFalse(Account.isSubscribed(c));
        assertTrue(Account.canCreateInspection(c));
        assertEquals("Free trial, 1 inspection", Account.summary(c));

        Inspection trial = newInspection(c);
        assertTrue(Account.isTrial(c, trial));
        assertTrue(Account.canOpen(c, trial));
        assertTrue(Account.canExport(c, trial));
        assertFalse(Account.canCreateInspection(c));
        assertEquals("Free trial used", Account.summary(c));

        // a second inspection (e.g. restored from an older install) stays locked
        Inspection second = newInspection(c);
        assertFalse(Account.canOpen(c, second));
        assertFalse(Account.canExport(c, second));

        // deleting the trial does not give it back
        Store.delete(c, trial.id);
        assertFalse(Account.canCreateInspection(c));

        // demo inspections open but do not export
        Inspection sample = SampleData.create(c);
        assertTrue(Account.isSample(sample));
        assertTrue(Account.canOpen(c, sample));
        assertFalse(Account.canExport(c, sample));

        Account.activate(c, com.inspectorsgadget.app.Config.PLAN_ANNUAL, System.currentTimeMillis() + 86400000L, Account.SOURCE_DEMO, "");
        assertTrue(Account.isSubscribed(c));
        assertTrue(Account.canCreateInspection(c));
        assertTrue(Account.canOpen(c, second));
        assertTrue(Account.canExport(c, sample));
        assertTrue(Account.summary(c).startsWith("Pro, Annual, renews "));

        // an expired subscription locks again
        Account.activate(c, com.inspectorsgadget.app.Config.PLAN_MONTHLY, System.currentTimeMillis() - 1000, Account.SOURCE_DEMO, "");
        assertFalse(Account.isSubscribed(c));
        assertFalse(Account.canOpen(c, second));
    }

    @Test
    public void newInspectionButtonOpensPaywallAfterTrial() {
        newInspection(ctx());
        Activity main = Robolectric.buildActivity(MainActivity.class, new Intent()).setup().get();
        ShadowLooper.idleMainLooper();
        View create = findText(main.getWindow().getDecorView(), "+  New inspection");
        assertNotNull(create);
        create.performClick();
        Intent next = Shadows.shadowOf(main).getNextStartedActivity();
        assertEquals(PaywallActivity.class.getName(), next.getComponent().getClassName());
        assertEquals(PaywallActivity.REASON_TRIAL_USED, next.getStringExtra(PaywallActivity.EXTRA_REASON));
        assertNotNull(findText(main.getWindow().getDecorView(), "☆  Free trial used"));

        Activity pay = Robolectric.buildActivity(PaywallActivity.class, next).setup().get();
        View root = pay.getWindow().getDecorView();
        assertNotNull(findText(root, "Choose Monthly"));
        assertNotNull(findText(root, "Choose Annual"));
        assertNotNull(findText(root, com.inspectorsgadget.app.Config.PRICE_ANNUAL + " per year"));
        assertNotNull(findText(root, "Restore purchases"));
    }

    @Test
    public void agreementPdfReportsSignatureFields() throws Exception {
        Context c = ctx();
        Inspection in = newInspection(c);
        List<ESign.Field> fields = new ArrayList<>();
        // PdfDocument is native-only under Robolectric; the preview runs the same layout code
        List<android.graphics.Bitmap> pages = Agreement.preview(c, in, fields);
        assertEquals(2, pages.size());
        int sig = 0, initials = 0, date = 0;
        for (ESign.Field f : fields) {
            assertTrue(f.page >= 1);
            assertTrue(f.x >= 0 && f.x + f.width <= ReportGenerator.W);
            assertTrue(f.y >= 0 && f.y + f.height <= ReportGenerator.H);
            if (ESign.Field.SIGNATURE.equals(f.type)) {
                sig++;
                assertEquals(1, f.page);
            }
            if (ESign.Field.INITIALS.equals(f.type)) initials++;
            if (ESign.Field.DATE.equals(f.type)) date++;
        }
        assertEquals(1, sig);
        assertEquals(2, initials);
        assertEquals(1, date);
        assertEquals("page two initials", 2, fields.get(fields.size() - 1).page);
        // save page one with the fields outlined, to check placement by eye
        android.graphics.Canvas cv = new android.graphics.Canvas(pages.get(0));
        android.graphics.Paint p = new android.graphics.Paint();
        p.setStyle(android.graphics.Paint.Style.STROKE);
        p.setStrokeWidth(3);
        p.setColor(0xFFE53935);
        for (ESign.Field f : fields) {
            if (f.page == 1) cv.drawRect(f.x * 2, f.y * 2, (f.x + f.width) * 2, (f.y + f.height) * 2, p);
        }
        File out = new File(System.getProperty("pdfOut", "out"), "esign_fields_page1.png");
        out.getParentFile().mkdirs();
        java.io.FileOutputStream os = new java.io.FileOutputStream(out);
        pages.get(0).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, os);
        os.close();
    }

    @Test
    public void demoESignatureRoundTripMarksSigned() throws Exception {
        Context c = ctx();
        Inspection in = newInspection(c);
        List<ESign.Field> fields = new ArrayList<>();
        Agreement.preview(c, in, fields);
        File pdf = fakePdf(c);
        ESign.Envelope e = ESign.sendAgreement(c, in, pdf, fields);
        assertEquals(ESign.SENT, e.state);
        assertTrue(ESign.pending(in));
        assertEquals(Agreement.SENT, Agreement.status(in));
        assertTrue(Agreement.statusText(in).startsWith("E-signature sent "));

        assertFalse("not signed yet", ESign.refresh(c, in) && Agreement.status(in) == Agreement.SIGNED);

        // simulate the client signing: move the demo send time into the past
        File rec = new File(new File(c.getFilesDir(), "esign-demo"), e.id + ".json");
        JSONObject o = new JSONObject(new String(ESign.readBytes(rec), StandardCharsets.UTF_8));
        o.put("sentAt", System.currentTimeMillis() - 3600000L);
        ESign.write(rec, o.toString().getBytes(StandardCharsets.UTF_8));

        assertTrue(ESign.refresh(c, in));
        assertEquals(Agreement.SIGNED, Agreement.status(in));
        assertEquals(Forms.SIGNED_HOW[2], in.str("pa.signedHow"));
        assertFalse(ESign.pending(in));
        File signed = Agreement.signedCopy(in);
        assertNotNull(signed);
        assertTrue(signed.exists() && signed.length() == pdf.length());
    }

    @Test
    public void demoESignatureNeedsClientEmail() throws Exception {
        Context c = ctx();
        Inspection in = newInspection(c);
        in.setUser("info.email", "");
        List<ESign.Field> fields = new ArrayList<>();
        File pdf = fakePdf(c);
        try {
            ESign.sendAgreement(c, in, pdf, fields);
            fail("expected an error");
        } catch (Exception expected) {
            assertEquals("No client email on file", expected.getMessage());
        }
        assertFalse(ESign.pending(in));
    }

    private static void shot(Activity a, String name, boolean full) throws Exception {
        ShadowLooper.idleMainLooper();
        View decor = a.getWindow().getDecorView();
        int w = a.getResources().getDisplayMetrics().widthPixels;
        int h = a.getResources().getDisplayMetrics().heightPixels;
        View target = decor;
        if (full) {
            ViewGroup content = a.findViewById(android.R.id.content);
            target = ((ViewGroup) content.getChildAt(0)).getChildAt(0);
            target.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            target.layout(0, 0, target.getMeasuredWidth(), target.getMeasuredHeight());
            h = Math.min(target.getMeasuredHeight(), 12000);
        } else {
            decor.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
            decor.layout(0, 0, w, h);
        }
        android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
        b.eraseColor(0xFFF3F5F8);
        target.draw(new android.graphics.Canvas(b));
        File out = new File(System.getProperty("pdfOut", "out"), name);
        out.getParentFile().mkdirs();
        java.io.FileOutputStream os = new java.io.FileOutputStream(out);
        b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, os);
        os.close();
    }

    @Test
    public void screens() throws Exception {
        Context c = ctx();
        Inspection in = newInspection(c);
        Activity main = Robolectric.buildActivity(MainActivity.class, new Intent()).setup().get();
        shot(main, "screen_13_home_trial.png", false);
        Intent pi = new Intent(c, PaywallActivity.class).putExtra(PaywallActivity.EXTRA_REASON, PaywallActivity.REASON_TRIAL_USED);
        shot(Robolectric.buildActivity(PaywallActivity.class, pi).setup().get(), "screen_14_paywall.png", false);
        Intent fi = new Intent(c, FormActivity.class).putExtra(FormActivity.EXTRA_INSPECTION, in.id)
                .putExtra(FormActivity.EXTRA_FORM, Forms.AGREEMENT);
        Activity form = Robolectric.buildActivity(FormActivity.class, fi).setup().get();
        assertNotNull(findText(form.getWindow().getDecorView(), "✍  Send for e-signature"));
        shot(form, "screen_15_agreement_esign.png", true);
        List<ESign.Field> fields = new ArrayList<>();
        Agreement.preview(c, in, fields);
        ESign.sendAgreement(c, in, fakePdf(c), fields);
        Activity sent = Robolectric.buildActivity(FormActivity.class, fi).setup().get();
        assertNotNull(findText(sent.getWindow().getDecorView(), "↻  Check e-signature status"));
        shot(sent, "screen_16_agreement_esign_sent.png", true);
    }
}
