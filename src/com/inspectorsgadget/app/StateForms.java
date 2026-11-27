package com.inspectorsgadget.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Catalog of official state and national inspection forms, each tied to a state, an inspection
 * type and the licenses that may sign it. Data from the 2026-11-27 state forms research
 * (research/State-Required-Inspection-Forms.xlsx, "Form detail" tab). No field mapping yet:
 * this only answers which forms apply to an inspection and whether the inspector can issue them.
 */
public final class StateForms {
    private StateForms() {}

    /** Code for forms that apply in every state. */
    public static final String ALL = "ALL";

    public enum Status { MANDATORY, CONDITIONAL, CUSTOMARY, CONTENT, SELLER }

    public enum Confidence { HIGH, MEDIUM, LOW }

    // ---------------- licenses ----------------

    public static final String HOME_INSPECTOR = "home", BUILDING_CODE = "codeinsp", CONTRACTOR = "contractor",
            ROOFING_CONTRACTOR = "roofer", ENGINEER = "engineer", ARCHITECT = "architect", OTHER = "other",
            PEST = "pest", SEPTIC = "septic", MOLD_ASSESSOR = "moldassessor", MOLD_REMEDIATOR = "moldremediator",
            LEAD = "lead", ASBESTOS = "asbestos", FORTIFIED = "fortified", WATER_LAB = "waterlab";

    /** Every license, in the order offered on the inspector profile. */
    public static final String[] LICENSES = {HOME_INSPECTOR, BUILDING_CODE, CONTRACTOR, ROOFING_CONTRACTOR,
            ENGINEER, ARCHITECT, PEST, SEPTIC, MOLD_ASSESSOR, MOLD_REMEDIATOR, LEAD, ASBESTOS, FORTIFIED,
            WATER_LAB, OTHER};

    /** Licenses that are not issued by a state, so they count in any state. */
    static final List<String> NATIONAL_LICENSES = Arrays.asList(FORTIFIED);

    /** Key on an inspector record holding the licenses held (as titles). */
    public static final String LICENSES_KEY = "ip.licenses";
    /** Key on an inspector record holding the licensing state. */
    public static final String LICENSE_STATE_KEY = "ip.state";
    /** Key on an inspection holding the property state. */
    public static final String PROPERTY_STATE_KEY = "info.state";

    public static String licenseTitle(String id) {
        switch (id) {
            case HOME_INSPECTOR: return "Home inspector";
            case BUILDING_CODE: return "Building code inspector";
            case CONTRACTOR: return "General, building or residential contractor";
            case ROOFING_CONTRACTOR: return "Roofing contractor";
            case ENGINEER: return "Professional engineer";
            case ARCHITECT: return "Architect";
            case PEST: return "Pest control, WDO / termite";
            case SEPTIC: return "Septic / onsite wastewater inspector";
            case MOLD_ASSESSOR: return "Mold assessor";
            case MOLD_REMEDIATOR: return "Mold remediation contractor";
            case LEAD: return "Lead inspector / risk assessor";
            case ASBESTOS: return "Asbestos inspector";
            case FORTIFIED: return "IBHS FORTIFIED evaluator";
            case WATER_LAB: return "Certified water testing laboratory";
            case OTHER: return "Other individual recognized by the insurer";
            default: return id;
        }
    }

    static String[] licenseTitles() {
        String[] out = new String[LICENSES.length];
        for (int i = 0; i < out.length; i++) out[i] = licenseTitle(LICENSES[i]);
        return out;
    }

    /** License ids from titles (or ids), in catalog order. */
    static List<String> licenseIds(List<String> titlesOrIds) {
        List<String> out = new ArrayList<>();
        for (String id : LICENSES)
            if (titlesOrIds.contains(id) || titlesOrIds.contains(licenseTitle(id))) out.add(id);
        return out;
    }

    /** The wind mitigation certification license types (Forms.LICENSE_TYPES), in order. */
    private static final String[] WIND_CERT_LICENSES = {HOME_INSPECTOR, BUILDING_CODE, CONTRACTOR, ENGINEER,
            ARCHITECT, OTHER};

    /** License held for a Forms.LICENSE_TYPES entry, or null. */
    static String licenseForWindCertType(String licenseType) {
        for (int i = 0; i < Forms.LICENSE_TYPES.length && i < WIND_CERT_LICENSES.length; i++)
            if (Forms.LICENSE_TYPES[i].equals(licenseType)) return WIND_CERT_LICENSES[i];
        return null;
    }

