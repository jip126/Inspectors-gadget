package com.inspectorsgadget.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Subscription plans: monthly or annual. Opened from the home screen and from any locked action. */
public class PaywallActivity extends Activity {
    public static final String EXTRA_REASON = "reason";

    public static final String REASON_TRIAL_USED = "Free trial inspection used";
    public static final String REASON_LOCKED = "Requires Pro";

    public static void open(Activity a, String reason) {
        Intent i = new Intent(a, PaywallActivity.class);
        if (reason != null) i.putExtra(EXTRA_REASON, reason);
        a.startActivity(i);
    }

    private Billing.Gateway billing;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (getActionBar() != null) getActionBar().hide();
        billing = Billing.gateway();
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.LOGO_BG);
        LinearLayout root = Ui.vbox(this);
        int p = Ui.dp(this, 20);
        root.setPadding(p, Ui.dp(this, 32), p, p);
        scroll.addView(root);

        TextView title = Ui.text(this, "Inspector's Gadget Pro", 24, Color.WHITE, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);
        String reason = getIntent().getStringExtra(EXTRA_REASON);
        boolean subscribed = Account.isSubscribed(this);
        if (subscribed) reason = Account.summary(this);
        if (reason != null) {
            TextView r = Ui.text(this, reason, 14, Ui.ACCENT, true);
            r.setGravity(Gravity.CENTER_HORIZONTAL);
            root.addView(r, Ui.matchWrap(this, 6));
        }

        LinearLayout features = Ui.vbox(this);
        features.setPadding(0, Ui.dp(this, 18), 0, Ui.dp(this, 6));
        for (String f : new String[]{"Unlimited inspections", "General, 4-Point and Wind Mitigation reports",
                "E-signature agreements", "Company profile and inspector roster"}) {
            features.addView(Ui.text(this, "✓  " + f, 15, Ui.SILVER, false), Ui.matchWrap(this, 6));
        }
        root.addView(features);

        for (final Billing.Plan plan : billing.plans()) {
            boolean current = subscribed && plan.id.equals(Account.plan(this));
            LinearLayout card = Ui.vbox(this);
            int cp = Ui.dp(this, 16);
            card.setPadding(cp, cp, cp, cp);
            card.setBackground(Ui.rounded(this, 0xFF0E1C2E, current ? Ui.ACCENT : 0x55C0C7D0, 12));
            LinearLayout.LayoutParams lp = Ui.matchWrap(this, 12);
            LinearLayout top = Ui.hbox(this);
            top.addView(Ui.text(this, plan.name, 18, Color.WHITE, true),
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            if (plan.badge.length() > 0) top.addView(Ui.pill(this, plan.badge, Ui.LOGO_BG, Ui.ACCENT));
            card.addView(top);
            card.addView(Ui.text(this, plan.price + " " + plan.period, 16, Ui.SILVER, false), Ui.matchWrap(this, 4));
            Button buy = Ui.button(this, current ? "Current plan" : "Choose " + plan.name, Ui.ACCENT, true);
            buy.setTextColor(Ui.LOGO_BG);
            buy.setEnabled(!current);
            buy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    billing.purchase(PaywallActivity.this, plan, new Billing.Listener() {
                        @Override
                        public void done(boolean ok, String message) { finishWith(ok, message); }
                    });
                }
            });
            card.addView(buy, Ui.matchWrap(this, 12));
            root.addView(card, lp);
        }

        Button restore = Ui.button(this, "Restore purchases", Ui.SILVER, false);
        restore.setBackground(Ui.rounded(this, 0x00000000, 0x55C0C7D0, 10));
        restore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                billing.restore(PaywallActivity.this, new Billing.Listener() {
                    @Override
                    public void done(boolean ok, String message) { finishWith(ok, message); }
                });
            }
        });
        root.addView(restore, Ui.matchWrap(this, 18));

        if (subscribed && Account.SOURCE_PLAY.equals(Account.source(this))) {
            Button manage = Ui.button(this, "Manage subscription", Ui.SILVER, false);
            manage.setBackground(Ui.rounded(this, 0x00000000, 0x55C0C7D0, 10));
            manage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                            "https://play.google.com/store/account/subscriptions?sku=" + Config.SUBSCRIPTION_PRODUCT
                                    + "&package=" + getPackageName())));
                }
            });
            root.addView(manage, Ui.matchWrap(this, 10));
        }

        Button close = Ui.button(this, "Not now", Ui.SILVER, false);
        close.setBackgroundColor(0x00000000);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { finish(); }
        });
        root.addView(close, Ui.matchWrap(this, 6));
        setContentView(scroll);
    }

    private void finishWith(boolean ok, String message) {
        if (message != null) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        if (ok) finish();
        else build();
    }
}
