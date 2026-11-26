package com.inspectorsgadget.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Subscription purchases behind one interface.
 *
 * {@link DemoGateway} simulates purchases so the paywall can be shown and tested now. For the Play
 * Store release, add a PlayGateway built on the Google Play Billing Library (needs a Gradle build,
 * see backend/README.md, "Google Play Billing") that, after each purchase or restore, sends the
 * purchase token to {@link RelayClient#verifyPlayPurchase} and calls {@link Account#activate} with
 * the server's answer. Nothing else in the app changes.
 */
public final class Billing {
    private Billing() {}

    public static final class Plan {
        public final String id, name, price, period, badge;

        Plan(String id, String name, String price, String period, String badge) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.period = period;
            this.badge = badge;
        }
    }

    public interface Listener {
        void done(boolean ok, String message);
    }

    public interface Gateway {
        List<Plan> plans();

        void purchase(Activity a, Plan p, Listener l);

        void restore(Activity a, Listener l);
    }

    public static Gateway gateway() { return new DemoGateway(); }

    public static List<Plan> defaultPlans() {
        List<Plan> out = new ArrayList<>();
        out.add(new Plan(Config.PLAN_MONTHLY, "Monthly", Config.PRICE_MONTHLY, "per month", ""));
        out.add(new Plan(Config.PLAN_ANNUAL, "Annual", Config.PRICE_ANNUAL, "per year", Config.ANNUAL_SAVINGS));
        return out;
    }

    static long periodMillis(String planId) {
        return (Config.PLAN_ANNUAL.equals(planId) ? 365L : 30L) * 24 * 60 * 60 * 1000;
    }

    /** Simulated store for the demo build. No money moves. */
    static final class DemoGateway implements Gateway {
        @Override
        public List<Plan> plans() { return defaultPlans(); }

        @Override
        public void purchase(final Activity a, final Plan p, final Listener l) {
            new AlertDialog.Builder(a).setTitle("Demo purchase")
                    .setMessage(p.name + " plan, " + p.price + " " + p.period + ". No charge in the demo.")
                    .setPositiveButton("Subscribe", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            Account.activate(a, p.id, System.currentTimeMillis() + periodMillis(p.id),
                                    Account.SOURCE_DEMO, "");
                            l.done(true, "Subscribed, " + p.name);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) { l.done(false, null); }
                    }).show();
        }

        @Override
        public void restore(Activity a, Listener l) {
            l.done(Account.isSubscribed(a), Account.isSubscribed(a) ? "Subscription active" : "No subscription found");
        }
    }
}