    /** Licenses an inspector holds: those selected, plus the one named by the license type. */
    public static List<String> licensesHeld(Inspection ip) {
        if (ip == null) return new ArrayList<>();
        List<String> raw = new ArrayList<>(ip.list(LICENSES_KEY));
        String fromType = licenseForWindCertType(ip.str("ip.licenseType"));
        if (fromType != null) raw.add(fromType);
        return licenseIds(raw);
    }

    static void setLicensesHeld(Inspection ip, List<String> ids) {
        org.json.JSONArray a = new org.json.JSONArray();
        for (String id : licenseIds(ids)) a.put(licenseTitle(id));
        ip.setUser(LICENSES_KEY, a);
    }

    // ---------------- states ----------------

    public static final String[] STATES = {"AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL", "GA", "HI",
            "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV",
            "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT",
            "VA", "WA", "WV", "WI", "WY"};

    private static final String[] STATE_NAMES = {"Alabama", "Alaska", "Arizona", "Arkansas", "California",
            "Colorado", "Connecticut", "Delaware", "District of Columbia", "Florida", "Georgia", "Hawaii", "Idaho",
            "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts",
            "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire",
            "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon",
            "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah",
            "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming"};

    /** Two-letter code for a state code or name typed in any case ("fl", "Florida"), or "". */
    public static String stateCode(String s) {
        if (s == null) return "";
        String t = s.trim().replace(".", "");
        if (t.length() == 0) return "";
        for (int i = 0; i < STATES.length; i++)
            if (STATES[i].equalsIgnoreCase(t) || STATE_NAMES[i].equalsIgnoreCase(t)) return STATES[i];
        return "";
    }

    public static String stateName(String code) {
        for (int i = 0; i < STATES.length; i++) if (STATES[i].equals(code)) return STATE_NAMES[i];
        return code;
    }

    /** Property state of an inspection; the company state when none is entered. */
    public static String propertyState(Inspection in, Inspection company) {
        String s = in == null ? "" : stateCode(in.str(PROPERTY_STATE_KEY));
        if (s.length() == 0 && company != null) s = stateCode(company.str("co.state"));
        return s;
    }

    /** State an inspector is licensed in; the company state when none is selected. */
    public static String licenseState(Inspection ip, Inspection company) {
        String s = ip == null ? "" : stateCode(ip.str(LICENSE_STATE_KEY));
        if (s.length() == 0 && company != null) s = stateCode(company.str("co.state"));
        return s;
    }

    // ---------------- forms ----------------

    static final int F_REPLACES_NATIONAL = 1, F_UNALTERED = 2;

    public static final class OfficialForm {
        public final String id, state, type, number, name, issuer, statusText, appliesWhen, source;
        public final Status status;
        public final Confidence confidence;
        /** A state form that is used instead of the national form of the same type. */
        public final boolean replacesNational;
        /** The issuing body requires the form reproduced unaltered. */
        public final boolean unaltered;
        /** Licenses that may issue the form; empty when it is not an inspector's form. */
        public final List<String> licenses;

        OfficialForm(String id, String state, String type, String number, String name, String issuer,
                     Status status, String statusText, String appliesWhen, String source, Confidence confidence,
                     int flags, List<String> licenses) {
            this.id = id;
            this.state = state;
            this.type = type;
            this.number = number;
            this.name = name;
            this.issuer = issuer;
            this.status = status;
            this.statusText = statusText;
            this.appliesWhen = appliesWhen;
            this.source = source;
            this.confidence = confidence;
            this.replacesNational = (flags & F_REPLACES_NATIONAL) != 0;
            this.unaltered = (flags & F_UNALTERED) != 0;
            this.licenses = Collections.unmodifiableList(licenses);
        }

        public boolean national() { return ALL.equals(state); }

        /** Short label, e.g. "FL OIR-B1-1802". */
        public String label() {
            String n = number.replaceAll("\\s*\\(.*$", "").trim();
            if (n.startsWith("No ") || n.startsWith("None") || n.length() > 32) n = name;
            return (national() ? "" : state + " ") + n;
        }

        public boolean inspectorForm() { return !licenses.isEmpty(); }
    }

    private static Map<String, OfficialForm> catalog;

    private static List<String> cred(String... ids) { return Arrays.asList(ids); }

