package com.inspectorsgadget.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/** A filled-in demo inspection with generated placeholder photos. */
public final class SampleData {
    private SampleData() {}

    public static Inspection create(Context ctx) {
        Inspection in = Store.create(ctx);
        Account.markSample(in);
        String today = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date());

        v(in, "info.client", "Jordan & Casey Rivera");
        v(in, "info.phone", "(813) 555-0142");
        v(in, "info.email", "rivera.family@example.com");
        v(in, "info.insurer", "Citizens Property Insurance");
        v(in, "info.policy", "APP-2026-118734");
        v(in, "info.address", "4127 Palm Breeze Lane");
        v(in, "info.city", "Tampa, FL");
        v(in, "info.zip", "33611");
        v(in, "info.county", "Hillsborough");
        v(in, "info.yearBuilt", "1998");
        v(in, "info.stories", "1");
        v(in, "info.sqft", "1850");
        v(in, "info.occupancy", "Occupied");
        v(in, "info.date", today);
        v(in, "info.weather", "Sunny, 82°F");
        v(in, "info.contact", "Casey Rivera");
        v(in, "info.cellPhone", "(813) 555-0187");
        // Inspector, license and company details come from the company profile and roster
        Company.applyDefault(ctx, in);
        Forms.setTypes(in, java.util.Arrays.asList(Forms.GENERAL, Forms.FOUR_POINT, Forms.WIND, Forms.WDO, Forms.ROOF));

        // Pre-inspection agreement
        v(in, "pa.time", "9:00 AM");
        v(in, "pa.faces", "South");
        v(in, "pa.temp", "82°F");
        v(in, "pa.feeHome", "425.00");
        a(in, "pa.attend", Forms.ATTEND[0], Forms.ATTEND[1]);
        v(in, "pa.agentName", "Morgan Lee");
        v(in, "pa.agentPhone", "(813) 555-0110");
        v(in, "pa.agentBrokerage", "Bayshore Realty");
        a(in, "pa.release", Forms.RELEASE[0]);
        v(in, "pa.sentDate", today);
        v(in, "pa.signed", Forms.SIGNED[0]);
        v(in, "pa.signedHow", Forms.SIGNED_HOW[2]);
        v(in, "pa.signedDate", today);
        v(in, "pa.clientInitials", "JR");

        a(in, "g.ext.siding", "Stucco", "Concrete block");
        v(in, "g.ext.grading", "Satisfactory");
        v(in, "g.ext.siding_cond", "Marginal");
        v(in, "g.ext.trim", "Satisfactory");
        v(in, "g.ext.walks", "Satisfactory");
        v(in, "g.ext.decks", "Satisfactory");
        v(in, "g.ext.veg", "Marginal");
        v(in, "g.ext.notes", "Hairline stucco cracks at the northeast corner, typical settlement. "
                + "Shrubs in contact with the south wall; trim back 12 inches.");

        v(in, "g.openings.windowType", "Single hung");
        v(in, "g.openings.glazing", "Mixed");
        v(in, "g.openings.windows", Forms.OPENING[2]);
        v(in, "g.openings.garageGlazed", Forms.OPENING[0]);
        v(in, "g.openings.skylights", Forms.OPENING[0]);
        v(in, "g.openings.glassBlock", Forms.OPENING[0]);
        v(in, "g.openings.entryDoors", Forms.OPENING[1]);
        v(in, "g.openings.garageDoors", Forms.OPENING[4]);
        v(in, "g.openings.docs", "Impact window labels visible on 9 of 9 windows, FL# 12345.2");
        v(in, "g.openings.windowsCond", "Satisfactory");
        v(in, "g.openings.doorsCond", "Satisfactory");

