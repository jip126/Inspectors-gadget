package com.inspectorsgadget.app;

import java.util.ArrayList;
import java.util.List;

import static com.inspectorsgadget.app.Forms.*;

/**
 * Checklists for the specialty inspection types. Each item takes its own photos and a
 * dictated comment (same as the general inspection); flagged answers become findings.
 */
final class InspectionTypes {
    private InspectionTypes() {}

    static final String[] RESULT = {"Normal", "Inadequate", "Not tested"};
    static final String[] PRESENCE = {"No issues observed", "Suspected mold growth", "Water staining",
            "Elevated moisture", "Musty odor"};

    static List<Form> build() {
        List<Form> out = new ArrayList<>();
        out.add(wdo());
        out.add(sewer());
        out.add(mold());
        out.add(roof());
        out.add(chimney());
        out.add(structural());
        out.add(hvac());
        out.add(electrical());
        out.add(plumbing());
        out.add(lead());
        out.add(asbestos());
        out.add(radon());
        out.add(pool());
        out.add(soil());
        out.add(oilTank());
        return out;
    }

    private static Form form(String id, String title, String shortTitle, String desc, int color, String note) {
        Form f = new Form(id, title, shortTitle, desc, color);
        f.checklist = true;
        f.reportNote = note;
        return f;
    }

    private static String slug(String s) { return s.toLowerCase().replaceAll("[^a-z0-9]", ""); }

    private static void summary(Form f, String prefix) {
        Section s = f.section(prefix + ".summary", "Summary", "Overall findings and recommendations");
        s.noPhotos();
        s.notes(prefix + ".summary.notes", "Summary & recommendations");
    }

    // ---------------------------------------------------------------- Pest / termite (WDO)

    private static Form wdo() {
        Form f = form(WDO, "Wood-Destroying Organism (WDO) Inspection", "WDO",
                "Termites, wood-boring beetles and wood-decay fungi", 0xFF7A4B1E,
                "Visual inspection of readily accessible areas of the structures listed for visible evidence of "
                        + "wood-destroying organisms at the time of inspection. Areas obstructed or inaccessible are "
                        + "listed in this report. This report is not a guarantee that wood-destroying organisms or "
                        + "damage are absent from areas not inspected.");
        Section s = f.section("wdo.scope", "Scope & Structures", "Structures, requestor, license");
        s.text("wdo.structures", "Structures inspected");
        s.multi("wdo.construction", "Construction", "Concrete block", "Wood frame", "Stucco over frame",
                "Brick veneer", "Slab on grade", "Crawlspace", "Raised wood floor");
        s.text("wdo.requestedBy", "Inspection requested by");
        s.text("wdo.sentTo", "Report sent to");
        s.text("wdo.bizLicense", "Pest control business license #");
        s.text("wdo.idCard", "Inspector ID card #");

        s = f.section("wdo.findings", "Findings", "Live WDO, evidence, damage");
        s.choice("wdo.result", "Result", "No visible signs of WDO observed", "Visible evidence of WDO observed")
                .flag("Visible evidence of WDO observed");
        s.header("Live WDO");
        s.multi("wdo.live", "Live organisms observed", "Subterranean termites", "Formosan termites",
                "Drywood termites", "Dampwood termites", "Powderpost beetles", "Old house borers",
                "Wood-decay fungi").flag("*");
        s.text("wdo.liveLoc", "Location of live WDO");
        s.header("Evidence of WDO");
        s.multi("wdo.evidence", "Evidence observed", "Shelter (mud) tubes", "Frass / fecal pellets",
                "Swarmer wings", "Dead insects", "Exit holes", "Fungal fruiting bodies", "Bait station hits").flag("*");
        s.text("wdo.evidenceLoc", "Location of evidence");
        s.header("Damage");
        s.choice("wdo.damage", "Damage caused by WDO", "None observed", "Visible damage observed")
                .flag("Visible damage observed");
        s.text("wdo.damageLoc", "Location and extent of damage");
        s.header("Conducive conditions");
        s.multi("wdo.conducive", "Conditions conducive to WDO", "Wood-to-soil contact", "Moisture / leaks",
                "Soil or mulch above slab edge", "Vegetation contacting structure", "Stored wood or debris",
                "Cellulose debris in crawlspace", "Poor crawlspace ventilation", "Untreated wood near grade").flag("*");

        s = f.section("wdo.access", "Obstructions & Inaccessible Areas", "Attic, interior, exterior, crawlspace");
        for (String area : new String[]{"Attic", "Interior", "Exterior", "Crawlspace", "Garage", "Other"}) {
            String k = "wdo.access." + slug(area);
            s.header(area);
            s.choice(k, "Access", "Fully accessible", "Partially inaccessible", "Not accessible", "N/A");
            s.text(k + "Why", "Obstruction or reason");
        }

        s = f.section("wdo.treat", "Previous Treatment & Notice", "Prior treatment, notice, treatment by this company");
        s.yn("wdo.prevTreat", "Evidence of previous treatment observed");
        s.text("wdo.prevTreatDesc", "Evidence of previous treatment");
        s.text("wdo.noticeLoc", "Notice of inspection affixed at");
        s.yn("wdo.treatedNow", "Structure treated by this company at time of inspection");
        s.text("wdo.organism", "Common name of organism treated");
        s.text("wdo.pesticide", "Pesticide used");
        s.choice("wdo.method", "Method of treatment", "Whole structure", "Spot treatment", "Bait system",
                "Fumigation", "Other");
        s.text("wdo.terms", "Terms and conditions of treatment");
        s.text("wdo.treatNotice", "Treatment notice location");
        summary(f, "wdo");
        return f;
    }

    // ---------------------------------------------------------------- Sewer / septic

