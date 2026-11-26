package com.inspectorsgadget.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Saves each inspection as inspections/<id>/inspection.json in app-private storage. */
public final class Store {
    private Store() {}

    private static final Map<String, Inspection> cache = new HashMap<>();
    private static final Handler handler = new Handler(Looper.getMainLooper());

    static File rootDir(Context c) {
        File d = new File(c.getFilesDir(), "inspections");
        d.mkdirs();
        return d;
    }

    public static synchronized Inspection create(Context c) {
        String id = "i" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        File dir = new File(rootDir(c), id);
        dir.mkdirs();
        Inspection in = new Inspection(id, dir, new JSONObject());
        cache.put(dir.getPath(), in);
        save(in);
        return in;
    }

    /** Creates a record with a fixed id (company profile, inspectors). */
    public static synchronized Inspection createWithId(Context c, String id) {
        File dir = new File(rootDir(c), id);
        dir.mkdirs();
        Inspection in = new Inspection(id, dir, new JSONObject());
        cache.put(dir.getPath(), in);
        save(in);
        return in;
    }

    /** Loads a record from its directory without needing a Context. */
    public static synchronized Inspection peek(File dir) {
        Inspection in = cache.get(dir.getPath());
        if (in != null) return in;
        File f = new File(dir, "inspection.json");
        if (!f.exists()) return null;
        try {
            in = new Inspection(dir.getName(), dir, new JSONObject(readText(f)));
            cache.put(dir.getPath(), in);
            return in;
        } catch (Exception e) {
            return null;
        }
    }

    public static synchronized Inspection get(Context c, String id) {
        File dir = new File(rootDir(c), id);
        Inspection in = cache.get(dir.getPath());
        if (in != null) return in;
        File f = new File(dir, "inspection.json");
        if (!f.exists()) return null;
        try {
            in = new Inspection(id, dir, new JSONObject(readText(f)));
            cache.put(dir.getPath(), in);
            return in;
        } catch (Exception e) {
            return null;
        }
    }

    public static synchronized List<Inspection> all(Context c) {
        List<Inspection> out = new ArrayList<>();
        File[] dirs = rootDir(c).listFiles();
        if (dirs != null) {
            for (File d : dirs) {
                if (d.getName().startsWith("_")) continue; // company profile and inspectors
                Inspection in = get(c, d.getName());
                if (in != null) out.add(in);
            }
        }
        Collections.sort(out, new Comparator<Inspection>() {
            @Override
            public int compare(Inspection a, Inspection b) {
                return Long.compare(b.updated(), a.updated());
            }
        });
        return out;
    }

    public static synchronized void delete(Context c, String id) {
        File dir = new File(rootDir(c), id);
        cache.remove(dir.getPath());
        deleteRec(dir);
    }

    private static void deleteRec(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteRec(k);
        f.delete();
    }

    public static synchronized void save(Inspection in) {
        try {
            File tmp = new File(in.dir, "inspection.json.tmp");
            FileOutputStream os = new FileOutputStream(tmp);
            os.write(in.root.toString().getBytes(StandardCharsets.UTF_8));
            os.getFD().sync();
            os.close();
            tmp.renameTo(new File(in.dir, "inspection.json"));
        } catch (Exception e) {
            android.util.Log.e("InspectorsGadget", "save failed", e);
        }
    }

    /** Debounced save, so typing does not write the file on every key press. */
    public static void saveSoon(final Inspection in) {
        handler.removeCallbacksAndMessages(in);
        handler.postAtTime(new Runnable() {
            @Override
            public void run() { save(in); }
        }, in, android.os.SystemClock.uptimeMillis() + 700);
    }

    static String readText(File f) throws Exception {
        InputStream is = new FileInputStream(f);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
        is.close();
        return new String(bo.toByteArray(), StandardCharsets.UTF_8);
    }
}
