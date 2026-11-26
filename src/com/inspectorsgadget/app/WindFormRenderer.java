package com.inspectorsgadget.app;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Lays out the wind mitigation answers in the order and wording of the
 * Uniform Mitigation Verification Inspection Form (OIR-B1-1802, Rev. 01/12),
 * with checked boxes for the chosen answers, followed by the photo documentation.
 */
final class WindFormRenderer {
    private WindFormRenderer() {}

    static final int BLACK = 0xFF111111;
    static final int FILL = 0xFF1A3D7C; // filled-in values print in blue like ink

    static void render(ReportGenerator.Doc d, Inspection in) {
        int W = ReportGenerator.W, M = ReportGenerator.M;
        int full = W - 2 * M;

        center(d, "Uniform Mitigation Verification Inspection Form", 15, Typeface.BOLD);
        center(d, "Maintain a copy of this form and any documentation provided with the insurance policy", 9,
                Typeface.ITALIC);
        d.y += 6;
        grid(d, new String[][]{{"Inspection Date", in.str("wm.owner.date")}});
        heading(d, "Owner Information");
        grid(d, new String[][]{
                {"Owner Name", in.str("wm.owner.name")}, {"Contact Person", in.str("wm.owner.contact")},
                {"Address", in.str("wm.owner.address")}, {"Home Phone", in.str("wm.owner.homePhone")},
                {"City", in.str("wm.owner.city")}, {"Zip", in.str("wm.owner.zip")},
                {"Work Phone", in.str("wm.owner.workPhone")}, {"County", in.str("wm.owner.county")},
                {"Cell Phone", in.str("wm.owner.cellPhone")}, {"Insurance Company", in.str("wm.owner.insurer")},
                {"Policy #", in.str("wm.owner.policy")}, {"Year of Home", in.str("wm.owner.year")},
                {"# of Stories", in.str("wm.owner.stories")}, {"Email", in.str("wm.owner.email")}});
        d.y += 4;
        para(d, "NOTE: Any documentation used in validating the compliance or existence of each construction or "
                + "mitigation attribute must accompany this form. At least one photograph must accompany this form to "
                + "validate each attribute marked in questions 3 though 7. The insurer may ask additional questions "
                + "regarding the mitigated feature(s) verified on this form.", 8.5f, Typeface.BOLD);

        // 1. Building code
        question(d, "1. Building Code:", "Was the structure built in compliance with the Florida Building Code (FBC 2001 "
                + "or later) OR for homes located in the HVHZ (Miami-Dade or Broward counties), South Florida Building "
                + "Code (SFBC-94)?");
        String q1 = letter(in.str("wm.building.answer"));
        String yb = in.str("wm.building.year"), permit = in.str("wm.building.permit");
        box(d, q1.equals("A"), "A. Built in compliance with the FBC: Year Built " + blank(q1.equals("A") ? yb : "")
                + ". For homes built in 2002/2003 provide a permit application with a date after 3/1/2002: Building "
                + "Permit Application Date (MM/DD/YYYY) " + blank(q1.equals("A") ? permit : ""));
        box(d, q1.equals("B"), "B. For the HVHZ Only: Built in compliance with the SFBC-94: Year Built "
                + blank(q1.equals("B") ? yb : "") + ". For homes built in 1994, 1995, and 1996 provide a permit "
                + "application with a date after 9/1/1994: Building Permit Application Date (MM/DD/YYYY) "
                + blank(q1.equals("B") ? permit : ""));
        box(d, q1.equals("C"), "C. Unknown or does not meet the requirements of Answer “A” or “B”");

        // 2. Roof covering
        question(d, "2. Roof Covering:", "Select all roof covering types in use. Provide the permit application date OR "
                + "FBC/MDC Product Approval number OR Year of Original Installation/Replacement OR indicate that no "
                + "information was available to verify compliance for each roof covering identified.");
        coverTable(d, in, full);
        String q2 = letter(in.str("wm.cover.answer"));
        box(d, q2.equals("A"), "A. All roof coverings listed above meet the FBC with a FBC or Miami-Dade Product "
                + "Approval listing current at time of installation OR have a roofing permit application date on or after "
                + "3/1/02 OR the roof is original and built in 2004 or later.");
        box(d, q2.equals("B"), "B. All roof coverings have a Miami-Dade Product Approval listing current at time of "
                + "installation OR (for the HVHZ only) a roofing permit application after 9/1/1994 and before 3/1/2002 "
                + "OR the roof is original and built in 1997 or later.");
        box(d, q2.equals("C"), "C. One or more roof coverings do not meet the requirements of Answer “A” or “B”.");
        box(d, q2.equals("D"), "D. No roof coverings meet the requirements of Answer “A” or “B”.");

        // 3. Roof deck attachment
        question(d, "3. Roof Deck Attachment:", "What is the weakest form of roof deck attachment?");
        String q3 = letter(in.str("wm.deck.answer"));
        box(d, q3.equals("A"), "A. Plywood/Oriented strand board (OSB) roof sheathing attached to the roof truss/rafter "
                + "(spaced a maximum of 24\" inches o.c.) by staples or 6d nails spaced at 6\" along the edge and 12\" in "
                + "the field. -OR- Batten decking supporting wood shakes or wood shingles. -OR- Any system of screws, "
                + "nails, adhesives, other deck fastening system or truss/rafter spacing that has an equivalent mean "
                + "uplift less than that required for Options B or C below.");
        box(d, q3.equals("B"), "B. Plywood/OSB roof sheathing with a minimum thickness of 7/16\" inch attached to the roof "
                + "truss/rafter (spaced a maximum of 24\" inches o.c.) by 8d common nails spaced a maximum of 12\" inches "
                + "in the field. -OR- Any system of screws, nails, adhesives, other deck fastening system or truss/rafter "
                + "spacing that is shown to have an equivalent or greater resistance 8d nails spaced a maximum of 12 "
                + "inches in the field or has a mean uplift resistance of at least 103 psf.");
        box(d, q3.equals("C"), "C. Plywood/OSB roof sheathing with a minimum thickness of 7/16\" inch attached to the roof "
                + "truss/rafter (spaced a maximum of 24\" inches o.c.) by 8d common nails spaced a maximum of 6\" inches "
                + "in the field. -OR- Dimensional lumber/Tongue & Groove decking with a minimum of 2 nails per board (or "
                + "1 nail per board if each board is equal to or less than 6 inches in width). -OR- Any system of screws, "
                + "nails, adhesives, other deck fastening system or truss/rafter spacing that is shown to have an "
                + "equivalent or greater resistance than 8d common nails spaced a maximum of 6 inches in the field or has "
                + "a mean uplift resistance of at least 182 psf.");
        box(d, q3.equals("D"), "D. Reinforced Concrete Roof Deck.");
        box(d, q3.equals("E"), "E. Other: " + blank(in.str("wm.deck.other")));
        box(d, q3.equals("F"), "F. Unknown or unidentified.");
        box(d, q3.equals("G"), "G. No attic access.");

        // 4. Roof to wall
        question(d, "4. Roof to Wall Attachment:", "What is the WEAKEST roof to wall connection? (Do not include attachment "
                + "of hip/valley jacks within 5 feet of the inside or outside corner of the roof in determination of "
                + "WEAKEST type)");
        String q4 = letter(in.str("wm.r2w.answer"));
        box(d, q4.equals("A"), "A. Toe Nails: Truss/rafter anchored to top plate of wall using nails driven at an angle "
                + "through the truss/rafter and attached to the top plate of the wall, or metal connectors that do not "
                + "meet the minimal conditions or requirements of B, C, or D.");
        List<String> minimal = in.list("wm.r2w.minimal");
        para(d, "Minimal conditions to qualify for categories B, C, or D. All visible metal connectors are:", 8.5f,
                Typeface.ITALIC);
        sub(d, minimal.size() > 0 && minimal.get(0).startsWith("Secured"), "Secured to truss/rafter with a minimum of three (3) nails, and");
        sub(d, minimal.contains(Forms.field("wm.r2w.minimal").options[1]), "Attached to the wall top plate of the wall "
                + "framing, or embedded in the bond beam, with less than a ½\" gap from the blocking or truss/rafter and "
                + "blocked no more than 1.5\" of the truss/rafter, and free of visible severe corrosion.");
        box(d, q4.equals("B"), "B. Clips: Metal connectors that do not wrap over the top of the truss/rafter, or metal "
                + "connectors with a minimum of 1 strap that wraps over the top of the truss/rafter and does not meet the "
                + "nail position requirements of C or D, but is secured with a minimum of 3 nails.");
        box(d, q4.equals("C"), "C. Single Wraps: Metal connectors consisting of a single strap that wraps over the top of "
                + "the truss/rafter and is secured with a minimum of 2 nails on the front side and a minimum of 1 nail on "
                + "the opposing side.");
        box(d, q4.equals("D"), "D. Double Wraps: Metal connectors consisting of 2 separate straps that are attached to the "
                + "wall frame, or embedded in the bond beam, on either side of the truss/rafter where each strap wraps over "
                + "the top of the truss/rafter and is secured with a minimum of 2 nails on the front side, and a minimum "
                + "of 1 nail on the opposing side, or metal connectors consisting of a single strap that wraps over the top "
                + "of the truss/rafter, is secured to the wall on both sides, and is secured to the top plate with a "
                + "minimum of three nails on each side.");
        box(d, q4.equals("E"), "E. Structural: Anchor bolts structurally connected or reinforced concrete roof.");
        box(d, q4.equals("F"), "F. Other: " + blank(in.str("wm.r2w.other")));
        box(d, q4.equals("G"), "G. Unknown or unidentified");
        box(d, q4.equals("H"), "H. No attic access");

        // 5. Roof geometry
        question(d, "5. Roof Geometry:", "What is the roof shape? (Do not consider roofs of porches or carports that are "
                + "attached only to the fascia or wall of the host structure over unenclosed space in the determination "
                + "of roof perimeter or roof area for roof geometry classification).");
        String q5 = letter(in.str("wm.geom.answer"));
        box(d, q5.equals("A"), "A. Hip Roof: Hip roof with no other roof shapes greater than 10% of the total roof system "
                + "perimeter. Total length of non-hip features: " + blank(in.str("wm.geom.nonHip"))
                + " feet; Total roof system perimeter: " + blank(in.str("wm.geom.perimeter")) + " feet");
        box(d, q5.equals("B"), "B. Flat Roof: Roof on a building with 5 or more units where at least 90% of the main roof "
                + "area has a roof slope of less than 2:12. Roof area with slope less than 2:12 "
                + blank(in.str("wm.geom.flatArea")) + " sq ft; Total roof area " + blank(in.str("wm.geom.totalArea"))
                + " sq ft");
        box(d, q5.equals("C"), "C. Other Roof: Any roof that does not qualify as either (A) or (B) above.");

        // 6. SWR
        question(d, "6. Secondary Water Resistance (SWR):", "(standard underlayments or hot-mopped felts do not qualify "
                + "as an SWR)");
        String q6 = letter(in.str("wm.swr.answer"));
        box(d, q6.equals("A"), "A. SWR (also called Sealed Roof Deck): Self-adhering polymer modified-bitumen roofing "
                + "underlayment applied directly to the sheathing or foam adhesive SWR barrier (not foamed-on insulation) "
                + "applied as a supplemental means to protect the dwelling from water intrusion in the event of roof "
                + "covering loss.");
        box(d, q6.equals("B"), "B. No SWR.");
        box(d, q6.equals("C"), "C. Unknown or undetermined.");

        // 7. Opening protection
        question(d, "7. Opening Protection:", "What is the weakest form of wind borne debris protection installed on the "
                + "structure? First, use the table to determine the weakest form of protection for each category of "
                + "opening. Second, (a) check one answer below (A, B, C, N, or X) based upon the lowest protection level "
                + "for ALL Glazed openings and (b) check the protection level for all Non-Glazed openings (.1, .2, or .3) "
                + "as applicable.");
        openingChart(d, in, full);
        String q7 = in.str("wm.open.answer");
        String q7code = q7.contains(" - ") ? q7.substring(0, q7.indexOf(" - ")) : "";
        String[][] classes = {
                {"A", "A. Exterior Openings Cyclic Pressure and 9-lb Large Missile (4.5 lb for skylights only): All Glazed "
                        + "openings are protected at a minimum, with impact resistant coverings or products listed as wind "
                        + "borne debris protection devices in the product approval system of the State of Florida or "
                        + "Miami-Dade County and meet the requirements of one of the following for “Cyclic Pressure and "
                        + "Large Missile Impact” (Level A in the table above): Miami-Dade County PA 201, 202, and 203; "
                        + "FBC TAS 201, 202, and 203; ASTM E 1886 and ASTM E 1996; SSTD 12; skylights only ASTM E 1886 and "
                        + "E 1996; garage doors only ANSI/DASMA 115.",
                        "A.1 All Non-Glazed openings classified as A in the table above, or no Non-Glazed openings exist",
                        "A.2 One or More Non-Glazed openings classified as Level D in the table above, and no Non-Glazed openings classified as Level B, C, N, or X in the table above",
                        "A.3 One or More Non-Glazed Openings is classified as Level B, C, N, or X in the table above"},
                {"B", "B. Exterior Opening Protection - Cyclic Pressure and 4 to 8-lb Large Missile (2-4.5 lb for skylights "
                        + "only): All Glazed openings are protected, at a minimum, with impact resistant coverings or "
                        + "products listed as windborne debris protection devices in the product approval system of the "
                        + "State of Florida or Miami-Dade County and meet the requirements of one of the following for "
                        + "“Cyclic Pressure and Large Missile Impact” (Level B in the table above): ASTM E 1886 and "
                        + "ASTM E 1996 (Large Missile 4.5 lb.); SSTD 12 (Large Missile 4 lb. to 8 lb.); skylights only "
                        + "ASTM E 1886 and ASTM E 1996 (Large Missile 2 to 4.5 lb.)",
                        "B.1 All Non-Glazed openings classified as A or B in the table above, or no Non-Glazed openings exist",
                        "B.2 One or More Non-Glazed openings classified as Level D in the table above, and no Non-Glazed openings classified as Level C, N, or X in the table above",
                        "B.3 One or More Non-Glazed openings is classified as Level C, N, or X in the table above"},
                {"C", "C. Exterior Opening Protection - Wood Structural Panels meeting FBC 2007: All Glazed openings are "
                        + "covered with plywood/OSB meeting the requirements of Table 1609.1.2 of the FBC 2007 (Level C in "
                        + "the table above).",
                        "C.1 All Non-Glazed openings classified as A, B, or C in the table above, or no Non-Glazed openings exist",
                        "C.2 One or More Non-Glazed openings classified as Level D in the table above, and no Non-Glazed openings classified as Level N or X in the table above",
                        "C.3 One or More Non-Glazed openings is classified as Level N or X in the table above"},
                {"N", "N. Exterior Opening Protection (unverified shutter systems with no documentation): All Glazed "
                        + "openings are protected with protective coverings not meeting the requirements of Answer “A”, "
                        + "“B”, or “C” or systems that appear to meet Answer “A” or “B” with no "
                        + "documentation of compliance (Level N in the table above).",
                        "N.1 All Non-Glazed openings classified as Level A, B, C, or N in the table above, or no Non-Glazed openings exist",
                        "N.2 One or More Non-Glazed openings classified as Level D in the table above, and no Non-Glazed openings classified as Level X in the table above",
                        "N.3 One or More Non-Glazed openings is classified as Level X in the table above"}};
        for (String[] cl : classes) {
            box(d, q7code.startsWith(cl[0]), cl[1]);
            for (int i = 2; i < cl.length; i++) sub(d, q7code.equals(cl[i].substring(0, 3)), cl[i]);
        }
        box(d, q7code.equals("X"), "X. None or Some Glazed Openings: One or more Glazed openings classified and Level X "
                + "in the table above.");

        // Certification
        d.y += 8;
        center(d, "MITIGATION INSPECTIONS MUST BE CERTIFIED BY A QUALIFIED INSPECTOR.", 10, Typeface.BOLD);
        center(d, "Section 627.711(2), Florida Statutes, provides a listing of individuals who may sign this form.", 8.5f,
                Typeface.NORMAL);
        d.y += 4;
        grid(d, new String[][]{
                {"Qualified Inspector Name", in.str("wm.cert.inspector")},
                {"License Type", shortLicense(in.str("wm.cert.licenseType"))},
                {"License or Certificate #", in.str("wm.cert.license")},
                {"Inspection Company", in.str("wm.cert.company")}, {"Phone", in.str("wm.cert.phone")}});
        para(d, "Qualified Inspector – I hold an active license as a: (check one)", 9, Typeface.BOLD);
        String lt = in.str("wm.cert.licenseType");
        String[] lic = {
                "Home inspector licensed under Section 468.8314, Florida Statutes who has completed the statutory number of hours of hurricane mitigation training approved by the Construction Industry Licensing Board and completion of a proficiency exam.",
                "Building code inspector certified under Section 468.607, Florida Statutes.",
                "General, building or residential contractor licensed under Section 489.111, Florida Statutes.",
                "Professional engineer licensed under Section 471.015, Florida Statutes.",
                "Professional architect licensed under Section 481.213, Florida Statutes.",
                "Any other individual or entity recognized by the insurer as possessing the necessary qualifications to properly complete a uniform mitigation verification form pursuant to Section 627.711(2), Florida Statutes."};
        for (int i = 0; i < lic.length; i++) box(d, lt.equals(Forms.LICENSE_TYPES[i]), lic[i]);
        para(d, "Individuals other than licensed contractors licensed under Section 489.111, Florida Statutes, or "
                + "professional engineer licensed under Section 471.015, Florida Statutes, must inspect the structures "
                + "personally and not through employees or other persons. Licensees under s.471.015 or s.489.111 may "
                + "authorize a direct employee who possesses the requisite skill, knowledge, and experience to conduct a "
                + "mitigation verification inspection.", 8, Typeface.NORMAL);
        para(d, "I, " + blank(in.str("wm.cert.inspector")) + " am a qualified inspector and I personally performed the "
                + "inspection or (licensed contractors and professional engineers only) I had my employee ("
                + blank(in.str("wm.cert.employee")) + ") perform the inspection and I agree to be responsible for his/her work.",
                9, Typeface.NORMAL);
        signature(d, in, "Qualified Inspector Signature", "wm.cert.signature", in.str("wm.cert.date"));
        para(d, "An individual or entity who knowingly or through gross negligence provides a false or fraudulent "
                + "mitigation verification form is subject to investigation by the Florida Division of Insurance Fraud and "
                + "may be subject to administrative action by the appropriate licensing agency or to criminal prosecution. "
                + "(Section 627.711(4)-(7), Florida Statutes) The Qualified Inspector who certifies this form shall be "
                + "directly liable for the misconduct of employees as if the authorized mitigation inspector personally "
                + "performed the inspection.", 7.5f, Typeface.NORMAL);
        para(d, "Homeowner to complete: I certify that the named Qualified Inspector or his or her employee did perform "
                + "an inspection of the residence identified on this form and that proof of identification was provided to "
                + "me or my Authorized Representative.", 9, Typeface.BOLD);
        signature(d, in, "Signature", "wm.cert.homeowner", in.str("wm.cert.homeownerDate"));
        para(d, "An individual or entity who knowingly provides or utters a false or fraudulent mitigation verification "
                + "form with the intent to obtain or receive a discount on an insurance premium to which the individual or "
                + "entity is not entitled commits a misdemeanor of the first degree. (Section 627.711(7), Florida Statutes)",
                7.5f, Typeface.NORMAL);
        para(d, "The definitions on this form are for inspection purposes only and cannot be used to certify any product "
                + "or construction feature as offering protection from hurricanes.", 7.5f, Typeface.NORMAL);

        // Photo documentation (required for questions 3 through 7)
        boolean anyPhotos = false;
        for (Forms.Section s : Forms.form(Forms.WIND).sections) if (!in.sectionPhotos(s.key).isEmpty()) anyPhotos = true;
        if (anyPhotos) {
            d.newPage();
            center(d, "Photo Documentation", 14, Typeface.BOLD);
            d.y += 4;
            for (Forms.Section s : Forms.form(Forms.WIND).sections) {
                List<String> photos = new ArrayList<>();
                for (String p : in.sectionPhotos(s.key)) if (in.photoExists(p)) photos.add(p);
                if (photos.isEmpty()) continue;
                d.ensure(270); // keep the heading with its first row of photos
                heading(d, s.title);
                d.photos(in, photos);
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    static String letter(String answer) {
        if (answer == null || answer.length() == 0) return "";
        int i = answer.indexOf(' ');
        return i > 0 ? answer.substring(0, i) : answer;
    }

    static String blank(String v) { return v == null || v.length() == 0 ? "__________" : v; }

    static String shortLicense(String v) {
        int i = v.indexOf(" (");
        return i > 0 ? v.substring(0, i) : v;
    }

    static void center(ReportGenerator.Doc d, String s, float size, int style) {
        TextPaint tp = ReportGenerator.Doc.paint(size, style, BLACK);
        d.ensure(size + 6);
        d.c.drawText(s, (ReportGenerator.W - tp.measureText(s)) / 2f, d.y + size, tp);
        d.y += size + 5;
    }

    static void heading(ReportGenerator.Doc d, String s) {
        d.ensure(40);
        d.y += 4;
        d.c.drawText(s, ReportGenerator.M, d.y + 11, ReportGenerator.Doc.paint(11, Typeface.BOLD, BLACK));
        d.y += 16;
    }

    static void para(ReportGenerator.Doc d, String s, float size, int style) {
        d.y += 2;
        d.text(s, size, style, BLACK, ReportGenerator.W - 2 * ReportGenerator.M, ReportGenerator.M);
        d.y += 3;
    }

    static void question(ReportGenerator.Doc d, String number, String text) {
        d.ensure(70);
        d.y += 8;
        d.text(number + "  " + text, 9.5f, Typeface.BOLD, BLACK, ReportGenerator.W - 2 * ReportGenerator.M,
                ReportGenerator.M);
        d.y += 3;
    }

    static void box(ReportGenerator.Doc d, boolean checked, String text) { checkLine(d, checked, text, 0); }

    static void sub(ReportGenerator.Doc d, boolean checked, String text) { checkLine(d, checked, text, 22); }

    static void checkLine(ReportGenerator.Doc d, boolean checked, String text, int indent) {
        int x = ReportGenerator.M + indent;
        int width = ReportGenerator.W - ReportGenerator.M - x - 16;
        StaticLayout l = ReportGenerator.Doc.layout(text, ReportGenerator.Doc.paint(8.5f,
                checked ? Typeface.BOLD : Typeface.NORMAL, checked ? FILL : BLACK), width);
        d.ensure(Math.min(l.getHeight(), 120) + 4);
        drawBox(d, x, d.y + 1.5f, checked);
        d.c.save();
        d.c.translate(x + 15, d.y);
        l.draw(d.c);
        d.c.restore();
        d.y += l.getHeight() + 4;
    }

    static void drawBox(ReportGenerator.Doc d, float x, float y, boolean checked) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(0.8f);
        p.setColor(BLACK);
        d.c.drawRect(x, y, x + 8, y + 8, p);
        if (checked) {
            p.setColor(FILL);
            p.setStrokeWidth(1.4f);
            d.c.drawLine(x + 1.5f, y + 1.5f, x + 6.5f, y + 6.5f, p);
            d.c.drawLine(x + 6.5f, y + 1.5f, x + 1.5f, y + 6.5f, p);
        }
    }

    /** Label: value pairs laid out in two columns. */
    static void grid(ReportGenerator.Doc d, String[][] pairs) {
        int M = ReportGenerator.M, colW = (ReportGenerator.W - 2 * M) / 2;
        TextPaint lp = ReportGenerator.Doc.paint(9, Typeface.NORMAL, BLACK);
        TextPaint vp = ReportGenerator.Doc.paint(9, Typeface.BOLD, FILL);
        Paint line = new Paint();
        line.setColor(0xFF777777);
        for (int i = 0; i < pairs.length; i += 2) {
            d.ensure(18);
            for (int k = 0; k < 2 && i + k < pairs.length; k++) {
                float x = M + k * colW;
                String label = pairs[i + k][0] + ": ";
                d.c.drawText(label, x, d.y + 10, lp);
                float lw = lp.measureText(label);
                String v = pairs[i + k][1];
                float maxW = colW - lw - 10;
                while (v.length() > 1 && vp.measureText(v) > maxW) v = v.substring(0, v.length() - 1);
                d.c.drawText(v, x + lw, d.y + 10, vp);
                d.c.drawLine(x + lw, d.y + 12.5f, x + colW - 10, d.y + 12.5f, line);
            }
            d.y += 17;
        }
    }

    static void coverTable(ReportGenerator.Doc d, Inspection in, int full) {
        int M = ReportGenerator.M;
        float[] cols = {0, 0.34f, 0.52f, 0.72f, 0.88f, 1f};
        String[] head = {"2.1 Roof Covering Type", "Permit Application Date", "FBC or MDC Product Approval #",
                "Year of Original Installation or Replacement", "No Information Provided for Compliance"};
        TextPaint hp = ReportGenerator.Doc.paint(7.5f, Typeface.BOLD, BLACK);
        TextPaint vp = ReportGenerator.Doc.paint(9, Typeface.BOLD, FILL);
        TextPaint tp = ReportGenerator.Doc.paint(9, Typeface.NORMAL, BLACK);
        Paint grid = new Paint();
        grid.setColor(0xFF555555);
        grid.setStyle(Paint.Style.STROKE);
        grid.setStrokeWidth(0.6f);
        float headH = 34;
        d.ensure(headH + 6 * 16 + 10);
        float top = d.y;
        for (int c = 0; c < head.length; c++) {
            float x0 = M + cols[c] * full, x1 = M + cols[c + 1] * full;
            StaticLayout l = ReportGenerator.Doc.layout(head[c], hp, (int) (x1 - x0 - 6));
            d.c.save();
            d.c.translate(x0 + 3, top + 2);
            l.draw(d.c);
            d.c.restore();
        }
        float y = top + headH;
        List<String> types = in.list("wm.cover.types");
        for (int i = 0; i < Forms.WM_COVER_TYPES.length; i++) {
            String t = Forms.WM_COVER_TYPES[i];
            boolean on = types.contains(t);
            drawBox(d, M + 3, y + 3.5f, on);
            String name = (i + 1) + ". " + t;
            if (i == 5) name += in.str("wm.cover.otherDesc").length() > 0 ? ": " + in.str("wm.cover.otherDesc") : "";
            d.c.drawText(name, M + 15, y + 11, on ? vp : tp);
            String[] vals = {in.str(Forms.coverKey(i, "permit")), in.str(Forms.coverKey(i, "approval")),
                    in.str(Forms.coverKey(i, "year"))};
            for (int c = 0; c < 3; c++) {
                float x0 = M + cols[c + 1] * full;
                String v = vals[c].length() > 0 ? vals[c] : "";
                d.c.drawText(v, x0 + 4, y + 11, vp);
            }
            drawBox(d, M + cols[4] * full + (cols[5] - cols[4]) * full / 2 - 4, y + 3.5f,
                    !in.list(Forms.coverKey(i, "noinfo")).isEmpty());
            y += 16;
        }
        d.c.drawRect(M, top, M + full, y, grid);
        d.c.drawLine(M, top + headH, M + full, top + headH, grid);
        for (int c = 1; c < cols.length - 1; c++) d.c.drawLine(M + cols[c] * full, top, M + cols[c] * full, y, grid);
        d.y = y + 6;
    }

    static void openingChart(ReportGenerator.Doc d, Inspection in, int full) {
        int M = ReportGenerator.M;
        float labelW = full * 0.46f, colW = (full - labelW) / 6f;
        String[] colHead = {"Windows or Entry Doors", "Garage Doors", "Skylights", "Glass Block", "Entry Doors",
                "Garage Doors"};
        String[] rows = {"N/A  Not Applicable - there are no openings of this type on the structure",
                "A  Verified cyclic pressure & large missile (9-lb for windows doors / 4.5 lb for skylights)",
                "B  Verified cyclic pressure & large missile (4-8 lb for windows doors / 2 lb for skylights)",
                "C  Verified plywood/OSB meeting Table 1609.1.2 of the FBC 2007",
                "D  Verified Non-Glazed Entry or Garage doors indicating compliance with ASTM E 330, ANSI/DASMA 108, or PA/TAS 202 for wind pressure resistance",
                "N  Opening Protection products that appear to be A or B but are not verified; other protective coverings that cannot be identified as A, B, or C",
                "X  No Windborne Debris Protection"};
        TextPaint hp = ReportGenerator.Doc.paint(7.5f, Typeface.BOLD, BLACK);
        TextPaint rp = ReportGenerator.Doc.paint(7.5f, Typeface.NORMAL, BLACK);
        TextPaint xp = ReportGenerator.Doc.paint(11, Typeface.BOLD, FILL);
        Paint grid = new Paint();
        grid.setColor(0xFF555555);
        grid.setStyle(Paint.Style.STROKE);
        grid.setStrokeWidth(0.6f);
        float[] rowH = new float[rows.length];
        StaticLayout[] rl = new StaticLayout[rows.length];
        float total = 40;
        for (int r = 0; r < rows.length; r++) {
            rl[r] = ReportGenerator.Doc.layout(rows[r], rp, (int) labelW - 6);
            rowH[r] = Math.max(16, rl[r].getHeight() + 6);
            total += rowH[r];
        }
        d.ensure(total + 6);
        float top = d.y;
        // group headers
        Paint fill = new Paint();
        fill.setColor(0xFFE8E8E8);
        d.c.drawRect(M + labelW, top, M + full, top + 12, fill);
        d.c.drawText("Glazed Openings", M + labelW + colW * 2 - hp.measureText("Glazed Openings") / 2, top + 9, hp);
        d.c.drawText("Non-Glazed Openings", M + labelW + colW * 5 - hp.measureText("Non-Glazed Openings") / 2, top + 9, hp);
        StaticLayout intro = ReportGenerator.Doc.layout("Place an “X” in each row to identify all forms of protection "
                + "in use for each opening type.", rp, (int) labelW - 6);
        d.c.save();
        d.c.translate(M + 3, top + 3);
        intro.draw(d.c);
        d.c.restore();
        for (int c = 0; c < 6; c++) {
            StaticLayout l = ReportGenerator.Doc.layout(colHead[c], hp, (int) colW - 4);
            d.c.save();
            d.c.translate(M + labelW + c * colW + 2, top + 14);
            l.draw(d.c);
            d.c.restore();
        }
        float y = top + 40;
        for (int r = 0; r < rows.length; r++) {
            d.c.save();
            d.c.translate(M + 3, y + 3);
            rl[r].draw(d.c);
            d.c.restore();
            for (int c = 0; c < 6; c++) {
                boolean disabled = r == 4 && c < 4; // D only applies to non-glazed
                float x0 = M + labelW + c * colW;
                if (disabled) d.c.drawRect(x0, y, x0 + colW, y + rowH[r], fill);
                if (in.list(Forms.WM_OPENINGS[c][0]).contains(Forms.OPENING[r]))
                    d.c.drawText("X", x0 + colW / 2 - xp.measureText("X") / 2, y + rowH[r] / 2 + 4, xp);
            }
            d.c.drawLine(M, y, M + full, y, grid);
            y += rowH[r];
        }
        d.c.drawRect(M, top, M + full, y, grid);
        d.c.drawLine(M + labelW + 4 * colW, top, M + labelW + 4 * colW, y, grid);
        for (int c = 0; c < 6; c++) d.c.drawLine(M + labelW + c * colW, top + 12, M + labelW + c * colW, y, grid);
        d.c.drawLine(M + labelW, top + 12, M + full, top + 12, grid);
        d.y = y + 8;
    }

    static void signature(ReportGenerator.Doc d, Inspection in, String label, String key, String date) {
        int M = ReportGenerator.M;
        d.ensure(60);
        float y0 = d.y;
        TextPaint lp = ReportGenerator.Doc.paint(9, Typeface.NORMAL, BLACK);
        if (in.has(key)) {
            Bitmap b = Images.decode(new File(new File(in.dir, "signatures"), in.str(key)), 900);
            if (b != null) {
                float h = 38, w = Math.min(200, b.getWidth() * h / b.getHeight());
                d.c.drawBitmap(b, null, new RectF(M + 150, y0, M + 150 + w, y0 + h), new Paint(Paint.FILTER_BITMAP_FLAG));
                b.recycle();
            }
        }
        Paint line = new Paint();
        line.setColor(BLACK);
        d.c.drawText(label + ":", M, y0 + 36, lp);
        d.c.drawLine(M + 150, y0 + 40, M + 360, y0 + 40, line);
        d.c.drawText("Date:", M + 375, y0 + 36, lp);
        d.c.drawText(date, M + 405, y0 + 36, ReportGenerator.Doc.paint(9, Typeface.BOLD, FILL));
        d.c.drawLine(M + 403, y0 + 40, M + 532, y0 + 40, line);
        d.y = y0 + 50;
    }
}
