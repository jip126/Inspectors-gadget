package com.inspectorsgadget.app;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Company profile and inspector roster. Both are stored as records in the same store as
 * inspections (ids starting with "_") so they can be edited with the regular section screen.
 */
public final class Company {
    private Company() {}

    public static final String PROFILE_ID = "_company";
    public static final String INSPECTOR_PREFIX = "_ip_";

    public static final String DEFAULT_FEE_TERMS = "The fee for the inspection is due upon completion of the physical "
            + "inspection. A $50.00 fee will be applied to all returned checks and to any invoice outstanding 30 days or "
            + "more from the time of the inspection. Any follow-up visit to the inspected Premises shall be a minimum "
            + "charge of $100.00 due at the time of re-inspection.";

    /** Page two of the agreement. {COMPANY} is replaced with the company name. */
    public static final String DEFAULT_TERMS =
            "4. Scope of Inspection. The scope of the inspection is strictly limited as set forth in this Agreement. The "
            + "parties understand and agree that this inspection will be of readily accessible areas of the building and "
            + "is limited to visual observations of apparent conditions existing at the time of the inspection only. "
            + "Latent and concealed defects and deficiencies, including but not limited to, basement flooding, basement "
            + "seepage and roof leakage, are excluded from this inspection. The parties agree that the American Society "
            + "of Home Inspectors® (ASHI®) Standards of Practice, most current edition, shall define the standard of "
            + "duty and the conditions, limitations, and exclusions of the inspection and are incorporated by reference "
            + "herein. Copies of these standards are available upon request.\n\n"
            + "5. Items Not Inspected. The inspection and report exclude and do not cover those items indicated as "
            + "“untested” or “not inspected” or the possible presence of or danger from any potentially harmful "
            + "substances and environmental hazards including, but not limited to, the following items: asbestos; lead "
            + "paint; formaldehyde; toxic and flammable materials; soil contamination; water testing; termite and pest "
            + "infestation; water softener and filter system; sprinkler systems; central vacuum systems; telephone "
            + "systems; intercom systems; security systems; antennas; cable television; doorbells; appliances; "
            + "playground equipment; swimming pools and pool equipment; spas; energy efficiency measurements; "
            + "recreational equipment; recreational facilities (boat docks and tennis courts); landscaping; underground "
            + "storage tanks; underground drainage; irrigation; outdoor grills; low voltage exterior lighting; remote "
            + "overhead door transmitters and receivers; concealed or underground electric and plumbing; systems which "
            + "are shut down or otherwise secured; private sewer systems; water wells; chimney draft; heating system "
            + "accessories; solar heating systems; zoning or other ordinances; and building code conformity. All items "
            + "indicated as being excluded in the ASHI® Standards of Practice are also excluded herein. CLIENT "
            + "understands that these systems and conditions and information about them are excluded from this "
            + "Inspection and Report. Any general comments which may appear about these systems and conditions are "
            + "provided as a courtesy only and DO NOT represent or form a part of the Inspection.\n\n"
            + "6. Third Party Liability. It is understood and agreed that the inspection and report are performed and "
            + "prepared for the confidential and exclusive use and possession of CLIENT. No other person or entity may "
            + "rely on the report issued pursuant to this agreement.\n\n"
            + "7. Limitation of Liability. It is understood and agreed that in the event of any claim whatsoever against "
            + "{COMPANY}, it’s employees, inspectors, or other agents, whether based on contract, negligence or other "
            + "tort, statute, or otherwise, in any way directly or indirectly resulting from or relating to the "
            + "inspection, the report or performance or nonperformance of services by {COMPANY}, that, any liability of "
            + "{COMPANY}, it’s employees, inspectors or other agents shall be solely and exclusively limited to the "
            + "amount of the inspection fee actually paid by CLIENT.\n\n"
            + "8. Limitation of Actions. No action shall be maintained by CLIENT against {COMPANY} unless written notice, "
            + "sent by certified mail return receipt requested, setting forth that an installed system or component of "
            + "the Premises which was inspected by the Inspector was not in the condition reported by the Inspector, is "
            + "delivered by CLIENT to {COMPANY} within ten (10) business days after the discovery of such defect becomes "
            + "known to CLIENT. CLIENT agrees that, with the exception of emergency conditions, CLIENT or CLIENT’S "
            + "agents, employees or independent contractors will make no alterations, modifications or repairs to the "
            + "claimed discrepancy prior to a re-inspection by the Inspector. Furthermore, any action must be commenced "
            + "by CLIENT within one (1) year after the date of the Inspection or will be deemed waived and forever "
            + "barred.\n\n"
            + "9. Entire Agreement. This Agreement constitutes the entire Agreement between the parties and supersedes "
            + "any and all prior agreements between the parties. CLIENT agrees that any representation, promise, "
            + "condition, or warranty, express or implied, not included in this Agreement shall not be binding on any "
            + "party.\n\n"
            + "10. Interpretations. It is mutually understood and agreed that all provisions herein are severable and "
            + "that, in the event that any of them shall be held to be invalid, any competent court may modify or reform "
            + "any such provisions to make it enforceable and that the remaining provisions hereof shall nevertheless be "
            + "valid and binding.\n\n"
            + "11. Assignment. The rights and obligations under this Agreement may not be assigned by any of the parties "
            + "hereto without the prior written consent of the other party.\n\n"
            + "12. Attorney’s Fees. The prevailing party in any dispute arising out of this Agreement, the Inspection, "
            + "or Report(s) shall be awarded reasonable attorney’s fees and other costs.";