    private static Form sewer() {
        Form f = form(SEWER, "Sewer & Septic Inspection", "Sewer/Septic",
                "Sewer lateral camera, septic tank and drain field", 0xFF4E5D2E,
                "Condition observed at the time of inspection from the access points listed. Buried components "
                        + "not reached by camera or excavation are not inspected. Future performance of the system "
                        + "depends on use and maintenance and is not guaranteed.");
        Section s = f.section("sep.system", "System", "Disposal type, records, use");
        s.choice("sep.type", "Waste disposal", "Municipal sewer", "Private septic system", "Unknown");
        s.text("sep.records", "Permit / records reviewed");
        s.num("sep.age", "System age (years)");
        s.date("sep.lastPumped", "Date last pumped");
        s.choice("sep.use", "Water use before inspection", "Normal occupancy", "Vacant, load test performed",
                "Vacant, no load test", "Unknown");

        s = f.section("sep.lateral", "Sewer Line Camera Inspection", "Cleanout, lateral, main");
        s.choice("sep.cleanout", "Cleanout present and accessible", YN).flag("No");
        s.text("sep.access", "Camera access point");
        s.num("sep.length", "Length inspected (ft)");
        s.multi("sep.pipe", "Pipe material", "PVC", "ABS", "Cast iron", "Clay (vitrified)", "Orangeburg",
                "Concrete", "HDPE").flag("Orangeburg");
        s.multi("sep.defects", "Defects observed", "Root intrusion", "Belly / sag", "Offset joint",
                "Separated joint", "Crack", "Fracture", "Collapse", "Corrosion / scale", "Grease or debris buildup",
                "Standing water").flag("*");
        s.text("sep.defectLoc", "Defect locations (ft from access)");
        s.cond("sep.flow", "Flow test");
        s.cond("sep.lateralCond", "Overall sewer line");

        s = f.section("sep.tank", "Septic Tank", "Tank, baffles, levels");
        s.choice("sep.tankMat", "Tank material", "Concrete", "Fiberglass", "Polyethylene", "Steel", "Unknown")
                .flag("Steel");
        s.num("sep.tankGal", "Tank capacity (gallons)");
        s.choice("sep.compart", "Compartments", "Single", "Two", "Unknown");
        s.choice("sep.lids", "Lids / risers secure", YN).flag("No");
        s.choice("sep.level", "Liquid level", "Normal (at outlet)", "Above outlet", "Below outlet")
                .flag("Above outlet", "Below outlet");
        s.num("sep.sludge", "Sludge depth (in)");
        s.num("sep.scum", "Scum layer (in)");
        s.cond("sep.baffleIn", "Inlet baffle");
        s.cond("sep.baffleOut", "Outlet baffle / effluent filter");
        s.cond("sep.tankStruct", "Tank structure");
        s.choice("sep.pump", "Pumping recommended", YN).flag("Yes");

        s = f.section("sep.field", "Drain Field", "Distribution, absorption area, setbacks");
        s.choice("sep.fieldType", "Drain field type", "Gravity trench", "Chamber system", "Mound",
                "Pressure dosed", "Aerobic treatment unit", "Unknown");
        s.cond("sep.dbox", "Distribution box");
        s.cond("sep.lift", "Lift pump & high water alarm");
        s.choice("sep.surfacing", "Effluent surfacing", YN).flag("Yes");
        s.choice("sep.saturated", "Saturated soil or lush growth over field", YN).flag("Yes");
        s.choice("sep.odor", "Sewage odor", YN).flag("Yes");
        s.text("sep.setback", "Setbacks (well, surface water, structure)");
        s.cond("sep.loadTest", "Hydraulic load test");
        s.cond("sep.fieldCond", "Overall drain field");
        summary(f, "sep");
        return f;
    }

    // ---------------------------------------------------------------- Mold

    private static Form mold() {
        Form f = form(MOLD, "Mold Assessment", "Mold",
                "Visual assessment, moisture sources, sampling", 0xFF2F6B4F,
                "Visual assessment and sampling of the areas listed at the time of inspection. Hidden growth inside "
                        + "wall cavities and other concealed areas may not be detected. Laboratory results apply only to "
                        + "the locations and times sampled.");
        Section s = f.section("mold.scope", "Scope & Conditions", "Reason, temperature, humidity, instruments");
        s.choice("mold.reason", "Reason for assessment", "Real estate transaction", "Visible growth", "Odor",
                "Water event", "Occupant health concern", "Post-remediation verification");
        s.num("mold.inTemp", "Indoor temperature (°F)");
        s.num("mold.inRh", "Indoor relative humidity (%)");
        s.num("mold.outTemp", "Outdoor temperature (°F)");
        s.num("mold.outRh", "Outdoor relative humidity (%)");
        s.yn("mold.hvacOn", "HVAC operating during inspection");
        s.multi("mold.tools", "Instruments used", "Moisture meter", "Thermal imaging camera", "Hygrometer",
                "Air sampling pump", "Borescope", "Particle counter");

        s = f.section("mold.visual", "Visual Inspection", "Growth, staining, moisture, odor by area");
        for (String area : new String[]{"Attic", "Living areas", "Bedrooms", "Kitchen", "Bathrooms", "Laundry",
                "HVAC equipment & ducts", "Crawlspace / basement", "Garage", "Exterior walls"}) {
            s.multi("mold.v." + slug(area), area, PRESENCE)
                    .flag("Suspected mold growth", "Water staining", "Elevated moisture", "Musty odor");
        }
        s.num("mold.area", "Approx. area of visible growth (sq ft)");

        s = f.section("mold.moisture", "Moisture Sources", "Leaks, condensation, ventilation");
        s.choice("mold.activeLeak", "Active leak", YN).flag("Yes");
        s.choice("mold.priorWater", "Evidence of prior water intrusion", YN).flag("Yes");
        s.choice("mold.condensation", "Condensation on windows, ducts or registers", YN).flag("Yes");
        s.choice("mold.highRh", "Indoor relative humidity above 60%", YN).flag("Yes");
        s.cond("mold.bathVent", "Bathroom exhaust ventilation");
        s.cond("mold.dryerVent", "Dryer venting");
        s.cond("mold.drainage", "Exterior grading & drainage");
        s.text("mold.readings", "Moisture readings (location, %)");

        s = f.section("mold.samples", "Samples", "Air, surface and control samples");
        s.text("mold.lab", "Laboratory");
        s.date("mold.labDate", "Date results received");
        for (int i = 1; i <= 5; i++) {
            String k = "mold.s" + i;
            s.header("Sample " + i);
            s.text(k + ".loc", "Location");
            s.choice(k + ".type", "Sample type", "Air (spore trap)", "Outdoor control", "Swab", "Tape lift", "Bulk");
            s.text(k + ".id", "Sample / cassette #");
            s.text(k + ".result", "Lab result");
            s.choice(k + ".level", "Result", "Normal / below outdoor control", "Elevated", "Pending")
                    .flag("Elevated");
        }

        s = f.section("mold.summary", "Summary", "Overall findings and recommendations");
        s.noPhotos();
        s.choice("mold.remediation", "Remediation recommended", YN).flag("Yes");
        s.choice("mold.condition", "Condition", "Condition 1 (normal fungal ecology)",
                "Condition 2 (settled spores)", "Condition 3 (actual growth)")
                .flag("Condition 2 (settled spores)", "Condition 3 (actual growth)");
        s.notes("mold.summary.notes", "Summary & recommendations");
        return f;
    }

    // ---------------------------------------------------------------- Roof

