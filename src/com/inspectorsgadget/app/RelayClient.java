package com.inspectorsgadget.app;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * HTTPS client for the backend relay (backend/ in this repository). The relay holds the e-signature
 * provider's API keys and the Google Play service account, so neither is ever inside the APK.
 * Every call blocks: run it off the main thread.
 */
public final class RelayClient {
    private final Context c;
    private final String base;

    public RelayClient(Context c) { this(c, Config.RELAY_URL); }

    RelayClient(Context c, String base) {
        this.c = c.getApplicationContext();
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    public static final class HttpException extends Exception {
        public final int code;

        HttpException(int code, String msg) {
            super(msg);
            this.code = code;
        }
    }

    /** Registers this install's company with the backend once and keeps the token. */
    public void ensureAccount() throws Exception {
        if (Account.relayToken(c).length() > 0) return;
        Inspection co = Company.profile(c);
        JSONObject body = new JSONObject()
                .put("companyName", co.str("co.name"))
                .put("email", co.str("co.email"))
                .put("installId", Account.installId(c));
        JSONObject r = request("POST", "/v1/accounts", body, false);
        Account.setRelayAccount(c, r.getString("accountId"), r.getString("token"));
    }

    /** Sends a Google Play purchase to the backend, which verifies it with Google and returns the entitlement. */
    public JSONObject verifyPlayPurchase(String productId, String purchaseToken) throws Exception {
        ensureAccount();
        return request("POST", "/v1/subscriptions/play", new JSONObject()
                .put("productId", productId).put("purchaseToken", purchaseToken), true);
    }

    /** Current entitlement as the backend sees it: {active, plan, expiresAt, trialEnvelopesLeft}. */
    public JSONObject entitlement() throws Exception {
        ensureAccount();
        return request("GET", "/v1/me", null, true);
    }

    public JSONObject post(String path, JSONObject body) throws Exception {
        ensureAccount();
        return request("POST", path, body, true);
    }

    public JSONObject get(String path) throws Exception {
        ensureAccount();
        return request("GET", path, null, true);
    }

    public void download(String path, File out) throws Exception {
        ensureAccount();
        HttpURLConnection h = open("GET", path, true);
        int code = h.getResponseCode();
        if (code / 100 != 2) throw new HttpException(code, readAll(h.getErrorStream()));
        InputStream is = h.getInputStream();
        File tmp = new File(out.getPath() + ".part");
        OutputStream os = new FileOutputStream(tmp);
        byte[] buf = new byte[16384];
        int n;
        while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
        os.close();
        is.close();
        if (!tmp.renameTo(out)) throw new Exception("Could not save " + out.getName());
    }

    JSONObject request(String method, String path, JSONObject body, boolean auth) throws Exception {
        HttpURLConnection h = open(method, path, auth);
        if (body != null) {
            h.setDoOutput(true);
            h.setRequestProperty("Content-Type", "application/json");
            OutputStream os = h.getOutputStream();
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            os.close();
        }
        int code = h.getResponseCode();
        String text = readAll(code / 100 == 2 ? h.getInputStream() : h.getErrorStream());
        if (code == 401 && auth) Account.setRelayAccount(c, "", ""); // token revoked: register again next time
        if (code / 100 != 2) {
            String msg = text;
            try {
                msg = new JSONObject(text).optString("error", text);
            } catch (Exception ignored) {
            }
            throw new HttpException(code, msg);
        }
        return text.length() == 0 ? new JSONObject() : new JSONObject(text);
    }

    private HttpURLConnection open(String method, String path, boolean auth) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(base + path).openConnection();
        h.setRequestMethod(method);
        h.setConnectTimeout(15000);
        h.setReadTimeout(60000);
        h.setRequestProperty("Accept", "application/json");
        if (auth) h.setRequestProperty("Authorization", "Bearer " + Account.relayToken(c));
        return h;
    }

    static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
        is.close();
        return new String(bo.toByteArray(), StandardCharsets.UTF_8);
    }
}
