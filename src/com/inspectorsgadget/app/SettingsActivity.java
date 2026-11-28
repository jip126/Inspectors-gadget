package com.inspectorsgadget.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Account, company and app settings. */
public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("Settings");
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
        Updater.resumeInstall(this);
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = Ui.vbox(this);
        int pad = Ui.dp(this, 14);
        content.setPadding(pad, pad, pad, Ui.dp(this, 40));
        scroll.addView(content);

        content.addView(header("Account"));
        LinearLayout account = Ui.card(this);
        String who = Auth.email(this);
        Inspection ip = Company.signedInInspector(this);
        account.addView(row(ip != null ? Company.inspectorName(ip) : Auth.name(this), who, null));
        if (Company.inspectors(this).size() > 1) {
            account.addView(divider());
            account.addView(row("Change inspector", null, new View.OnClickListener() {
                @Override
                public void onClick(View v) { chooseInspector(); }
            }));
        }
        final boolean bio = Biometrics.isEnabled(this);
        if (bio || Biometrics.available(this) != Biometrics.NONE) {
            account.addView(divider());
            account.addView(row("Biometric login", bio ? "On" : "Off", new View.OnClickListener() {
                @Override
                public void onClick(View v) { toggleBiometrics(bio); }
            }));
        }
        account.addView(divider());
        account.addView(row("Log out", null, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Auth.logout(SettingsActivity.this);
                LoginActivity.open(SettingsActivity.this, true);
            }
        }));
        content.addView(account);

        content.addView(header("Company"));
        LinearLayout company = Ui.card(this);
        Inspection co = Company.profile(this);
        int n = Company.inspectors(this).size();
        company.addView(row(co.str("co.name").length() > 0 ? co.str("co.name") : "Company",
                n + " inspector" + (n == 1 ? "" : "s"), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(SettingsActivity.this, CompanyActivity.class));
                    }
                }));
        company.addView(divider());
        company.addView(row("Plan", Account.summary(this), new View.OnClickListener() {
            @Override
            public void onClick(View v) { PaywallActivity.open(SettingsActivity.this, null); }
        }));
        content.addView(company);

        content.addView(header("App"));
        LinearLayout app = Ui.card(this);
        app.addView(row("Version", Updater.installedName(this) + " (" + Updater.installedCode(this) + ")", null));
        app.addView(divider());
        app.addView(row("Check for updates", null, new View.OnClickListener() {
            @Override
            public void onClick(View v) { Updater.checkInteractive(SettingsActivity.this); }
        }));
        content.addView(app);

        setContentView(scroll);
    }

    private TextView header(String s) {
        TextView t = Ui.text(this, s, 15, Ui.MUTED, true);
        t.setPadding(Ui.dp(this, 4), Ui.dp(this, 8), 0, Ui.dp(this, 8));
        return t;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(Ui.BORDER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.topMargin = Ui.dp(this, 10);
        lp.bottomMargin = Ui.dp(this, 10);
        v.setLayoutParams(lp);
        return v;
    }

    private View row(String title, String value, View.OnClickListener click) {
        LinearLayout r = Ui.hbox(this);
        r.setMinimumHeight(Ui.dp(this, 36));
        LinearLayout t = Ui.vbox(this);
        t.addView(Ui.text(this, title, 16, Ui.TEXT, click != null || value == null));
        if (value != null && value.length() > 0) t.addView(Ui.muted(this, value));
        r.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (click != null) {
            r.addView(Ui.text(this, "›", 24, Ui.MUTED, false));
            r.setOnClickListener(click);
        }
        return r;
    }

    private void chooseInspector() {
        final List<Inspection> roster = Company.inspectors(this);
        String[] names = new String[roster.size()];
        Inspection current = Company.signedInInspector(this);
        int checked = -1;
        for (int i = 0; i < roster.size(); i++) {
            names[i] = Company.inspectorName(roster.get(i));
            if (current != null && roster.get(i).id.equals(current.id)) checked = i;
        }
        new AlertDialog.Builder(this).setTitle("Signed in as")
                .setSingleChoiceItems(names, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        Company.setSignedInInspector(SettingsActivity.this, roster.get(w).id);
                        d.dismiss();
                        build();
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void toggleBiometrics(boolean on) {
        if (on) {
            Biometrics.disable(this);
            Toast.makeText(this, "Biometric login off", Toast.LENGTH_SHORT).show();
            build();
            return;
        }
        Biometrics.enable(this, new Biometrics.Listener() {
            @Override
            public void success(String token) {
                Toast.makeText(SettingsActivity.this, "Biometric login on", Toast.LENGTH_SHORT).show();
                build();
            }

            @Override
            public void cancelled() {}

            @Override
            public void failed(String message) {
                Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