    private static Form roof() {
        Form f = form(ROOF, "Roof Inspection", "Roof",
                "Covering, flashing, drainage, decking", 0xFF8C3B2E,
                "Visual inspection of the roof covering, flashing and drainage from the vantage point listed, and "
                        + "of the roof structure from accessible attic areas. Remaining life is an estimate. This report "
                        + "is not a warranty against leaks.");
        Section s = f.section("roof.overview", "Roof Overview", "Method, covering, shape, age");
        s.choice("roof.method", "Inspected from", "Walked roof", "Ladder at eaves", "Drone", "Ground w/ binoculars")
                .src("g.roof.method");
        s.choice("roof.covering", "Predominant roof covering", ROOF_COVER).src("g.roof.covering");
        s.choice("roof.secondary", "Secondary roof covering", ROOF_COVER).src("g.roof.secondary");
        s.choice("roof.shape", "Roof shape", ROOF_SHAPE).src("g.roof.shape");
        s.num("roof.pitch", "Predominant pitch (x/12)");
        s.num("roof.layers", "Layers of covering");
        s.num("roof.yearInstalled", "Year installed / replaced").src("g.roof.yearInstalled");
        s.num("roof.age", "Roof age (years)").src("g.roof.age");
        s.num("roof.life", "Remaining useful life (years)").src("g.roof.life");
        s.date("roof.permit", "Date of last roofing permit").src("g.roof.permitDate");

        s = f.section("roof.cover", "Roof Covering", "Field, ridges, fasteners, damage");
        s.multi("roof.damage", "Signs of damage", ROOF_DAMAGE).flag("*").src("g.roof.damage");
        s.cond("roof.field", "Field of roof");
        s.cond("roof.ridge", "Ridge & hip caps");
        s.cond("roof.fasteners", "Fasteners / exposed nails");
        s.choice("roof.storm", "Hail or wind damage", YN).flag("Yes");
        s.choice("roof.ponding", "Ponding (low slope)", "Yes", "No", "N/A").flag("Yes");
        s.choice("roof.leaks", "Visible signs of leaks", YN).flag("Yes").src("g.roof.leaks");

        s = f.section("roof.flash", "Flashing & Penetrations", "Valleys, walls, chimneys, vents, skylights");
        s.cond("roof.dripEdge", "Drip edge");
        s.cond("roof.valleys", "Valleys");
        s.cond("roof.wallFlash", "Step & wall flashing");
        s.cond("roof.chimneyFlash", "Chimney flashing");
        s.cond("roof.boots", "Plumbing vent boots");
        s.cond("roof.vents", "Roof vents & exhaust caps");
        s.cond("roof.skylights", "Skylights");

        s = f.section("roof.drain", "Drainage & Edges", "Gutters, downspouts, soffit, fascia");
        s.cond("roof.gutters", "Gutters").src("g.roof.gutters");
        s.cond("roof.downspouts", "Downspouts & extensions");
        s.cond("roof.soffit", "Soffit & fascia");

        s = f.section("roof.under", "Attic & Decking", "Deck, leaks, ventilation, underlayment");
        s.choice("roof.deck", "Roof deck material", DECK_MAT).src("g.attic.deck");
        s.choice("roof.atticLeaks", "Leak stains on underside of decking", YN).flag("Yes").src("g.attic.leaks");
        s.cond("roof.decking", "Decking from attic");
        s.cond("roof.vent", "Attic ventilation").src("g.attic.vent");
        s.choice("roof.swr", "Secondary water resistance", SWR).src("g.attic.swr");

        s = f.section("roof.summary", "Summary", "Overall condition and recommendation");
        s.noPhotos();
        s.cond("roof.overall", "Overall roof condition");
        s.choice("roof.recommend", "Recommendation", "No action needed", "Maintenance / repair",
                "Replace within 1 to 3 years", "Replace now")
                .flag("Maintenance / repair", "Replace within 1 to 3 years", "Replace now");
        s.notes("roof.summary.notes", "Summary & recommendations");
        return f;
    }

    // ---------------------------------------------------------------- Chimney

    private static Form chimney() {
        Form f = form(CHIMNEY, "Chimney Inspection", "Chimney",
                "Exterior, flue, firebox, appliance connection", 0xFF5A3A2A,
                "Inspection performed to the NFPA 211 level listed. Concealed portions of the flue are evaluated "
                        + "only where a video scan was performed. Draft performance under all conditions is not "
                        + "guaranteed.");
        Section s = f.section("chim.overview", "Chimney & Appliance", "Level, type, liner, appliance");
        s.choice("chim.level", "NFPA 211 inspection level", "Level 1", "Level 2", "Level 3");
        s.choice("chim.type", "Chimney type", "Masonry", "Factory-built (prefab)", "Metal flue / Class A");
        s.choice("chim.appliance", "Connected appliance", "Wood-burning fireplace", "Gas fireplace",
                "Wood stove / insert", "Pellet stove", "Gas furnace / water heater", "Oil appliance");
        s.num("chim.flues", "Number of flues");
        s.choice("chim.liner", "Flue liner", "Clay tile", "Stainless steel", "Cast-in-place", "Aluminum",
                "None / unlined").flag("None / unlined");
        s.yn("chim.scan", "Video scan performed");

        s = f.section("chim.ext", "Exterior", "Crown, cap, masonry, flashing, clearance");
        s.cond("chim.crown", "Crown / chase cover");
        s.cond("chim.cap", "Rain cap & spark arrestor");
        s.cond("chim.masonry", "Masonry & mortar joints");
        s.cond("chim.flashing", "Flashing");
        s.cond("chim.height", "Height & clearance (3-2-10 rule)");
        s.choice("chim.spalling", "Spalling or efflorescence", YN).flag("Yes");
        s.choice("chim.leaning", "Leaning or separation from structure", YN).flag("Yes");

        s = f.section("chim.flue", "Flue", "Liner, creosote, obstruction, sizing");
        s.cond("chim.linerCond", "Liner condition");
        s.choice("chim.cracks", "Cracked or missing liner tiles", YN).flag("Yes");
        s.choice("chim.creosote", "Creosote", "None", "Stage 1 (light soot)", "Stage 2 (flaky)",
                "Stage 3 (glazed)").flag("Stage 2 (flaky)", "Stage 3 (glazed)");
        s.choice("chim.obstruction", "Obstruction (nests, debris)", YN).flag("Yes");
        s.choice("chim.sizing", "Flue sized for the appliance", YN).flag("No");
        s.cond("chim.smokeChamber", "Smoke chamber");
        s.cond("chim.damper", "Damper");

        s = f.section("chim.firebox", "Firebox & Hearth", "Firebox, hearth, clearances, connector");
        s.cond("chim.fireboxCond", "Firebox");
        s.cond("chim.hearth", "Hearth extension");
        s.cond("chim.clearance", "Clearance to combustibles");
        s.cond("chim.mantel", "Mantel & surround");
        s.cond("chim.connector", "Connector pipe");
        s.cond("chim.gas", "Gas valve, logs & pilot");
        s.choice("chim.co", "CO detector present", YN).flag("No");

        s = f.section("chim.summary", "Summary", "Overall condition and recommendation");
        s.noPhotos();
        s.cond("chim.overall", "Overall chimney condition");
        s.choice("chim.sweep", "Sweeping recommended", YN).flag("Yes");
        s.choice("chim.safe", "Safe for use", "Yes", "No, do not use until repaired")
                .flag("No, do not use until repaired");
        s.notes("chim.summary.notes", "Summary & recommendations");
        return f;
    }

    // ---------------------------------------------------------------- Foundation / structural

