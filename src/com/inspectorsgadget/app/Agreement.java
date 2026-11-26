package com.inspectorsgadget.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.TextPaint;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Pre-inspection agreement: status, PDF, sending to the client, and the signed copy. */
public final class Agreement {
    private Agreement() {}

    public static final int NOT_SENT = 0, SENT = 1, SIGNED = 2;

    public static int status(Inspection in) {
        if ("Signed".equals(in.str("pa.signed"))) return SIGNED;
        if (in.str("pa.sentDate").length() > 0) return SENT;
        return NOT_SENT;
    }

    public static String statusText(Inspection in) {
        switch (status(in)) {
            case SIGNED:
                return "Signed" + (in.str("pa.signedDate").length() > 0 ? " " + in.str("pa.signedDate") : "");
            case SENT:
                return "Sent " + in.str("pa.sentDate") + ", waiting for signature";
            default:
                return "Not sent yet";
        }
    }

    static String today() { return new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date()); }

    /** Marks the agreement signed when the client signed on the device. */
    public static boolean syncStatus(Inspection in) {
        if (in.str("pa.clientSig").length() > 0 && !"Signed".equals(in.str("pa.signed"))) {
            in.setUser("pa.signed", "Signed");
            if (in.str("pa.signedHow").length() == 0) in.setUser("pa.signedHow", Forms.SIGNED_HOW[0]);
            if (in.str("pa.signedDate").length() == 0) in.setUser("pa.signedDate", today());
            return true;
        }
        return false;
    }

    public static File signedCopy(Inspection in) {
        String n = in.str("pa.signedCopy");
        return n.length() == 0 ? null : new File(in.dir, n);
    }

    /** Copies a signed agreement (PDF or photo) into the inspection and marks it signed. */
    public static boolean attachSignedCopy(Context c, Inspection in, Uri uri) {
        try {
            String type = c.getContentResolver().getType(uri);
            String ext = type != null && type.contains("pdf") ? ".pdf" : type != null && type.contains("png") ? ".png" : ".jpg";
            String name = "signed_agreement" + ext;
            InputStream is = c.getContentResolver().openInputStream(uri);
            OutputStream os = new FileOutputStream(new File(in.dir, name));
            byte[] buf = new byte[16384];
            int n;
            while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
            os.close();
            is.close();
            in.setUser("pa.signedCopy", name);
            in.setUser("pa.signed", "Signed");
            in.setUser("pa.signedHow", Forms.SIGNED_HOW[1]);
            if (in.str("pa.signedDate").length() == 0) in.setUser("pa.signedDate", today());
            Store.save(in);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Opens the email app with the agreement PDF attached and the client's address filled in. */
    public static void send(Activity a, Inspection in, File pdf) {
        Uri uri = FileShareProvider.uriFor(a, pdf);
        Inspection co = Company.profile(a);
        String company = co.str("co.name");
        Intent s = new Intent(Intent.ACTION_SEND);
        s.setType("application/pdf");
        if (in.str("info.email").length() > 0) s.putExtra(Intent.EXTRA_EMAIL, new String[]{in.str("info.email")});
        s.putExtra(Intent.EXTRA_SUBJECT, "Inspection Agreement - " + in.title());
        s.putExtra(Intent.EXTRA_TEXT, "Hello " + (in.str("info.client").length() > 0 ? in.str("info.client") : "") + ",\n\n"
                + "Attached is the inspection agreement for " + in.title()
                + (in.str("info.date").length() > 0 ? ", scheduled for " + in.str("info.date") : "")
                + (in.str("pa.time").length() > 0 ? " at " + in.str("pa.time") : "") + ".\n\n"
                + "Please review it, sign and initial page one, and reply with the signed copy before the inspection.\n\n"
                + "Thank you,\n" + Forms.joinNonEmpty(in.str("info.inspector"), company) + "\n" + co.str("co.phone"));
        s.putExtra(Intent.EXTRA_STREAM, uri);
        s.setClipData(ClipData.newRawUri(pdf.getName(), uri));
        s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            a.startActivity(Intent.createChooser(s, "Send agreement to client"));
            if (in.str("pa.sentDate").length() == 0 || status(in) == NOT_SENT) in.setUser("pa.sentDate", today());
            if (in.str("pa.signed").length() == 0) in.setUser("pa.signed", "Not signed yet");
            Store.save(in);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(a, "No email app found", Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------------- PDF

    public static File createPdf(Context c, Inspection in) throws Exception {
        Company.applyDefault(c, in);
        Logic.prefillForm(in, Forms.AGREEMENT);
        ReportGenerator.Doc d = newDoc(c, in, false);
        render(d, in, Company.profile(c));
        File dir = new File(c.getCacheDir(), "reports");
        dir.mkdirs();
        String addr = in.str("info.address").replaceAll("[^A-Za-z0-9]+", "_");
        File out = new File(dir, "Agreement_" + (addr.length() > 0 ? addr : in.id) + ".pdf");
        d.finish(out);
        PdfCompressor.compressFile(out, 1000, 70);
        Store.save(in);
        return out;
    }

    public static List<Bitmap> preview(Context c, Inspection in) {
        Company.applyDefault(c, in);
        Logic.prefillForm(in, Forms.AGREEMENT);
        ReportGenerator.Doc d = newDoc(c, in, true);
        render(d, in, Company.profile(c));
        d.finishPage();
        return d.previews;
    }

    static ReportGenerator.Doc newDoc(Context c, Inspection in, boolean preview) {
        String company = Company.profile(c).str("co.name");
        return new ReportGenerator.Doc(Forms.form(Forms.AGREEMENT), preview, true,
                company + " · Inspection Agreement · " + in.title());
    }

    static void render(ReportGenerator.Doc d, Inspection in, Inspection co) {
        int W = ReportGenerator.W, M = ReportGenerator.M, full = W - 2 * M;
        String company = co.str("co.name").length() > 0 ? co.str("co.name") : "COMPANY";
        String coAddr = Forms.joinNonEmpty(co.str("co.address"), co.str("co.city"), co.str("co.state"));

        WindFormRenderer.grid(d, new String[][]{
                {"Date of Inspection", in.str("info.date")}, {"House Faces", in.str("pa.faces")},
                {"Time of Inspection", in.str("pa.time")}, {"Estimated Age", in.str("pa.age")},
                {"Weather", in.str("info.weather")}, {"Temperature", in.str("pa.temp")}});
        d.y += 6;

        File logo = Company.logoFile(in.dir.getParentFile());
        if (logo != null) {
            Bitmap b = Images.decode(logo, 700);
            if (b != null) {
                float h = 54, w = Math.min(220, b.getWidth() * h / b.getHeight());
                h = w * b.getHeight() / b.getWidth();
                d.c.drawBitmap(b, null, new RectF((W - w) / 2f, d.y, (W + w) / 2f, d.y + h), new Paint(Paint.FILTER_BITMAP_FLAG));
                d.y += h + 6;
                b.recycle();
            }
        }
        WindFormRenderer.center(d, company, 13, Typeface.BOLD);
        String contact = Forms.joinNonEmpty(coAddr, co.str("co.phone"), co.str("co.email"), co.str("co.website"));
        if (contact.length() > 0) WindFormRenderer.center(d, contact, 8.5f, Typeface.NORMAL);
        d.y += 4;
        WindFormRenderer.center(d, "INSPECTION AGREEMENT", 15, Typeface.BOLD);
        WindFormRenderer.center(d, "(Please Read Carefully)", 9, Typeface.ITALIC);
        d.y += 4;

        String day = "____", month = "______________", year = "____";
        String dateStr = in.str("pa.sentDate").length() > 0 ? in.str("pa.sentDate") : today();
        int dn = Forms.dateNum(dateStr);
        if (dn > 0) {
            Calendar cal = Calendar.getInstance();
            cal.set(dn / 10000, (dn / 100) % 100 - 1, dn % 100);
            day = ordinal(dn % 100);
            month = new SimpleDateFormat("MMMM", Locale.US).format(cal.getTime());
            year = String.valueOf(dn / 10000);
        }
        WindFormRenderer.para(d, "THIS AGREEMENT is made this " + day + " day of " + month + ", " + year + ", between "
                + company + (coAddr.length() > 0 ? ", located at " + coAddr : "") + " (“COMPANY”), and "
                + WindFormRenderer.blank(in.str("info.client")) + " (“CLIENT”).", 9.5f, Typeface.NORMAL);

        WindFormRenderer.question(d, "1. Address of House to Be Inspected.", "The house to be inspected is located at (the “Premises”):");
        WindFormRenderer.grid(d, new String[][]{{"Address", in.str("info.address")}, {"City", in.str("info.city")},
                {"State", in.str("pa.state")}, {"Zip", in.str("info.zip")}});

        WindFormRenderer.question(d, "2. Services To Be Performed.", "COMPANY agrees to perform the following services for CLIENT (Check All That Apply):");
        List<String> services = in.list("pa.services");
        WindFormRenderer.box(d, services.contains("Home Inspection"), "a. Home Inspection. A limited visual inspection and "
                + "prepare a written report of the apparent condition of the readily accessible installed systems and "
                + "components of the Premises (the house and one attached or detached garage) existing at the time of the "
                + "inspection. Latent and concealed defects and deficiencies are excluded from the inspection. Sheds, "
                + "outbuildings, and other accessory structures are excluded. Installed components included in the "
                + "inspection are structural system/foundation, exterior, roof system, plumbing system, electrical system, "
                + "heating system, cooling system (weather permitting), interior, insulation and ventilation, fireplaces and "
                + "solid fuel burning appliances (if present).");
        WindFormRenderer.box(d, services.contains("Other Inspection"), "b. Other Inspection"
                + (in.str("pa.otherDesc").length() > 0 ? ": " + in.str("pa.otherDesc") : "")
                + ". (If checked, must attach Other Inspection Rider to be effective).");
        WindFormRenderer.para(d, "CLIENT acknowledges that his/her presence at the inspection is optional. CLIENT "
                + "acknowledges that he/she has been advised of the time and date of the inspection.", 9, Typeface.NORMAL);
        List<String> attend = in.list("pa.attend");
        if (!attend.isEmpty()) {
            String who = Forms.joinNonEmpty(attend.toArray(new String[0]));
            if (attend.contains("Real estate agent") && in.str("pa.agentName").length() > 0)
                who += " (agent: " + Forms.joinNonEmpty(in.str("pa.agentName"), in.str("pa.agentBrokerage")) + ")";
            WindFormRenderer.para(d, "Expected to attend the inspection: " + who, 9, Typeface.ITALIC);
        }

        WindFormRenderer.question(d, "3. Inspection Fee.", "CLIENT agrees to pay to COMPANY an inspection fee in the amount of:");
        WindFormRenderer.grid(d, new String[][]{{"Home Inspection $", in.str("pa.feeHome")},
                {"Other Inspection $", in.str("pa.feeOther")}, {"Total Fee $", in.str("pa.feeTotal")}});
        String feeTerms = co.str("co.feeTerms").length() > 0 ? co.str("co.feeTerms") : Company.DEFAULT_FEE_TERMS;
        WindFormRenderer.para(d, feeTerms.replace("{COMPANY}", company), 8.5f, Typeface.NORMAL);
        d.y += 4;
        WindFormRenderer.center(d, "SEE PAGE TWO FOR ADDITIONAL TERMS, CONDITIONS AND LIMITATIONS", 9.5f, Typeface.BOLD);
        WindFormRenderer.para(d, "IN WITNESS WHEREOF the parties have executed this Agreement on the day and year first "
                + "written above.", 9, Typeface.NORMAL);
        signatureBlock(d, in, company);

        // page two: terms
        d.newPage();
        String terms = co.str("co.terms").length() > 0 ? co.str("co.terms") : Company.DEFAULT_TERMS;
        for (String p : terms.replace("{COMPANY}", company).split("\\n\\s*\\n")) {
            String t = p.trim();
            if (t.length() == 0) continue;
            WindFormRenderer.para(d, t, 8.5f, Typeface.NORMAL);
            d.y += 2;
        }
        d.y += 6;
        WindFormRenderer.para(d, "CLIENT initials: " + WindFormRenderer.blank(in.str("pa.clientInitials")), 9, Typeface.BOLD);
    }

    static String ordinal(int n) {
        String suf = (n % 100 >= 11 && n % 100 <= 13) ? "th" : n % 10 == 1 ? "st" : n % 10 == 2 ? "nd" : n % 10 == 3 ? "rd" : "th";
        return n + suf;
    }

    static void signatureBlock(ReportGenerator.Doc d, Inspection in, String company) {
        int M = ReportGenerator.M, W = ReportGenerator.W;
        d.ensure(150);
        float colW = (W - 2 * M) / 2f;
        float y0 = d.y + 4;
        TextPaint bold = ReportGenerator.Doc.paint(9.5f, Typeface.BOLD, WindFormRenderer.BLACK);
        TextPaint norm = ReportGenerator.Doc.paint(9, Typeface.NORMAL, WindFormRenderer.BLACK);
        d.c.drawText(company + ":", M, y0 + 10, bold);
        d.c.drawText("CLIENT:", M + colW + 10, y0 + 10, bold);
        drawSig(d, in, "pa.inspectorSig", M + 20, y0 + 16);
        drawSig(d, in, "pa.clientSig", M + colW + 10, y0 + 16);
        Paint line = new Paint();
        line.setColor(WindFormRenderer.BLACK);
        d.c.drawText("By:", M, y0 + 56, norm);
        d.c.drawLine(M + 18, y0 + 58, M + colW - 10, y0 + 58, line);
        d.c.drawLine(M + colW + 10, y0 + 58, W - M, y0 + 58, line);
        d.c.drawText(in.str("info.inspector"), M + 18, y0 + 72, norm);
        String lic = Forms.joinNonEmpty(in.str("info.inspectorTitle"), in.str("info.license"));
        d.c.drawText(lic, M + 18, y0 + 84, norm);
        if (in.str("info.issuer").length() > 0) d.c.drawText("Issued by " + in.str("info.issuer"), M + 18, y0 + 96, norm);
        d.c.drawText(in.str("info.client"), M + colW + 10, y0 + 72, norm);
        d.y = y0 + 104;

        // release consent
        List<String> rel = in.list("pa.release");
        String ini = in.str("pa.clientInitials");
        float x = M + colW + 10;
        float width = W - M - x;
        d.c.save();
        d.c.translate(x, d.y);
        android.text.StaticLayout l = ReportGenerator.Doc.layout("CLIENT’s initials " + WindFormRenderer.blank(ini)
                + " indicate agreement to allow COMPANY to release a copy of the report to:", norm, (int) width);
        l.draw(d.c);
        d.c.restore();
        d.y += l.getHeight() + 4;
        float bx = x;
        for (String r : Forms.RELEASE) {
            WindFormRenderer.drawBox(d, bx, d.y, rel.contains(r));
            String label = r + (r.equals("Other") && in.str("pa.releaseOther").length() > 0 ? ": " + in.str("pa.releaseOther") : "");
            d.c.drawText(label, bx + 11, d.y + 8, norm);
            bx += 11 + norm.measureText(label) + 10;
            if (bx > W - M - 60) {
                bx = x;
                d.y += 14;
            }
        }
        d.y += 18;
    }

    static void drawSig(ReportGenerator.Doc d, Inspection in, String key, float x, float y) {
        if (!in.has(key)) return;
        Bitmap b = Images.decode(new File(new File(in.dir, "signatures"), in.str(key)), 900);
        if (b == null) return;
        float h = 38, w = Math.min(200, b.getWidth() * h / b.getHeight());
        d.c.drawBitmap(b, null, new RectF(x, y, x + w, y + h), new Paint(Paint.FILTER_BITMAP_FLAG));
        b.recycle();
    }
}
