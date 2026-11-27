package com.inspectorsgadget.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

/** Company profile and the roster of inspectors/employees. */
public class CompanyActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("Company & inspectors");
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    private void build() {
        Inspection co = Company.profile(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = Ui.vbox(this);
        int pad = Ui.dp(this, 14);
        content.setPadding(pad, pad, pad, Ui.dp(this, 40));
        scroll.addView(content);

        LinearLayout card = Ui.card(this);
        LinearLayout row = Ui.hbox(this);
        File logo = Company.logoFile(Store.rootDir(this));
        if (logo != null) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Images.loadThumb(iv, logo, 300);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 64), Ui.dp(this, 64));
            lp.rightMargin = Ui.dp(this, 12);
            row.addView(iv, lp);
        }
        LinearLayout t = Ui.vbox(this);
        t.addView(Ui.text(this, co.str("co.name").length() > 0 ? co.str("co.name") : "Your company", 18, Ui.TEXT, true));
        String addr = Forms.joinNonEmpty(co.str("co.address"), co.str("co.city"), co.str("co.state"));
        if (addr.length() > 0) t.addView(Ui.muted(this, addr));
        String contact = Forms.joinNonEmpty(co.str("co.phone"), co.str("co.email"));
        if (contact.length() > 0) t.addView(Ui.muted(this, contact));
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(Ui.text(this, "›", 26, Ui.MUTED, false));
        card.addView(row);
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { open(Company.PROFILE_ID, "co"); }
        });
        content.addView(card);

        content.addView(Ui.text(this, "Inspectors", 15, Ui.MUTED, true), Ui.matchWrap(this, 6));
        List<Inspection> roster = Company.inspectors(this);
        for (final Inspection ip : roster) {
            LinearLayout c = Ui.card(this);
            ((LinearLayout.LayoutParams) c.getLayoutParams()).topMargin = Ui.dp(this, 10);
            c.addView(Ui.text(this, "👤  " + Company.inspectorName(ip), 16, Ui.TEXT, true));
            String lic = Forms.joinNonEmpty(ip.str("ip.title"), ip.str("ip.license"), ip.str("ip.issuer"));
            if (lic.length() > 0) c.addView(Ui.muted(this, lic));
            List<String> held = StateForms.licensesHeld(ip);
            if (!held.isEmpty()) {
                StringBuilder sb = new StringBuilder(StateForms.licenseState(ip, co));
                for (String l : held) sb.append(sb.length() > 0 ? " \u00B7 " : "").append(StateForms.licenseTitle(l));
                c.addView(Ui.muted(this, sb.toString()));
            }
            if (ip.str("ip.signature").length() == 0)
                c.addView(Ui.text(this, "No saved signature", 12, Ui.AMBER, false));
            c.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { open(ip.id, "ip"); }
            });
            c.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    new AlertDialog.Builder(CompanyActivity.this).setTitle("Remove inspector?")
                            .setMessage(Company.inspectorName(ip) + " will no longer be offered on new inspections. "
                                    + "Inspections already done keep their details.")
                            .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int w) {
                                    Store.delete(CompanyActivity.this, ip.id);
                                    build();
                                }
                            }).setNegativeButton("Cancel", null).show();
                    return true;
                }
            });
            content.addView(c);
        }
        Button add = Ui.button(this, "+  Add inspector", Ui.PRIMARY, true);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Inspection ip = Company.addInspector(CompanyActivity.this);
                open(ip.id, "ip");
            }
        });
        content.addView(add, Ui.matchWrap(this, 12));
        setContentView(scroll);
    }

    private void open(String id, String section) {
        Intent i = new Intent(this, SectionActivity.class);
        i.putExtra(SectionActivity.EXTRA_INSPECTION, id);
        i.putExtra(SectionActivity.EXTRA_SECTION, section);
        startActivity(i);
    }
}
