package com.inspectorsgadget.app;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Field definitions for the three inspection modules.
 *
 * Fields in the 4-Point and Wind Mitigation forms point at a general inspection
 * field with src(...) or compute their value with map(...). Prefill uses those
 * links to fill the reports from what the inspector already captured.
 */
public final class Forms {
    private Forms() {}

    public static final int TEXT = 0, NUMBER = 1, DATE = 2, CHOICE = 3, MULTI = 4,
            NOTES = 5, SIGNATURE = 6, HEADER = 7, PICK_INSPECTOR = 8;

    public static final String INFO = "info", GENERAL = "general", FOUR_POINT = "fourpoint", WIND = "wind",
            AGREEMENT = "agreement", COMPANY = "company", INSPECTOR = "inspector";

    public interface Mapper {
        /** Returns a String, a List of Strings, or null. */
        Object map(Inspection in);
    }

    /** Key of the inspector's comment on one item. */
    public static String commentKey(String fieldKey) { return fieldKey + ".comment"; }

    public static class Field {
        public final String key, label;
        public final int type;
        public String[] options;
        public String hint, src;
        public Map<String, String> translate;
        public Mapper mapper;
        /** Value(s) that make this field a finding. "*" = any selection. */
        public String[] flags;
        /** Form this field belongs to (set when it is added to a section). */
        public String formId;

        Field(String key, String label, int type, String[] options) {
            this.key = key;
            this.label = label;
            this.type = type;
            this.options = options;
        }

        public Field src(String s) { src = s; return this; }
        public Field hint(String h) { hint = h; return this; }
        public Field map(Mapper m) { mapper = m; return this; }
        public Field flag(String... f) { flags = f; return this; }

        /** Pairs of "general value", "this form's value". */
        public Field tr(String... pairs) {
            translate = new HashMap<>();
            for (int i = 0; i + 1 < pairs.length; i += 2) translate.put(pairs[i], pairs[i + 1]);
            return this;
        }

        public boolean prefillable() { return src != null || mapper != null; }

        public boolean inputs() { return type != HEADER; }

        /** General-inspection items that can have their own photos (shown inline under the item). */
        public boolean takesPhotos() {
            return GENERAL.equals(formId) && (type == TEXT || type == NUMBER || type == DATE || type == CHOICE || type == MULTI);
        }
    }

    public static class Section {
        public final String key, title, subtitle;
        public String formId;
        public final List<Field> fields = new ArrayList<>();
        public boolean photos = true;
        /** General inspection sections whose photos are offered for import. */
        public String[] photoSources = new String[0];
        /** Photo shot list shown as a reminder (from the 4-Point minimum photo requirements). */
        public String[] shotList;
        /** Title for the photo card (e.g. "Company logo"). */
        public String photoLabel = "Photos";

        Section(String key, String title, String subtitle) {
            this.key = key;
            this.title = title;
            this.subtitle = subtitle;
        }

        Field add(Field f) {
            f.formId = formId;
            fields.add(f);
            return f;
        }
        Field text(String k, String l) { return add(new Field(k, l, TEXT, null)); }
        Field num(String k, String l) { return add(new Field(k, l, NUMBER, null)); }
        Field date(String k, String l) { return add(new Field(k, l, DATE, null)); }
        Field choice(String k, String l, String... o) { return add(new Field(k, l, CHOICE, o)); }
        Field multi(String k, String l, String... o) { return add(new Field(k, l, MULTI, o)); }
        Field notes(String k, String l) { return add(new Field(k, l, NOTES, null)); }
        Field sig(String k, String l) { return add(new Field(k, l, SIGNATURE, null)); }
        Field cond(String k, String l) { return add(new Field(k, l, CHOICE, COND)).flag("Marginal", "Deficient"); }
        Field yn(String k, String l) { return add(new Field(k, l, CHOICE, YN)); }
        Field su(String k, String l) { return add(new Field(k, l, CHOICE, SU)).flag("Unsatisfactory"); }
        void header(String l) { add(new Field("", l, HEADER, null)); }
        Section noPhotos() { photos = false; return this; }
        Section photosFrom(String... keys) { photoSources = keys; return this; }
    }

    public static class Form {
        public final String id, title, shortTitle, description;
        public final int color;
        public final List<Section> sections = new ArrayList<>();

        Form(String id, String title, String shortTitle, String description, int color) {
            this.id = id;
            this.title = title;
            this.shortTitle = shortTitle;
            this.description = description;
            this.color = color;
        }

        Section section(String key, String title, String subtitle) {
            Section s = new Section(key, title, subtitle);
            s.formId = id;
            sections.add(s);
            return s;
        }
    }

    // ---------------- option lists ----------------

    static final String[] COND = {"Satisfactory", "Marginal", "Deficient", "Not inspected"};
    static final String[] YN = {"Yes", "No"};
    static final String[] SU = {"Satisfactory", "Unsatisfactory", "N/A"};
    static final String[] ROOF_COVER = {"Asphalt/fiberglass shingle", "Concrete/clay tile", "Metal",
            "Built-up", "Membrane", "Wood shake", "Other"};
    static final String[] ROOF_SHAPE = {"Hip", "Gable", "Flat", "Mixed hip/gable", "Other"};
    static final String[] ROOF_DAMAGE = {"Cracking", "Cupping/curling", "Excessive granule loss",
            "Exposed asphalt", "Exposed felt", "Missing/loose/cracked tabs or tiles",
            "Soft spots in decking", "Visible hail damage"};
    static final String[] PANEL_TYPE = {"Circuit breaker", "Fuse"};
    static final String[] WIRING = {"Copper", "NM, BX or conduit", "Aluminum branch wiring",
            "Cloth wiring", "Active knob and tube"};
    static final String[] AL_REPAIR = {"Connections repaired via COPALUM crimp",
            "Connections repaired via AlumiConn"};
    static final String[] HAZARDS = {"Blowing fuses", "Tripping breakers", "Empty sockets", "Loose wiring",
            "Improper grounding", "Corrosion", "Over fusing", "Double taps", "Exposed wiring",
            "Unsafe wiring", "Improper breaker size", "Scorching", "Other"};
    static final String[] PIPES = {"Copper", "PVC/CPVC", "Galvanized", "PEX", "Polybutylene", "Other"};
    static final String[] FIXTURES = {"Dishwasher", "Refrigerator", "Washing machine", "Water heater",
            "Showers/tubs", "Toilets", "Sinks", "Sump pump", "Main shut off valve", "All other visible"};
    static final String[] DECK_MAT = {"Plywood", "OSB", "Dimensional lumber / T&G planks",
            "Reinforced concrete", "Other", "Not visible"};
    static final String[] NAIL = {"Staples or 6d nails", "8d nails", "Other", "Unknown"};
    static final String[] SPACING = {"6\" edge / 12\" field", "6\" edge / 6\" field", "Other", "Unknown"};
    static final String[] R2W = {"Toe nails", "Clips", "Single wraps", "Double wraps",
            "Structural (anchor bolts, cast-in-place)", "Other", "Unknown"};
    static final String[] ACCESS = {"Full", "Limited", "None"};
    static final String[] SWR = {"Yes - sealed roof deck", "No", "Unknown"};
    /** Opening Protection Level Chart rows, in form order (lowest row = weakest). */
    static final String[] OPENING = {
            "N/A - No openings of this type",
            "A - Verified cyclic pressure & large missile (9 lb; 4.5 lb skylights)",
            "B - Verified cyclic pressure & large missile (4-8 lb; 2 lb skylights)",
            "C - Verified plywood/OSB meeting Table 1609.1.2 of the FBC 2007",
            "D - Verified non-glazed entry/garage door (ASTM E 330, ANSI/DASMA 108, PA/TAS 202)",
            "N - Appears A or B but not verified, or other coverings not identifiable as A, B or C",
            "X - No windborne debris protection"};
    static final String[] LICENSE_TYPES = {
            "Home inspector (468.8314 F.S.)", "Building code inspector (468.607 F.S.)",
            "General, building or residential contractor (489.111 F.S.)", "Professional engineer (471.015 F.S.)",
            "Professional architect (481.213 F.S.)", "Other individual recognized by the insurer (627.711(2) F.S.)"};