        v(in, "g.roof.covering", "Asphalt/fiberglass shingle");
        v(in, "g.roof.shape", "Hip");
        v(in, "g.roof.yearInstalled", "2014");
        v(in, "g.roof.age", "12");
        v(in, "g.roof.life", "5");
        v(in, "g.roof.permitDate", "03/18/2014");
        v(in, "g.roof.replacement", "Full replacement");
        v(in, "g.roof.method", "Walked roof");
        a(in, "g.roof.damage", "Excessive granule loss", "Missing/loose/cracked tabs or tiles");
        v(in, "g.roof.leaks", "No");
        v(in, "g.roof.flashing", "Satisfactory");
        v(in, "g.roof.gutters", "Marginal");
        v(in, "g.roof.cond", "Marginal");
        v(in, "g.roof.notes", "Granule loss on the south slope and three lifted tabs near the ridge. "
                + "Recommend a roofer secure lifted tabs. Gutter at rear entry is loose.");

        v(in, "g.attic.access", "Full");
        v(in, "g.attic.deck", "Plywood");
        v(in, "g.attic.nail", "8d nails");
        v(in, "g.attic.spacing", Forms.SPACING[0]);
        v(in, "g.attic.r2w", "Clips");
        v(in, "g.attic.swr", "No");
        v(in, "g.attic.leaks", "No");
        v(in, "g.attic.insulation", "Blown fiberglass");
        v(in, "g.attic.insDepth", "9");
        v(in, "g.attic.vent", "Satisfactory");
        v(in, "g.attic.cond", "Satisfactory");
        v(in, "g.attic.notes", "Clips with three nails observed at every truss checked. No leak stains.");

        v(in, "g.struct.foundation", "Slab on grade");
        v(in, "g.struct.walls", "Concrete block (CBS)");
        v(in, "g.struct.movement", "No");
        v(in, "g.struct.cond", "Satisfactory");

        v(in, "g.elec.service", "Underground");
        v(in, "g.elec.mainLocation", "Garage, east wall");
        v(in, "g.elec.mainType", "Circuit breaker");
        v(in, "g.elec.mainAmps", "150");
        v(in, "g.elec.mainSufficient", "Yes");
        v(in, "g.elec.mainBrand", "Square D QO");
        v(in, "g.elec.mainAge", "28");
        v(in, "g.elec.mainUpdated", "1998");
        v(in, "g.elec.second", "No");
        a(in, "g.elec.wiring", "Copper", "NM, BX or conduit");
        a(in, "g.elec.hazards", "Double taps");
        v(in, "g.elec.gfci", "Yes");
        v(in, "g.elec.smoke", "Yes");
        v(in, "g.elec.cond", "Marginal");
        v(in, "g.elec.notes", "Double tap on breaker 14 (two conductors under one lug). "
                + "Recommend a licensed electrician correct.");

        a(in, "g.plumb.pipes", "Copper", "PVC/CPVC");
        v(in, "g.plumb.drains", "PVC");
        v(in, "g.plumb.pipeAge", "28");
        v(in, "g.plumb.repiped", "No");
        v(in, "g.plumb.shutoff", "Front yard near hose bib");
        v(in, "g.plumb.whLocation", "Garage");
        v(in, "g.plumb.whType", "Tank - electric");
        v(in, "g.plumb.whGallons", "50");
        v(in, "g.plumb.whYear", "2019");
        v(in, "g.plumb.tpr", "Yes");
        v(in, "g.plumb.leakEvidence", "Yes");
        v(in, "g.plumb.activeLeak", "No");
        v(in, "g.plumb.priorLeak", "Yes");
        for (String fx : Forms.FIXTURES) v(in, Forms.fixtureKey("g.plumb.fx.", fx), "Satisfactory");
        v(in, Forms.fixtureKey("g.plumb.fx.", "Sump pump"), "N/A");
        v(in, Forms.fixtureKey("g.plumb.fx.", "Sinks"), "Unsatisfactory");
        v(in, "g.plumb.cond", "Marginal");
        v(in, "g.plumb.notes", "Dried water staining in the kitchen sink base cabinet. "
                + "Slip nut on the P-trap is loose; no active leak at time of inspection.");