    private static Form structural() {
        Form f = form(STRUCTURAL, "Foundation & Structural Inspection", "Structural",
                "Foundation, framing, movement, drainage", 0xFF46505E,
                "Visual observation of accessible structural components and indicators of movement at the time of "
                        + "inspection. This is not an engineering evaluation. Concealed framing and soils below the "
                        + "foundation are not inspected.");
        Section s = f.section("fnd.overview", "Foundation & Framing", "Foundation, walls, floor framing");
        s.choice("fnd.type", "Foundation", "Slab on grade", "Crawlspace", "Basement", "Piers")
                .src("g.struct.foundation");
        s.choice("fnd.walls", "Wall construction", "Concrete block (CBS)", "Wood frame", "Steel frame",
                "Poured concrete", "Other").src("g.struct.walls");
        s.choice("fnd.floor", "Floor framing", "Slab", "Wood joists", "Engineered I-joists", "Steel beams");

        s = f.section("fnd.ext", "Exterior Observations", "Cracks, bowing, grading, erosion");
        s.cond("fnd.foundationWalls", "Visible foundation walls / slab edge");
        s.choice("fnd.stepCracks", "Stair-step cracking in block or brick", YN).flag("Yes");
        s.choice("fnd.crackWidth", "Largest crack width", "None", "Hairline (under 1/16\")", "1/16\" to 1/4\"",
                "Over 1/4\"").flag("1/16\" to 1/4\"", "Over 1/4\"");
        s.choice("fnd.bowing", "Bowing or leaning walls", YN).flag("Yes");
        s.choice("fnd.gaps", "Gaps at windows, doors or trim", YN).flag("Yes");
        s.cond("fnd.grading", "Grading away from foundation");
        s.cond("fnd.discharge", "Gutter & downspout discharge");
        s.choice("fnd.erosion", "Erosion or soil washout at foundation", YN).flag("Yes");

        s = f.section("fnd.int", "Interior Observations", "Drywall, doors, floors, separations");
        s.choice("fnd.drywall", "Drywall cracks at openings", YN).flag("Yes");
        s.choice("fnd.sticking", "Sticking doors or windows", YN).flag("Yes");
        s.choice("fnd.sloping", "Sloping or uneven floors", YN).flag("Yes");
        s.choice("fnd.tile", "Cracked floor tile", YN).flag("Yes");
        s.choice("fnd.separation", "Wall to ceiling or floor separation", YN).flag("Yes");

        s = f.section("fnd.sub", "Crawlspace / Basement", "Piers, beams, joists, moisture");
        s.choice("fnd.access", "Access", "Full", "Limited", "None", "N/A (slab)");
        s.cond("fnd.piers", "Piers & posts");
        s.cond("fnd.beams", "Beams & girders");
        s.cond("fnd.joists", "Joists & subfloor");
        s.cond("fnd.sill", "Sill plate & anchorage");
        s.cond("fnd.vapor", "Vapor barrier");
        s.choice("fnd.water", "Standing water or moisture", YN).flag("Yes");
        s.choice("fnd.rot", "Wood rot or damage", YN).flag("Yes");

        s = f.section("fnd.measure", "Measurements", "Elevation survey");
        s.choice("fnd.instrument", "Elevation instrument", "Digital manometer", "Laser level", "Not performed");
        s.text("fnd.reference", "Reference point");
        s.num("fnd.elevDiff", "Max floor elevation difference (in)");
        s.text("fnd.span", "Over distance (ft)");
        s.notes("fnd.readings", "Elevation readings");

        s = f.section("fnd.summary", "Summary", "Overall condition and recommendation");
        s.noPhotos();
        s.cond("fnd.overall", "Overall structure");
        s.choice("fnd.engineer", "Structural engineer evaluation recommended", YN).flag("Yes");
        s.notes("fnd.summary.notes", "Summary & recommendations");
        return f;
    }

    // ---------------------------------------------------------------- HVAC

    private static Form hvac() {
        Form f = form(HVAC, "HVAC Inspection", "HVAC",
                "Cooling, heating, air handler, distribution", 0xFF1F5F8B,
                "Equipment operated with normal controls at the time of inspection. Heat exchangers, refrigerant "
                        + "charge and concealed ductwork are not fully evaluated without disassembly. Remaining life "
                        + "is an estimate.");
        Section s = f.section("hvac.cool", "Cooling Equipment", "Condenser, refrigerant, performance");
        s.choice("hvac.system", "System type", "Split system", "Heat pump", "Package unit", "Gas furnace",
                "Mini-split", "Window/wall units", "Other").src("g.hvac.system");
        s.text("hvac.brand", "Brand / model").src("g.hvac.brand");
        s.text("hvac.serial", "Serial #");
        s.num("hvac.tons", "Capacity (tons)");
        s.num("hvac.seer", "SEER / SEER2");
        s.num("hvac.coolYear", "Year manufactured");
        s.choice("hvac.refrigerant", "Refrigerant", "R-410A", "R-454B", "R-32", "R-22", "Other").flag("R-22");
        s.cond("hvac.coil", "Condenser coil & fins");
        s.cond("hvac.pad", "Pad, level & clearance");
        s.cond("hvac.disconnect", "Service disconnect");
        s.cond("hvac.lineset", "Refrigerant line insulation");
        s.num("hvac.supplyT", "Supply air temp (°F)");
        s.num("hvac.returnT", "Return air temp (°F)");
        s.num("hvac.split", "Temperature split (°F)");
        s.choice("hvac.coolOp", "Cooling operation", RESULT).flag("Inadequate");

        s = f.section("hvac.heat", "Heating Equipment", "Heat source, burners, venting");
        s.choice("hvac.heatType", "Heat source", "Heat pump", "Electric resistance", "Gas furnace", "Oil furnace",
                "Boiler", "None");
        s.num("hvac.heatYear", "Year manufactured");
        s.cond("hvac.burners", "Burners / heating elements");
        s.cond("hvac.exchanger", "Heat exchanger (visible portions)");
        s.cond("hvac.flue", "Venting / flue");
        s.choice("hvac.co", "CO detected at equipment or registers", YN).flag("Yes");
        s.choice("hvac.heatOp", "Heating operation", RESULT).flag("Inadequate");

        s = f.section("hvac.air", "Air Handler & Distribution", "Blower, coil, filter, ducts, controls");
        s.cond("hvac.blower", "Blower");
        s.cond("hvac.evap", "Evaporator coil");
        s.cond("hvac.filter", "Filter").src("g.hvac.filter");
        s.text("hvac.filterSize", "Filter size");
        s.cond("hvac.ducts", "Ductwork").src("g.hvac.ducts");
        s.choice("hvac.ductLeak", "Disconnected or leaking ducts", YN).flag("Yes");
        s.cond("hvac.ductIns", "Duct insulation");
        s.cond("hvac.registers", "Supply registers & returns");
        s.cond("hvac.tstat", "Thermostat");

        s = f.section("hvac.cond", "Condensate", "Drain, pan, safety switch");
        s.cond("hvac.drain", "Primary condensate drain");
        s.choice("hvac.float", "Secondary pan or float switch present", YN).flag("No");
        s.choice("hvac.panWater", "Water in secondary pan", YN).flag("Yes");
        s.choice("hvac.growth", "Microbial growth on coil or pan", YN).flag("Yes");

        s = f.section("hvac.summary", "Summary", "Age, service, overall condition");
        s.noPhotos();
        s.num("hvac.age", "System age (years)").src("g.hvac.age");
        s.date("hvac.serviced", "Date of last servicing").src("g.hvac.serviced");
        s.num("hvac.life", "Remaining useful life (years)");
        s.cond("hvac.overall", "Overall HVAC");
        s.notes("hvac.summary.notes", "Summary & recommendations");
        return f;
    }

    // ---------------------------------------------------------------- Electrical