    static String fixtureKey(String prefix, String fixture) {
        return prefix + fixture.toLowerCase().replaceAll("[^a-z]", "");
    }

    // ---------------- registry ----------------

    private static Map<String, Form> forms;
    private static Map<String, Field> fieldIndex;
    private static Map<String, Section> sectionIndex;

    public static synchronized Map<String, Form> all() {
        if (forms == null) build();
        return forms;
    }

    public static Form form(String id) { return all().get(id); }

    public static Field field(String key) { all(); return fieldIndex.get(key); }

    public static Section section(String key) { all(); return sectionIndex.get(key); }

    private static void build() {
        forms = new LinkedHashMap<>();
        forms.put(INFO, buildInfo());
        forms.put(AGREEMENT, buildAgreement());
        forms.put(COMPANY, buildCompany());
        forms.put(INSPECTOR, buildInspector());
        forms.put(GENERAL, buildGeneral());
        forms.put(FOUR_POINT, buildFourPoint());
        forms.put(WIND, buildWind());
        fieldIndex = new HashMap<>();
        sectionIndex = new HashMap<>();
        for (Form f : forms.values()) {
            for (Section s : f.sections) {
                sectionIndex.put(s.key, s);
                for (Field fl : s.fields) if (fl.inputs()) fieldIndex.put(fl.key, fl);
            }
        }
    }

    // ---------------- Property & client ----------------

    private static Form buildInfo() {
        Form f = new Form(INFO, "Property & Client", "Property", "Shared by all three reports", Ui.PRIMARY);
        Section s = f.section("info", "Property & Client", "Used on every report");
        s.noPhotos();
        s.header("Client");
        s.text("info.client", "Insured / applicant name");
        s.text("info.contact", "Contact person");
        s.text("info.phone", "Home phone");
        s.text("info.workPhone", "Work phone");
        s.text("info.cellPhone", "Cell phone");
        s.text("info.email", "Email");
        s.text("info.insurer", "Insurance company");
        s.text("info.policy", "Application / policy #");
        s.header("Property");
        s.text("info.address", "Address inspected");
        s.text("info.city", "City");
        s.text("info.zip", "ZIP");
        s.text("info.county", "County");
        s.num("info.yearBuilt", "Actual year built");
        s.num("info.stories", "Number of stories");
        s.num("info.sqft", "Approx. living area (sq ft)");
        s.choice("info.occupancy", "Occupancy", "Occupied", "Vacant", "Furnished, unoccupied");
        s.header("Inspection");
        s.date("info.date", "Date inspected");
        s.text("info.weather", "Weather at inspection");
        s.add(new Field("info.inspectorId", "Inspector conducting the inspection", PICK_INSPECTOR, null));
        s.text("info.inspector", "Inspector name");
        s.text("info.inspectorTitle", "Inspector title");
        s.choice("info.licenseType", "License type", LICENSE_TYPES);
        s.text("info.initials", "Inspector initials");
        s.text("info.license", "License #");
        s.text("info.issuer", "License issuing body");
        s.date("info.licenseExp", "License expiration");
        s.text("info.company", "Company");
        s.text("info.companyPhone", "Company phone");
        return f;
    }

    // ---------------- Pre-inspection agreement ----------------

    static final String[] FACES = {"North", "South", "East", "West", "Northeast", "Northwest", "Southeast", "Southwest"};
    public static final String[] SERVICES = {"Home Inspection", "Other Inspection"};
    public static final String[] RELEASE = {"Real Estate Agent", "Seller", "Attorney", "Other"};
    public static final String[] ATTEND = {"Client", "Real estate agent", "Neither"};
    public static final String[] SIGNED = {"Signed", "Not signed yet"};
    public static final String[] SIGNED_HOW = {"Signed on this device", "Signed copy received", "Signed electronically"};

    private static Form buildAgreement() {
        Form f = new Form(AGREEMENT, "Pre-Inspection Agreement", "Agreement",
                "Send to the client and get it signed before the inspection starts.", 0xFF8A6D1F);
        Section s = f.section("pa.details", "Agreement Details", "Inspector, client, premises, services and fees");
        s.noPhotos();
        s.header("Inspector");
        s.add(new Field("info.inspectorId", "Inspector conducting the inspection", PICK_INSPECTOR, null));
        s.header("Inspection");
        s.date("info.date", "Date of inspection");
        s.text("pa.time", "Time of inspection");
        s.choice("pa.faces", "House faces", FACES);
        s.num("pa.age", "Estimated age (years)").map(new Mapper() {
            @Override
            public Object map(Inspection in) { return ageFromYear(in.str("info.yearBuilt")); }
        });
        s.text("info.weather", "Weather");
        s.text("pa.temp", "Temperature");
        s.header("Client");
        s.text("info.client", "Client name");
        s.text("info.email", "Client email (agreement is sent here)");
        s.text("info.phone", "Client phone");
        s.header("1. Address of house to be inspected");
        s.text("info.address", "Address");
        s.text("info.city", "City");
        s.text("pa.state", "State").map(new Mapper() {
            @Override
            public Object map(Inspection in) { return "FL"; }
        });
        s.text("info.zip", "Zip");
        s.header("2. Services to be performed");
        s.multi("pa.services", "Check all that apply", SERVICES);
        s.text("pa.otherDesc", "Other inspection (e.g. 4-Point, Wind Mitigation)");
        s.header("3. Inspection fee");
        s.num("pa.feeHome", "Home inspection ($)").map(new Mapper() {
            @Override
            public Object map(Inspection in) {
                Inspection co = Store.peek(new java.io.File(in.dir.getParentFile(), Company.PROFILE_ID));
                String fee = co == null ? "" : co.str("co.feeHome");
                return fee.length() > 0 ? fee : null;
            }
        });
        s.num("pa.feeOther", "Other inspection ($)");
        s.num("pa.feeTotal", "Total fee ($)").map(new Mapper() {
            @Override
            public Object map(Inspection in) {
                double t = parseD(in.str("pa.feeHome")) + parseD(in.str("pa.feeOther"));
                return t > 0 ? String.format(java.util.Locale.US, "%.2f", t) : null;
            }
        });
        s.header("Who will be present");
        s.multi("pa.attend", "Attending the inspection", ATTEND);
        s.text("pa.agentName", "Real estate agent name");
        s.text("pa.agentPhone", "Agent phone");
        s.text("pa.agentEmail", "Agent email");
        s.text("pa.agentBrokerage", "Agent brokerage");
        s.header("Report release");
        s.multi("pa.release", "Client allows the report to be released to", RELEASE);
        s.text("pa.releaseOther", "Other (name)");

        // Intake: same fields as Property & Client Details; not printed on the client's agreement
        s = f.section("pa.intake", "Intake", "Contact, insurance and property details");
        s.noPhotos();
        s.header("Client");
        s.text("info.contact", "Contact person");
        s.text("info.workPhone", "Work phone");
        s.text("info.cellPhone", "Cell phone");
        s.header("Insurance");
        s.text("info.insurer", "Insurance company");
        s.text("info.policy", "Application / policy #");
        s.header("Property");
        s.text("info.county", "County");
        s.num("info.yearBuilt", "Actual year built");
        s.num("info.stories", "Number of stories");
        s.num("info.sqft", "Approx. living area (sq ft)");
        s.choice("info.occupancy", "Occupancy", "Occupied", "Vacant", "Furnished, unoccupied");

        s = f.section("pa.status", "Signatures & Status", "Sent, signed, and the signed copy");
        s.noPhotos();
        s.date("pa.sentDate", "Date sent to client");
        s.choice("pa.signed", "Agreement signed", SIGNED).flag("Not signed yet");
        s.choice("pa.signedHow", "How it was signed", SIGNED_HOW);
        s.date("pa.signedDate", "Date signed");
        s.text("pa.clientInitials", "Client initials (allows report release)");
        s.sig("pa.clientSig", "Client signature (sign on this device)");
        s.sig("pa.inspectorSig", "Inspector signature").src("info.inspectorSig");
        return f;
    }

