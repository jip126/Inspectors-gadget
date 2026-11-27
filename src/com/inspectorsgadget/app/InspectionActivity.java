package com.inspectorsgadget.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Hub for one property: the three inspection modules and their reports. */
public class InspectionActivity extends Activity {
    public static final String EXTRA_INSPECTION = "insp";
    private Inspection insp;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        insp = Store.get(this, getIntent().getStringExtra(EXTRA_INSPECTION));
        if (insp == null) {
            finish();
            return;
        }
        if (!Account.canOpen(this, insp)) {
            PaywallActivity.open(this, PaywallActivity.REASON_TRIAL_USED);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (insp == null) return;
        Company.applyDefault(this, insp);
        boolean changed = true; // inspector/company details may have changed
        changed |= Agreement.syncStatus(insp);
        Logic.prefillForm(insp, Forms.AGREEMENT);
        for (String t : Forms.types(insp))
            if (!Forms.GENERAL.equals(t)) changed |= Logic.prefillForm(insp, t) > 0;
        if (changed) Store.save(insp);
        setTitle(insp.title());
        build();
        refreshESign();
    }

    /** Picks up a remote signature that completed while the app was closed. */
    private void refreshESign() {
        if (!ESign.pending(insp)) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean changed = false;
                try {
                    changed = ESign.refresh(InspectionActivity.this, insp);
                } catch (Exception e) {
                    android.util.Log.w("InspectorsGadget", "e-signature status", e);
                }
                if (changed) runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) build();
                    }
                });
            }
        }).start();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = Ui.vbox(this);
        int pad = Ui.dp(this, 14);
        content.setPadding(pad, pad, pad, Ui.dp(this, 40));
        scroll.addView(content);

        // Property card
        LinearLayout head = Ui.card(this);
        head.setBackground(Ui.rounded(this, Ui.PRIMARY, 0, 12));
        head.addView(Ui.text(this, insp.title(), 20, Color.WHITE, true));
        String city = Forms.joinNonEmpty(insp.str("info.city"), insp.str("info.zip"));
        if (city.length() > 0) head.addView(Ui.text(this, city, 14, 0xFFCFD8E3, false));
        String line = Forms.joinNonEmpty(
                insp.str("info.client").length() > 0 ? "Client: " + insp.str("info.client") : "",
                insp.str("info.yearBuilt").length() > 0 ? "Built " + insp.str("info.yearBuilt") : "",
                insp.str("info.date"));
        if (line.length() > 0) head.addView(Ui.text(this, line, 14, 0xFFCFD8E3, false), Ui.matchWrap(this, 4));
        String who = Forms.joinNonEmpty(insp.str("info.inspector"),
                insp.str("info.license").length() > 0 ? "Lic. " + insp.str("info.license") : "",
                insp.str("info.issuer"));
        if (who.length() > 0) head.addView(Ui.text(this, "Inspector: " + who, 13, 0xFFC9A04A, false), Ui.matchWrap(this, 4));
        Button edit = Ui.button(this, "Property & client details", Color.WHITE, false);
        edit.setTextColor(Ui.PRIMARY);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openSection("info"); }
        });
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        elp.topMargin = Ui.dp(this, 10);
        head.addView(edit, elp);
        content.addView(head);

        content.addView(moduleCard(Forms.form(Forms.AGREEMENT), "1", null));
        java.util.List<String> types = Forms.types(insp);
        int n = 2;
        for (String t : types) content.addView(moduleCard(Forms.form(t), String.valueOf(n++),
                Forms.GENERAL.equals(t) ? null : ""));
        if (!addableTypes().isEmpty()) {
            Button add = Ui.button(this, "+  Add inspection type", Ui.PRIMARY, false);
            add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { addType(); }
            });
            content.addView(add, Ui.matchWrap(this, 0));
            content.addView(Ui.spacer(this, 12));
        }

        LinearLayout reports = Ui.card(this);
        reports.addView(Ui.text(this, "Reports", 16, Ui.TEXT, true));
        java.util.List<Forms.Form> reportForms = new java.util.ArrayList<>();
        reportForms.add(Forms.form(Forms.AGREEMENT));
        for (String t : types) reportForms.add(Forms.form(t));
        for (final Forms.Form f : reportForms) {
            Button rb = Ui.button(this, "📄  " + f.title, f.color, false);
            rb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Forms.AGREEMENT.equals(f.id)) openForm(f.id);
                    else ReportGenerator.createAndOpen(InspectionActivity.this, insp, f.id);
                }
            });
            reports.addView(rb, Ui.matchWrap(this, 8));
        }
        content.addView(reports);
        setContentView(scroll);
    }

    private View moduleCard(final Forms.Form f, String number, String note) {
        LinearLayout card = Ui.card(this);
        LinearLayout top = Ui.hbox(this);
        TextView num = Ui.text(this, number, 18, Color.WHITE, true);
        num.setGravity(android.view.Gravity.CENTER);
        num.setBackground(Ui.rounded(this, f.color, 0, 18));
        top.addView(num, new LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36)));
        LinearLayout titles = Ui.vbox(this);
        titles.setPadding(Ui.dp(this, 12), 0, 0, 0);
        titles.addView(Ui.text(this, f.title, 17, Ui.TEXT, true));
        top.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(Ui.text(this, "›", 26, Ui.MUTED, false));
        card.addView(top);

        if (Forms.AGREEMENT.equals(f.id)) {
            int st = Agreement.status(insp);
            card.addView(Ui.pill(this, (st == Agreement.SIGNED ? "✓ " : "") + Agreement.statusText(insp),
                    st == Agreement.SIGNED ? 0xFF1B7F3B : st == Agreement.SENT ? Ui.AMBER : Ui.RED,
                    st == Agreement.SIGNED ? 0xFFE3F4E8 : st == Agreement.SENT ? 0xFFFFF4DC : Ui.RED_LIGHT),
                    wrap(10));
            java.util.List<String> att = insp.list("pa.attend");
            if (!att.isEmpty()) card.addView(Ui.muted(this, "Attending: " + android.text.TextUtils.join(", ", att)),
                    Ui.matchWrap(this, 6));
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { openForm(f.id); }
            });
            return card;
        }
        int[] p = Logic.progress(insp, f.id);
        int findings = Logic.findings(insp, f.id).size();
        LinearLayout row = Ui.hbox(this);
        row.setPadding(0, Ui.dp(this, 10), 0, 0);
        row.addView(Ui.pill(this, p[0] + "/" + p[1] + " answered", f.color, Ui.PRIMARY_LIGHT));
        addGap(row);
        row.addView(Ui.pill(this, "📷 " + Logic.photoCount(insp, f.id), Ui.MUTED, 0xFFEDEFF2));
        addGap(row);
        row.addView(Ui.pill(this, findings + " findings", findings > 0 ? Ui.RED : Ui.MUTED,
                findings > 0 ? Ui.RED_LIGHT : 0xFFEDEFF2));
        card.addView(row);
        if (note != null) {
            int pre = Logic.prefilledCount(insp, f.id);
            if (pre > 0) card.addView(Ui.text(this, "↺ " + pre + " prefilled", 13, Ui.TEAL, false), Ui.matchWrap(this, 8));
        }
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (f.checklist && Agreement.status(insp) != Agreement.SIGNED) gate(f.id);
                else openForm(f.id);
            }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                confirmRemoveType(f);
                return true;
            }
        });
        return card;
    }

    /** Licensed types of the conducting inspector that this inspection does not cover yet. */
    private java.util.List<String> addableTypes() {
        java.util.List<String> out = Company.licensedTypes(Company.inspectorFor(this, insp));
        out.removeAll(Forms.types(insp));
        return out;
    }

    private void addType() {
        final java.util.List<String> ids = addableTypes();
        final String[] names = new String[ids.size()];
        final boolean[] checked = new boolean[ids.size()];
        for (int i = 0; i < names.length; i++) names[i] = Forms.form(ids.get(i)).title;
        new android.app.AlertDialog.Builder(this).setTitle("Add inspection type")
                .setMultiChoiceItems(names, checked, new android.content.DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which, boolean on) { checked[which] = on; }
                })
                .setPositiveButton("Add", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        java.util.List<String> t = Forms.types(insp);
                        for (int i = 0; i < checked.length; i++) if (checked[i]) t.add(ids.get(i));
                        Forms.setTypes(insp, t);
                        for (String id : t) if (!Forms.GENERAL.equals(id)) Logic.prefillForm(insp, id);
                        Logic.prefillForm(insp, Forms.AGREEMENT);
                        Store.save(insp);
                        build();
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void confirmRemoveType(final Forms.Form f) {
        if (Forms.AGREEMENT.equals(f.id) || Forms.types(insp).size() <= 1) return;
        new android.app.AlertDialog.Builder(this).setTitle("Remove " + f.title + "?")
                .setMessage("Answers and photos are kept.")
                .setPositiveButton("Remove", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        java.util.List<String> t = Forms.types(insp);
                        t.remove(f.id);
                        Forms.setTypes(insp, t);
                        Logic.prefillForm(insp, Forms.AGREEMENT);
                        Store.save(insp);
                        build();
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    /** The agreement should be signed before the inspection starts. */
    private void gate(final String formId) {
        new android.app.AlertDialog.Builder(this).setTitle("Agreement not signed")
                .setMessage("Agreement status: " + Agreement.statusText(insp) + ".")
                .setPositiveButton("Open agreement", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) { openForm(Forms.AGREEMENT); }
                })
                .setNegativeButton("Continue anyway", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) { openForm(formId); }
                }).show();
    }

    private void openForm(String id) {
        Intent i = new Intent(this, FormActivity.class);
        i.putExtra(FormActivity.EXTRA_INSPECTION, insp.id);
        i.putExtra(FormActivity.EXTRA_FORM, id);
        startActivity(i);
    }

    private LinearLayout.LayoutParams wrap(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, top);
        return lp;
    }

    private void addGap(LinearLayout row) {
        row.addView(new View(this), new LinearLayout.LayoutParams(Ui.dp(this, 6), 1));
    }

    private void openSection(String key) {
        Intent i = new Intent(this, SectionActivity.class);
        i.putExtra(SectionActivity.EXTRA_INSPECTION, insp.id);
        i.putExtra(SectionActivity.EXTRA_SECTION, key);
        startActivity(i);
    }
}