    private static void add(String id, String state, String type, String number, String name, String issuer,
                            Status status, String statusText, String appliesWhen, String source,
                            Confidence confidence, int flags, List<String> licenses) {
        catalog.put(id, new OfficialForm(id, state, type, number, name, issuer, status, statusText, appliesWhen,
                source, confidence, flags, licenses));
    }

    private static synchronized Map<String, OfficialForm> catalog() {
        if (catalog != null) return catalog;
        catalog = new LinkedHashMap<>();
        add("all-npma-33", "ALL", Forms.WDO, "NPMA-33 (rev. 2019)", "Wood Destroying Insect Inspection Report", "National Pest Management Association", Status.CUSTOMARY, "Customary (lender-required)", "Default WDI form in states without an official state form; VA and HUD/FHA loans require it where a WDI report is required", "https://npmapestworld.org/npmaforms", Confidence.HIGH, 0, cred(PEST));
        add("all-lead-inspection-risk-assessm", "ALL", Forms.LEAD, "No fixed form (content set by 40 CFR 745.227)", "Lead inspection / risk assessment report", "US EPA / HUD", Status.CONTENT, "Mandatory content", "Any lead inspection or risk assessment by a certified inspector or risk assessor on target housing", "https://www.ecfr.gov/current/title-40/chapter-I/subchapter-R/part-745/subpart-L/section-745.227", Confidence.HIGH, 0, cred(LEAD));
        add("all-epa-hud-sample-disclosure", "ALL", Forms.LEAD, "EPA/HUD sample disclosure", "Disclosure of Information on Lead-Based Paint and Lead-Based Paint Hazards", "US EPA / HUD", Status.SELLER, "Mandatory (seller or landlord form, not the inspector's)", "Sale or lease of pre-1978 housing; inspector reports get attached to it", "https://www.epa.gov/lead/real-estate-disclosure", Confidence.HIGH, 0, cred());
        add("all-fortified-roof-silver-gold", "ALL", Forms.WIND, "FORTIFIED Roof / Silver / Gold", "FORTIFIED Home evaluation and designation certificate", "IBHS (Insurance Institute for Business & Home Safety)", Status.CONDITIONAL, "Conditional", "Triggers mandatory insurer discounts or grants in AL, LA, MS, NC, SC, GA, OK and others; evaluation done by IBHS-certified evaluators", "https://fortifiedhome.org", Confidence.HIGH, 0, cred(FORTIFIED));
        add("all-40-cfr-61-subpart-m-notifica", "ALL", Forms.ASBESTOS, "40 CFR 61 Subpart M notification (state or local agency form)", "Asbestos NESHAP demolition / renovation notification", "US EPA, delegated to state air agencies", Status.CONDITIONAL, "Conditional", "Demolition or large renovations of regulated facilities; not part of single-family home sale inspections", "https://www.epa.gov/asbestos/asbestos-national-emission-standards-hazardous-air-pollutants-neshap", Confidence.MEDIUM, 0, cred(ASBESTOS));
        add("fl-oir-b1-1802", "FL", Forms.WIND, "OIR-B1-1802 (Rev. 01/12)", "Uniform Mitigation Verification Inspection Form", "Florida Office of Insurance Regulation", Status.MANDATORY, "Mandatory", "Required form for any insurer wind mitigation credit (Rule 69O-170.0155, F.A.C.)", "https://floir.com", Confidence.HIGH, 0 | F_UNALTERED, cred(HOME_INSPECTOR, BUILDING_CODE, CONTRACTOR, ENGINEER, ARCHITECT, OTHER));
        add("fl-mit-bt-ii-iii", "FL", Forms.WIND, "MIT-BT II & III", "Building Type II and III Mitigation Verification", "Citizens Property Insurance Corp.", Status.CONDITIONAL, "Mandatory (Citizens)", "Wind credits on mid-rise and high-rise (Type II and III) buildings insured by Citizens", "https://www.citizensfla.com/inspections", Confidence.MEDIUM, 0, cred(BUILDING_CODE, CONTRACTOR, ENGINEER, ARCHITECT));
        add("fl-citizens-4-point", "FL", Forms.FOUR_POINT, "Citizens 4-Point (Insp4pt, current rev. on Citizens site)", "4-Point Inspection Form", "Citizens Property Insurance Corp.", Status.MANDATORY, "Mandatory (Citizens), customary with other FL carriers", "Older homes (Citizens: 30+ years) at new business; most private FL carriers accept or require the same form", "https://www.citizensfla.com/inspections", Confidence.HIGH, 0, cred(HOME_INSPECTOR, BUILDING_CODE, CONTRACTOR, ENGINEER, ARCHITECT));
        add("fl-citizens-roof-inspection-for", "FL", Forms.ROOF, "Citizens Roof Inspection Form (updated 03/2025)", "Roof Inspection Form", "Citizens Property Insurance Corp.", Status.MANDATORY, "Mandatory (Citizens), customary with other FL carriers", "Older roofs at new business or renewal per Citizens underwriting rules", "https://www.citizensfla.com/-/20250320-roof-and-4-point-inspection-form-updates", Confidence.HIGH, 0, cred(HOME_INSPECTOR, BUILDING_CODE, CONTRACTOR, ENGINEER, ARCHITECT, ROOFING_CONTRACTOR));
        add("fl-fdacs-13645", "FL", Forms.WDO, "FDACS-13645", "Wood-Destroying Organisms Inspection Report", "Florida Dept. of Agriculture and Consumer Services", Status.MANDATORY, "Mandatory", "Every WDO inspection report issued by a licensed pest control business (s. 482.226, F.S.; Rule 5E-14.142)", "https://forms.fdacs.gov/13645.pdf", Confidence.HIGH, F_REPLACES_NATIONAL | F_UNALTERED, cred(PEST));
        add("fl-dep-4015", "FL", Forms.SEWER, "DEP 4015 (page 4)", "Existing System and System Repair Evaluation", "Florida Dept. of Environmental Protection", Status.CONDITIONAL, "Conditional / customary", "Required for existing-system approvals, modifications and repairs; commonly used for sale inspections. No statewide point-of-sale mandate", "https://floridadep.gov/water/onsite-sewage", Confidence.MEDIUM, 0, cred(SEPTIC, ENGINEER));
        add("tx-trec-rei-7-6", "TX", Forms.GENERAL, "TREC REI 7-6", "Property Inspection Report Form", "Texas Real Estate Commission", Status.MANDATORY, "Mandatory", "Licensed inspectors on 1-4 family resale inspections must use the promulgated form (22 TAC 535.222-535.223)", "https://www.trec.texas.gov/forms/property-inspection-report-form", Confidence.HIGH, 0 | F_UNALTERED, cred(HOME_INSPECTOR));
        add("tx-spcs-t-5", "TX", Forms.WDO, "SPCS/T-5", "Official Texas Wood Destroying Insect Report", "Texas Dept. of Agriculture, Structural Pest Control Service", Status.MANDATORY, "Mandatory", "WDI inspections for real estate transactions (4 TAC 7.175-7.176)", "https://texasagriculture.gov/RegulatoryPrograms/Pesticides/StructuralPestControlService/StructuralPestControlServiceForms.aspx", Confidence.HIGH, F_REPLACES_NATIONAL | F_UNALTERED, cred(PEST));
        add("tx-tdi-wpi-8", "TX", Forms.WIND, "TDI WPI-8 (also WPI-8-C, WPI-8-E; WPI-2 inspection request)", "Windstorm Certificate of Compliance", "Texas Dept. of Insurance", Status.CONDITIONAL, "Mandatory (for TWIA eligibility)", "New construction, repairs and additions in the designated catastrophe area (coastal counties)", "https://tdi.texas.gov/WIND/inspectionproc.html", Confidence.HIGH, 0, cred(ENGINEER));
        add("tx-cmdr", "TX", Forms.MOLD, "CMDR", "Certificate of Mold Damage Remediation", "Texas Dept. of Licensing and Regulation", Status.CONDITIONAL, "Mandatory (after remediation)", "Issued by licensed mold remediator after a licensed assessor's clearance; seller must give copies to buyer", "https://www.tdlr.texas.gov/mld/mld.htm", Confidence.MEDIUM, 0, cred(MOLD_REMEDIATOR));
        add("la-wdir", "LA", Forms.WDO, "WDIR (LDAF prescribed form)", "Louisiana Wood Destroying Insect Report", "Louisiana Dept. of Agriculture and Forestry", Status.MANDATORY, "Mandatory", "All WDI reports for real estate transactions (LAC 7:XXV.121); reports filed with LDAF", "https://www.ldaf.la.gov/business/pest-control/reporting", Confidence.HIGH, F_REPLACES_NATIONAL, cred(PEST));
        add("ga-georgia-wiir", "GA", Forms.WDO, "Georgia WIIR", "Official Georgia Wood Infestation Inspection Report", "Georgia Dept. of Agriculture, Structural Pest Control Commission", Status.MANDATORY, "Mandatory", "Real estate WDI inspections, including HUD and VA loans (replaces NPMA-33 in GA)", "https://agr.georgia.gov/sites/default/files/documents/pest-control/spcc-notice-14-10-official-georgia-wood-infestation-inspection-report-factsheet.pdf", Confidence.HIGH, F_REPLACES_NATIONAL, cred(PEST));
        add("al-adai-prescribed-form", "AL", Forms.WDO, "ADAI prescribed form (Ala. Admin. Code 80-10-9-.18)", "Alabama Official Wood Infestation Report", "Alabama Dept. of Agriculture and Industries", Status.MANDATORY, "Mandatory", "WDI reports for real estate transactions", "https://admincode.legislature.state.al.us/api/rule/80-10-9-.18", Confidence.MEDIUM, F_REPLACES_NATIONAL, cred(PEST));
        add("ms-mdac-prescribed-form", "MS", Forms.WDO, "MDAC prescribed form (2 Miss. Code R. 1-3-11-01-117.01)", "Mississippi Official Wood Destroying Insect Report", "Mississippi Dept. of Agriculture and Commerce, Bureau of Plant Industry", Status.MANDATORY, "Mandatory", "WDI reports for real estate transactions", "https://www.law.cornell.edu/regulations/mississippi/2-Miss-Code-R-SS-1-3-11-01-117-01", Confidence.HIGH, F_REPLACES_NATIONAL, cred(PEST));
        add("tn-tda-prescribed-form", "TN", Forms.WDO, "TDA prescribed form", "Tennessee Wood Destroying Insect Infestation Inspection Report", "Tennessee Dept. of Agriculture", Status.MANDATORY, "Mandatory", "WDI reports for real estate transactions (Tenn. Code 62-21 and TDA pest control rules)", "https://www.tn.gov/agriculture/businesses/pest-control.html", Confidence.MEDIUM, F_REPLACES_NATIONAL, cred(PEST));
        add("sc-cl-100", "SC", Forms.WDO, "CL-100", "Official South Carolina Wood Infestation Report", "Clemson University Dept. of Pesticide Regulation", Status.MANDATORY, "Mandatory", "Any wood infestation report issued for a real estate transaction (S.C. Reg. 27-1085)", "https://www.clemson.edu/public/regulatory/pesticide-regulation/bulletins/bulletin-4-wood-infestation-reports.pdf", Confidence.HIGH, F_REPLACES_NATIONAL, cred(PEST));
        add("sc-insurer-wind-mitigation-cred", "SC", Forms.WIND, "No single state form (S.C. Code 38-75-485 discounts)", "Insurer wind mitigation credit forms / SC Safe Home inspection", "SC Dept. of Insurance (SC Safe Home); individual insurers", Status.CUSTOMARY, "Customary", "Insurer discounts for mitigation features; SC Safe Home grant pre- and post-mitigation inspections", "https://doi.sc.gov/605/SC-Safe-Home/Grant", Confidence.MEDIUM, 0, cred(HOME_INSPECTOR, BUILDING_CODE, CONTRACTOR, ENGINEER, ARCHITECT));
        add("nc-nc-wdir-100", "NC", Forms.WDO, "NC WDIR-100", "North Carolina Wood Destroying Insect Report", "NC Dept. of Agriculture and Consumer Services, Structural Pest Control", Status.MANDATORY, "Mandatory", "WDI reports in NC (replaces NPMA-33 in NC)", "https://ncagr.gov/divisions/structural-pests-and-pesticides/nc-wdir-100-memorandum/open", Confidence.MEDIUM, F_REPLACES_NATIONAL, cred(PEST));
        add("ok-odaff-1", "OK", Forms.WDO, "ODAFF-1", "Oklahoma Official Termite / Wood Destroying Organisms Report", "Oklahoma Dept. of Agriculture, Food and Forestry", Status.MANDATORY, "Mandatory", "WDO reports for real estate transactions (OAC 35:30-17-107)", "https://ag.ok.gov/wp-content/uploads/2021/11/Oklahoma-Offical-Termite-Wood-Destroying-Organisms-Report-Form-ODAFF-1.pdf", Confidence.HIGH, F_REPLACES_NATIONAL, cred(PEST));
        add("az-arizona-wdiir", "AZ", Forms.WDO, "Arizona WDIIR", "Wood Destroying Insect Inspection Report", "Arizona Dept. of Agriculture, Office of Pest Management", Status.MANDATORY, "Mandatory", "All WDI inspections for real estate transactions", "https://opm.azda.gov/Assets/PDFDocuments/WDIIR.pdf", Confidence.HIGH, F_REPLACES_NATIONAL, cred(PEST));
        add("az-adeq-report-of-inspection", "AZ", Forms.SEWER, "ADEQ Report of Inspection (A.A.C. R18-9-A316)", "Transfer of Ownership Inspection Report (plus buyer's Notice of Transfer)", "Arizona Dept. of Environmental Quality", Status.MANDATORY, "Mandatory", "Sale of any property served by an onsite wastewater system; inspection within 6 months before transfer", "https://azdeq.gov/node/398", Confidence.HIGH, 0, cred(SEPTIC));
        add("ca-spcb-standard-report-form", "CA", Forms.WDO, "SPCB standard report form (16 CCR 1996)", "Wood Destroying Pests and Organisms Inspection Report", "California Structural Pest Control Board", Status.MANDATORY, "Mandatory", "Every WDO inspection report issued by a Branch 3 licensee; NPMA-33 added only as a lender supplement", "https://www.pestboard.ca.gov", Confidence.MEDIUM, F_REPLACES_NATIONAL, cred(PEST));
        add("nm-lw-902", "NM", Forms.SEWER, "LW-902", "Property Transfer Evaluation Report", "New Mexico Environment Dept., Liquid Waste Program", Status.MANDATORY, "Mandatory", "Transfer of property served by an onsite liquid waste system (20.7.3.902 NMAC), by a certified third-party evaluator", "https://www.env.nm.gov/septic/wp-content/uploads/sites/14/2020/06/LW-Property-Transfer-Evaluation-Report-Form-LW902-200701.pdf", Confidence.HIGH, 0, cred(SEPTIC));
        add("nv-npma-33-commonly-used-conten", "NV", Forms.WDO, "NPMA-33 commonly used; content set by NAC 555 (amended by R005-25)", "Wood Destroying Insect Inspection Report", "Nevada Dept. of Agriculture", Status.CUSTOMARY, "Customary (verify)", "Report content is regulated; check whether R005-25 (2025) adopted a state form before building", "https://agri.nv.gov/Plant/PEST/Correspondence_FHA/VA_HUD", Confidence.LOW, F_REPLACES_NATIONAL, cred(PEST));
        add("wa-wdo-inspection-report", "WA", Forms.WDO, "No state form; required content in WAC 16-228-2045", "WDO inspection report", "Washington State Dept. of Agriculture", Status.CONTENT, "Mandatory content", "Any WDO inspection report by a licensed structural pest inspector", "https://app.leg.wa.gov/wac/default.aspx?cite=16-228-2045", Confidence.MEDIUM, F_REPLACES_NATIONAL, cred(PEST));
        add("hi-npma-33-commonly-used", "HI", Forms.WDO, "NPMA-33 commonly used", "Termite inspection report", "Hawaii Pesticides Branch; lenders", Status.CUSTOMARY, "Customary (verify)", "Lender-required termite reports", "https://hdoa.hawaii.gov/pi/pest/", Confidence.LOW, F_REPLACES_NATIONAL, cred(PEST));
        add("ma-title-5-official-inspection", "MA", Forms.SEWER, "Title 5 Official Inspection Form (310 CMR 15.301)", "Title 5 Official Inspection Form", "Massachusetts Dept. of Environmental Protection", Status.MANDATORY, "Mandatory", "Sale or transfer of any property with a septic system (inspection within 2 years before sale)", "https://www.mass.gov/doc/title-5-official-inspection-form/download", Confidence.HIGH, 0, cred(SEPTIC));
        add("ma-ma-dph-clppp-letters", "MA", Forms.LEAD, "MA DPH CLPPP letters", "Letter of Lead Safe Compliance / Letter of Interim Control", "Massachusetts Dept. of Public Health, Childhood Lead Poisoning Prevention Program", Status.CONDITIONAL, "Conditional", "Pre-1978 homes where a child under 6 lives (rental or owner-occupied); sellers must give the Property Transfer Lead Paint Notification", "https://www.mass.gov/lead-law-and-regulations", Confidence.MEDIUM, 0, cred(LEAD));
        add("de-dnrec-pre-transfer-inspectio", "DE", Forms.SEWER, "DNREC pre-transfer inspection (7 Del. Admin. Code 7101)", "Pre-Transfer Septic Inspection Report", "Delaware Dept. of Natural Resources and Environmental Control", Status.MANDATORY, "Mandatory", "Property transfer with an onsite system, by a DNREC-licensed Class H inspector", "https://dnrec.delaware.gov/water/digital-resources/septic-system-inspections", Confidence.HIGH, 0, cred(SEPTIC));
        add("ia-iowa-dnr-542-series-form", "IA", Forms.SEWER, "Iowa DNR 542-series form (IAC 567-69.2)", "Time of Transfer Inspection Report", "Iowa Dept. of Natural Resources", Status.MANDATORY, "Mandatory", "Transfer of any building served by a private septic system, by a DNR-certified inspector; filed with county", "https://www.iowadnr.gov/environmental-protection/water-quality/private-septic-systems/time-transfer", Confidence.HIGH, 0, cred(SEPTIC));
        add("mn-mpca-wq-wwists4-31a", "MN", Forms.SEWER, "MPCA wq-wwists4-31a", "Existing SSTS Compliance Inspection Form", "Minnesota Pollution Control Agency", Status.CONDITIONAL, "Conditional", "Required where the county or city ordinance requires inspection at transfer (many counties); also for permits", "https://www.pca.state.mn.us/water/inspections", Confidence.HIGH, 0, cred(SEPTIC));
        add("ri-ridem-accepted-inspection", "RI", Forms.SEWER, "RIDEM-accepted inspection (R.I. Gen. Laws 23-19.15-12)", "Cesspool inspection at transfer", "Rhode Island Dept. of Environmental Management", Status.MANDATORY, "Mandatory (cesspools)", "Sale of a property served by a cesspool; cesspool must be replaced within 12 months of sale", "https://dem.ri.gov/septic", Confidence.MEDIUM, 0, cred(SEPTIC));
        add("ri-ri-housing-resources-commiss", "RI", Forms.LEAD, "RI Housing Resources Commission / RIDOH certificate", "Lead Certificate of Conformance", "Rhode Island Housing Resources Commission", Status.MANDATORY, "Mandatory (rentals)", "Pre-1978 rental units", "https://health.ri.gov/lead", Confidence.MEDIUM, 0, cred(LEAD));
        add("me-dhhs-inspection-report-form", "ME", Forms.SEWER, "DHHS inspection report form (30-A M.R.S. 4216)", "Subsurface wastewater system inspection (shoreland transfer)", "Maine DHHS, Subsurface Wastewater Program", Status.CONDITIONAL, "Conditional", "Transfer of property in the shoreland zone served by a private system", "https://www.maine.gov/dhhs/mecdc/environmental-health/plumb/inspections.htm", Confidence.MEDIUM, 0, cred(SEPTIC));
        add("nh-nhdes-site-assessment-form", "NH", Forms.SEWER, "NHDES site assessment form (RSA 485-A:39)", "Waterfront property site assessment study", "NH Dept. of Environmental Services", Status.CONDITIONAL, "Conditional", "Sale of developed waterfront property within 200 ft of a great pond or fourth-order stream", "https://www.des.nh.gov", Confidence.MEDIUM, 0, cred(SEPTIC));
        add("nj-pwta-reporting-form", "NJ", Forms.PLUMBING, "PWTA reporting form", "Private Well Testing Act test report", "New Jersey Dept. of Environmental Protection", Status.MANDATORY, "Mandatory", "Sale of property served by a private well (lab completes, results to buyer and seller)", "https://www.nj.gov/dep/pwta/njtestreportingform.pdf", Confidence.HIGH, 0, cred(WATER_LAB));
        add("nj-dca-lead-safe-certification", "NJ", Forms.LEAD, "DCA Lead-Safe Certification form", "Lead-Safe Certification", "NJ Dept. of Community Affairs", Status.MANDATORY, "Mandatory (rentals)", "Pre-1978 rental units at tenant turnover and periodic inspection (P.L. 2021, c.182)", "https://www.nj.gov/dca/divisions/codes/forms/pdf_lead/lead_safe_cert_fill.pdf", Confidence.HIGH, 0, cred(LEAD));
        add("md-mde-form-330-lead-form-e", "MD", Forms.LEAD, "MDE Form 330; Lead Form E", "Lead Paint Risk Reduction Inspection Certificate (and Lead-Free certificate)", "Maryland Dept. of the Environment", Status.MANDATORY, "Mandatory (rentals)", "Pre-1978 rental units at each tenant turnover", "https://mde.maryland.gov/programs/Land/LeadPoisoningPrevention/Pages/rentalowners_inspections.aspx", Confidence.HIGH, 0, cred(LEAD));
        add("vt-vdh-emp-compliance-statement", "VT", Forms.LEAD, "VDH EMP compliance statement", "Essential Maintenance Practices Compliance Statement", "Vermont Dept. of Health", Status.MANDATORY, "Mandatory (rentals)", "Pre-1978 rental housing and child care facilities", "https://www.healthvermont.gov/environment/children/lead-poisoning-prevention", Confidence.MEDIUM, 0, cred(LEAD));
        add("dc-doee-clearance", "DC", Forms.LEAD, "DOEE clearance (DC Lead-Hazard Prevention and Elimination Act)", "Lead clearance report for rental units", "DC Dept. of Energy and Environment", Status.CONDITIONAL, "Conditional (verify)", "Pre-1978 rentals at turnover or when a child under 6 or pregnant person resides", "https://doee.dc.gov/service/lead-poisoning-prevention", Confidence.LOW, 0, cred(LEAD));
        add("in-npma-33", "IN", Forms.WDO, "NPMA-33 (OISC secured version)", "Wood Destroying Insect Inspection Report", "Office of Indiana State Chemist", Status.CUSTOMARY, "Customary (state-published)", "Real estate WDI inspections", "https://www.oisc.purdue.edu/pesticide/pdf/npma_33_secured.pdf", Confidence.MEDIUM, F_REPLACES_NATIONAL, cred(PEST));
        add("oh-npma-33", "OH", Forms.WDO, "NPMA-33", "Wood Destroying Insect Inspection Report", "Ohio Dept. of Agriculture (publishes the form)", Status.CUSTOMARY, "Customary (state-published)", "Real estate WDI inspections", "https://agri.ohio.gov", Confidence.MEDIUM, F_REPLACES_NATIONAL, cred(PEST));
        return catalog;
    }