    private static Form electrical() {
        Form f = form(ELECTRICAL, "Electrical Inspection", "Electrical",
                "Service, panels, wiring, devices, protection", 0xFFB0761A,
                "Visual inspection of accessible electrical components with the panel cover removed where safe, "
                        + "and testing of a representative number of receptacles. Concealed wiring is not inspected. "
                        + "This is not a code compliance inspection.");
        Section s = f.section("elec.service", "Service", "Entrance, meter, grounding, bonding");
        s.choice("elec.entrance", "Service entrance", "Overhead", "Underground").src("g.elec.service");
        s.num("elec.amps", "Service rating (amps)").src("g.elec.mainAmps");
        s.choice("elec.voltage", "Voltage", "120/240V single phase", "120/208V three phase");
        s.choice("elec.conductor", "Service conductors", "Copper", "Aluminum", "Copper-clad aluminum");
        s.cond("elec.drop", "Service drop / lateral");
        s.cond("elec.mast", "Mast & weatherhead");
        s.cond("elec.meter", "Meter base");
        s.cond("elec.ground", "Grounding electrode");
        s.cond("elec.bond", "Water & gas bonding");

        s = f.section("elec.panel", "Panels", "Main and sub panels");
        s.text("elec.panelLoc", "Main panel location").src("g.elec.mainLocation");
        s.text("elec.panelBrand", "Brand / model").src("g.elec.mainBrand");
        s.choice("elec.panelType", "Type", PANEL_TYPE).src("g.elec.mainType");
        s.choice("elec.recall", "Panel with known defects", "None of these", "Federal Pacific Stab-Lok",
                "Zinsco / Sylvania", "Challenger", "Pushmatic").flag("Federal Pacific Stab-Lok", "Zinsco / Sylvania",
                "Challenger", "Pushmatic");
        s.num("elec.panelAge", "Panel age (years)").src("g.elec.mainAge");
        s.cond("elec.deadfront", "Cover & deadfront");
        s.cond("elec.labels", "Circuit labeling");
        s.cond("elec.clearance", "Working clearance");
        s.choice("elec.doubleTap", "Double taps", YN).flag("Yes");
        s.choice("elec.oversize", "Breaker or fuse oversized for conductor", YN).flag("Yes");
        s.choice("elec.scorch", "Scorching or arcing", YN).flag("Yes");
        s.choice("elec.corrosion", "Corrosion", YN).flag("Yes");
        s.choice("elec.subNeutral", "Subpanel neutrals and grounds separated", "Yes", "No", "N/A").flag("No");

        s = f.section("elec.branch", "Branch Wiring", "Wiring type, condition, splices");
        s.multi("elec.wiring", "Wiring type present", WIRING)
                .flag("Aluminum branch wiring", "Cloth wiring", "Active knob and tube").src("g.elec.wiring");
        s.multi("elec.alRepair", "Aluminum wiring remediation", AL_REPAIR).src("g.elec.alRepair");
        s.cond("elec.wiringCond", "Visible wiring condition");
        s.choice("elec.splices", "Exposed splices or open junction boxes", YN).flag("Yes");
        s.choice("elec.extCords", "Extension cords used as permanent wiring", YN).flag("Yes");

        s = f.section("elec.devices", "Devices & Protection", "Receptacles, GFCI, AFCI, detectors");
        s.cond("elec.receptacles", "Receptacles (representative number tested)");
        s.choice("elec.polarity", "Reverse polarity", YN).flag("Yes");
        s.choice("elec.openGround", "Open grounds", YN).flag("Yes");
        s.choice("elec.gfci", "GFCI protection where required", YN).flag("No").src("g.elec.gfci");
        s.choice("elec.afci", "AFCI protection", "Present", "Partial", "Not present");
        s.cond("elec.lighting", "Lighting & switches");
        s.cond("elec.exterior", "Exterior receptacles & covers");
        s.choice("elec.smoke", "Smoke detectors present", YN).flag("No").src("g.elec.smoke");
        s.choice("elec.coDet", "CO detectors present", "Yes", "No", "N/A (all electric)").flag("No");
        s.cond("elec.generator", "Generator & transfer switch");
        s.cond("elec.solar", "Solar / battery equipment");

        s = f.section("elec.summary", "Summary", "Overall condition");
        s.noPhotos();
        s.cond("elec.overall", "Overall electrical system");
        s.choice("elec.electrician", "Licensed electrician evaluation recommended", YN).flag("Yes");
        s.notes("elec.summary.notes", "Summary & recommendations");
        return f;
    }

    // ---------------------------------------------------------------- Plumbing / water quality

    static final String[] PLW_FIXTURES = {"Kitchen sink", "Bathroom sinks", "Toilets", "Tubs & showers",
            "Hose bibs", "Laundry connections", "Dishwasher connection", "Ice maker line"};
    static final String[] ANALYTES = {"Total coliform", "E. coli", "Lead (ppb)", "Copper (mg/L)", "Nitrate (mg/L)",
            "Arsenic (ppb)", "Iron (mg/L)", "Hardness (gpg)", "pH", "Total dissolved solids (mg/L)"};

    private static Form plumbing() {
        Form f = form(PLUMBING, "Plumbing & Water Quality Inspection", "Plumbing",
                "Supply, drains, water heater, fixtures, water testing", 0xFF1E6F86,
                "Visual inspection and operation of accessible plumbing with normal controls. Concealed and "
                        + "underground piping is not inspected. Water test results apply to the sample collected and "
                        + "are compared with EPA drinking water standards.");
        Section s = f.section("plw.supply", "Water Supply", "Source, piping, pressure, shut off");
        s.choice("plw.source", "Water source", "Municipal", "Private well", "Shared well");
        s.choice("plw.service", "Main service line", "Copper", "PEX", "PVC", "Galvanized", "Polybutylene", "Lead",
                "Unknown").flag("Galvanized", "Polybutylene", "Lead");
        s.multi("plw.pipes", "Supply piping", PIPES).flag("Polybutylene", "Galvanized").src("g.plumb.pipes");
        s.text("plw.shutoff", "Main shut off location").src("g.plumb.shutoff");
        s.num("plw.psi", "Static water pressure (psi)");
        s.choice("plw.prv", "Pressure reducing valve", YN);
        s.cond("plw.flow", "Functional flow");
        s.choice("plw.hammer", "Water hammer", YN).flag("Yes");

        s = f.section("plw.dwv", "Drain, Waste & Vent", "Drains, traps, vents, cleanouts");
        s.choice("plw.drains", "Drain / waste piping", "PVC", "ABS", "Cast iron", "Galvanized", "Other")
                .src("g.plumb.drains");
        s.cond("plw.drainFlow", "Functional drainage");
        s.cond("plw.traps", "Traps");
        s.cond("plw.vents", "Vent piping");
        s.cond("plw.cleanouts", "Cleanouts");
        s.choice("plw.slow", "Slow or blocked drains", YN).flag("Yes");

        s = f.section("plw.wh", "Water Heater", "Unit, TPR, venting, temperature");
        s.choice("plw.whType", "Type", "Tank - electric", "Tank - gas", "Tankless - electric", "Tankless - gas",
                "Heat pump").src("g.plumb.whType");
        s.text("plw.whLoc", "Location").src("g.plumb.whLocation");
        s.num("plw.whGal", "Capacity (gallons)").src("g.plumb.whGallons");
        s.num("plw.whYear", "Year manufactured").src("g.plumb.whYear");
        s.cond("plw.whUnit", "Tank / unit");
        s.choice("plw.tpr", "TPR valve present", YN).flag("No").src("g.plumb.tpr");
        s.cond("plw.tprLine", "TPR discharge line");
        s.cond("plw.whVent", "Venting & combustion air");
        s.cond("plw.whPan", "Drain pan & straps");
        s.num("plw.hotTemp", "Hot water temperature (°F)");

        s = f.section("plw.fixtures", "Fixtures & Leaks", "Sinks, toilets, tubs, connections");
        for (String fx : PLW_FIXTURES) s.cond("plw.fx." + slug(fx), fx);
        s.choice("plw.activeLeak", "Active leak", YN).flag("Yes").src("g.plumb.activeLeak");
        s.choice("plw.priorLeak", "Evidence of prior leak", YN).flag("Yes").src("g.plumb.priorLeak");
        s.choice("plw.backflow", "Cross-connection or missing backflow protection", YN).flag("Yes");

        s = f.section("plw.wq", "Water Quality Testing", "Sample, laboratory, results");
        s.choice("plw.sampleFrom", "Sample collected from", "Kitchen tap", "Well head", "Pressure tank", "Other");
        s.date("plw.sampleDate", "Date collected");
        s.text("plw.lab", "Laboratory");
        s.date("plw.resultDate", "Date results received");
        for (String a : ANALYTES) s.text("plw.wq." + slug(a), a);
        s.multi("plw.exceeds", "Results exceeding EPA limits", ANALYTES).flag("*");

        s = f.section("plw.summary", "Summary", "Overall condition");
        s.noPhotos();
        s.cond("plw.overall", "Overall plumbing");
        s.choice("plw.treatment", "Water treatment recommended", YN).flag("Yes");
        s.notes("plw.summary.notes", "Summary & recommendations");
        return f;
    }

