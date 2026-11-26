package com.inspectorsgadget.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Section list for one module (General, 4-Point or Wind Mitigation). */
public class FormActivity extends Activity {
    public static final String EXTRA_INSPECTION = "insp", EXTRA_FORM = "form";

    private Inspection insp;
    private Forms.Form form;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        insp = Store.get(this, getIntent().getStringExtra(EXTRA_INSPECTION));
        form = Forms.form(getIntent().getStringExtra(EXTRA_FORM));
        if (insp == null || form == null) {
            finish();
            return;
        }
        setTitle(form.title);
        if (getActionBar() != null) {
            getActionBar().setSubtitle(insp.title());
            getActionBar().setBackgroundDrawable(new ColorDrawable(form.color));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (insp == null) return;
        boolean changed = false;
        if (Forms.AGREEMENT.equals(form.id)) {
            Company.applyDefault(this, insp);
            changed = Agreement.syncStatus(insp);
        }
        if (!Forms.GENERAL.equals(form.id) && Logic.prefillForm(insp, form.id) > 0) changed = true;
        if (changed) Store.save(insp);
        build();
        if (Forms.AGREEMENT.equals(form.id) && ESign.pending(insp)) checkESign(false);
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = Ui.vbox(this);
        int pad = Ui.dp(this, 14);
        content.setPadding(pad, pad, pad, Ui.dp(this, 40));
        scroll.addView(content);

        LinearLayout head = Ui.card(this);
        head.addView(Ui.title(this, form.title));
        int[] p = Logic.progress(insp, form.id);
        int findings = Logic.findings(insp, form.id).size();
        LinearLayout stats = Ui.hbox(this);
        stats.setPadding(0, Ui.dp(this, 10), 0, 0);
        stats.addView(Ui.pill(this, p[0] + " / " + p[1] + " answered", form.color, Ui.PRIMARY_LIGHT));
        if (!Forms.AGREEMENT.equals(form.id)) {
            stats.addView(gap());
            stats.addView(Ui.pill(this, Logic.photoCount(insp, form.id) + " photos", Ui.MUTED, 0xFFEDEFF2));
            stats.addView(gap());
            stats.addView(Ui.pill(this, findings + " findings", findings > 0 ? Ui.RED : Ui.MUTED,
                    findings > 0 ? Ui.RED_LIGHT : 0xFFEDEFF2));
        }
        head.addView(stats);

        if (Forms.AGREEMENT.equals(form.id)) {
            int st = Agreement.status(insp);
            int fg = st == Agreement.SIGNED ? 0xFF1B7F3B : st == Agreement.SENT ? Ui.AMBER : Ui.RED;
            int bg = st == Agreement.SIGNED ? 0xFFE3F4E8 : st == Agreement.SENT ? 0xFFFFF4DC : Ui.RED_LIGHT;
            TextView status = Ui.text(this, (st == Agreement.SIGNED ? "✓  " : "•  ") + Agreement.statusText(insp),
                    14, fg, true);
            status.setBackground(Ui.rounded(this, bg, 0, 8));
            status.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8));
            head.addView(status, Ui.matchWrap(this, 10));
        }
        if (!Forms.GENERAL.equals(form.id)) {
            int pre = Logic.prefilledCount(insp, form.id);
            if (pre > 0) {
                TextView info = Ui.pill(this, "↺ " + pre + " prefilled", Ui.TEAL, Ui.TEAL_LIGHT);
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                ilp.topMargin = Ui.dp(this, 10);
                head.addView(info, ilp);
            }
        }
        content.addView(head);

        for (final Forms.Section s : form.sections) {
            LinearLayout card = Ui.card(this);
            LinearLayout top = Ui.hbox(this);
            TextView t = Ui.text(this, s.title, 16, Ui.TEXT, true);
            top.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            top.addView(Ui.text(this, "›", 24, Ui.MUTED, false));
            card.addView(top);

            int[] sp = Logic.progress(insp, s);
            List<Logic.Finding> fs = Logic.findings(insp, s);
            int photos = Logic.sectionPhotoIds(insp, s).size();
            LinearLayout row = Ui.hbox(this);
            row.setPadding(0, Ui.dp(this, 8), 0, 0);
            row.addView(Ui.pill(this, sp[0] + "/" + sp[1], sp[0] == sp[1] && sp[1] > 0 ? 0xFF1B7F3B : form.color,
                    sp[0] == sp[1] && sp[1] > 0 ? 0xFFE3F4E8 : Ui.PRIMARY_LIGHT));
            if (s.photos) {
                row.addView(gap());
                row.addView(Ui.pill(this, "📷 " + photos, Ui.MUTED, 0xFFEDEFF2));
            }
            if (!fs.isEmpty()) {
                row.addView(gap());
                row.addView(Ui.pill(this, fs.size() + " finding" + (fs.size() == 1 ? "" : "s"), Ui.RED, Ui.RED_LIGHT));
            }
            card.addView(row);

            // progress bar
            LinearLayout bar = Ui.hbox(this);
            bar.setBackground(Ui.rounded(this, 0xFFE6EAF0, 0, 3));
            View fill = new View(this);
            fill.setBackground(Ui.rounded(this, form.color, 0, 3));
            float frac = sp[1] == 0 ? 0 : (float) sp[0] / sp[1];
            bar.addView(fill, new LinearLayout.LayoutParams(0, Ui.dp(this, 5), Math.max(0.0001f, frac)));
            bar.addView(new View(this), new LinearLayout.LayoutParams(0, Ui.dp(this, 5), Math.max(0.0001f, 1 - frac)));
            card.addView(bar, Ui.matchWrap(this, 10));

            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(FormActivity.this, SectionActivity.class);
                    i.putExtra(SectionActivity.EXTRA_INSPECTION, insp.id);
                    i.putExtra(SectionActivity.EXTRA_SECTION, s.key);
                    startActivity(i);
                }
            });
            content.addView(card);
        }

        if (Forms.AGREEMENT.equals(form.id)) {
            agreementButtons(content);
            setContentView(scroll);
            return;
        }
        Button pdf = Ui.button(this, "📄  Create " + form.shortTitle + " report (PDF)", form.color, true);
        pdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { ReportGenerator.createAndOpen(FormActivity.this, insp, form.id); }
        });
        content.addView(pdf, Ui.matchWrap(this, 4));
        setContentView(scroll);
    }

    private static final int PICK_SIGNED = 41;

    private void agreementButtons(LinearLayout content) {
        boolean isSigned = Agreement.status(insp) == Agreement.SIGNED;
        if (ESign.pending(insp)) {
            Button check = Ui.button(this, "↻  Check e-signature status", form.color, true);
            check.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { checkESign(true); }
            });
            content.addView(check, Ui.matchWrap(this, 4));
        } else if (!isSigned) {
            Button esign = Ui.button(this, "✍  Send for e-signature", form.color, true);
            esign.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { sendForESignature(); }
            });
            content.addView(esign, Ui.matchWrap(this, 4));
        }
        Button send = Ui.button(this, "✉  Send agreement by email", form.color, false);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { buildAgreement(true); }
        });
        content.addView(send, Ui.matchWrap(this, 8));
        Button prev = Ui.button(this, "📄  Preview agreement (PDF)", form.color, false);
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { buildAgreement(false); }
        });
        content.addView(prev, Ui.matchWrap(this, 8));
        Button attach = Ui.button(this, "📎  Attach signed copy (PDF or photo)", form.color, false);
        attach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("*/*");
                i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "image/*"});
                i.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(Intent.createChooser(i, "Signed agreement"), PICK_SIGNED);
                } catch (Exception e) {
                    Toast.makeText(FormActivity.this, "No file picker found", Toast.LENGTH_LONG).show();
                }
            }
        });
        content.addView(attach, Ui.matchWrap(this, 8));
        final java.io.File signed = Agreement.signedCopy(insp);
        if (signed != null && signed.exists()) {
            Button open = Ui.button(this, "✓  Open signed copy", 0xFF1B7F3B, false);
            open.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReportGenerator.view(FormActivity.this, signed,
                            signed.getName().endsWith(".pdf") ? "application/pdf" : "image/*");
                }
            });
            content.addView(open, Ui.matchWrap(this, 8));
        }
    }

    private boolean exportAllowed() {
        if (Account.canExport(this, insp)) return true;
        PaywallActivity.open(this, PaywallActivity.REASON_LOCKED);
        return false;
    }

    private void sendForESignature() {
        if (!exportAllowed()) return;
        if (!insp.str("info.email").contains("@")) {
            Toast.makeText(this, "No client email on file", Toast.LENGTH_LONG).show();
            return;
        }
        final android.app.ProgressDialog pd = android.app.ProgressDialog.show(this, null, "Sending for e-signature…", true, false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String err = null;
                try {
                    List<ESign.Field> fields = new java.util.ArrayList<>();
                    java.io.File pdf = Agreement.createPdf(FormActivity.this, insp, fields);
                    ESign.sendAgreement(FormActivity.this, insp, pdf, fields);
                } catch (Throwable t) {
                    android.util.Log.e("InspectorsGadget", "e-signature send failed", t);
                    err = t.getMessage() != null ? t.getMessage() : "Could not send for e-signature";
                }
                final String error = err;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            pd.dismiss();
                        } catch (Exception ignored) {
                        }
                        Toast.makeText(FormActivity.this, error != null ? error
                                : "Sent to " + insp.str("info.email"), Toast.LENGTH_LONG).show();
                        build();
                    }
                });
            }
        }).start();
    }

    private void checkESign(final boolean announce) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg = null;
                boolean changed = false;
                try {
                    changed = ESign.refresh(FormActivity.this, insp);
                    if (Agreement.status(insp) == Agreement.SIGNED) msg = "Agreement signed";
                    else if (announce) msg = ESign.statusText(insp);
                } catch (Exception e) {
                    android.util.Log.w("InspectorsGadget", "e-signature status", e);
                    if (announce) msg = "Could not reach the e-signature service";
                }
                final String m = msg;
                final boolean ch = changed;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) return;
                        if (m != null && (announce || ch)) Toast.makeText(FormActivity.this, m, Toast.LENGTH_LONG).show();
                        if (ch) build();
                    }
                });
            }
        }).start();
    }

    private void buildAgreement(final boolean sendIt) {
        if (!exportAllowed()) return;
        if (sendIt && insp.str("info.email").length() == 0)
            Toast.makeText(this, "No client email on file", Toast.LENGTH_LONG).show();
        final android.app.ProgressDialog pd = android.app.ProgressDialog.show(this, null, "Preparing agreement…", true, false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                java.io.File out = null;
                try {
                    out = Agreement.createPdf(FormActivity.this, insp);
                } catch (Throwable t) {
                    android.util.Log.e("InspectorsGadget", "agreement failed", t);
                }
                final java.io.File f = out;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            pd.dismiss();
                        } catch (Exception ignored) {
                        }
                        if (f == null) {
                            Toast.makeText(FormActivity.this, "Could not create the agreement", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (sendIt) Agreement.send(FormActivity.this, insp, f);
                        else ReportGenerator.view(FormActivity.this, f, "application/pdf");
                        build();
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK_SIGNED && res == RESULT_OK && data != null && data.getData() != null) {
            boolean ok = Agreement.attachSignedCopy(this, insp, data.getData());
            Toast.makeText(this, ok ? "Signed agreement saved" : "Could not read that file", Toast.LENGTH_LONG).show();
            build();
        }
    }

    private View gap() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this, 6), 1));
        return v;
    }
}