        v(in, "g.hvac.centralAc", "Yes");
        v(in, "g.hvac.centralHeat", "Yes");
        v(in, "g.hvac.system", "Heat pump");
        v(in, "g.hvac.brand", "Carrier 25HCE4");
        v(in, "g.hvac.age", "7");
        v(in, "g.hvac.updated", "2019");
        v(in, "g.hvac.serviced", "04/02/2026");
        v(in, "g.hvac.working", "Yes");
        v(in, "g.hvac.condensate", "Yes");
        v(in, "g.hvac.woodStove", "No");
        v(in, "g.hvac.spaceHeater", "No");
        v(in, "g.hvac.portable", "No");
        v(in, "g.hvac.filter", "Satisfactory");
        v(in, "g.hvac.ducts", "Satisfactory");
        v(in, "g.hvac.cond", "Satisfactory");
        v(in, "g.hvac.notes", "Cooling differential 18°F, within normal range.");

        v(in, "g.interior.walls", "Satisfactory");
        v(in, "g.interior.floors", "Satisfactory");
        v(in, "g.interior.doors", "Satisfactory");
        v(in, "g.interior.ceilingStains", "No");
        v(in, "g.kitchen.cabinets", "Satisfactory");
        v(in, "g.kitchen.range", "Satisfactory");
        v(in, "g.kitchen.dishwasher", "Satisfactory");
        v(in, "g.bath.count", "2");
        v(in, "g.bath.fixtures", "Satisfactory");
        v(in, "g.bath.vent", "Satisfactory");
        v(in, "g.garage.type", "Attached");
        v(in, "g.garage.door", "Satisfactory");
        v(in, "g.fireplace.type", "None");
        v(in, "g.summary.notes", "Overall the home is in fair condition for its age. Priority items: "
                + "electrical double tap, roof tab repair, kitchen sink trap.");

        // Generated placeholder photos with markup
        try {
            photo(in, "g.ext.siding", 0, "Front elevation (east)", null);
            photo(in, "g.roof.damage", 1, "Lifted tabs and granule loss, south slope",
                    stroke(DrawingView.CIRCLE, 0xFFFFEA00, 0.010f, 0.52f, 0.30f, 0.74f, 0.52f));
            photo(in, "g.elec.hazards", 2, "Double tap at breaker 14",
                    stroke(DrawingView.ARROW, 0xFFFF1744, 0.010f, 0.18f, 0.80f, 0.43f, 0.55f),
                    stroke(DrawingView.CIRCLE, 0xFFFF1744, 0.009f, 0.42f, 0.44f, 0.60f, 0.62f));
            photo(in, "g.elec.mainBrand", 3, "Panel label / data plate", null);
            photo(in, "g.plumb.tpr", 4, "Water heater with TPR valve", null);
            photo(in, "g.plumb.priorLeak", 5, "Stain in sink base cabinet",
                    stroke(DrawingView.BOX, 0xFF00E676, 0.009f, 0.30f, 0.55f, 0.72f, 0.85f));
            photo(in, "g.hvac.brand", 6, "Heat pump condenser", null);
            photo(in, "g.attic.r2w", 7, "Hurricane clip at truss", stroke(DrawingView.ARROW, 0xFF00E5FF, 0.010f,
                    0.82f, 0.15f, 0.56f, 0.45f));
            photo(in, "g.openings.docs", 8, "Impact glass label", null);
        } catch (Exception e) {
            android.util.Log.e("InspectorsGadget", "sample photos", e);
        }
        // Wind mitigation needs a photo for each attribute in questions 3-7
        String[][] wmPhotos = {{"g.attic", "wm.deck"}, {"g.attic", "wm.r2w"}, {"g.roof", "wm.geom"},
                {"g.roof", "wm.cover"}, {"g.openings", "wm.open"}};
        for (String[] w : wmPhotos)
            for (String pid : Logic.sectionPhotoIds(in, Forms.section(w[0]))) in.addPhotoToSection(w[1], pid);
        v(in, Forms.commentKey("g.ext.siding_cond"), "Hairline stucco cracks at the northeast corner and below the front window. Seal and paint.");
        v(in, Forms.commentKey("g.roof.damage"), "Lifted tabs and granule loss on the south slope, about 12 shingles. Repair by a licensed roofer.");
        v(in, Forms.commentKey("g.elec.hazards"), "Double tap at breaker 14. Have an electrician separate the conductors.");