    // ---------------------------------------------------------------- Lead-based paint

    private static Form lead() {
        Form f = form(LEAD, "Lead-Based Paint Inspection", "Lead Paint",
                "XRF readings, dust and soil samples, hazards", 0xFF6B4C8A,
                "Performed under EPA 40 CFR Part 745 and HUD Guidelines for the Evaluation and Control of Lead-Based "
                        + "Paint Hazards. Results apply only to the components and locations tested. Lead-based paint "
                        + "is 1.0 mg/cm² or greater by XRF.");
        Section s = f.section("lead.scope", "Scope", "Type, certification, method");
        s.choice("lead.type", "Service performed", "Lead-based paint inspection", "Risk assessment",
                "Lead hazard screen", "Clearance examination");
        s.choice("lead.pre1978", "Built before 1978", YN).map(new Mapper() {
            @Override
            public Object map(Inspection in) { return builtBefore(in, 1978); }
        });
        s.text("lead.cert", "EPA / state certification #");
        s.multi("lead.methods", "Methods", "XRF", "Paint chip sampling", "Dust wipe sampling", "Soil sampling");
        s.text("lead.xrf", "XRF instrument (make, model, serial)");

        s = f.section("lead.readings", "Paint Readings", "Component, location, result");
        for (int i = 1; i <= 8; i++) {
            String k = "lead.r" + i;
            s.header("Reading " + i);
            s.text(k + ".loc", "Room / side");
            s.text(k + ".component", "Component");
            s.choice(k + ".substrate", "Substrate", "Wood", "Drywall", "Plaster", "Metal", "Masonry", "Concrete");
            s.num(k + ".value", "Reading (mg/cm²)");
            s.choice(k + ".result", "Result", "Negative", "Positive", "Inconclusive").flag("Positive", "Inconclusive");
            s.choice(k + ".cond", "Paint condition", "Intact", "Fair", "Poor (deteriorated)").flag("Poor (deteriorated)");
        }

        s = f.section("lead.dust", "Dust & Soil Samples", "Wipe and soil results");
        for (int i = 1; i <= 4; i++) {
            String k = "lead.d" + i;
            s.header("Dust wipe " + i);
            s.text(k + ".loc", "Location");
            s.choice(k + ".surface", "Surface", "Floor", "Window sill", "Window trough");
            s.num(k + ".value", "Result (µg/ft²)");
            s.choice(k + ".result", "Result", "Below hazard level", "At or above hazard level")
                    .flag("At or above hazard level");
        }
        s.header("Soil");
        s.num("lead.soilPlay", "Play area bare soil (ppm)");
        s.num("lead.soilYard", "Rest of yard / dripline (ppm)");
        s.choice("lead.soilResult", "Soil result", "Below hazard level", "At or above hazard level")
                .flag("At or above hazard level");

        s = f.section("lead.hazards", "Hazard Observations", "Deteriorated, friction, impact, chewable surfaces");
        s.choice("lead.deteriorated", "Deteriorated paint", YN).flag("Yes");
        s.choice("lead.friction", "Friction or impact surfaces (windows, doors, floors)", YN).flag("Yes");
        s.choice("lead.chewable", "Chewable surfaces accessible to children", YN).flag("Yes");
        s.choice("lead.bareSoil", "Bare soil at play areas or dripline", YN).flag("Yes");
        s.choice("lead.children", "Children under 6 or pregnant occupant", YN);

        s = f.section("lead.summary", "Summary", "Result and recommendations");
        s.noPhotos();
        s.choice("lead.result", "Result", "No lead-based paint identified", "Lead-based paint present",
                "Lead-based paint hazards present").flag("Lead-based paint present", "Lead-based paint hazards present");
        s.notes("lead.summary.notes", "Summary, interim controls & abatement recommendations");
        return f;
    }

    // ---------------------------------------------------------------- Asbestos

    static final String[] ASB_MATERIALS = {"Vinyl floor tile & mastic", "Sheet vinyl flooring",
            "Popcorn / acoustic ceiling", "Drywall joint compound", "Plaster", "Pipe insulation", "Duct wrap / tape",
            "Boiler or furnace insulation", "Roofing", "Siding / transite", "Window glazing & caulk",
            "Vermiculite insulation"};

