package com.inspectorsgadget.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w400dp-h860dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class StateFormsTest {

    @org.junit.Before
    public void signedIn() { Auth.setUnlockedForTest(true); }

    private static final File OUT = new File(System.getProperty("pdfOut", "/tmp"));

    private static List<String> ids(List<StateForms.OfficialForm> forms) {
        List<String> out = new ArrayList<>();
        for (StateForms.OfficialForm f : forms) out.add(f.id);
        return out;
    }

    private static void noDashes(String where, String s) {
        assertFalse("em dash in " + where + ": " + s, s.contains("—"));
        assertFalse("en dash in " + where + ": " + s, s.contains("–"));
    }

    @Test
    public void catalogIsComplete() {
        List<StateForms.OfficialForm> all = StateForms.all();
        assertEquals(48, all.size());
        // radon inspector license, and radon inspections map to it
        assertTrue(Arrays.asList(StateForms.licenseTitles()).contains("Radon inspector"));
        assertEquals(Arrays.asList(StateForms.RADON), StateForms.get("all-ansi-aarst-mah").licenses);
        assertEquals(Forms.RADON, StateForms.get("all-ansi-aarst-mah").type);
        // license and property state are dropdowns showing state names
        Forms.Field ls = Forms.field(StateForms.LICENSE_STATE_KEY);
        assertTrue(ls.dropdown);
        assertEquals("Florida", ls.optionLabel(Arrays.asList(ls.options).indexOf("FL")));
        assertTrue(Forms.field(StateForms.PROPERTY_STATE_KEY).dropdown);
        Set<String> states = new HashSet<>(Arrays.asList(StateForms.STATES));
        states.add(StateForms.ALL);
        for (StateForms.OfficialForm f : all) {
            assertTrue(f.id, Forms.isType(f.type));
            assertTrue(f.id + " " + f.state, states.contains(f.state));
            assertTrue(f.id, f.source.startsWith("https://"));
            for (String l : f.licenses) assertTrue(f.id + " " + l, Arrays.asList(StateForms.LICENSES).contains(l));
            for (String s : new String[]{f.name, f.number, f.issuer, f.statusText, f.appliesWhen, f.label()})
                noDashes(f.id, s);
        }
        for (String l : StateForms.LICENSES) noDashes(l, StateForms.licenseTitle(l));
        assertEquals(51, StateForms.STATES.length);
        // types with no official form anywhere
        for (String t : new String[]{Forms.CHIMNEY, Forms.STRUCTURAL, Forms.HVAC, Forms.ELECTRICAL, Forms.POOL,
                Forms.SOIL, Forms.OIL_TANK})
            assertFalse(t, StateForms.hasAnyForm(t));
        assertEquals("FL OIR-B1-1802", StateForms.get("fl-oir-b1-1802").label());
        assertTrue(StateForms.get("fl-oir-b1-1802").unaltered);
        assertTrue(StateForms.get("tx-trec-rei-7-6").unaltered);
        assertFalse(StateForms.get("all-npma-33").unaltered);
        // narrow cases are conditional, not required on every house
        assertEquals(StateForms.Status.CONDITIONAL, StateForms.get("fl-mit-bt-ii-iii").status);
        assertEquals(StateForms.Status.CONDITIONAL, StateForms.get("tx-tdi-wpi-8").status);
        assertEquals("FL Citizens Roof Inspection Form", StateForms.get("fl-citizens-roof-inspection-for").label());
        assertEquals("FORTIFIED Roof / Silver / Gold", StateForms.get("all-fortified-roof-silver-gold").label());
    }

    @Test
    public void formsByStateAndType() {
        assertEquals(Arrays.asList("fl-oir-b1-1802", "fl-mit-bt-ii-iii", "all-fortified-roof-silver-gold"),
                ids(StateForms.forState("FL", Forms.WIND)));
        assertEquals(Collections.singletonList("fl-citizens-4-point"), ids(StateForms.forState("FL", Forms.FOUR_POINT)));
        assertEquals(Collections.singletonList("fl-citizens-roof-inspection-for"), ids(StateForms.forState("FL", Forms.ROOF)));
        // a state WDO form replaces NPMA-33
        assertEquals(Collections.singletonList("fl-fdacs-13645"), ids(StateForms.forState("FL", Forms.WDO)));
        assertEquals(Collections.singletonList("tx-spcs-t-5"), ids(StateForms.forState("TX", Forms.WDO)));
        assertEquals(Collections.singletonList("oh-npma-33"), ids(StateForms.forState("OH", Forms.WDO)));
        // states without their own WDO form use NPMA-33
        assertEquals(Collections.singletonList("all-npma-33"), ids(StateForms.forState("CO", Forms.WDO)));
        assertEquals(Collections.singletonList("tx-trec-rei-7-6"), ids(StateForms.forState("TX", Forms.GENERAL)));
        assertTrue(StateForms.forState("FL", Forms.GENERAL).isEmpty());
        assertTrue(StateForms.forState("FL", Forms.CHIMNEY).isEmpty());
        // national lead forms stay alongside state ones
        List<String> lead = ids(StateForms.forState("MD", Forms.LEAD));
        assertTrue(lead.contains("md-mde-form-330-lead-form-e"));
        assertTrue(lead.contains("all-epa-hud-sample-disclosure"));

        assertEquals("FL", StateForms.stateCode("Florida"));
        assertEquals("TX", StateForms.stateCode(" tx "));
        assertEquals("", StateForms.stateCode("Nowhere"));
    }

    @Test
    public void licensesDecideWhoCanIssue() {
        List<String> home = Collections.singletonList(StateForms.HOME_INSPECTOR);
        List<String> pest = Collections.singletonList(StateForms.PEST);
        StateForms.OfficialForm wind = StateForms.get("fl-oir-b1-1802"), wdo = StateForms.get("fl-fdacs-13645"),
                roof = StateForms.get("fl-citizens-roof-inspection-for"), trec = StateForms.get("tx-trec-rei-7-6"),
                fortified = StateForms.get("all-fortified-roof-silver-gold"),
                disclosure = StateForms.get("all-epa-hud-sample-disclosure");
        assertTrue(StateForms.canIssue(wind, home, "FL", "FL"));
        assertFalse(StateForms.canIssue(wdo, home, "FL", "FL"));
        assertTrue(StateForms.canIssue(wdo, pest, "FL", "FL"));
        assertTrue(StateForms.canIssue(roof, Collections.singletonList(StateForms.ROOFING_CONTRACTOR), "FL", "FL"));
        // a Florida license does not cover a Texas property
        assertFalse(StateForms.canIssue(trec, home, "FL", "TX"));
        assertTrue(StateForms.canIssue(trec, home, "TX", "TX"));
        assertFalse(StateForms.canIssue(wind, home, "", "FL"));
        // national credential and seller forms
        assertTrue(StateForms.canIssue(fortified, Collections.singletonList(StateForms.FORTIFIED), "FL", "AL"));
        assertTrue(StateForms.canIssue(disclosure, new ArrayList<String>(), "", "FL"));
    }

    @Test
    public void inspectorLicensesAndHub() throws Exception {
        Activity main = Robolectric.buildActivity(MainActivity.class, new Intent()).setup().get();
        Inspection ip = Company.inspectors(main).get(0);
        // seeded inspector: Florida home inspector
        assertEquals("FL", ip.str(StateForms.LICENSE_STATE_KEY));
        assertEquals(Collections.singletonList(StateForms.HOME_INSPECTOR), StateForms.licensesHeld(ip));
        // older records without the field: license type counts
        ip.setUser(StateForms.LICENSES_KEY, new org.json.JSONArray());
        assertEquals(Collections.singletonList(StateForms.HOME_INSPECTOR), StateForms.licensesHeld(ip));
        assertEquals(Arrays.asList(StateForms.LICENSES).size(), Forms.field(StateForms.LICENSES_KEY).options.length);
        Forms.setLicensedTypes(ip, Arrays.asList(Forms.GENERAL, Forms.FOUR_POINT, Forms.WIND, Forms.WDO, Forms.ROOF));
        Store.save(ip);

        Inspection in = Store.create(main);
        in.setUser("info.address", "100 Gulf Breeze Pkwy");
        Forms.setTypes(in, Arrays.asList(Forms.GENERAL, Forms.FOUR_POINT, Forms.WIND, Forms.WDO, Forms.ROOF));
        Company.applyDefault(main, in);
        Store.save(in);
        // property state comes from the company profile
        assertEquals("FL", in.str(StateForms.PROPERTY_STATE_KEY));
        assertTrue(in.isPrefilled(StateForms.PROPERTY_STATE_KEY));

        Activity hub = Robolectric.buildActivity(InspectionActivity.class,
                new Intent().putExtra(InspectionActivity.EXTRA_INSPECTION, in.id)).setup().get();
        ShadowLooper.idleMainLooper();
        List<String> h = texts(hub.getWindow().getDecorView());
        assertTrue(h.toString(), h.contains("State Forms · Florida"));
        assertTrue(h.contains("FL OIR-B1-1802"));
        assertTrue(h.contains("FL FDACS-13645"));
        assertTrue(h.contains("FL Citizens 4-Point"));
        assertFalse(h.contains("NPMA-33"));
        // home inspector lacks the licenses for FDACS-13645, MIT-BT II & III and FORTIFIED
        assertEquals(3, Collections.frequency(h, "License not on file"));
        for (String s : h) noDashes("hub", s);
        shot(hub, new File(OUT, "screen_21_state_forms.png"));

        // pest, engineer and FORTIFIED licenses added: nothing missing
        StateForms.setLicensesHeld(ip, Arrays.asList(StateForms.HOME_INSPECTOR, StateForms.PEST,
                StateForms.ENGINEER, StateForms.FORTIFIED));
        Store.save(ip);
        hub = Robolectric.buildActivity(InspectionActivity.class,
                new Intent().putExtra(InspectionActivity.EXTRA_INSPECTION, in.id)).setup().get();
        assertFalse(texts(hub.getWindow().getDecorView()).contains("License not on file"));

        // Texas property: Texas forms
        in.setUser(StateForms.PROPERTY_STATE_KEY, "TX");
        Store.save(in);
        hub = Robolectric.buildActivity(InspectionActivity.class,
                new Intent().putExtra(InspectionActivity.EXTRA_INSPECTION, in.id)).setup().get();
        h = texts(hub.getWindow().getDecorView());
        assertTrue(h.contains("TX TREC REI 7-6"));
        assertTrue(h.contains("TX SPCS/T-5"));
        assertFalse(h.contains("FL OIR-B1-1802"));
        assertTrue(h.contains("License not on file"));

        shot(Robolectric.buildActivity(SectionActivity.class, new Intent()
                .putExtra(SectionActivity.EXTRA_INSPECTION, ip.id)
                .putExtra(SectionActivity.EXTRA_SECTION, "ip")).setup().get(), new File(OUT, "screen_22_inspector_licenses.png"));
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

    private static void shot(Activity a, File f) throws Exception {
        ShadowLooper.idleMainLooper();
        int w = a.getResources().getDisplayMetrics().widthPixels;
        ViewGroup content = a.findViewById(android.R.id.content);
        View target = ((ViewGroup) content.getChildAt(0)).getChildAt(0);
        target.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        target.layout(0, 0, target.getMeasuredWidth(), target.getMeasuredHeight());
        Bitmap b = Bitmap.createBitmap(w, Math.min(target.getMeasuredHeight(), 12000), Bitmap.Config.ARGB_8888);
        b.eraseColor(0xFFF3F5F8);
        target.draw(new android.graphics.Canvas(b));
        java.io.FileOutputStream os = new java.io.FileOutputStream(f);
        b.compress(Bitmap.CompressFormat.PNG, 100, os);
        os.close();
    }
}
