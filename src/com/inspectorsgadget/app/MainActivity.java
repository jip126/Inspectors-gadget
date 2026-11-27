package com.inspectorsgadget.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Home screen: list of inspections. */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (getActionBar() != null) getActionBar().hide();
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = Ui.vbox(this);
        scroll.addView(root);

        LinearLayout hero = Ui.vbox(this);
        hero.setBackgroundColor(Ui.LOGO_BG);
        int p = Ui.dp(this, 20);
        hero.setPadding(p, Ui.dp(this, 28), p, p);
        android.widget.ImageView logo = new android.widget.ImageView(this);
        logo.setImageResource(R.drawable.logo);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("The Inspector's Gadget");
        hero.addView(logo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        StringBuilder licensed = new StringBuilder();
        for (String t : Company.licensedTypes(Company.inspectorFor(this, null))) {
            if (licensed.length() > 0) licensed.append(" \u00B7 ");
            licensed.append(Forms.form(t).shortTitle);
        }
        TextView tag = Ui.text(this, licensed.toString(), 14, Ui.SILVER, false);
        tag.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        hero.addView(tag, Ui.matchWrap(this, 2));

        LinearLayout actions = Ui.hbox(this);
        actions.setPadding(0, Ui.dp(this, 16), 0, 0);
        Button create = Ui.button(this, "+  New inspection", Ui.ACCENT, true);
        create.setTextColor(Ui.LOGO_BG);
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Account.canCreateInspection(MainActivity.this)) newInspection();
                else PaywallActivity.open(MainActivity.this, PaywallActivity.REASON_TRIAL_USED);
            }
        });
        actions.addView(create, Ui.weight(this, 1.2f, 0));
        View gap = new View(this);
        actions.addView(gap, new LinearLayout.LayoutParams(Ui.dp(this, 10), 1));
        Button demo = Ui.button(this, "Load demo", Ui.SILVER, false);
        demo.setBackground(Ui.rounded(this, 0x00000000, Ui.SILVER, 10));
        demo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Inspection in = SampleData.create(MainActivity.this);
                Toast.makeText(MainActivity.this, "Demo inspection created", Toast.LENGTH_SHORT).show();
                open(in);
            }
        });
        actions.addView(demo, Ui.weight(this, 1f, 0));
        hero.addView(actions);
        Inspection co = Company.profile(this);
        int n = Company.inspectors(this).size();
        Button company = Ui.button(this, "🏢  " + (co.str("co.name").length() > 0 ? co.str("co.name") : "Company")
                + " · " + n + " inspector" + (n == 1 ? "" : "s"), Ui.SILVER, false);
        company.setBackground(Ui.rounded(this, 0x00000000, 0x55C0C7D0, 10));
        company.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, CompanyActivity.class)); }
        });
        hero.addView(company, Ui.matchWrap(this, 10));
        boolean pro = Account.isSubscribed(this);
        Button plan = Ui.button(this, (pro ? "★  " : "☆  ") + Account.summary(this), pro ? Ui.ACCENT : Ui.SILVER, false);
        plan.setBackground(Ui.rounded(this, 0x00000000, pro ? Ui.ACCENT : 0x55C0C7D0, 10));
        plan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { PaywallActivity.open(MainActivity.this, null); }
        });
        hero.addView(plan, Ui.matchWrap(this, 8));
        root.addView(hero);

        LinearLayout list = Ui.vbox(this);
        int lp = Ui.dp(this, 14);
        list.setPadding(lp, lp, lp, lp);
        root.addView(list);

        List<Inspection> all = Store.all(this);
        list.addView(Ui.text(this, all.isEmpty() ? "No inspections" : "Inspections", 15, Ui.MUTED, true));
        SimpleDateFormat fmt = new SimpleDateFormat("MMM d, h:mm a", Locale.US);
        for (final Inspection in : all) {
            LinearLayout card = Ui.card(this);
            ((LinearLayout.LayoutParams) card.getLayoutParams()).topMargin = Ui.dp(this, 10);
            card.addView(Ui.text(this, in.title(), 17, Ui.TEXT, true));
            String sub = Forms.joinNonEmpty(in.str("info.client"), in.str("info.city"));
            if (sub.length() > 0) card.addView(Ui.muted(this, sub));
            FlowLayout row = new FlowLayout(this);
            row.setPadding(0, Ui.dp(this, 8), 0, 0);
            for (String t : Forms.types(in)) {
                Forms.Form f = Forms.form(t);
                int[] pr = Logic.progress(in, f.id);
                int pct = pr[1] == 0 ? 0 : Math.round(100f * pr[0] / pr[1]);
                row.addView(Ui.pill(this, f.shortTitle + " " + pct + "%", f.color, Ui.PRIMARY_LIGHT));
            }
            card.addView(row);
            if (Account.isSample(in)) card.addView(Ui.pill(this, "Demo", Ui.MUTED, 0xFFEDEFF2), wrapTop(6));
            else if (!Account.canOpen(this, in)) card.addView(Ui.pill(this, "🔒 Pro", Ui.AMBER, 0xFFFFF4DC), wrapTop(6));
            card.addView(Ui.muted(this, "Updated " + fmt.format(new Date(in.updated()))), Ui.matchWrap(this, 6));
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { open(in); }
            });
            card.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    confirmDelete(in);
                    return true;
                }
            });
            list.addView(card);
        }
        setContentView(scroll);
    }

    private void open(Inspection in) {
        if (!Account.canOpen(this, in)) {
            PaywallActivity.open(this, PaywallActivity.REASON_TRIAL_USED);
            return;
        }
        Intent i = new Intent(this, InspectionActivity.class);
        i.putExtra(InspectionActivity.EXTRA_INSPECTION, in.id);
        startActivity(i);
    }

    private void newInspection() {
        ScrollView sv = new ScrollView(this);
        LinearLayout box = Ui.vbox(this);
        int p = Ui.dp(this, 20);
        box.setPadding(p, Ui.dp(this, 8), p, 0);
        sv.addView(box);
        final EditText addr = new EditText(this);
        addr.setHint("Property address");
        addr.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        box.addView(addr);
        final EditText client = new EditText(this);
        client.setHint("Client name");
        client.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        box.addView(client);

        final List<Inspection> roster = Company.inspectors(this);
        final String[] inspectorId = {roster.isEmpty() ? "" : roster.get(0).id};
        final List<String> selected = new ArrayList<>();
        final FlowLayout typeChips = new FlowLayout(this);
        if (roster.size() > 1) {
            box.addView(Ui.text(this, "Inspector", 14, Ui.TEXT, true), Ui.matchWrap(this, 14));
            final FlowLayout people = new FlowLayout(this);
            for (final Inspection ip : roster) {
                final TextView chip = Ui.chip(this, Company.inspectorName(ip));
                Ui.styleChip(this, chip, ip.id.equals(inspectorId[0]), Ui.PRIMARY);
                chip.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        inspectorId[0] = ip.id;
                        for (int i = 0; i < people.getChildCount(); i++)
                            Ui.styleChip(MainActivity.this, (TextView) people.getChildAt(i), people.getChildAt(i) == chip, Ui.PRIMARY);
                        fillTypeChips(typeChips, ip, selected);
                    }
                });
                people.addView(chip);
            }
            box.addView(people, Ui.matchWrap(this, 6));
        }
        box.addView(Ui.text(this, "Inspection types", 14, Ui.TEXT, true), Ui.matchWrap(this, 14));
        box.addView(typeChips, Ui.matchWrap(this, 6));
        fillTypeChips(typeChips, roster.isEmpty() ? null : roster.get(0), selected);

        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle("New inspection").setView(sv)
                .setPositiveButton("Start", null).setNegativeButton("Cancel", null).create();
        dlg.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (selected.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Select an inspection type", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        dlg.dismiss();
                        Inspection in = Store.create(MainActivity.this);
                        in.setUser("info.address", addr.getText().toString().trim());
                        in.setUser("info.client", client.getText().toString().trim());
                        in.setUser("info.date", new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date()));
                        Forms.setTypes(in, selected);
                        if (inspectorId[0].length() > 0) Company.applyInspector(MainActivity.this, in, inspectorId[0]);
                        Company.applyDefault(MainActivity.this, in);
                        Logic.prefillForm(in, Forms.AGREEMENT);
                        Store.save(in);
                        Account.onInspectionCreated(MainActivity.this, in);
                        open(in);
                    }
                });
            }
        });
        dlg.show();
    }

    /** Chips for the inspector's licensed types; General is preselected when licensed. */
    private void fillTypeChips(final FlowLayout box, Inspection inspector, final List<String> selected) {
        box.removeAllViews();
        final List<String> licensed = Company.licensedTypes(inspector);
        selected.retainAll(licensed);
        if (selected.isEmpty()) selected.add(licensed.contains(Forms.GENERAL) ? Forms.GENERAL : licensed.get(0));
        for (final String t : licensed) {
            final Forms.Form f = Forms.form(t);
            final TextView chip = Ui.chip(this, f.title);
            Ui.styleChip(this, chip, selected.contains(t), f.color);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selected.contains(t)) selected.remove(t);
                    else selected.add(t);
                    Ui.styleChip(MainActivity.this, chip, selected.contains(t), f.color);
                }
            });
            box.addView(chip);
        }
    }

    private LinearLayout.LayoutParams wrapTop(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, top);
        return lp;
    }

    private void confirmDelete(final Inspection in) {
        new AlertDialog.Builder(this).setTitle("Delete inspection?")
                .setMessage(in.title() + " and all of its photos will be removed from this device.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        Store.delete(MainActivity.this, in.id);
                        build();
                    }
                }).setNegativeButton("Cancel", null).show();
    }
}
