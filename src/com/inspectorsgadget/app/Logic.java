package com.inspectorsgadget.app;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Prefill, findings and progress rules shared by the screens and the PDF reports. */
public final class Logic {
    private Logic() {}

    /**
     * Fills the fields of one section from the general inspection. A field is filled when it
     * has no value yet, or when its current value came from an earlier prefill and was not
     * edited since (so later changes to the general inspection flow through).
     *
     * @return number of fields whose value changed
     */
    public static int prefill(Inspection in, Forms.Section s) {
        int changed = 0;
        for (Forms.Field f : s.fields) {
            if (!f.inputs() || !f.prefillable()) continue;
            if (in.has(f.key) && !in.isPrefilled(f.key)) continue;
            Object v = compute(in, f);
            if (Inspection.isEmpty(v)) {
                // the general answer this came from was cleared or no longer applies
                if (in.isPrefilled(f.key)) {
                    in.clearPrefill(f.key);
                    changed++;
                }
                continue;
            }
            Object stored = (v instanceof List) ? new JSONArray((List<?>) v) : v;
            Object cur = in.raw(f.key);
            if (cur != null && cur.toString().equals(stored.toString()) && in.isPrefilled(f.key)) continue;
            in.setPrefill(f.key, stored);
            changed++;
        }
        return changed;
    }

    public static int prefillForm(Inspection in, String formId) {
        int n = 0;
        for (Forms.Section s : Forms.form(formId).sections) n += prefill(in, s);
        return n;
    }

    static Object compute(Inspection in, Forms.Field f) {
        if (f.mapper != null) return f.mapper.map(in);
        Object srcVal = in.raw(f.src);
        if (Inspection.isEmpty(srcVal)) return null;
        List<String> src = Inspection.toList(srcVal);
        if (f.type == Forms.MULTI) {
            List<String> out = new ArrayList<>();
            for (String v : src) {
                String t = translate(f, v);
                if (t != null && allowed(f, t) && !out.contains(t)) out.add(t);
            }
            return out.isEmpty() ? null : out;
        }
        if (f.type == Forms.CHOICE) {
            for (String v : src) {
                String t = translate(f, v);
                if (t != null && allowed(f, t)) return t;
            }
            return null;
        }
        return in.str(f.src);
    }

    private static String translate(Forms.Field f, String v) {
        if (f.translate != null && f.translate.containsKey(v)) return f.translate.get(v);
        return v;
    }

    private static boolean allowed(Forms.Field f, String v) {
        return f.options == null || Arrays.asList(f.options).contains(v);
    }

    // ---------------- findings ----------------

    public static class Finding {
        public final Forms.Section section;
        public final Forms.Field field;
        public final String value;

        Finding(Forms.Section s, Forms.Field f, String v) {
            section = s;
            field = f;
            value = v;
        }
    }

    public static boolean isFinding(Inspection in, Forms.Field f) {
        if (f.flags == null || !in.has(f.key)) return false;
        List<String> vals = in.list(f.key);
        for (String flag : f.flags) {
            if ("*".equals(flag) && !vals.isEmpty()) return true;
            if (vals.contains(flag)) return true;
        }
        return false;
    }

    /** For multi-select fields with specific flags, only the flagged selections. */
    public static String findingValue(Inspection in, Forms.Field f) {
        if (f.type != Forms.MULTI || f.flags == null || Arrays.asList(f.flags).contains("*")) return in.str(f.key);
        List<String> hits = new ArrayList<>();
        for (String v : in.list(f.key)) if (Arrays.asList(f.flags).contains(v)) hits.add(v);
        return Forms.joinNonEmpty(hits.toArray(new String[0]));
    }

    public static List<Finding> findings(Inspection in, Forms.Section s) {
        List<Finding> out = new ArrayList<>();
        for (Forms.Field f : s.fields) {
            if (isFinding(in, f)) out.add(new Finding(s, f, findingValue(in, f)));
        }
        return out;
    }

    public static List<Finding> findings(Inspection in, String formId) {
        List<Finding> out = new ArrayList<>();
        for (Forms.Section s : Forms.form(formId).sections) out.addAll(findings(in, s));
        return out;
    }

    // ---------------- progress ----------------

    public static int[] progress(Inspection in, Forms.Section s) {
        int total = 0, done = 0;
        for (Forms.Field f : s.fields) {
            if (!f.inputs()) continue;
            total++;
            if (in.has(f.key)) done++;
        }
        return new int[]{done, total};
    }

    public static int[] progress(Inspection in, String formId) {
        int d = 0, t = 0;
        for (Forms.Section s : Forms.form(formId).sections) {
            int[] p = progress(in, s);
            d += p[0];
            t += p[1];
        }
        return new int[]{d, t};
    }

    public static int prefilledCount(Inspection in, String formId) {
        int n = 0;
        for (Forms.Section s : Forms.form(formId).sections)
            for (Forms.Field f : s.fields) if (f.inputs() && in.isPrefilled(f.key)) n++;
        return n;
    }

    public static int photoCount(Inspection in, String formId) {
        List<String> seen = new ArrayList<>();
        for (Forms.Section s : Forms.form(formId).sections)
            for (String p : in.sectionPhotos(s.key)) if (!seen.contains(p)) seen.add(p);
        return seen.size();
    }

    /** Photos from the given general sections that are not yet attached to the target section. */
    public static List<String> importablePhotos(Inspection in, Forms.Section target) {
        List<String> out = new ArrayList<>();
        List<String> have = in.sectionPhotos(target.key);
        for (String src : target.photoSources) {
            for (String p : in.sectionPhotos(src)) {
                if (!have.contains(p) && !out.contains(p) && in.photoExists(p)) out.add(p);
            }
        }
        return out;
    }
}