    public static List<OfficialForm> all() { return new ArrayList<>(catalog().values()); }

    public static OfficialForm get(String id) { return catalog().get(id); }

    /** Forms for an inspection type in a state: the state's own forms, then national forms it keeps. */
    public static List<OfficialForm> forState(String state, String type) {
        List<OfficialForm> own = new ArrayList<>(), national = new ArrayList<>();
        boolean replaced = false;
        for (OfficialForm f : catalog().values()) {
            if (!f.type.equals(type)) continue;
            if (f.national()) national.add(f);
            else if (f.state.equals(state)) {
                own.add(f);
                replaced |= f.replacesNational;
            }
        }
        if (!replaced) own.addAll(national);
        return own;
    }

    /** Forms for every type of an inspection, in type order. */
    public static List<OfficialForm> forTypes(String state, List<String> types) {
        List<OfficialForm> out = new ArrayList<>();
        for (String t : types) out.addAll(forState(state, t));
        return out;
    }

    /** Inspection types with no official form anywhere. */
    public static boolean hasAnyForm(String type) {
        for (OfficialForm f : catalog().values()) if (f.type.equals(type)) return true;
        return false;
    }

    /**
     * Whether licenses held in licenseState let the inspector issue the form for a property in
     * propertyState. Forms that are not an inspector's (seller disclosures) always pass.
     */
    public static boolean canIssue(OfficialForm f, List<String> held, String licenseState, String propertyState) {
        if (!f.inspectorForm()) return true;
        boolean inState = licenseState != null && licenseState.length() > 0 && licenseState.equals(propertyState);
        for (String l : f.licenses) {
            if (!held.contains(l)) continue;
            if (inState || NATIONAL_LICENSES.contains(l)) return true;
        }
        return false;
    }

    public static String statusLabel(Status s) {
        switch (s) {
            case MANDATORY: return "Required";
            case CONDITIONAL: return "Conditional";
            case CUSTOMARY: return "Customary";
            case CONTENT: return "Content rules";
            case SELLER: return "Seller form";
            default: return s.name().toLowerCase(Locale.US);
        }
    }
}
