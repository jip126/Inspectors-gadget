package com.inspectorsgadget.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks the latest GitHub release of {@link Config#UPDATE_REPO} and installs its APK.
 *
 * publish-update.bat creates releases tagged v{versionName} with one asset named
 * InspectorsGadget-{versionName}-{versionCode}.apk. The versionCode in the asset name decides
 * whether the release is newer than the installed build.
 */
public final class Updater {
    private Updater() {}

    private static final Pattern CODE_IN_NAME = Pattern.compile("-(\\d+)\\.apk$", Pattern.CASE_INSENSITIVE);
    private static final String APK_MIME = "application/vnd.android.package-archive";

    /** A published build. */
    public static final class Release {
        public final String versionName;
        public final int versionCode;
        public final String apkUrl;
        public final long apkSize;
        public final String notes;

        Release(String versionName, int versionCode, String apkUrl, long apkSize, String notes) {
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.apkUrl = apkUrl;
            this.apkSize = apkSize;
            this.notes = notes;
        }
    }

    public static int installedCode(Context c) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return pi.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String installedName(Context c) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return pi.versionName == null ? "" : pi.versionName;
        } catch (Exception e) {
            return "";
        }
    }

    /** Parses a GitHub "latest release" response. Returns null when it has no APK asset. */
    public static Release parse(String json) throws Exception {
        JSONObject o = new JSONObject(json);
        String tag = o.optString("tag_name", "");
        String name = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
        JSONArray assets = o.optJSONArray("assets");
        if (assets == null) return null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject a = assets.getJSONObject(i);
            String file = a.optString("name", "");
            if (!file.toLowerCase().endsWith(".apk")) continue;
            Matcher m = CODE_IN_NAME.matcher(file);
            int code = m.find() ? Integer.parseInt(m.group(1)) : -1;
            return new Release(name, code, a.optString("browser_download_url", ""), a.optLong("size", 0),
                    o.optString("body", "").trim());
        }
        return null;
    }

    /** True when the release is newer than the installed build. */
    public static boolean isNewer(Release r, int installedCode, String installedName) {
        if (r == null) return false;
        if (r.versionCode > 0) return r.versionCode > installedCode;
        return compareNames(r.versionName, installedName) > 0;
    }

    /** Compares dotted version names numerically ("0.10" is newer than "0.9"); suffixes are ignored. */
    public static int compareNames(String a, String b) {
        String[] x = numericPart(a).split("\\.");
        String[] y = numericPart(b).split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length && x[i].length() > 0 ? Integer.parseInt(x[i]) : 0;
            int q = i < y.length && y[i].length() > 0 ? Integer.parseInt(y[i]) : 0;
            if (p != q) return p < q ? -1 : 1;
        }
        return 0;
    }

    private static String numericPart(String s) {
        Matcher m = Pattern.compile("^\\d+(\\.\\d+)*").matcher(s == null ? "" : s.trim());
        return m.find() ? m.group() : "0";
    }

    /** Runs the full flow: check, confirm, download, install. */
    public static void checkInteractive(final Activity a) {
        final ProgressDialog wait = ProgressDialog.show(a, null, "Checking for updates", true, false);
        final Handler ui = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                Release r = null;
                String error = null;
                try {
                    r = parse(get("https://api.github.com/repos/" + Config.UPDATE_REPO + "/releases/latest"));
                } catch (NotFound e) {
                    r = null;
                } catch (Exception e) {
                    error = "Couldn't reach the update server";
                }
                final Release rel = r;
                final String err = error;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        if (a.isFinishing()) return;
                        wait.dismiss();
                        if (err != null) {
                            Toast.makeText(a, err, Toast.LENGTH_LONG).show();
                        } else if (!isNewer(rel, installedCode(a), installedName(a))) {
                            new AlertDialog.Builder(a).setTitle("Up to date")
                                    .setMessage("Version " + installedName(a))
                                    .setPositiveButton("OK", null).show();
                        } else {
                            offer(a, rel);
                        }
                    }
                });
            }
        }).start();
    }

    private static void offer(final Activity a, final Release r) {
        StringBuilder msg = new StringBuilder();
        msg.append("Installed: ").append(installedName(a)).append("\nAvailable: ").append(r.versionName);
        if (r.apkSize > 0) msg.append(" (").append(String.format(java.util.Locale.US, "%.1f MB", r.apkSize / 1048576.0)).append(")");
        if (r.notes.length() > 0) msg.append("\n\n").append(r.notes);
        new AlertDialog.Builder(a).setTitle("Update available").setMessage(msg.toString())
                .setPositiveButton("Install", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) { download(a, r); }
                }).setNegativeButton("Later", null).show();
    }

    private static void download(final Activity a, final Release r) {
        final ProgressDialog bar = new ProgressDialog(a);
        bar.setMessage("Downloading " + r.versionName);
        bar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        bar.setIndeterminate(r.apkSize <= 0);
        bar.setMax(100);
        bar.setCancelable(false);
        bar.show();
        final Handler ui = new Handler(Looper.getMainLooper());
        final File out = apkFile(a);
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean ok;
                try {
                    fetch(r.apkUrl, out, r.apkSize, new Progress() {
                        @Override
                        public void at(final int pct) {
                            ui.post(new Runnable() {
                                @Override
                                public void run() { bar.setProgress(pct); }
                            });
                        }
                    });
                    ok = true;
                } catch (Exception e) {
                    out.delete();
                    ok = false;
                }
                final boolean done = ok;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        if (a.isFinishing()) return;
                        bar.dismiss();
                        if (done) install(a);
                        else Toast.makeText(a, "Download failed", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private static boolean pendingInstall;

    /** Call from onResume: continues an install that was waiting on the install permission. */
    public static void resumeInstall(Activity a) {
        if (!pendingInstall) return;
        pendingInstall = false;
        if (apkFile(a).isFile() && (Build.VERSION.SDK_INT < 26 || a.getPackageManager().canRequestPackageInstalls())) install(a);
    }

    static File apkFile(Context c) {
        File dir = new File(c.getCacheDir(), "updates");
        dir.mkdirs();
        return new File(dir, "update.apk");
    }

    /** Opens the system installer for the downloaded APK, asking for install permission first if needed. */
    public static void install(final Activity a) {
        if (Build.VERSION.SDK_INT >= 26 && !a.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(a).setTitle("Allow installs")
                    .setMessage("Allow installs from " + a.getString(R.string.app_name) + ".")
                    .setPositiveButton("Open settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            pendingInstall = true;
                            a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:" + a.getPackageName())));
                        }
                    })
                    .setNegativeButton("Cancel", null).show();
            return;
        }
        pendingInstall = false;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(FileShareProvider.uriFor(a, apkFile(a)), APK_MIME);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            a.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(a, "No installer available", Toast.LENGTH_LONG).show();
        }
    }

    // ---- HTTP ----

    private static final class NotFound extends IOException {}

    private interface Progress {
        void at(int pct);
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setConnectTimeout(15000);
        h.setReadTimeout(30000);
        h.setInstanceFollowRedirects(true);
        h.setRequestProperty("User-Agent", "InspectorsGadget-Android");
        return h;
    }

    private static String get(String url) throws IOException {
        HttpURLConnection h = open(url);
        h.setRequestProperty("Accept", "application/vnd.github+json");
        try {
            int code = h.getResponseCode();
            if (code == 404) throw new NotFound();
            if (code / 100 != 2) throw new IOException("HTTP " + code);
            InputStream in = h.getInputStream();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) buf.write(b, 0, n);
            in.close();
            return buf.toString("UTF-8");
        } finally {
            h.disconnect();
        }
    }

    private static void fetch(String url, File out, long expected, Progress p) throws IOException {
        HttpURLConnection h = open(url);
        h.setRequestProperty("Accept", "application/octet-stream");
        try {
            if (h.getResponseCode() / 100 != 2) throw new IOException("HTTP " + h.getResponseCode());
            long total = expected > 0 ? expected : h.getContentLength();
            InputStream in = h.getInputStream();
            OutputStream os = new FileOutputStream(out);
            byte[] b = new byte[32768];
            long got = 0;
            int n, last = -1;
            try {
                while ((n = in.read(b)) > 0) {
                    os.write(b, 0, n);
                    got += n;
                    if (total > 0) {
                        int pct = (int) Math.min(100, got * 100 / total);
                        if (pct != last) p.at(last = pct);
                    }
                }
            } finally {
                os.close();
                in.close();
            }
            if (expected > 0 && got != expected) throw new IOException("Incomplete download");
        } finally {
            h.disconnect();
        }
    }
}