    /** The company profile record, created with defaults on first use. */
    public static synchronized Inspection profile(Context c) {
        Inspection p = Store.get(c, PROFILE_ID);
        if (p == null) {
            p = Store.createWithId(c, PROFILE_ID);
            p.setUser("co.name", "360 Home Inspections LLC");
            p.setUser("co.address", "2540 Bedford Way");
            p.setUser("co.city", "Tallahassee");
            p.setUser("co.state", "FL");
            p.setUser("co.feeTerms", DEFAULT_FEE_TERMS);
            p.setUser("co.terms", DEFAULT_TERMS);
            p.setUser("co.feeHome", "");
            Store.save(p);
            Inspection ip = addInspector(c);
            ip.setUser("ip.name", "Justin Huffman");
            ip.setUser("ip.title", "Home Inspector");
            ip.setUser("ip.licenseType", Forms.LICENSE_TYPES[0]);
            ip.setUser("ip.license", "HI-12683");
            ip.setUser("ip.issuer", "Florida DBPR");
            ip.setUser(StateForms.LICENSE_STATE_KEY, "FL");
            StateForms.setLicensesHeld(ip, java.util.Arrays.asList(StateForms.HOME_INSPECTOR));
            Forms.setLicensedTypes(ip, java.util.Arrays.asList(Forms.DEFAULT_TYPES));
            Store.save(ip);
        }
        return p;
    }

    /** Inspection types the inspector is licensed for; the default three when none are selected. */
    public static List<String> licensedTypes(Inspection ip) {
        List<String> ids = ip == null ? new ArrayList<String>() : Forms.typeIds(ip.list("ip.types"));
        if (ids.isEmpty()) ids = new ArrayList<>(java.util.Arrays.asList(Forms.DEFAULT_TYPES));
        return ids;
    }

    /** The inspector conducting this inspection (or the first on the roster). */
    public static Inspection inspectorFor(Context c, Inspection in) {
        String id = in == null ? "" : in.str("info.inspectorId");
        if (id.length() > 0) {
            Inspection ip = Store.get(c, id);
            if (ip != null) return ip;
        }
        List<Inspection> roster = inspectors(c);
        return roster.isEmpty() ? null : roster.get(0);
    }

    /**
     * The roster inspector who is signed in: the one chosen in the account menu, else the one whose
     * email matches the login, else the first on the roster.
     */
    public static Inspection signedInInspector(Context c) {
        List<Inspection> roster = inspectors(c);
        if (roster.isEmpty()) return null;
        String email = Auth.email(c);
        String linked = Auth.prefs(c).getString("inspector." + email, "");
        for (Inspection ip : roster) if (ip.id.equals(linked)) return ip;
        for (Inspection ip : roster) {
            if (email.length() > 0 && email.equalsIgnoreCase(ip.str("ip.email").trim())) return ip;
        }
        return roster.get(0);
    }

    /** Links the signed-in account to a roster inspector. */
    public static void setSignedInInspector(Context c, String inspectorId) {
        Auth.prefs(c).edit().putString("inspector." + Auth.email(c), inspectorId).apply();
    }

    public static Inspection addInspector(Context c) {
        String id = INSPECTOR_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Inspection ip = Store.createWithId(c, id);
        ip.setUser("ip.issuer", "Florida DBPR");
        ip.setUser("ip.licenseType", Forms.LICENSE_TYPES[0]);
        String st = StateForms.stateCode(profile(c).str("co.state"));
        if (st.length() > 0) ip.setUser(StateForms.LICENSE_STATE_KEY, st);
        Store.save(ip);
        return ip;
    }