    // ---------------- Company profile & inspector roster ----------------

    private static Form buildCompany() {
        Form f = new Form(COMPANY, "Company Profile", "Company", "Prefilled into agreements and reports", Ui.PRIMARY);
        Section s = f.section("co", "Company Profile", "Business details used on agreements and reports");
        s.photoLabel = "Company logo (optional)";
        s.text("co.name", "Company name");
        s.text("co.address", "Street address");
        s.text("co.city", "City");
        s.text("co.state", "State");
        s.text("co.zip", "Zip");
        s.text("co.phone", "Phone");
        s.text("co.email", "Email");
        s.text("co.website", "Website");
        s.text("co.license", "Business license / registration #");
        s.num("co.feeHome", "Default home inspection fee ($)");
        s.notes("co.feeTerms", "Fee terms (agreement page 1)");
        s.notes("co.terms", "Terms and conditions (agreement page 2). {COMPANY} is replaced with the company name");
        return f;
    }

    private static Form buildInspector() {
        Form f = new Form(INSPECTOR, "Inspector", "Inspector", "Employee details printed on reports", Ui.PRIMARY);
        Section s = f.section("ip", "Inspector Details", "Printed on agreements and reports this inspector conducts");
        s.noPhotos();
        s.text("ip.name", "Full name");
        s.text("ip.title", "Title (e.g. Home Inspector)");
        s.choice("ip.licenseType", "License type", LICENSE_TYPES);
        s.text("ip.license", "License / certificate #");
        s.text("ip.issuer", "Issuing body (e.g. Florida DBPR)");
        s.date("ip.expires", "License expiration");
        s.text("ip.phone", "Phone");
        s.text("ip.email", "Email");
        s.sig("ip.signature", "Saved signature (applied to agreements and reports)");
        return f;
    }