    private static Form asbestos() {
        Form f = form(ASBESTOS, "Asbestos Inspection", "Asbestos",
                "Suspect materials, samples, assessment", 0xFF5E6B73,
                "Suspect materials sampled from accessible areas and analyzed by an accredited laboratory. Materials "
                        + "not sampled are assumed to contain asbestos until tested. Concealed materials may exist "
                        + "and should be tested before renovation or demolition.");
        Section s = f.section("asb.scope", "Scope", "Purpose, license, laboratory");
        s.choice("asb.purpose", "Purpose", "Pre-renovation", "Pre-demolition", "Real estate transaction",
                "Damage assessment");
        s.choice("asb.pre1981", "Built before 1981", YN).map(new Mapper() {
            @Override
            public Object map(Inspection in) { return builtBefore(in, 1981); }
        });
        s.text("asb.cert", "Asbestos inspector license / AHERA cert #");
        s.text("asb.lab", "Laboratory (NVLAP accredited)");
        s.multi("asb.method", "Analysis", "PLM", "PLM point count", "TEM");

        s = f.section("asb.materials", "Suspect Materials", "Presence and sampling status");
        for (String m : ASB_MATERIALS) {
            s.choice("asb.m." + slug(m), m, "Not present", "Present, not sampled", "Sampled, none detected",
                    "Sampled, asbestos detected", "Assumed asbestos-containing")
                    .flag("Sampled, asbestos detected", "Assumed asbestos-containing");
        }

        s = f.section("asb.samples", "Samples", "Location, friability, condition, result");
        for (int i = 1; i <= 6; i++) {
            String k = "asb.s" + i;
            s.header("Sample " + i);
            s.text(k + ".loc", "Location");
            s.text(k + ".material", "Material");
            s.choice(k + ".friable", "Friability", "Friable", "Non-friable");
            s.choice(k + ".cond", "Condition", "Good", "Damaged", "Significantly damaged")
                    .flag("Damaged", "Significantly damaged");
            s.text(k + ".result", "Result (% and type)");
            s.choice(k + ".acm", "Classification", "None detected", "Trace (under 1%)", "ACM (over 1%)")
                    .flag("ACM (over 1%)");
        }

        s = f.section("asb.assess", "Assessment", "Quantity, disturbance, response");
        s.text("asb.quantity", "Estimated quantity of ACM (sq ft / linear ft)");
        s.choice("asb.disturbance", "Potential for disturbance", "Low", "Moderate", "High").flag("High");
        s.choice("asb.action", "Response action", "No action", "Operations & maintenance", "Repair / encapsulate",
                "Abatement before renovation").flag("Repair / encapsulate", "Abatement before renovation");
        summary(f, "asb");
        return f;
    }

    // ---------------------------------------------------------------- Radon

    private static Form radon() {
        Form f = form(RADON, "Radon Inspection", "Radon",
                "Closed-house conditions, devices, results, mitigation", 0xFF8A5A14,
                "Short-term radon measurement performed under ANSI/AARST MAH protocols. Results reflect conditions "
                        + "during the test period only. The EPA action level is 4.0 pCi/L; mitigation is recommended "
                        + "at or above that level and should be considered between 2.0 and 4.0 pCi/L.");
        Section s = f.section("rad.scope", "Scope", "Purpose, license, test type");
        s.choice("rad.purpose", "Purpose", "Real estate transaction", "Homeowner test", "Post-mitigation test",
                "New construction");
        s.text("rad.cert", "Radon license / NRPP or NRSB cert #");
        s.choice("rad.testType", "Test type", "Continuous radon monitor (CRM)", "Charcoal canister",
                "Electret ion chamber", "Alpha track");
        s.choice("rad.duration", "Test duration", "48 hours", "72 hours", "96 hours", "90 days or more");
        s.text("rad.lab", "Laboratory (if passive devices)");

        s = f.section("rad.conditions", "Test Conditions", "Closed-house conditions and placement");
        s.choice("rad.closed12", "Closed-house conditions 12 hours before test", YN).flag("No");
        s.choice("rad.closedDuring", "Closed-house conditions maintained during test", YN).flag("No");
        s.choice("rad.tamper", "Signs of tampering or interference", YN).flag("Yes");
        s.choice("rad.weather", "Severe weather during test", YN).flag("Yes");
        s.choice("rad.hvac", "HVAC operating normally", YN);
        s.choice("rad.level", "Lowest livable level tested", "Basement", "First floor", "Crawlspace", "Slab level");
        s.text("rad.location", "Device location (room)");
        s.choice("rad.existingSystem", "Existing mitigation system", YN);

        s = f.section("rad.devices", "Devices & Results", "Placement times and readings");
        for (int i = 1; i <= 2; i++) {
            String k = "rad.d" + i;
            s.header("Device " + i);
            s.text(k + ".serial", "Device serial #");
            s.text(k + ".start", "Start (date and time)");
            s.text(k + ".end", "End (date and time)");
            s.num(k + ".result", "Result (pCi/L)");
        }
        s.num("rad.average", "Average result (pCi/L)");
        s.choice("rad.result", "Result", "Below 2.0 pCi/L", "2.0 to 3.9 pCi/L", "4.0 pCi/L or above")
                .flag("4.0 pCi/L or above");

        s = f.section("rad.system", "Mitigation System", "Existing system condition");
        s.choice("rad.sys.fan", "Fan operating", YN).flag("No");
        s.choice("rad.sys.manometer", "Manometer reading present", YN).flag("No");
        s.choice("rad.sys.label", "System labeled", YN).flag("No");
        s.cond("rad.sys.piping", "Piping and discharge");

        s = f.section("rad.summary", "Summary", "Result and recommendations");
        s.noPhotos();
        s.choice("rad.recommend", "Recommendation", "No action", "Consider mitigation", "Mitigation recommended",
                "Retest recommended").flag("Mitigation recommended", "Retest recommended");
        s.notes("rad.summary.notes", "Summary & recommendations");
        return f;
    }

    // ---------------------------------------------------------------- Pool / spa

    private static Form pool() {
        Form f = form(POOL, "Pool & Spa Inspection", "Pool/Spa",
                "Shell, equipment, electrical, barriers", 0xFF0F7FA8,
                "Visual inspection and operation of accessible pool and spa components with normal controls. "
                        + "Underground piping, leak detection and water chemistry beyond visual clarity are not "
                        + "evaluated unless listed.");
        Section s = f.section("pool.overview", "Pool & Spa", "Type, finish, sanitation");
        s.choice("pool.type", "Type", "In-ground pool", "Above-ground pool", "Pool & attached spa", "Spa only");
        s.choice("pool.finish", "Interior finish", "Plaster / marcite", "Pebble", "Quartz", "Vinyl liner",
                "Fiberglass", "Tile");
        s.num("pool.age", "Approx. age (years)");
        s.choice("pool.sanitation", "Sanitation", "Chlorine", "Salt chlorine generator", "Mineral", "Bromine");
        s.cond("pool.water", "Water clarity");
        s.cond("pool.enclosure", "Screen enclosure");

        s = f.section("pool.shell", "Shell & Deck", "Finish, tile, coping, deck");
        s.cond("pool.interior", "Interior finish");
        s.cond("pool.tile", "Waterline tile & coping");
        s.choice("pool.cracks", "Cracks in shell", YN).flag("Yes");
        s.cond("pool.deck", "Deck");
        s.choice("pool.trip", "Deck trip hazards", YN).flag("Yes");
        s.cond("pool.joint", "Expansion joint");
        s.cond("pool.skimmers", "Skimmers & returns");

        s = f.section("pool.equip", "Equipment", "Pump, filter, heater, valves");
        s.cond("pool.pump", "Pump & motor");
        s.choice("pool.filterType", "Filter type", "Cartridge", "Sand", "DE");
        s.cond("pool.filter", "Filter");
        s.cond("pool.heater", "Heater / heat pump");
        s.cond("pool.cell", "Salt cell / chlorinator");
        s.cond("pool.valves", "Valves & visible plumbing");
        s.choice("pool.leaks", "Equipment leaks", YN).flag("Yes");
        s.cond("pool.automation", "Timer / automation");
        s.cond("pool.spaJets", "Spa jets & blower");

        s = f.section("pool.elec", "Electrical & Bonding", "Bonding, lighting, GFCI, clearances");
        s.cond("pool.bonding", "Equipment bonding");
        s.cond("pool.light", "Underwater lighting");
        s.choice("pool.gfci", "GFCI protection on pool equipment and lighting", YN).flag("No");
        s.cond("pool.disconnect", "Equipment disconnect");
        s.choice("pool.overhead", "Overhead wires over pool or deck", YN).flag("Yes");

        s = f.section("pool.safety", "Safety & Barriers", "Barrier, gates, alarms, drains");
        s.cond("pool.barrier", "Barrier / fence (Ch. 515 F.S.)");
        s.cond("pool.gates", "Self-closing, self-latching gates");
        s.choice("pool.alarms", "Door and window alarms to pool area", "Yes", "No", "N/A").flag("No");
        s.choice("pool.vgb", "Anti-entrapment drain covers (VGB Act)", "Yes", "No", "Not visible").flag("No");
        s.cond("pool.ladders", "Ladders, steps & handrails");
        s.cond("pool.diving", "Diving board / slide");

        summary(f, "pool");
        return f;
    }