    public static List<Inspection> inspectors(Context c) {
        profile(c);
        List<Inspection> out = new ArrayList<>();
        File[] dirs = Store.rootDir(c).listFiles();
        if (dirs != null) {
            for (File d : dirs) {
                if (!d.getName().startsWith(INSPECTOR_PREFIX)) continue;
                Inspection ip = Store.get(c, d.getName());
                if (ip != null) out.add(ip);
            }
        }
        java.util.Collections.sort(out, new java.util.Comparator<Inspection>() {
            @Override
            public int compare(Inspection a, Inspection b) { return Long.compare(a.created(), b.created()); }
        });
        return out;
    }

    public static String inspectorName(Inspection ip) {
        String n = ip.str("ip.name");
        return n.length() > 0 ? n : "Unnamed inspector";
    }

    /** Company logo (first photo of the profile), or null. */
    public static File logoFile(File inspectionsRoot) {
        Inspection p = Store.peek(new File(inspectionsRoot, PROFILE_ID));
        if (p == null) return null;
        for (String pid : p.sectionPhotos("co")) if (p.photoExists(pid)) return p.displayFile(pid);
        return null;
    }

    static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String part : name.trim().split("\\s+")) if (part.length() > 0) sb.append(Character.toUpperCase(part.charAt(0)));
        return sb.toString();
    }

    /** Uses the first inspector when the inspection has none yet. */
    public static void applyDefault(Context c, Inspection in) {
        List<Inspection> roster = inspectors(c);
        String id = in.str("info.inspectorId");
        if (id.length() == 0 && !roster.isEmpty()) id = roster.get(0).id;
        if (id.length() > 0) applyInspector(c, in, id);
        else applyCompany(c, in);
    }

    /**
     * Copies the chosen inspector's and the company's details into the inspection. Values the
     * inspector later typed over by hand are kept (same rule as the general-inspection prefill).
     */
    public static void applyInspector(Context c, Inspection in, String inspectorId) {
        Inspection ip = Store.get(c, inspectorId);
        if (ip == null) return;
        in.setUser("info.inspectorId", inspectorId);
        fill(in, "info.inspector", ip.str("ip.name"));
        fill(in, "info.inspectorTitle", ip.str("ip.title"));
        fill(in, "info.licenseType", ip.str("ip.licenseType"));
        fill(in, "info.license", ip.str("ip.license"));
        fill(in, "info.issuer", ip.str("ip.issuer"));
        fill(in, "info.licenseExp", ip.str("ip.expires"));
        fill(in, "info.inspectorPhone", ip.str("ip.phone"));
        fill(in, "info.inspectorEmail", ip.str("ip.email"));
        fill(in, "info.initials", initials(ip.str("ip.name")));
        // saved signature
        String sig = ip.str("ip.signature");
        if (sig.length() > 0) {
            File src = new File(new File(ip.dir, "signatures"), sig);
            File dstDir = new File(in.dir, "signatures");
            dstDir.mkdirs();
            String name = "sig_inspector_" + inspectorId.substring(INSPECTOR_PREFIX.length()) + ".png";
            if (copy(src, new File(dstDir, name))) fill(in, "info.inspectorSig", name);
        } else if (in.isPrefilled("info.inspectorSig")) {
            in.clearPrefill("info.inspectorSig");
        }
        applyCompany(c, in);
    }

    public static void applyCompany(Context c, Inspection in) {
        Inspection p = profile(c);
        fill(in, "info.company", p.str("co.name"));
        fill(in, "info.companyPhone", p.str("co.phone"));
        fill(in, "info.companyEmail", p.str("co.email"));
        fill(in, "info.companyWebsite", p.str("co.website"));
        fill(in, "info.companyAddress", Forms.joinNonEmpty(p.str("co.address"),
                Forms.joinNonEmpty(p.str("co.city"), (p.str("co.state") + " " + p.str("co.zip")).trim())));
        fill(in, "info.companyLicense", p.str("co.license"));
        fill(in, StateForms.PROPERTY_STATE_KEY, StateForms.stateCode(p.str("co.state")));
    }

    private static void fill(Inspection in, String key, String value) {
        if (in.has(key) && !in.isPrefilled(key)) return; // typed by hand
        if (value == null || value.length() == 0) {
            if (in.isPrefilled(key)) in.clearPrefill(key);
            return;
        }
        if (!value.equals(in.str(key)) || !in.isPrefilled(key)) in.setPrefill(key, value);
    }

    static boolean copy(File src, File dst) {
        if (!src.exists()) return false;
        try {
            InputStream is = new FileInputStream(src);
            OutputStream os = new FileOutputStream(dst);
            byte[] buf = new byte[16384];
            int n;
            while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
            os.close();
            is.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
