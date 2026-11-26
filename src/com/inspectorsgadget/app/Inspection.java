package com.inspectorsgadget.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * One property inspection. Everything lives in a single JSON document:
 * values (field key -> String or JSONArray), photos (section key -> [photo ids]),
 * photoMeta (photo id -> file names, caption, markup strokes), prefilled (keys
 * whose current value was copied from the general inspection and not edited).
 */
public class Inspection {
    public final String id;
    public final File dir;
    final JSONObject root;

    Inspection(String id, File dir, JSONObject root) {
        this.id = id;
        this.dir = dir;
        this.root = root;
        ensure("values");
        ensure("photos");
        ensure("photoMeta");
        ensure("prefilled");
        if (!root.has("created")) put(root, "created", System.currentTimeMillis());
    }

    private void ensure(String k) {
        if (root.optJSONObject(k) == null) put(root, k, new JSONObject());
    }

    static void put(JSONObject o, String k, Object v) {
        try {
            o.put(k, v);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    JSONObject values() { return root.optJSONObject("values"); }
    JSONObject photos() { return root.optJSONObject("photos"); }
    JSONObject photoMeta() { return root.optJSONObject("photoMeta"); }
    JSONObject prefilled() { return root.optJSONObject("prefilled"); }

    public File photoDir() {
        File d = new File(dir, "photos");
        d.mkdirs();
        return d;
    }

    public long created() { return root.optLong("created"); }
    public long updated() { return root.optLong("updated", created()); }

    // ---- values ----

    public boolean has(String key) { return values().has(key); }

    public Object raw(String key) { return values().opt(key); }

    public String str(String key) {
        Object o = values().opt(key);
        if (o == null || o == JSONObject.NULL) return "";
        if (o instanceof JSONArray) return join((JSONArray) o);
        return o.toString();
    }

    public List<String> list(String key) {
        return toList(values().opt(key));
    }

    static List<String> toList(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof JSONArray) {
            JSONArray a = (JSONArray) o;
            for (int i = 0; i < a.length(); i++) out.add(a.optString(i));
        } else if (o != null && o != JSONObject.NULL && o.toString().length() > 0) {
            out.add(o.toString());
        }
        return out;
    }

    static String join(JSONArray a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(a.optString(i));
        }
        return sb.toString();
    }

    public static boolean isEmpty(Object v) {
        if (v == null || v == JSONObject.NULL) return true;
        if (v instanceof JSONArray) return ((JSONArray) v).length() == 0;
        return v.toString().trim().length() == 0;
    }

    /** A value the inspector typed or chose. Clears the prefilled marker. */
    public void setUser(String key, Object value) {
        if (isEmpty(value)) values().remove(key);
        else put(values(), key, value);
        prefilled().remove(key);
        touch();
    }

    /** A value copied from the general inspection. */
    public void setPrefill(String key, Object value) {
        put(values(), key, value);
        put(prefilled(), key, true);
        touch();
    }

    public void clearPrefill(String key) {
        values().remove(key);
        prefilled().remove(key);
        touch();
    }

    public boolean isPrefilled(String key) { return prefilled().optBoolean(key, false); }

    void touch() { put(root, "updated", System.currentTimeMillis()); }

    // ---- photos ----

    public List<String> sectionPhotos(String sectionKey) {
        return toList(photos().opt(sectionKey));
    }

    public void setSectionPhotos(String sectionKey, List<String> ids) {
        put(photos(), sectionKey, new JSONArray(ids));
        touch();
    }

    public void addPhotoToSection(String sectionKey, String photoId) {
        List<String> l = sectionPhotos(sectionKey);
        if (!l.contains(photoId)) l.add(photoId);
        setSectionPhotos(sectionKey, l);
    }

    public void removePhotoFromSection(String sectionKey, String photoId) {
        List<String> l = sectionPhotos(sectionKey);
        l.remove(photoId);
        setSectionPhotos(sectionKey, l);
    }

    /** Creates metadata for a new photo and returns its id. The image file is written by the caller. */
    public String newPhoto(String originSection) {
        String pid = "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        JSONObject m = new JSONObject();
        put(m, "file", pid + ".jpg");
        put(m, "origin", originSection);
        put(m, "taken", System.currentTimeMillis());
        put(photoMeta(), pid, m);
        touch();
        return pid;
    }

    public JSONObject meta(String pid) {
        JSONObject m = photoMeta().optJSONObject(pid);
        return m != null ? m : new JSONObject();
    }

    public boolean photoExists(String pid) { return photoMeta().has(pid) && originalFile(pid).exists(); }

    public File originalFile(String pid) { return new File(photoDir(), meta(pid).optString("file", pid + ".jpg")); }

    /** The marked-up version if there is one, otherwise the original. */
    public File displayFile(String pid) {
        String a = meta(pid).optString("annotated", "");
        if (a.length() > 0) {
            File f = new File(photoDir(), a);
            if (f.exists()) return f;
        }
        return originalFile(pid);
    }

    public String caption(String pid) { return meta(pid).optString("caption", ""); }

    public void setCaption(String pid, String caption) {
        put(meta(pid), "caption", caption);
        touch();
    }

    public void deletePhotoEverywhere(String pid) {
        Iterator<String> it = photos().keys();
        List<String> keys = new ArrayList<>();
        while (it.hasNext()) keys.add(it.next());
        for (String k : keys) removePhotoFromSection(k, pid);
        JSONObject m = meta(pid);
        new File(photoDir(), m.optString("file", "")).delete();
        String a = m.optString("annotated", "");
        if (a.length() > 0) new File(photoDir(), a).delete();
        photoMeta().remove(pid);
        touch();
    }

    /** Number of sections (other than the given one) that use this photo. */
    public int usageCount(String pid) {
        int n = 0;
        Iterator<String> it = photos().keys();
        while (it.hasNext()) if (sectionPhotos(it.next()).contains(pid)) n++;
        return n;
    }

    public String title() {
        String a = str("info.address");
        return a.length() > 0 ? a : "Untitled inspection";
    }
}
