package com.inspectorsgadget.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Renders a module (General, 4-Point, Wind Mitigation) to a Letter-size PDF. */
public final class ReportGenerator {
    private ReportGenerator() {}

    static final int W = 612, H = 792, M = 40;

    /** Export sizes: photo pixel cap and JPEG quality used inside the PDF. */
    public static final int SIZE_EMAIL = 0, SIZE_FULL = 1;
    static final String[] SIZE_LABELS = {"Email size (recommended)", "Full quality"};
    static final String[] SIZE_HINTS = {"Photos compressed so the PDF fits in an email",
            "Sharper photos, larger file"};
    static final int[] PHOTO_PX = {1000, 1800};
    static final int[] JPEG_Q = {60, 85};

    /** Asks which size to export, remembering the last choice. */
    public static void createAndOpen(final Activity a, final Inspection in, final String formId) {
        final android.content.SharedPreferences prefs = a.getSharedPreferences("export", 0);
        int last = prefs.getInt("size", SIZE_EMAIL);
        String[] items = {SIZE_LABELS[0] + "\n" + SIZE_HINTS[0], SIZE_LABELS[1] + "\n" + SIZE_HINTS[1]};
        new AlertDialog.Builder(a).setTitle("Report size")
                .setSingleChoiceItems(items, last, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        prefs.edit().putInt("size", which).apply();
                        d.dismiss();
                        createAndOpen(a, in, formId, which);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    public static void createAndOpen(final Activity a, final Inspection in, final String formId, final int size) {
        final ProgressDialog pd = ProgressDialog.show(a, null, "Building report…", true, false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                File out = null;
                String err = null;
                try {
                    out = create(a, in, formId, size);
                } catch (Throwable t) {
                    err = t.toString();
                }
                final File f = out;
                final String e = err;
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            pd.dismiss();
                        } catch (Exception ignored) {
                        }
                        if (f == null) {
                            Toast.makeText(a, "Report failed: " + e, Toast.LENGTH_LONG).show();
                            return;
                        }
                        offer(a, f);
                    }
                });
            }
        }).start();
    }

    private static void offer(final Activity a, final File f) {
        final Uri uri = FileShareProvider.uriFor(a, f);
        new AlertDialog.Builder(a).setTitle("Report ready")
                .setMessage(f.getName() + "\n" + sizeText(f.length()))
                .setPositiveButton("Open", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setDataAndType(uri, "application/pdf");
                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try {
                            a.startActivity(i);
                        } catch (ActivityNotFoundException e) {
                            share(a, uri, f.getName());
                        }
                    }
                })
                .setNeutralButton("Share", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) { share(a, uri, f.getName()); }
                })
                .setNegativeButton("Close", null).show();
    }

    /** Opens a file in another app, falling back to the share sheet. */
    static void view(Activity a, File f, String mime) {
        Uri uri = FileShareProvider.uriFor(a, f);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, mime);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            a.startActivity(i);
        } catch (ActivityNotFoundException e) {
            share(a, uri, f.getName());
        }
    }

    static String sizeText(long bytes) {
        if (bytes >= 1024 * 1024) return String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f));
        return (bytes / 1024) + " KB";
    }

    private static void share(Activity a, Uri uri, String name) {
        Intent s = new Intent(Intent.ACTION_SEND);
        s.setType("application/pdf");
        s.putExtra(Intent.EXTRA_STREAM, uri);
        s.putExtra(Intent.EXTRA_SUBJECT, name);
        s.setClipData(ClipData.newRawUri(name, uri));
        s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            a.startActivity(Intent.createChooser(s, "Share report"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(a, "No app available to share PDFs", Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------ rendering

    public static File create(android.content.Context ctx, Inspection in, String formId) throws Exception {
        return create(ctx, in, formId, SIZE_EMAIL);
    }

    public static File create(android.content.Context ctx, Inspection in, String formId, int size) throws Exception {
        Forms.Form form = Forms.form(formId);
        Company.applyDefault(ctx, in); // latest inspector and company details
        if (!Forms.GENERAL.equals(formId)) Logic.prefillForm(in, formId);
        Store.save(in);
        Doc d = newDoc(in, formId, false);
        d.photoPx = PHOTO_PX[size];
        render(d, in, formId);
        File dir = new File(ctx.getCacheDir(), "reports");
        dir.mkdirs();
        String addr = in.str("info.address").replaceAll("[^A-Za-z0-9]+", "_");
        if (addr.length() > 40) addr = addr.substring(0, 40);
        File out = new File(dir, form.shortTitle.replaceAll("[^A-Za-z0-9]+", "") + "_" + (addr.length() > 0 ? addr : in.id) + ".pdf");
        d.finish(out);
        // PdfDocument stores photos losslessly; re-encode them as JPEG so the file can be emailed.
        PdfCompressor.compressFile(out, PHOTO_PX[size], JPEG_Q[size]);
        return out;
    }

    /** Renders the report to bitmaps (2x scale) instead of a PDF. Used for previews and tests. */
    public static List<Bitmap> preview(Inspection in, String formId) {
        if (!Forms.GENERAL.equals(formId)) Logic.prefillForm(in, formId);
        Doc d = newDoc(in, formId, true);
        render(d, in, formId);
        d.finishPage();
        return d.previews;
    }

    static Doc newDoc(Inspection in, String formId, boolean preview) {
        if (Forms.WIND.equals(formId)) {
            String addr = Forms.joinNonEmpty(in.str("wm.owner.address"), in.str("wm.owner.city"), in.str("wm.owner.zip"));
            return new Doc(Forms.form(formId), preview, in.str("wm.cert.initials"), addr);
        }
        return new Doc(Forms.form(formId), preview, null, null);
    }

    static void render(Doc d, Inspection in, String formId) {
        if (Forms.WIND.equals(formId)) {
            WindFormRenderer.render(d, in);
            return;
        }
        Forms.Form form = Forms.form(formId);

        // Cover block
        d.ensure(140);
        File logo = in.dir == null ? null : Company.logoFile(in.dir.getParentFile());
        float titleW = W - 2 * M;
        if (logo != null) {
            Bitmap b = Images.decode(logo, 600);
            if (b != null) {
                float h = 46, w = Math.min(150, b.getWidth() * h / b.getHeight());
                h = w * b.getHeight() / b.getWidth();
                d.c.drawBitmap(b, null, new android.graphics.RectF(W - M - w, d.y, W - M, d.y + h),
                        new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG));
                b.recycle();
                titleW -= w + 10;
            }
        }
        d.text(form.title, 20, Typeface.BOLD, form.color, (int) titleW, M);
        d.y += 4;
        d.text("Prepared " + new SimpleDateFormat("MMMM d, yyyy", Locale.US).format(new Date())
                + (in.str("info.inspector").length() > 0 ? " by " + in.str("info.inspector") : ""), 10,
                Typeface.NORMAL, 0xFF5F6B7A, W - 2 * M, M);
        d.y += 10;
        String[][] info = {
                {"Insured / client", in.str("info.client")},
                {"Address inspected", Forms.joinNonEmpty(in.str("info.address"), in.str("info.city"), in.str("info.zip"))},
                {"Application / policy #", in.str("info.policy")},
                {"Insurance company", in.str("info.insurer")},
                {"Actual year built", in.str("info.yearBuilt")},
                {"Stories", in.str("info.stories")},
                {"Date inspected", in.str("info.date")},
                {"Inspector", Forms.joinNonEmpty(in.str("info.inspector"), in.str("info.inspectorTitle"))},
                {"License", Forms.joinNonEmpty(in.str("info.licenseType"), in.str("info.license").length() > 0
                        ? "# " + in.str("info.license") : "")},
                {"Issued by", Forms.joinNonEmpty(in.str("info.issuer"), in.str("info.licenseExp").length() > 0
                        ? "expires " + in.str("info.licenseExp") : "")},
                {"Company", Forms.joinNonEmpty(in.str("info.company"), in.str("info.companyLicense").length() > 0
                        ? "Lic. " + in.str("info.companyLicense") : "")},
                {"Company contact", Forms.joinNonEmpty(in.str("info.companyAddress"), in.str("info.companyPhone"),
                        in.str("info.companyEmail"))},
        };
        d.box(0xFFF3F5F8);
        for (String[] r : info) d.row(r[0], r[1].length() > 0 ? r[1] : "—", false);
        d.endBox();

        // Findings summary
        List<Logic.Finding> findings = Logic.findings(in, formId);
        d.sectionBar(findings.isEmpty() ? "Summary: no deficiencies flagged" : "Summary of findings (" + findings.size() + ")",
                findings.isEmpty() ? 0xFF1B7F3B : Ui.RED);
        for (Logic.Finding f : findings) {
            d.bullet(f.section.title + " — " + f.field.label + ": " + f.value, Ui.RED);
        }
        d.y += 6;

        boolean skipEmpty = Forms.GENERAL.equals(formId);
        for (Forms.Section s : form.sections) {
            d.sectionBar(s.title, form.color);
            if (s.subtitle != null && !Forms.GENERAL.equals(formId)) {
                d.text(s.subtitle, 9, Typeface.ITALIC, 0xFF5F6B7A, W - 2 * M, M);
                d.y += 4;
            }
            boolean any = false;
            for (Forms.Field f : s.fields) {
                if (f.type == Forms.HEADER) {
                    d.ensure(30);
                    d.y += 4;
                    d.text(f.label, 11, Typeface.BOLD, form.color, W - 2 * M, M);
                    d.y += 2;
                    continue;
                }
                if (f.type == Forms.NOTES) {
                    String v = in.str(f.key);
                    if (v.length() == 0 && skipEmpty) continue;
                    d.ensure(40);
                    d.text(f.label, 10, Typeface.BOLD, 0xFF1B1F24, W - 2 * M, M);
                    d.text(v.length() > 0 ? v : "—", 10, Typeface.NORMAL, 0xFF1B1F24, W - 2 * M, M);
                    d.y += 6;
                    any = true;
                    continue;
                }
                if (f.type == Forms.SIGNATURE) {
                    d.signature(f.label, new File(new File(in.dir, "signatures"), in.str(f.key)), in.has(f.key));
                    any = true;
                    continue;
                }
                String v = in.str(f.key);
                if (v.length() == 0 && skipEmpty) continue;
                d.row(f.label, v.length() > 0 ? v : "—", Logic.isFinding(in, f));
                any = true;
            }
            if (!any) d.text("Not inspected / no entries.", 10, Typeface.ITALIC, 0xFF5F6B7A, W - 2 * M, M);

            List<String> photos = new ArrayList<>();
            for (String p : in.sectionPhotos(s.key)) if (in.photoExists(p)) photos.add(p);
            if (!photos.isEmpty()) d.photos(in, photos);
            d.y += 8;
        }

        if (Forms.WIND.equals(formId)) {
            d.ensure(60);
            d.text("Based on the Florida OIR-B1-1802 Uniform Mitigation Verification Inspection form. "
                    + "Answers must be verified by the licensed inspector before submission to an insurer.",
                    8, Typeface.ITALIC, 0xFF5F6B7A, W - 2 * M, M);
        }
        if (Forms.GENERAL.equals(formId)) inspectedBy(d, in, form);
        if (Forms.FOUR_POINT.equals(formId)) {
            d.ensure(60);
            d.text("Underwriting will rely on the information in this form to determine insurability. It is not a "
                    + "warranty or assurance of the suitability, fitness or longevity of any of the systems inspected. "
                    + "A Florida-licensed inspector must complete, sign and date this form.",
                    8, Typeface.ITALIC, 0xFF5F6B7A, W - 2 * M, M);
        }

    }

    /** Sign-off with the conducting inspector's license details and saved signature. */
    static void inspectedBy(Doc d, Inspection in, Forms.Form form) {
        d.ensure(170);
        d.sectionBar("Inspected by", form.color);
        d.row("Inspector", Forms.joinNonEmpty(in.str("info.inspector"), in.str("info.inspectorTitle")), false);
        d.row("License", Forms.joinNonEmpty(in.str("info.licenseType"), in.str("info.license")), false);
        d.row("Issuing body", in.str("info.issuer").length() > 0 ? in.str("info.issuer") : "—", false);
        if (in.str("info.licenseExp").length() > 0) d.row("License expires", in.str("info.licenseExp"), false);
        String contact = Forms.joinNonEmpty(in.str("info.inspectorPhone"), in.str("info.inspectorEmail"));
        if (contact.length() > 0) d.row("Contact", contact, false);
        d.row("Company", Forms.joinNonEmpty(in.str("info.company"), in.str("info.companyAddress")), false);
        if (in.has("info.inspectorSig"))
            d.signature("Inspector signature", new File(new File(in.dir, "signatures"), in.str("info.inspectorSig")), true);
    }

    /** Small layout engine on top of PdfDocument with automatic page breaks. */
    static class Doc {
        final boolean preview;
        int photoPx = 900;
        final PdfDocument pdf;
        final List<Bitmap> previews = new ArrayList<>();
        final Forms.Form form;
        PdfDocument.Page page;
        Canvas c;
        float y;
        int pageNo;
        Integer boxColor;
        float boxTop;

        /** Non-null when rendering the state form layout (OIR-B1-1802 page header/footer). */
        final String stateInitials, stateAddress;

        /** Plain pages with no branded header (used for the client agreement). */
        boolean plain;
        String plainFooter = "";

        Doc(Forms.Form form, boolean preview, boolean plain, String footer) {
            this(form, preview, null, null, plain, footer);
        }

        Doc(Forms.Form form, boolean preview, String stateInitials, String stateAddress) {
            this(form, preview, stateInitials, stateAddress, false, "");
        }

        Doc(Forms.Form form, boolean preview, String stateInitials, String stateAddress, boolean plain, String footer) {
            this.plain = plain;
            this.plainFooter = footer;
            this.form = form;
            this.preview = preview;
            this.stateInitials = stateInitials;
            this.stateAddress = stateAddress;
            this.pdf = preview ? null : new PdfDocument();
            newPage();
        }

        void newPage() {
            if (c != null) finishPage();
            pageNo++;
            if (preview) {
                Bitmap b = Bitmap.createBitmap(W * 2, H * 2, Bitmap.Config.ARGB_8888);
                b.eraseColor(Color.WHITE);
                previews.add(b);
                c = new Canvas(b);
                c.scale(2, 2);
            } else {
                page = pdf.startPage(new PdfDocument.PageInfo.Builder(W, H, pageNo).create());
                c = page.getCanvas();
            }
            if (plain) {
                y = 40;
                return;
            }
            if (stateAddress != null) {
                TextPaint hp = paint(9, Typeface.NORMAL, Color.BLACK);
                String ini = stateInitials == null || stateInitials.length() == 0 ? "_____" : stateInitials;
                c.drawText("Inspectors Initials: " + ini + "     Property Address: " + stateAddress, M, 28, hp);
                Paint ln = new Paint();
                ln.setColor(0xFF000000);
                c.drawLine(M, 34, W - M, 34, ln);
                y = 48;
                if (boxColor != null) boxTop = y;
                return;
            }
            Paint bar = new Paint();
            bar.setColor(form.color);
            c.drawRect(0, 0, W, 26, bar);
            TextPaint tp = paint(9, Typeface.BOLD, Color.WHITE);
            c.drawText("INSPECTOR'S GADGET", M, 17, tp);
            float tw = tp.measureText(form.title);
            c.drawText(form.title, W - M - tw, 17, tp);
            y = 46;
            if (boxColor != null) boxTop = y;
        }

        void finishPage() {
            TextPaint tp = paint(8, Typeface.NORMAL, 0xFF8A94A3);
            String s = "Page " + pageNo;
            c.drawText(s, W - M - tp.measureText(s), H - 20, tp);
            if (plain) {
                c.drawText(plainFooter, M, H - 20, tp);
            } else if (stateAddress != null) {
                TextPaint fp = paint(7f, Typeface.NORMAL, 0xFF333333);
                c.drawText("*This verification form is valid for up to five (5) years provided no material changes have been made to the structure or inaccuracies found on the form.", M, H - 32, fp);
                c.drawText("OIR-B1-1802 (Rev. 01/12) Adopted by Rule 69O-170.0155", M, H - 20, tp);
            } else
                c.drawText("Generated with Inspector's Gadget (demo build)", M, H - 20, tp);
            if (!preview) pdf.finishPage(page);
        }

        void ensure(float h) {
            if (y + h > H - 44) {
                boolean hadBox = boxColor != null;
                newPage();
                if (hadBox) boxTop = y;
            }
        }

        static TextPaint paint(float size, int style, int color) {
            TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            tp.setTextSize(size);
            tp.setColor(color);
            tp.setTypeface(Typeface.create(Typeface.SANS_SERIF, style));
            return tp;
        }

        static StaticLayout layout(String s, TextPaint tp, int width) {
            return StaticLayout.Builder.obtain(s, 0, s.length(), tp, Math.max(10, width))
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(1f, 1f).build();
        }

        void text(String s, float size, int style, int color, int width, float x) {
            StaticLayout l = layout(s, paint(size, style, color), width);
            // split long paragraphs across pages line by line
            int line = 0;
            while (line < l.getLineCount()) {
                float lineH = l.getLineBottom(line) - l.getLineTop(line);
                ensure(lineH);
                int start = line;
                float avail = H - 44 - y;
                float used = 0;
                while (line < l.getLineCount()) {
                    float lh = l.getLineBottom(line) - l.getLineTop(line);
                    if (used + lh > avail && line > start) break;
                    used += lh;
                    line++;
                }
                c.save();
                c.translate(x, y);
                c.clipRect(0, l.getLineTop(start), width, l.getLineTop(start) + used);
                c.translate(0, -l.getLineTop(start));
                l.draw(c);
                c.restore();
                y += used;
            }
        }

        void sectionBar(String title, int color) {
            ensure(60);
            y += 6;
            Paint p = new Paint();
            p.setColor(color);
            c.drawRoundRect(new RectF(M, y, W - M, y + 22), 4, 4, p);
            c.drawText(title, M + 8, y + 15, paint(11, Typeface.BOLD, Color.WHITE));
            y += 30;
        }

        void bullet(String s, int color) {
            StaticLayout l = layout(s, paint(10, Typeface.NORMAL, 0xFF1B1F24), W - 2 * M - 14);
            ensure(l.getHeight() + 3);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(color);
            c.drawCircle(M + 4, y + 7, 2.6f, p);
            c.save();
            c.translate(M + 14, y);
            l.draw(c);
            c.restore();
            y += l.getHeight() + 3;
        }

        void box(int color) {
            boxColor = color;
            boxTop = y;
        }

        void endBox() {
            boxColor = null;
            y += 6;
        }

        void row(String label, String value, boolean finding) {
            int lw = (int) ((W - 2 * M) * 0.42f), vw = (W - 2 * M) - lw - 10;
            StaticLayout ll = layout(label, paint(10, Typeface.NORMAL, 0xFF5F6B7A), lw - 8);
            StaticLayout vl = layout(value, paint(10, finding ? Typeface.BOLD : Typeface.NORMAL,
                    finding ? Ui.RED : 0xFF1B1F24), vw);
            float h = Math.max(ll.getHeight(), vl.getHeight()) + 6;
            ensure(h);
            if (boxColor != null) {
                Paint bg = new Paint();
                bg.setColor(boxColor);
                c.drawRect(M, y - 2, W - M, y + h - 2, bg);
            }
            if (finding) {
                Paint bg = new Paint();
                bg.setColor(0xFFFDE7E7);
                c.drawRect(M + lw, y - 2, W - M, y + h - 3, bg);
            }
            c.save();
            c.translate(M + 6, y + 1);
            ll.draw(c);
            c.restore();
            c.save();
            c.translate(M + lw + 6, y + 1);
            vl.draw(c);
            c.restore();
            Paint line = new Paint();
            line.setColor(0xFFE3E7ED);
            c.drawLine(M, y + h - 2, W - M, y + h - 2, line);
            y += h;
        }

        void signature(String label, File f, boolean has) {
            ensure(90);
            c.drawText(label, M, y + 10, paint(10, Typeface.BOLD, 0xFF1B1F24));
            y += 16;
            Bitmap b = has ? Images.decode(f, 900) : null;
            RectF r = new RectF(M, y, M + 220, y + 70);
            if (b != null) {
                float s = Math.min(r.width() / b.getWidth(), r.height() / b.getHeight());
                float w = b.getWidth() * s, h = b.getHeight() * s;
                c.drawBitmap(b, null, new RectF(r.left, r.top, r.left + w, r.top + h),
                        new Paint(Paint.FILTER_BITMAP_FLAG));
                b.recycle();
            }
            Paint line = new Paint();
            line.setColor(0xFF1B1F24);
            c.drawLine(M, y + 72, M + 260, y + 72, line);
            y += 84;
        }

        void photos(Inspection in, List<String> ids) {
            float gap = 12;
            float cw = (W - 2 * M - gap) / 2f;
            for (int i = 0; i < ids.size(); i += 2) {
                Bitmap[] bm = new Bitmap[2];
                StaticLayout[] caps = new StaticLayout[2];
                float rowH = 0;
                for (int k = 0; k < 2 && i + k < ids.size(); k++) {
                    String pid = ids.get(i + k);
                    bm[k] = Images.decode(in.displayFile(pid), photoPx);
                    String cap = in.caption(pid);
                    if (in.meta(pid).optString("annotated", "").length() > 0)
                        cap = (cap.length() > 0 ? cap + " " : "") + "(marked up)";
                    caps[k] = layout(cap.length() > 0 ? cap : "Photo", paint(9, Typeface.NORMAL, 0xFF3A4350), (int) cw);
                    float ih = bm[k] == null ? 40 : Math.min(cw * bm[k].getHeight() / bm[k].getWidth(), 230);
                    rowH = Math.max(rowH, ih + caps[k].getHeight() + 8);
                }
                ensure(rowH + 6);
                for (int k = 0; k < 2; k++) {
                    if (caps[k] == null) continue;
                    float x = M + k * (cw + gap);
                    float ih = 40;
                    if (bm[k] != null) {
                        float s = Math.min(cw / bm[k].getWidth(), 230f / bm[k].getHeight());
                        float w = bm[k].getWidth() * s;
                        ih = bm[k].getHeight() * s;
                        c.drawBitmap(bm[k], null, new RectF(x, y, x + w, y + ih), new Paint(Paint.FILTER_BITMAP_FLAG));
                        bm[k].recycle();
                    }
                    c.save();
                    c.translate(x, y + ih + 3);
                    caps[k].draw(c);
                    c.restore();
                }
                y += rowH + 6;
            }
        }

        void finish(File out) throws Exception {
            finishPage();
            FileOutputStream os = new FileOutputStream(out);
            pdf.writeTo(os);
            os.close();
            pdf.close();
        }
    }
}
