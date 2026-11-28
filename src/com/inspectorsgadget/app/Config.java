package com.inspectorsgadget.app;

/**
 * Build-time settings for the online services.
 *
 * With RELAY_URL empty the app runs in demo mode: e-signature requests are simulated on the device
 * and purchases are simulated by {@link Billing.DemoGateway}. Set RELAY_URL to the deployed backend
 * (see backend/README.md) to send real signature requests through BoldSign or DocuSign.
 */
public final class Config {
    private Config() {}

    /** Base URL of the backend relay, for example "https://ig-relay.example.workers.dev". */
    public static final String RELAY_URL = "";

    /** Google Play subscription product and its two base plans (Play Console, Monetize, Subscriptions). */
    public static final String SUBSCRIPTION_PRODUCT = "inspectors_gadget_pro";
    public static final String PLAN_MONTHLY = "monthly";
    public static final String PLAN_ANNUAL = "annual";

    /** Pricing (set 11/26/2026). Shown until Google Play returns localized prices; keep Play Console in sync. */
    public static final String PRICE_MONTHLY = "$39.99";
    public static final String PRICE_ANNUAL = "$400";
    public static final String ANNUAL_SAVINGS = "Save 17%";

    /** Demo mode: seconds before a simulated client signs an e-signature request. */
    public static final int DEMO_SIGN_DELAY_SECONDS = 20;

    /** GitHub repository whose latest release is offered by Settings, Check for updates (see publish-update.bat). */
    public static final String UPDATE_REPO = "jip126/inspectors-gadget";

    /**
     * Built-in admin account for the demo period, so a fresh install can sign in without creating an
     * account. Remove before release: set DEMO_ADMIN to false (the account is then neither created
     * nor accepted, including one already saved on a device).
     */
    public static final boolean DEMO_ADMIN = true;
    public static final String DEMO_ADMIN_EMAIL = "admin@ig.app";
    public static final String DEMO_ADMIN_PASSWORD = "admin1234";
    public static final String DEMO_ADMIN_NAME = "Admin";

    public static boolean relayConfigured() { return RELAY_URL.length() > 0; }
}