        // Pest / termite (WDO)
        v(in, "wdo.structures", "Main residence and attached garage");
        a(in, "wdo.construction", "Concrete block", "Slab on grade");
        v(in, "wdo.requestedBy", "Morgan Lee, Sunshine Realty");
        v(in, "wdo.bizLicense", "JB123456");
        v(in, "wdo.idCard", "JE987654");
        v(in, "wdo.result", "Visible evidence of WDO observed");
        a(in, "wdo.evidence", "Frass / fecal pellets", "Exit holes");
        v(in, "wdo.evidenceLoc", "Garage door jamb, right side, and window sill in rear bedroom");
        v(in, Forms.commentKey("wdo.evidence"), "Drywood termite pellets below the garage door jamb. No live termites observed.");
        v(in, "wdo.damage", "Visible damage observed");
        v(in, "wdo.damageLoc", "Garage door jamb, lower 18 inches");
        a(in, "wdo.conducive", "Soil or mulch above slab edge", "Vegetation contacting structure");
        v(in, "wdo.access.attic", "Partially inaccessible");
        v(in, "wdo.access.atticWhy", "Insulation over ceiling joists and low clearance at eaves");
        v(in, "wdo.access.interior", "Partially inaccessible");
        v(in, "wdo.access.interiorWhy", "Furniture, stored items and floor coverings");
        v(in, "wdo.access.exterior", "Fully accessible");
        v(in, "wdo.access.crawlspace", "N/A");
        v(in, "wdo.access.garage", "Partially inaccessible");
        v(in, "wdo.access.garageWhy", "Stored items along the east wall");
        v(in, "wdo.prevTreat", "Yes");
        v(in, "wdo.prevTreatDesc", "Drill holes in garage slab, bait stations at perimeter");
        v(in, "wdo.noticeLoc", "Electrical panel door, garage");
        v(in, "wdo.treatedNow", "No");
        v(in, "wdo.summary.notes", "Evidence of drywood termites and damage at the garage door jamb. Recommend "
                + "treatment by a licensed pest control company and repair of the damaged jamb. Pull mulch back "
                + "below the slab edge and trim shrubs away from the walls.");

        // Roof (covering details prefill from the general inspection)
        v(in, "roof.pitch", "6");
        v(in, "roof.layers", "1");
        v(in, "roof.field", "Marginal");
        v(in, "roof.ridge", "Satisfactory");
        v(in, "roof.fasteners", "Satisfactory");
        v(in, "roof.storm", "No");
        v(in, "roof.dripEdge", "Satisfactory");
        v(in, "roof.valleys", "Satisfactory");
        v(in, "roof.wallFlash", "Satisfactory");
        v(in, "roof.boots", "Marginal");
        v(in, Forms.commentKey("roof.boots"), "Cracked rubber boot at the kitchen vent stack. Replace.");
        v(in, "roof.vents", "Satisfactory");
        v(in, "roof.downspouts", "Satisfactory");
        v(in, "roof.soffit", "Satisfactory");
        v(in, "roof.decking", "Satisfactory");
        v(in, "roof.overall", "Marginal");
        v(in, "roof.recommend", "Maintenance / repair");
        v(in, "roof.summary.notes", "Roof is serviceable with about 8 years of remaining life. Secure the lifted "
                + "tabs on the south slope and replace the cracked vent boot.");

