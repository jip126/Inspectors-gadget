package com.inspectorsgadget.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Subscription, free trial and backend account state for this install.
 *
 * Rules: one free trial inspection with every feature. Demo (sample) inspections can be opened and
 * edited but not exported. Everything else needs an active monthly or annual subscription.
 */
public final class Account {
    private Account() {}

    static final String PREFS = "account";
    static final String SAMPLE_KEY = "_sample";

    public static final String PLAN_NONE = "", SOURCE_DEMO = "demo", SOURCE_PLAY = "play";

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------ subscription

    /** Base plan id ({@link Config#PLAN_MONTHLY} or {@link Config#PLAN_ANNUAL}), or empty. */
    public static String plan(Context c) { return prefs(c).getString("plan", PLAN_NONE); }

    public static long expiresAt(Context c) { return prefs(c).getLong("expiresAt", 0); }

    public static String source(Context c) { return prefs(c).getString("source", ""); }

    public static boolean isSubscribed(Context c) {
        return plan(c).length() > 0 && expiresAt(c) > System.currentTimeMillis();
    }

    /** Records an active subscription, from a purchase, a restore or the server's verification. */
    public static void activate(Context c, String plan, long expiresAt, String source, String purchaseToken) {
        prefs(c).edit().putString("plan", plan).putLong("expiresAt", expiresAt).putString("source", source)
                .putString("purchaseToken", purchaseToken == null ? "" : purchaseToken).apply();
    }

    public static void deactivate(Context c) {
        prefs(c).edit().remove("plan").remove("expiresAt").remove("source").remove("purchaseToken").apply();
    }

    public static String purchaseToken(Context c) { return prefs(c).getString("purchaseToken", ""); }

    public static String planName(String plan) {
        return Config.PLAN_ANNUAL.equals(plan) ? "Annual" : Config.PLAN_MONTHLY.equals(plan) ? "Monthly" : "";
    }

    /** One line for the home screen, e.g. "Pro, Annual, renews 11/26/2027" or "Free trial". */
    public static String summary(Context c) {
        if (isSubscribed(c)) {
            return "Pro, " + planName(plan(c)) + ", renews "
                    + new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date(expiresAt(c)));
        }
        return trialInspectionId(c).length() == 0 ? "Free trial, 1 inspection" : "Free trial used";
    }

    // ------------------------------------------------------------ trial

    public static String trialInspectionId(Context c) {
        migrate(c);
        return prefs(c).getString("trialInspection", "");
    }

    /** True until the first inspection is created. Deleting the trial inspection does not give it back. */
    public static boolean trialAvailable(Context c) { return trialInspectionId(c).length() == 0; }

    public static boolean canCreateInspection(Context c) { return isSubscribed(c) || trialAvailable(c); }

    /** Call right after a (non-sample) inspection is created. The first one becomes the trial. */
    public static void onInspectionCreated(Context c, Inspection in) {
        if (isSubscribed(c) || isSample(in)) return;
        if (trialAvailable(c)) prefs(c).edit().putString("trialInspection", in.id).apply();
    }

    public static boolean isSample(Inspection in) { return in != null && "true".equals(in.str(SAMPLE_KEY)); }

    public static void markSample(Inspection in) { in.setUser(SAMPLE_KEY, "true"); }

    public static boolean isTrial(Context c, Inspection in) { return in != null && in.id.equals(trialInspectionId(c)); }

    /** Opening and editing: subscribers, the trial inspection, and demo inspections. */
    public static boolean canOpen(Context c, Inspection in) {
        return isSubscribed(c) || isTrial(c, in) || isSample(in);
    }

    /** PDF reports, sending the agreement and e-signature: subscribers and the trial inspection. */
    public static boolean canExport(Context c, Inspection in) { return isSubscribed(c) || isTrial(c, in); }

    /**
     * Installs that had inspections before the paywall existed: the oldest one becomes the trial,
     * so nothing already started is lost without a subscription.
     */
    static void migrate(Context c) {
        SharedPreferences p = prefs(c);
        if (p.getBoolean("migrated", false)) return;
        p.edit().putBoolean("migrated", true).apply();
        if (p.getString("trialInspection", "").length() > 0) return;
        List<Inspection> all = Store.all(c);
        Inspection oldest = null;
        for (Inspection in : all) {
            if (isSample(in)) continue;
            if (oldest == null || in.created() < oldest.created()) oldest = in;
        }
        if (oldest != null) p.edit().putString("trialInspection", oldest.id).apply();
    }

    // ------------------------------------------------------------ backend account

    /** Random id for this install, sent when registering with the backend. */
    public static String installId(Context c) {
        SharedPreferences p = prefs(c);
        String id = p.getString("installId", "");
        if (id.length() == 0) {
            id = UUID.randomUUID().toString();
            p.edit().putString("installId", id).apply();
        }
        return id;
    }

    public static String relayToken(Context c) { return prefs(c).getString("relayToken", ""); }

    public static String accountId(Context c) { return prefs(c).getString("accountId", ""); }

    public static void setRelayAccount(Context c, String accountId, String token) {
        prefs(c).edit().putString("accountId", accountId).putString("relayToken", token).apply();
    }
}