    static double parseD(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    // ---------------- General home inspection ----------------

    private static Form buildGeneral() {
        Form f = new Form(GENERAL, "General Home Inspection", "General",
                "Walk the home system by system. Photos, markup and dictated notes.", Ui.GENERAL);
        Section s;

        s = f.section("g.ext", "Exterior & Grounds", "Grading, siding, trim, walkways, decks");
        s.multi("g.ext.siding", "Wall cladding", "Stucco", "Vinyl", "Wood", "Fiber cement", "Brick",
                "Concrete block", "Other");
        s.cond("g.ext.grading", "Grading & drainage");
        s.cond("g.ext.siding_cond", "Wall cladding condition");
        s.cond("g.ext.trim", "Eaves, soffits & fascia");
        s.cond("g.ext.walks", "Driveway & walkways");
        s.cond("g.ext.decks", "Porches, decks & balconies");
        s.cond("g.ext.veg", "Vegetation clearance");
        s.notes("g.ext.notes", "Exterior notes");

        s = f.section("g.openings", "Windows, Doors & Opening Protection",
                "Glazing and hurricane protection (feeds wind mitigation)");
        s.choice("g.openings.windowType", "Predominant window type", "Single hung", "Double hung",
                "Horizontal slider", "Casement", "Fixed", "Mixed");
        s.choice("g.openings.glazing", "Glazing", "Standard glass", "Impact-rated glass", "Mixed");
        s.header("Opening protection by opening type (weakest form in use)");
        s.choice("g.openings.windows", "Windows / glazed entry doors", OPENING);
        s.choice("g.openings.garageGlazed", "Glazed garage doors", OPENING);
        s.choice("g.openings.skylights", "Skylights", OPENING);
        s.choice("g.openings.glassBlock", "Glass block", OPENING);
        s.choice("g.openings.entryDoors", "Entry doors (non-glazed)", OPENING);
        s.choice("g.openings.garageDoors", "Garage doors (non-glazed)", OPENING);
        s.text("g.openings.docs", "Product approval / documentation seen");
        s.cond("g.openings.windowsCond", "Windows condition");
        s.cond("g.openings.doorsCond", "Exterior doors condition");
        s.notes("g.openings.notes", "Windows & doors notes");

        s = f.section("g.roof", "Roof", "Covering, age, shape, damage, flashing, gutters");
        s.choice("g.roof.covering", "Predominant roof covering", ROOF_COVER);
        s.choice("g.roof.secondary", "Secondary roof covering (if any)", ROOF_COVER);
        s.choice("g.roof.shape", "Roof shape", ROOF_SHAPE);
        s.num("g.roof.yearInstalled", "Year installed / replaced");
        s.num("g.roof.age", "Roof age (years)");
        s.num("g.roof.life", "Remaining useful life (years)");
        s.date("g.roof.permitDate", "Date of last roofing permit");
        s.choice("g.roof.replacement", "Last update", "Full replacement", "Partial replacement", "Original");
        s.num("g.roof.replacementPct", "% replaced (if partial)");
        s.choice("g.roof.method", "Inspected from", "Walked roof", "Ladder at eaves", "Drone", "Ground w/ binoculars");
        s.multi("g.roof.damage", "Signs of damage", ROOF_DAMAGE).flag("*");
        s.choice("g.roof.leaks", "Visible signs of leaks", YN).flag("Yes");
        s.cond("g.roof.flashing", "Flashing & penetrations");
        s.cond("g.roof.gutters", "Gutters & downspouts");
        s.cond("g.roof.cond", "Overall roof condition");
        s.notes("g.roof.notes", "Roof notes");

        s = f.section("g.attic", "Attic & Roof Structure", "Decking, nailing, roof-to-wall, insulation");
        s.choice("g.attic.access", "Attic access", ACCESS);
        s.choice("g.attic.deck", "Roof deck material", DECK_MAT);
        s.choice("g.attic.nail", "Deck fastener", NAIL);
        s.choice("g.attic.spacing", "Fastener spacing", SPACING);
        s.choice("g.attic.r2w", "Roof-to-wall attachment", R2W);
        s.choice("g.attic.swr", "Secondary water resistance", SWR);
        s.choice("g.attic.leaks", "Leak stains on underside of decking", YN).flag("Yes");
        s.choice("g.attic.insulation", "Insulation", "Blown fiberglass", "Batt fiberglass", "Cellulose",
                "Spray foam", "None");
        s.num("g.attic.insDepth", "Insulation depth (inches)");
        s.cond("g.attic.vent", "Ventilation");
        s.cond("g.attic.cond", "Overall attic condition");
        s.notes("g.attic.notes", "Attic notes");

        s = f.section("g.struct", "Structure & Foundation", "Foundation, framing, movement");
        s.choice("g.struct.foundation", "Foundation", "Slab on grade", "Crawlspace", "Basement", "Piers");
        s.choice("g.struct.walls", "Wall construction", "Concrete block (CBS)", "Wood frame", "Steel frame",
                "Poured concrete", "Other");
        s.choice("g.struct.movement", "Evidence of settlement or movement", YN).flag("Yes");
        s.cond("g.struct.cond", "Overall structure");
        s.notes("g.struct.notes", "Structure notes");

        s = f.section("g.elec", "Electrical", "Service, panels, wiring, hazards");
        s.choice("g.elec.service", "Service entrance", "Overhead", "Underground");
        s.header("Main panel");
        s.text("g.elec.mainLocation", "Location");
        s.choice("g.elec.mainType", "Type", PANEL_TYPE);
        s.num("g.elec.mainAmps", "Total amps");
        s.choice("g.elec.mainSufficient", "Amperage sufficient for current usage", YN).flag("No");
        s.text("g.elec.mainBrand", "Brand / model");
        s.num("g.elec.mainAge", "Panel age (years)");
        s.num("g.elec.mainUpdated", "Year last updated");
        s.header("Second panel");
        s.choice("g.elec.second", "Second panel present", YN);
        s.choice("g.elec.secondType", "Type", PANEL_TYPE);
        s.num("g.elec.secondAmps", "Total amps");
        s.choice("g.elec.secondSufficient", "Amperage sufficient for current usage", YN).flag("No");
        s.text("g.elec.secondBrand", "Brand / model");
        s.num("g.elec.secondAge", "Panel age (years)");
        s.num("g.elec.secondUpdated", "Year last updated");
        s.header("Wiring & hazards");
        s.multi("g.elec.wiring", "Wiring type present", WIRING)
                .flag("Aluminum branch wiring", "Cloth wiring", "Active knob and tube");
        s.multi("g.elec.alRepair", "Aluminum wiring remediation", AL_REPAIR);
        s.multi("g.elec.hazards", "Hazards present", HAZARDS).flag("*");
        s.choice("g.elec.gfci", "GFCI protection where required", YN).flag("No");
        s.choice("g.elec.smoke", "Smoke detectors present", YN).flag("No");
        s.cond("g.elec.cond", "Overall electrical system");
        s.notes("g.elec.notes", "Electrical notes");

        s = f.section("g.plumb", "Plumbing", "Supply, drains, water heater, fixtures, leaks");
        s.multi("g.plumb.pipes", "Supply piping", PIPES).flag("Polybutylene", "Galvanized");
        s.choice("g.plumb.drains", "Drain / waste piping", "PVC", "ABS", "Cast iron", "Galvanized", "Other");
        s.num("g.plumb.pipeAge", "Age of piping system (years)");
        s.choice("g.plumb.repiped", "Re-piped", "Completely", "Partially", "No");
        s.num("g.plumb.repipedYear", "Year re-piped");
        s.text("g.plumb.shutoff", "Main shut off location");
        s.header("Water heater");
        s.text("g.plumb.whLocation", "Location");
        s.choice("g.plumb.whType", "Type", "Tank - electric", "Tank - gas", "Tankless - electric",
                "Tankless - gas", "Heat pump");
        s.num("g.plumb.whGallons", "Capacity (gallons)");
        s.num("g.plumb.whYear", "Year manufactured");
        s.choice("g.plumb.tpr", "TPR valve present", YN).flag("No");
        s.header("Leaks");
        s.choice("g.plumb.leakEvidence", "Any evidence of leakage", YN).flag("Yes");
        s.choice("g.plumb.activeLeak", "Indication of an active leak", YN).flag("Yes");
        s.choice("g.plumb.priorLeak", "Indication of a prior leak", YN).flag("Yes");
        s.header("Fixture condition");
        for (String fx : FIXTURES) s.su(fixtureKey("g.plumb.fx.", fx), fx);
        s.cond("g.plumb.cond", "Overall plumbing");
        s.notes("g.plumb.notes", "Plumbing notes");

        s = f.section("g.hvac", "Heating & Cooling", "Equipment, age, operation, hazards");
        s.choice("g.hvac.centralAc", "Central AC", YN);
        s.choice("g.hvac.centralHeat", "Central heat", YN);
        s.choice("g.hvac.system", "System type", "Split system", "Heat pump", "Package unit", "Gas furnace",
                "Mini-split", "Window/wall units", "Other");
        s.text("g.hvac.primaryHeat", "Primary heat source & fuel (if not central)");
        s.text("g.hvac.brand", "Brand / model");
        s.num("g.hvac.age", "System age (years)");
        s.num("g.hvac.updated", "Year last updated");
        s.date("g.hvac.serviced", "Date of last servicing");
        s.choice("g.hvac.working", "Heating & cooling in good working order", YN).flag("No");
        s.choice("g.hvac.condensate", "Condensate line / pan in good condition", YN).flag("No");
        s.choice("g.hvac.woodStove", "Wood stove or gas fireplace not professionally installed", YN).flag("Yes");
        s.choice("g.hvac.spaceHeater", "Space heater used as primary heat", YN).flag("Yes");
        s.choice("g.hvac.portable", "Heat source is portable", YN).flag("Yes");
        s.cond("g.hvac.filter", "Filter");
        s.cond("g.hvac.ducts", "Ductwork");
        s.cond("g.hvac.cond", "Overall HVAC");
        s.notes("g.hvac.notes", "HVAC notes");

        s = f.section("g.interior", "Interior", "Walls, ceilings, floors, stairs, doors");
        s.cond("g.interior.walls", "Walls & ceilings");
        s.cond("g.interior.floors", "Floors");
        s.cond("g.interior.stairs", "Stairs & railings");
        s.cond("g.interior.doors", "Interior doors");
        s.choice("g.interior.ceilingStains", "Water stains on interior ceilings", YN).flag("Yes");
        s.notes("g.interior.notes", "Interior notes");

        s = f.section("g.kitchen", "Kitchen & Appliances", "Cabinets, counters, built-in appliances");
        s.cond("g.kitchen.cabinets", "Cabinets & counters");
        s.cond("g.kitchen.range", "Range / cooktop / oven");
        s.cond("g.kitchen.dishwasher", "Dishwasher");
        s.cond("g.kitchen.disposal", "Disposal");
        s.cond("g.kitchen.micro", "Built-in microwave");
        s.cond("g.kitchen.fridge", "Refrigerator");
        s.notes("g.kitchen.notes", "Kitchen notes");

        s = f.section("g.bath", "Bathrooms", "Fixtures, ventilation, GFCI");
        s.num("g.bath.count", "Number of bathrooms");
        s.cond("g.bath.fixtures", "Sinks, toilets, tubs & showers");
        s.cond("g.bath.vent", "Exhaust ventilation");
        s.cond("g.bath.caulk", "Caulk & grout");
        s.notes("g.bath.notes", "Bathroom notes");

        s = f.section("g.garage", "Garage & Laundry", "Doors, openers, fire separation, laundry");
        s.choice("g.garage.type", "Garage", "Attached", "Detached", "Carport", "None");
        s.cond("g.garage.door", "Vehicle door & opener (auto-reverse)");
        s.cond("g.garage.fireDoor", "Fire-rated door to house");
        s.cond("g.garage.laundry", "Laundry hookups & dryer vent");
        s.notes("g.garage.notes", "Garage & laundry notes");

        s = f.section("g.fireplace", "Fireplace & Chimney", "Firebox, damper, flue, chimney");
        s.choice("g.fireplace.type", "Fireplace", "None", "Wood burning", "Gas", "Electric");
        s.cond("g.fireplace.cond", "Fireplace & chimney condition");
        s.notes("g.fireplace.notes", "Fireplace notes");

        s = f.section("g.summary", "Summary", "Overall impressions and recommendations");
        s.noPhotos();
        s.notes("g.summary.notes", "Summary & recommendations");
        return f;
    }

    // ---------------- 4-Point ----------------

    static final String[] SAT_UNSAT = {"Satisfactory", "Unsatisfactory"};

    private static Form buildFourPoint() {
        Form f = new Form(FOUR_POINT, "4-Point Inspection", "4-Point",
                "Electrical, HVAC, plumbing and roof for insurance underwriting.", Ui.FOUR_POINT);
        Section s;

        s = f.section("fp.photos", "Minimum Photo Requirements", "Shot list required by underwriting");
        s.shotList = new String[]{"Dwelling: each side", "Roof: each slope",
                "Plumbing: water heater, under-cabinet plumbing/drains, exposed valves",
                "Main electrical service panel with interior door label", "Electrical box with panel off",
                "All hazards or deficiencies noted in this report"};
        s.multi("fp.photos.done", "Photos captured", s.shotList);
        s.photosFrom("g.ext", "g.roof", "g.elec", "g.plumb", "g.hvac", "g.attic");

        s = f.section("fp.elec", "Electrical System",
                "Separate documentation of any aluminum wiring remediation must be provided");
        s.photosFrom("g.elec");
        s.header("Main panel");
        s.choice("fp.elec.mainType", "Type", PANEL_TYPE).src("g.elec.mainType");
        s.num("fp.elec.mainAmps", "Total amps").src("g.elec.mainAmps");
        s.choice("fp.elec.mainSufficient", "Is amperage sufficient for current usage?", YN)
                .src("g.elec.mainSufficient").flag("No");
        s.header("Second panel");
        s.choice("fp.elec.secondType", "Type", PANEL_TYPE).src("g.elec.secondType");
        s.num("fp.elec.secondAmps", "Total amps").src("g.elec.secondAmps");
        s.choice("fp.elec.secondSufficient", "Is amperage sufficient for current usage?", YN)
                .src("g.elec.secondSufficient").flag("No");
        s.multi("fp.elec.presence", "Indicate presence of any of the following", "Cloth wiring",
                "Active knob and tube", "Aluminum branch wiring").src("g.elec.wiring").flag("*");
        s.text("fp.elec.alDesc", "If aluminum branch wiring present, describe its usage");
        s.multi("fp.elec.alRepair", "Single strand aluminum remediation", AL_REPAIR).src("g.elec.alRepair");
        s.multi("fp.elec.hazards", "Hazards present", HAZARDS).src("g.elec.hazards").flag("*");
        s.choice("fp.elec.cond", "General condition of the electrical system", SAT_UNSAT)
                .src("g.elec.cond").tr("Marginal", "Unsatisfactory", "Deficient", "Unsatisfactory")
                .flag("Unsatisfactory");
        s.header("Supplemental information");
        s.num("fp.elec.mainAge", "Main panel age (years)").src("g.elec.mainAge");
        s.num("fp.elec.mainUpdated", "Main panel year last updated").src("g.elec.mainUpdated");
        s.text("fp.elec.mainBrand", "Main panel brand / model").src("g.elec.mainBrand");
        s.num("fp.elec.secondAge", "Second panel age (years)").src("g.elec.secondAge");
        s.num("fp.elec.secondUpdated", "Second panel year last updated").src("g.elec.secondUpdated");
        s.text("fp.elec.secondBrand", "Second panel brand / model").src("g.elec.secondBrand");
        s.multi("fp.elec.wiring", "Wiring type", "Copper", "NM, BX or conduit").src("g.elec.wiring");
        s.notes("fp.elec.notes", "Explain / comments").src("g.elec.notes");

        s = f.section("fp.hvac", "HVAC System", "Heating, ventilation and air conditioning");
        s.photosFrom("g.hvac");
        s.choice("fp.hvac.centralAc", "Central AC", YN).src("g.hvac.centralAc");
        s.choice("fp.hvac.centralHeat", "Central heat", YN).src("g.hvac.centralHeat");
        s.text("fp.hvac.primaryHeat", "If not central heat, primary heat source and fuel type")
                .src("g.hvac.primaryHeat");
        s.choice("fp.hvac.working", "Are the heating, ventilation and AC systems in good working order?", YN)
                .src("g.hvac.working").flag("No");
        s.date("fp.hvac.serviced", "Date of last HVAC servicing / inspection").src("g.hvac.serviced");
        s.header("Hazards present");
        s.choice("fp.hvac.woodStove", "Wood-burning stove or central gas fireplace not professionally installed", YN)
                .src("g.hvac.woodStove").flag("Yes");
        s.choice("fp.hvac.spaceHeater", "Space heater used as primary heat source", YN)
                .src("g.hvac.spaceHeater").flag("Yes");
        s.choice("fp.hvac.portable", "Is the source portable?", YN).src("g.hvac.portable").flag("Yes");
        s.choice("fp.hvac.condensate", "Does the AC condensation line / pan appear to be in good condition?", YN)
                .src("g.hvac.condensate").flag("No");
        s.header("Supplemental information");
        s.num("fp.hvac.age", "Age of system (years)").src("g.hvac.age");
        s.num("fp.hvac.updated", "Year last updated").src("g.hvac.updated");
        s.text("fp.hvac.brand", "Brand / model").src("g.hvac.brand");
        s.notes("fp.hvac.notes", "Explain / comments").src("g.hvac.notes");

        s = f.section("fp.plumb", "Plumbing System", "Water heater, leaks, fixtures, piping");
        s.photosFrom("g.plumb");
        s.choice("fp.plumb.tpr", "Is there a temperature/pressure relief valve on the water heater?", YN)
                .src("g.plumb.tpr").flag("No");
        s.choice("fp.plumb.leakEvidence", "Is there any evidence of leakage?", YN).src("g.plumb.leakEvidence").flag("Yes");
        s.choice("fp.plumb.activeLeak", "Is there any indication of an active leak?", YN)
                .src("g.plumb.activeLeak").flag("Yes");
        s.choice("fp.plumb.priorLeak", "Is there any indication of a prior leak?", YN)
                .src("g.plumb.priorLeak").flag("Yes");
        s.text("fp.plumb.whLocation", "Water heater location").src("g.plumb.whLocation");
        s.header("General condition of the following plumbing fixtures and connections");
        for (String fx : FIXTURES) {
            s.su(fixtureKey("fp.plumb.fx.", fx), fx).src(fixtureKey("g.plumb.fx.", fx));
        }
        s.header("Piping");
        s.multi("fp.plumb.pipes", "Type of pipes", PIPES).src("g.plumb.pipes").flag("Polybutylene");
        s.num("fp.plumb.pipeAge", "Age of piping system (years)").src("g.plumb.pipeAge");
        s.choice("fp.plumb.repiped", "Re-piped", "Completely", "Partially", "No").src("g.plumb.repiped");
        s.num("fp.plumb.repipedYear", "Year re-piped").src("g.plumb.repipedYear");
        s.notes("fp.plumb.notes", "Explain / comments").src("g.plumb.notes");

        s = f.section("fp.roof", "Roof", "Predominant and secondary roof");
        s.photosFrom("g.roof", "g.attic");
        s.header("Predominant roof");
        s.choice("fp.roof.covering", "Covering material", ROOF_COVER).src("g.roof.covering");
        s.num("fp.roof.age", "Roof age (years)").map(new Mapper() {
            @Override
            public Object map(Inspection in) {
                if (in.str("g.roof.age").length() > 0) return in.str("g.roof.age");
                return ageFromYear(in.str("g.roof.yearInstalled"));
            }
        });
        s.num("fp.roof.life", "Remaining useful life (years)").src("g.roof.life");
        s.date("fp.roof.permitDate", "Date of last roofing permit").src("g.roof.permitDate");
        s.num("fp.roof.updateYear", "Year of last update").src("g.roof.yearInstalled");
        s.choice("fp.roof.replacement", "Replacement", "Full replacement", "Partial replacement", "Original")
                .src("g.roof.replacement");
        s.num("fp.roof.replacementPct", "% replaced (if partial)").src("g.roof.replacementPct");
        s.choice("fp.roof.cond", "Overall condition", SAT_UNSAT).src("g.roof.cond")
                .tr("Marginal", "Unsatisfactory", "Deficient", "Unsatisfactory").flag("Unsatisfactory");
        s.multi("fp.roof.damage", "Any visible signs of damage / deterioration", ROOF_DAMAGE)
                .src("g.roof.damage").flag("*");
        s.choice("fp.roof.leaks", "Any visible signs of leaks?", YN).src("g.roof.leaks").flag("Yes");
        s.choice("fp.roof.atticLeaks", "Attic / underside of decking shows leaks?", YN)
                .src("g.attic.leaks").flag("Yes");
        s.choice("fp.roof.ceilingLeaks", "Interior ceilings show leaks?", YN)
                .src("g.interior.ceilingStains").flag("Yes");
        s.header("Secondary roof (if applicable)");
        s.choice("fp.roof.secCovering", "Covering material", ROOF_COVER).src("g.roof.secondary");
        s.num("fp.roof.secAge", "Roof age (years)");
        s.choice("fp.roof.secCond", "Overall condition", SAT_UNSAT).flag("Unsatisfactory");
        s.notes("fp.roof.notes", "Explain / comments").src("g.roof.notes");

        s = f.section("fp.cert", "Comments & Certification", "Additional observations, inspector signature");
        s.noPhotos();
        s.notes("fp.cert.comments", "Additional comments / observations");
        s.text("fp.cert.inspector", "Inspector name").src("info.inspector");
        s.text("fp.cert.title", "Title").src("info.inspectorTitle");
        s.text("fp.cert.license", "License number").src("info.license");
        s.text("fp.cert.issuer", "Issuing body").src("info.issuer");
        s.text("fp.cert.company", "Company").src("info.company");
        s.text("fp.cert.phone", "Phone").src("info.companyPhone");
        s.date("fp.cert.date", "Date").src("info.date");
        s.sig("fp.cert.signature", "Inspector signature").src("info.inspectorSig");
        return f;
    }

    // ---------------- Wind mitigation (OIR-B1-1802 Rev. 01/12) ----------------

    static final String[] WM_BUILD = {
            "A - Built in compliance with the FBC (FBC 2001 or later)",
            "B - HVHZ only: built in compliance with the SFBC-94",
            "C - Unknown or does not meet the requirements of A or B"};
    /** Roof covering types exactly as listed in question 2.1. */
    static final String[] WM_COVER_TYPES = {"Asphalt/Fiberglass Shingle", "Concrete/Clay Tile", "Metal",
            "Built Up", "Membrane", "Other"};
    static final String[] WM_COVER = {
            "A - All coverings meet the FBC (product approval current at installation, permit on/after 3/1/02, or original roof built 2004+)",
            "B - All coverings have a Miami-Dade product approval, or (HVHZ) permit after 9/1/1994 and before 3/1/2002, or original roof built 1997+",
            "C - One or more roof coverings do not meet A or B",
            "D - No roof coverings meet A or B"};
    static final String[] WM_DECK = {
            "A - Plywood/OSB, staples or 6d nails at 6\" edge / 12\" field; batten decking; or equivalent mean uplift less than B or C",
            "B - Plywood/OSB min 7/16\", 8d common nails max 12\" in the field; or mean uplift at least 103 psf",
            "C - Plywood/OSB min 7/16\", 8d common nails max 6\" in the field; dimensional lumber/T&G with 2 nails per board; or mean uplift at least 182 psf",
            "D - Reinforced concrete roof deck",
            "E - Other",
            "F - Unknown or unidentified",
            "G - No attic access"};
    static final String[] WM_R2W = {"A - Toe nails", "B - Clips", "C - Single wraps", "D - Double wraps",
            "E - Structural (anchor bolts structurally connected or reinforced concrete roof)", "F - Other",
            "G - Unknown or unidentified", "H - No attic access"};
    static final String[] WM_GEOM = {
            "A - Hip roof (no other roof shapes greater than 10% of the total roof system perimeter)",
            "B - Flat roof (5+ unit building, at least 90% of main roof area slope less than 2:12)",
            "C - Other roof"};
    static final String[] WM_SWR = {"A - SWR (also called sealed roof deck)", "B - No SWR", "C - Unknown or undetermined"};
    static final String[] WM_OPEN = {
            "A.1 - Glazed: level A. Non-glazed: all A, or none",
            "A.2 - Glazed: level A. Non-glazed: one or more D, none B, C, N or X",
            "A.3 - Glazed: level A. Non-glazed: one or more B, C, N or X",
            "B.1 - Glazed: level B. Non-glazed: all A or B, or none",
            "B.2 - Glazed: level B. Non-glazed: one or more D, none C, N or X",
            "B.3 - Glazed: level B. Non-glazed: one or more C, N or X",
            "C.1 - Glazed: plywood/OSB (C). Non-glazed: all A, B or C, or none",
            "C.2 - Glazed: plywood/OSB (C). Non-glazed: one or more D, none N or X",
            "C.3 - Glazed: plywood/OSB (C). Non-glazed: one or more N or X",
            "N.1 - Glazed: unverified (N). Non-glazed: all A, B, C or N, or none",
            "N.2 - Glazed: unverified (N). Non-glazed: one or more D, none X",
            "N.3 - Glazed: unverified (N). Non-glazed: one or more X",
            "X - None or some glazed openings have no protection"};

    static final String[][] WM_OPENINGS = {
            {"wm.open.windows", "Glazed: windows or entry doors", "g.openings.windows"},
            {"wm.open.garageGlazed", "Glazed: garage doors", "g.openings.garageGlazed"},
            {"wm.open.skylights", "Glazed: skylights", "g.openings.skylights"},
            {"wm.open.glassBlock", "Glazed: glass block", "g.openings.glassBlock"},
            {"wm.open.entryDoors", "Non-glazed: entry doors", "g.openings.entryDoors"},
            {"wm.open.garageDoors", "Non-glazed: garage doors", "g.openings.garageDoors"}};

    static String coverKey(int i, String field) { return "wm.cover.t" + (i + 1) + "." + field; }

    /** Index into WM_COVER_TYPES for a general-inspection covering value, or -1. */
    static int coverIndex(String general) {
        if (general == null || general.length() == 0) return -1;
        for (int i = 0; i < ROOF_COVER.length; i++) {
            if (ROOF_COVER[i].equals(general)) return i < 5 ? i : 5; // Wood shake and Other -> Other
        }
        return -1;
    }

    /** Parses MM/DD/YYYY to yyyymmdd, or 0. */
    static int dateNum(String d) {
        String[] p = d == null ? new String[0] : d.split("/");
        if (p.length != 3) return 0;
        int m = parseInt(p[0]), day = parseInt(p[1]), y = parseInt(p[2]);
        return y > 0 ? y * 10000 + m * 100 + day : 0;
    }

    private static Form buildWind() {
        Form f = new Form(WIND, "Wind Mitigation (OIR-B1-1802)", "Wind Mit",
                "Uniform Mitigation Verification Inspection Form, Rev. 01/12.", Ui.WIND);
        Section s;

        s = f.section("wm.owner", "Owner Information", "Header of the form, filled from Property & Client");
        s.noPhotos();
        s.date("wm.owner.date", "Inspection date").src("info.date");
        s.text("wm.owner.name", "Owner name").src("info.client");
        s.text("wm.owner.contact", "Contact person").src("info.contact");
        s.text("wm.owner.address", "Address").src("info.address");
        s.text("wm.owner.city", "City").src("info.city");
        s.text("wm.owner.zip", "Zip").src("info.zip");
        s.text("wm.owner.county", "County").src("info.county");
        s.text("wm.owner.homePhone", "Home phone").src("info.phone");
        s.text("wm.owner.workPhone", "Work phone").src("info.workPhone");
        s.text("wm.owner.cellPhone", "Cell phone").src("info.cellPhone");
        s.text("wm.owner.insurer", "Insurance company").src("info.insurer");
        s.text("wm.owner.policy", "Policy #").src("info.policy");
        s.num("wm.owner.year", "Year of home").src("info.yearBuilt");
        s.num("wm.owner.stories", "# of stories").src("info.stories");
        s.text("wm.owner.email", "Email").src("info.email");

        s = f.section("wm.building", "1. Building Code",
                "Built in compliance with the FBC 2001 or later, or (HVHZ) the SFBC-94?");
        s.noPhotos();
        s.choice("wm.building.answer", "Answer", WM_BUILD).map(new Mapper() {
            @Override
            public Object map(Inspection in) {
                int y = parseInt(in.str("info.yearBuilt"));
                if (y >= 2004) return WM_BUILD[0];
                if (y >= 2002 && dateNum(in.str("wm.building.permit")) >= 20020301) return WM_BUILD[0];
                if (y > 0 && y < 2002) return WM_BUILD[2];
                return null;
            }
        });
        s.num("wm.building.year", "Year built").src("info.yearBuilt");
        s.date("wm.building.permit", "Building permit application date (needed for 2002/2003 homes, or 1994-1996 in HVHZ)");

        s = f.section("wm.cover", "2. Roof Covering",
                "All covering types in use, with permit date, product approval # or year installed");
        s.photosFrom("g.roof");
        s.multi("wm.cover.types", "2.1 Roof covering types in use", WM_COVER_TYPES).map(new Mapper() {
            @Override
            public Object map(Inspection in) {
                List<String> l = new ArrayList<>();
                for (String k : new String[]{"g.roof.covering", "g.roof.secondary"}) {
                    int i = coverIndex(in.str(k));
                    if (i >= 0 && !l.contains(WM_COVER_TYPES[i])) l.add(WM_COVER_TYPES[i]);
                }
                return l.isEmpty() ? null : l;
            }
        });
        s.text("wm.cover.otherDesc", "Describe \"Other\" covering").map(new Mapper() {
            @Override
            public Object map(Inspection in) {
                for (String k : new String[]{"g.roof.covering", "g.roof.secondary"}) {
                    if (coverIndex(in.str(k)) == 5) return in.str(k);
                }
                return null;
            }
        });
        for (int i = 0; i < WM_COVER_TYPES.length; i++) {
            final int idx = i;
            s.header((i + 1) + ". " + WM_COVER_TYPES[i]);
            s.date(coverKey(i, "permit"), "Permit application date").map(new Mapper() {
                @Override
                public Object map(Inspection in) {
                    if (coverIndex(in.str("g.roof.covering")) != idx) return null;
                    String d = in.str("g.roof.permitDate");
                    return d.length() > 0 ? d : null;
                }
            });
            s.text(coverKey(i, "approval"), "FBC or MDC product approval #");
            s.num(coverKey(i, "year"), "Year of original installation or replacement").map(new Mapper() {
                @Override
                public Object map(Inspection in) {
                    if (coverIndex(in.str("g.roof.covering")) != idx) return null;
                    String y = in.str("g.roof.yearInstalled");
                    return y.length() > 0 ? y : null;
                }
            });
            s.multi(coverKey(i, "noinfo"), "Compliance", "No information provided for compliance");
        }
        s.header("Roof covering answer");
        s.choice("wm.cover.answer", "Answer", WM_COVER).map(new Mapper() {
            @Override
            public Object map(Inspection in) {
                List<String> types = in.list("wm.cover.types");
                if (types.isEmpty()) return null;
                int meets = 0, known = 0;
                for (String t : types) {
                    int i = java.util.Arrays.asList(WM_COVER_TYPES).indexOf(t);
                    if (i < 0) continue;
                    int permit = dateNum(in.str(coverKey(i, "permit")));
                    int year = parseInt(in.str(coverKey(i, "year")));
                    boolean approval = in.str(coverKey(i, "approval")).length() > 0;
                    if (permit == 0 && year == 0 && !approval) continue;
                    known++;
                    if (approval || permit >= 20020301 || (permit == 0 && year >= 2002)) meets++;
                }
                if (known == 0) return null;
                if (meets == types.size()) return WM_COVER[0];
                if (meets > 0) return WM_COVER[2];
                return WM_COVER[3];
            }
        });

        s = f.section("wm.deck", "3. Roof Deck Attachment", "What is the weakest form of roof deck attachment?");
        s.photosFrom("g.attic");
        s.choice("wm.deck.answer", "Answer", WM_DECK).map(new Mapper() {
            @Override
            public Object map(Inspection in) {
                if ("None".equals(in.str("g.attic.access"))) return WM_DECK[6];
                String deck = in.str("g.attic.deck");
                String nail = in.str("g.attic.nail");
                String sp = in.str("g.attic.spacing");
                if (deck.startsWith("Reinforced")) return WM_DECK[3];
                if (deck.startsWith("Dimensional")) return WM_DECK[2];
                if (deck.equals("Other")) return WM_DECK[4];
                if (deck.equals("Not visible")) return WM_DECK[5];
                if (nail.startsWith("Staples")) return WM_DECK[0];
                if (nail.startsWith("8d") && sp.equals(SPACING[1])) return WM_DECK[2];
                if (nail.startsWith("8d") && sp.equals(SPACING[0])) return WM_DECK[1];
                if (nail.length() > 0 || deck.length() > 0) return WM_DECK[5];
                return null;
            }
        });
        s.text("wm.deck.other", "If E (Other), describe");
        s.text("wm.deck.detail", "Observed (deck material, fastener, spacing)").map(new Mapper() {
            @Override
            public Object map(Inspection in) {
                String d = joinNonEmpty(in.str("g.attic.deck"), in.str("g.attic.nail"), in.str("g.attic.spacing"));
                return d.length() > 0 ? d : null;
            }
        });

        s = f.section("wm.r2w", "4. Roof to Wall Attachment",
                "Weakest roof to wall connection (ignore hip/valley jacks within 5 ft of corners)");
        s.photosFrom("g.attic");
        s.choice("wm.r2w.answer", "Answer", WM_R2W).map(new Mapper() {
            @Override
            public Object map(Inspection in) {
                if ("None".equals(in.str("g.attic.access"))) return WM_R2W[7];
                String v = in.str("g.attic.r2w");
                for (int i = 0; i < R2W.length; i++) if (R2W[i].equals(v)) return WM_R2W[i];
                return null;
            }
        });
        s.multi("wm.r2w.minimal", "Minimal conditions for B, C or D (all visible connectors)",
                "Secured to truss/rafter with a minimum of 3 nails",
                "Attached to top plate or embedded in bond beam, gap under 1/2\", blocked no more than 1.5\", no severe corrosion");
        s.text("wm.r2w.other", "If F (Other), describe");

        s = f.section("wm.geom", "5. Roof Geometry", "What is the roof shape? (ignore porches/carports on fascia)");
        s.photosFrom("g.roof");
        s.choice("wm.geom.answer", "Answer", WM_GEOM).src("g.roof.shape")
                .tr("Hip", WM_GEOM[0], "Flat", WM_GEOM[1], "Gable", WM_GEOM[2],
                        "Mixed hip/gable", WM_GEOM[2], "Other", WM_GEOM[2]);
        s.header("If A (Hip roof)");
        s.num("wm.geom.nonHip", "Total length of non-hip features (feet)");
        s.num("wm.geom.perimeter", "Total roof system perimeter (feet)");
        s.header("If B (Flat roof)");
        s.num("wm.geom.flatArea", "Roof area with slope less than 2:12 (sq ft)");
        s.num("wm.geom.totalArea", "Total roof area (sq ft)");

        s = f.section("wm.swr", "6. Secondary Water Resistance (SWR)",
                "Standard underlayments or hot-mopped felts do not qualify");
        s.photosFrom("g.attic", "g.roof");
        s.choice("wm.swr.answer", "Answer", WM_SWR).src("g.attic.swr")
                .tr(SWR[0], WM_SWR[0], SWR[1], WM_SWR[1], SWR[2], WM_SWR[2]);

        s = f.section("wm.open", "7. Opening Protection",
                "Mark every form of protection in use for each opening type; the weakest decides the answer");
        s.photosFrom("g.openings");
        s.header("Opening Protection Level Chart");
        for (String[] o : WM_OPENINGS) s.multi(o[0], o[1], OPENING).src(o[2]);
        s.header("Answer");
        s.choice("wm.open.answer", "Weakest protection: glazed class and non-glazed level", WM_OPEN).map(new Mapper() {
            @Override
            public Object map(Inspection in) { return openingAnswer(in); }
        });
        s.text("wm.open.docs", "Product approvals / documentation").src("g.openings.docs");

        s = f.section("wm.cert", "Inspector Certification", "Qualified inspector and homeowner signatures");
        s.noPhotos();
        s.text("wm.cert.initials", "Inspector's initials (each page)").src("info.initials");
        s.text("wm.cert.inspector", "Qualified inspector name").src("info.inspector");
        s.choice("wm.cert.licenseType", "I hold an active license as a", LICENSE_TYPES).src("info.licenseType");
        s.text("wm.cert.license", "License or certificate #").src("info.license");
        s.text("wm.cert.company", "Inspection company").src("info.company");
        s.text("wm.cert.phone", "Phone").src("info.companyPhone");
        s.text("wm.cert.employee", "Employee who performed the inspection (licensed contractors and engineers only)");
        s.sig("wm.cert.signature", "Qualified inspector signature").src("info.inspectorSig");
        s.date("wm.cert.date", "Date").src("info.date");
        s.sig("wm.cert.homeowner", "Homeowner signature");
        s.date("wm.cert.homeownerDate", "Homeowner date");
        s.notes("wm.cert.comments", "Inspector notes (not printed on the state form)");
        return f;
    }

    /** Weakest rank of the chart rows selected for one opening type. -1 = nothing selected / N/A only. */
    static int weakest(List<String> levels) {
        int worst = -1;
        for (String v : levels) {
            for (int i = 1; i < OPENING.length; i++) if (OPENING[i].equals(v)) worst = Math.max(worst, i);
        }
        return worst; // 1=A 2=B 3=C 4=D 5=N 6=X
    }

    /** Question 7 answer per the form's rules, from the protection level chart. */
    static String openingAnswer(Inspection in) {
        int glazed = -1;
        boolean anyChart = false;
        for (int i = 0; i < 4; i++) {
            List<String> l = in.list(WM_OPENINGS[i][0]);
            if (!l.isEmpty()) anyChart = true;
            int w = weakest(l);
            if (w == 4) w = 5; // D does not apply to glazed openings
            glazed = Math.max(glazed, w);
        }
        java.util.Set<Integer> ng = new java.util.HashSet<>();
        for (int i = 4; i < 6; i++) {
            List<String> l = in.list(WM_OPENINGS[i][0]);
            if (!l.isEmpty()) anyChart = true;
            for (String v : l) for (int k = 1; k < OPENING.length; k++) if (OPENING[k].equals(v)) ng.add(k);
        }
        if (!anyChart || glazed < 0) return null;
        if (glazed == 6) return WM_OPEN[12];
        int cls = glazed == 1 ? 0 : glazed == 2 ? 1 : glazed == 3 ? 2 : 3; // A, B, C, N
        // Non-glazed levels that are "acceptable" for .1 per class
        int[][] ok = {{1}, {1, 2}, {1, 2, 3}, {1, 2, 3, 5}};
        java.util.Set<Integer> okSet = new java.util.HashSet<>();
        for (int v : ok[cls]) okSet.add(v);
        boolean allOk = true;
        for (int v : ng) if (!okSet.contains(v)) allOk = false;
        int sub;
        if (allOk) sub = 1;
        else {
            boolean anyBad = false;
            for (int v : ng) if (v != 4 && !okSet.contains(v)) anyBad = true;
            sub = (ng.contains(4) && !anyBad) ? 2 : 3;
        }
        return WM_OPEN[cls * 3 + sub - 1];
    }

    // ---------------- helpers ----------------

    static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    static String ageFromYear(String year) {
        int y = parseInt(year);
        if (y < 1800) return null;
        int now = Calendar.getInstance().get(Calendar.YEAR);
        return String.valueOf(Math.max(0, now - y));
    }

    static String joinNonEmpty(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.length() == 0) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(p);
        }
        return sb.toString();
    }
}