    // ---------------------------------------------------------------- Soil / landscaping

    private static Form soil() {
        Form f = form(SOIL, "Soil & Landscape Inspection", "Soil/Landscape",
                "Grading, soils, drainage, trees, irrigation", 0xFF5B7A2A,
                "Visual observation of site grading, drainage and vegetation at the time of inspection. Subsurface "
                        + "soil conditions are not evaluated unless testing is listed. This is not a geotechnical or "
                        + "arborist report.");
        Section s = f.section("soil.site", "Site & Grading", "Topography, grading, surface drainage");
        s.choice("soil.topo", "Topography", "Level", "Gentle slope", "Moderate slope", "Steep slope");
        s.text("soil.floodZone", "Flood zone");
        s.cond("soil.grading", "Grading away from foundation");
        s.choice("soil.negative", "Negative grade toward structure", YN).flag("Yes");
        s.cond("soil.swales", "Swales");
        s.cond("soil.surface", "Surface drainage");
        s.choice("soil.ponding", "Ponding or standing water", YN).flag("Yes");
        s.choice("soil.erosion", "Erosion", YN).flag("Yes");

        s = f.section("soil.soils", "Soil Conditions", "Soil type, settlement, sinkhole indicators");
        s.choice("soil.type", "Predominant soil", "Sand", "Clay", "Loam", "Silt", "Fill", "Organic / muck")
                .flag("Organic / muck");
        s.choice("soil.expansive", "Expansive soil indicators (shrinkage cracks)", YN).flag("Yes");
        s.choice("soil.sinkhole", "Depressions or sinkhole indicators", YN).flag("Yes");
        s.choice("soil.settlement", "Settlement of slabs, walks or driveway", YN).flag("Yes");
        s.text("soil.testing", "Soil testing performed (type, results)");

        s = f.section("soil.drain", "Drainage Systems", "French drains, extensions, basins, walls");
        s.cond("soil.french", "French / subsurface drains");
        s.cond("soil.extensions", "Downspout extensions");
        s.cond("soil.basins", "Catch basins & inlets");
        s.cond("soil.retaining", "Retaining walls");
        s.choice("soil.wallLean", "Leaning or cracked retaining wall", YN).flag("Yes");

        s = f.section("soil.land", "Trees & Vegetation", "Trees, roots, clearance, irrigation, hardscape");
        s.choice("soil.overhang", "Trees overhanging roof", YN).flag("Yes");
        s.choice("soil.hazardTree", "Dead, leaning or hazard trees", YN).flag("Yes");
        s.choice("soil.roots", "Roots near foundation or sewer line", YN).flag("Yes");
        s.cond("soil.clearance", "Vegetation clearance from structure");
        s.cond("soil.irrigation", "Irrigation system");
        s.choice("soil.spray", "Irrigation spraying structure", YN).flag("Yes");
        s.cond("soil.hardscape", "Walks, patios & hardscape");
        s.cond("soil.fence", "Fencing & gates");

        summary(f, "soil");
        return f;
    }

    // ---------------------------------------------------------------- Underground oil tank

    private static Form oilTank() {
        Form f = form(OIL_TANK, "Underground Oil Tank Inspection", "Oil Tank",
                "Tank search, tank details, soil sampling", 0xFF3D3D3D,
                "Search performed with the methods listed over accessible areas of the property. Tanks under "
                        + "structures, paving or heavy obstruction may not be detected. Soil results apply only to the "
                        + "sample locations.");
        Section s = f.section("ust.search", "Tank Search", "Methods and indicators");
        s.multi("ust.methods", "Search methods", "Visual", "Metal detector", "Magnetometer",
                "Ground penetrating radar", "Records review");
        s.choice("ust.fill", "Fill pipe found", YN).flag("Yes");
        s.choice("ust.vent", "Vent pipe found", YN).flag("Yes");
        s.choice("ust.lines", "Supply / return lines at foundation", YN).flag("Yes");
        s.choice("ust.burner", "Abandoned oil burner or equipment", YN).flag("Yes");
        s.text("ust.records", "Records reviewed (permits, closure, seller disclosure)");
        s.choice("ust.result", "Search result", "No tank indicated", "Tank located", "Inconclusive")
                .flag("Tank located", "Inconclusive");

        s = f.section("ust.tank", "Tank Details", "Location, size, status");
        s.text("ust.location", "Tank location");
        s.num("ust.capacity", "Estimated capacity (gallons)");
        s.num("ust.depth", "Depth to top of tank (in)");
        s.choice("ust.material", "Tank material", "Steel", "Fiberglass", "Unknown");
        s.choice("ust.status", "Status", "In use", "Abandoned in place (filled)", "Abandoned, not filled",
                "Removed", "Unknown").flag("Abandoned, not filled", "Unknown");
        s.text("ust.closure", "Closure documentation");
        s.choice("ust.contents", "Water or product in tank (sounding)", "Yes", "No", "Not sounded").flag("Yes");

        s = f.section("ust.sampling", "Soil Sampling", "Samples, laboratory, results");
        s.yn("ust.sampled", "Soil sampling performed");
        s.num("ust.samples", "Number of samples");
        s.text("ust.lab", "Laboratory");
        s.text("ust.tph", "TPH results (mg/kg)");
        s.choice("ust.soilResult", "Soil result", "Below action level", "Above action level", "Pending")
                .flag("Above action level");
        s.choice("ust.odor", "Petroleum odor or staining", YN).flag("Yes");

        s = f.section("ust.summary", "Summary", "Recommendation");
        s.noPhotos();
        s.choice("ust.recommend", "Recommendation", "No action", "Soil testing", "Further search with GPR",
                "Tank removal / closure").flag("Further search with GPR", "Tank removal / closure");
        s.notes("ust.summary.notes", "Summary & recommendations");
        return f;
    }

    /** "Yes" / "No" from the actual year built, or null when unknown. */
    static String builtBefore(Inspection in, int year) {
        int y = Forms.parseInt(in.str("info.yearBuilt"));
        if (y <= 0) return null;
        return y < year ? "Yes" : "No";
    }
}