        Logic.prefillForm(in, Forms.AGREEMENT);
        for (String t : Forms.types(in)) if (!Forms.GENERAL.equals(t)) Logic.prefillForm(in, t);
        Store.save(in);
        return in;
    }

    private static void v(Inspection in, String k, String val) { in.setUser(k, val); }

    private static void a(Inspection in, String k, String... vals) {
        JSONArray arr = new JSONArray();
        for (String s : vals) arr.put(s);
        in.setUser(k, arr);
    }

    private static JSONObject stroke(int tool, int color, float w, float x0, float y0, float x1, float y1) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", tool);
            o.put("c", color);
            o.put("w", (double) w);
            JSONArray p = new JSONArray();
            p.put((double) x0).put((double) y0).put((double) x1).put((double) y1);
            o.put("p", p);
            return o;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void photo(Inspection in, String section, int scene, String caption, JSONObject... strokes)
            throws Exception {
        String pid = in.newPhoto(section);
        Bitmap b = scene(scene);
        Images.writeJpeg(b, in.originalFile(pid), 88);
        in.setCaption(pid, caption);
        if (strokes != null && strokes.length > 0 && strokes[0] != null) {
            JSONArray arr = new JSONArray();
            for (JSONObject s : strokes) arr.put(s);
            Canvas c = new Canvas(b);
            DrawingView.renderJson(c, arr, b.getWidth(), b.getHeight(), true);
            String name = pid + "_a.jpg";
            Images.writeJpeg(b, new File(in.photoDir(), name), 88);
            in.meta(pid).put("annotated", name);
            in.meta(pid).put("strokes", arr);
        }
        b.recycle();
        in.addPhotoToSection(section, pid);
    }

    /** Simple illustrated stand-ins for real photos. */
    static Bitmap scene(int n) {
        int w = 1280, h = 960;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Random r = new Random(n * 31L + 7);
        switch (n) {
            case 0: { // house front
                p.setShader(new LinearGradient(0, 0, 0, h, 0xFF8EC5FC, 0xFFE0F2FF, Shader.TileMode.CLAMP));
                c.drawRect(0, 0, w, h, p);
                p.setShader(null);
                p.setColor(0xFF5E9E4F);
                c.drawRect(0, h * 0.75f, w, h, p);
                p.setColor(0xFFF1E4C8);
                c.drawRect(260, 420, 1020, 740, p);
                p.setColor(0xFF6B4E3D);
                Path roof = new Path();
                roof.moveTo(200, 430);
                roof.lineTo(640, 220);
                roof.lineTo(1080, 430);
                roof.close();
                c.drawPath(roof, p);
                p.setColor(0xFF7A5230);
                c.drawRect(590, 560, 690, 740, p);
                p.setColor(0xFFBFE3FF);
                c.drawRect(340, 520, 480, 620, p);
                c.drawRect(800, 520, 940, 620, p);
                break;
            }
            case 1: { // shingles
                c.drawColor(0xFF4A4A4A);
                for (int row = 0; row < 14; row++) {
                    for (int col = -1; col < 14; col++) {
                        float x = col * 100 + (row % 2) * 50, y = row * 70;
                        p.setColor(Color.rgb(70 + r.nextInt(30), 70 + r.nextInt(30), 72 + r.nextInt(30)));
                        c.drawRect(x + 2, y + 2, x + 98, y + 68, p);
                    }
                }
                p.setColor(0xFF8A8172);
                for (int i = 0; i < 400; i++) c.drawCircle(700 + r.nextInt(240), 320 + r.nextInt(180), 3 + r.nextInt(4), p);
                p.setColor(0xFF2E2E2E);
                c.save();
                c.rotate(-12, 820, 380);
                c.drawRect(760, 340, 880, 410, p);
                c.restore();
                break;
            }
            case 2: case 3: { // electrical panel
                c.drawColor(0xFFD9D4C7);
                p.setColor(0xFF9EA3A8);
                c.drawRoundRect(new RectF(340, 80, 940, 900), 16, 16, p);
                p.setColor(0xFF2B2F33);
                c.drawRect(380, 120, 900, 860, p);
                for (int i = 0; i < 10; i++) {
                    for (int s = 0; s < 2; s++) {
                        float x = s == 0 ? 420 : 660, y = 160 + i * 68;
                        p.setColor(0xFF15171A);
                        c.drawRect(x, y, x + 200, y + 56, p);
                        p.setColor(0xFFE6E6E6);
                        c.drawRect(x + 70, y + 14, x + 130, y + 42, p);
                    }
                }
                if (n == 2) {
                    p.setStrokeWidth(8);
                    p.setColor(0xFFB22222);
                    c.drawLine(580, 480, 520, 700, p);
                    p.setColor(0xFF111111);
                    c.drawLine(600, 480, 560, 720, p);
                } else {
                    p.setColor(0xFFFFFFFF);
                    c.drawRect(420, 200, 860, 520, p);
                    Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
                    t.setColor(0xFF222222);
                    t.setTextSize(34);
                    t.setTypeface(Typeface.DEFAULT_BOLD);
                    c.drawText("LOAD CENTER  150A", 450, 260, t);
                    t.setTypeface(Typeface.DEFAULT);
                    for (int i = 0; i < 6; i++) c.drawText((i + 1) + ".  ____________", 450, 320 + i * 34, t);
                }
                break;
            }
            case 4: { // water heater
                c.drawColor(0xFFCFC8BC);
                p.setColor(0xFFF4F4F4);
                c.drawRoundRect(new RectF(460, 140, 820, 900), 60, 60, p);
                p.setColor(0xFFB0B0B0);
                c.drawRect(600, 40, 630, 160, p);
                c.drawRect(660, 40, 690, 160, p);
                p.setColor(0xFFC9A227);
                c.drawRect(820, 300, 900, 330, p);
                c.drawRect(880, 300, 900, 760, p);
                p.setColor(0xFF2F6DB5);
                c.drawRect(560, 420, 720, 520, p);
                break;
            }
            case 5: { // sink cabinet
                c.drawColor(0xFF6E4B2E);
                p.setColor(0xFFE8DCC2);
                c.drawRect(120, 380, 1160, 940, p);
                p.setColor(0xFFDADADA);
                c.drawRect(560, 0, 600, 520, p);
                c.drawRect(560, 500, 820, 540, p);
                p.setColor(0xFF9C7A4A);
                c.drawOval(new RectF(420, 640, 900, 840), p);
                p.setColor(0xFFB89868);
                c.drawOval(new RectF(500, 680, 820, 800), p);
                break;
            }
            case 6: { // condenser
                c.drawColor(0xFF7FA66B);
                p.setColor(0xFFBDBDBD);
                c.drawRect(0, 760, w, h, p);
                p.setColor(0xFFECECEC);
                c.drawRect(360, 240, 920, 800, p);
                p.setColor(0xFF333333);
                c.drawCircle(640, 420, 200, p);
                p.setColor(0xFF555555);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(6);
                for (int i = 1; i < 8; i++) c.drawCircle(640, 420, i * 25, p);
                p.setStyle(Paint.Style.FILL);
                break;
            }
            case 7: { // attic clip
                c.drawColor(0xFF3B2F25);
                p.setColor(0xFFC49A6C);
                c.drawRect(0, 560, w, 680, p);
                c.save();
                c.rotate(-30, 640, 560);
                c.drawRect(300, 470, 1100, 560, p);
                c.restore();
                p.setColor(0xFFB7BCC2);
                c.drawRect(560, 400, 680, 640, p);
                p.setColor(0xFF6F757C);
                for (int i = 0; i < 3; i++) c.drawCircle(590 + i * 30, 600, 9, p);
                p.setColor(0xFFE2C9A0);
                for (int i = 0; i < 30; i++) c.drawCircle(r.nextInt(w), 700 + r.nextInt(260), 10 + r.nextInt(20), p);
                break;
            }
            default: { // window label
                p.setShader(new LinearGradient(0, 0, w, h, 0xFF9CC9E8, 0xFFDDEFFA, Shader.TileMode.CLAMP));
                c.drawRect(0, 0, w, h, p);
                p.setShader(null);
                p.setColor(0xFFFFFFFF);
                c.drawRect(380, 300, 900, 640, p);
                Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
                t.setColor(0xFF1A1A1A);
                t.setTextSize(42);
                t.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText("IMPACT RESISTANT", 420, 380, t);
                t.setTextSize(34);
                t.setTypeface(Typeface.DEFAULT);
                c.drawText("ASTM E1886 / E1996", 420, 450, t);
                c.drawText("Missile Level D", 420, 500, t);
                c.drawText("FL# 12345.2", 420, 560, t);
            }
        }
        Paint tag = new Paint(Paint.ANTI_ALIAS_FLAG);
        tag.setColor(0xAA000000);
        c.drawRect(0, h - 56, 360, h, tag);
        tag.setColor(Color.WHITE);
        tag.setTextSize(30);
        tag.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText("DEMO PHOTO", 24, h - 18, tag);
        return b;
    }
}
