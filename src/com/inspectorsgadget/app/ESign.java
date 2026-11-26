package com.inspectorsgadget.app;

import android.content.Context;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Provider-agnostic remote e-signature.
 *
 * The app only knows {@link Service}. {@link RelayService} talks to the backend, which forwards the
 * request to BoldSign or DocuSign (chosen on the server, see backend/src/esign/). {@link DemoService}
 * simulates the round trip on the device when no backend is configured.
 *
 * Inspection keys: pa.esign.id, pa.esign.provider, pa.esign.state, pa.esign.checked.
 */
public final class ESign {
    private ESign() {}

    /** Lifecycle of a signature request, the same for every provider. */
    public static final String SENT = "sent", VIEWED = "viewed", COMPLETED = "completed", DECLINED = "declined",
            EXPIRED = "expired", VOIDED = "voided";

    /** A place on the PDF for the client to fill in. PDF points, origin top-left, pages from 1. */
    public static final class Field {
        public static final String SIGNATURE = "signature", INITIALS = "initials", DATE = "date";
        public final String type;
        public final int page;
        public final float x, y, width, height;

        public Field(String type, int page, float x, float y, float width, float height) {
            this.type = type;
            this.page = page;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        JSONObject toJson() throws Exception {
            return new JSONObject().put("type", type).put("page", page).put("x", round(x)).put("y", round(y))
                    .put("width", round(width)).put("height", round(height));
        }

        private static double round(float v) { return Math.round(v * 10) / 10.0; }
    }

    public static final class Request {
        public String title, message, signerName, signerEmail, reference;
        public File pdf;
        public List<Field> fields = new ArrayList<>();

        JSONObject toJson() throws Exception {
            JSONArray f = new JSONArray();
            for (Field x : fields) f.put(x.toJson());
            return new JSONObject()
                    .put("title", title)
                    .put("message", message)
                    .put("reference", reference)
                    .put("signer", new JSONObject().put("name", signerName).put("email", signerEmail))
                    .put("fields", f)
                    .put("pdfBase64", Base64.encodeToString(readBytes(pdf), Base64.NO_WRAP));
        }
    }

    public static final class Envelope {
        public final String id, provider, state;

        public Envelope(String id, String provider, String state) {
            this.id = id;
            this.provider = provider;
            this.state = state;
        }

        static Envelope fromJson(JSONObject o) {
            return new Envelope(o.optString("id"), o.optString("provider"), o.optString("state", SENT));
        }
    }

    public interface Service {
        Envelope send(Request r) throws Exception;

        Envelope status(String id) throws Exception;

        /** Saves the completed, provider-sealed PDF (with its audit certificate) to {@code out}. */
        void download(String id, File out) throws Exception;
    }

    public static Service service(Context c) {
        return Config.relayConfigured() ? new RelayService(new RelayClient(c)) : new DemoService(c);
    }

    // ------------------------------------------------------------ workflow

    public static boolean pending(Inspection in) {
        String st = in.str("pa.esign.state");
        return in.str("pa.esign.id").length() > 0 && (SENT.equals(st) || VIEWED.equals(st));
    }

    public static String state(Inspection in) { return in.str("pa.esign.state"); }

    /** Sends the agreement for signature and records it on the inspection. Blocking. */
    public static Envelope sendAgreement(Context c, Inspection in, File pdf, List<Field> fields) throws Exception {
        Inspection co = Company.profile(c);
        Request r = new Request();
        r.title = "Inspection Agreement - " + in.title();
        r.message = "Please review and sign the inspection agreement for " + in.title()
                + (in.str("info.date").length() > 0 ? ", scheduled for " + in.str("info.date") : "") + ".\n\n"
                + Forms.joinNonEmpty(in.str("info.inspector"), co.str("co.name")) + "\n" + co.str("co.phone");
        r.signerName = in.str("info.client");
        r.signerEmail = in.str("info.email");
        r.reference = in.id;
        r.pdf = pdf;
        r.fields = fields;
        Envelope e = service(c).send(r);
        in.setUser("pa.esign.id", e.id);
        in.setUser("pa.esign.provider", e.provider);
        in.setUser("pa.esign.state", e.state);
        in.setUser("pa.sentDate", Agreement.today());
        if (Agreement.status(in) != Agreement.SIGNED) in.setUser("pa.signed", "Not signed yet");
        Store.save(in);
        return e;
    }

    /**
     * Checks a pending request. When the client has signed, downloads the signed PDF into the
     * inspection and marks the agreement signed. Returns true when anything changed. Blocking.
     */
    public static boolean refresh(Context c, Inspection in) throws Exception {
        if (!pending(in)) return false;
        Service s = service(c);
        String id = in.str("pa.esign.id");
        Envelope e = s.status(id);
        boolean changed = !e.state.equals(in.str("pa.esign.state"));
        if (COMPLETED.equals(e.state)) {
            String name = "signed_agreement.pdf";
            s.download(id, new File(in.dir, name));
            in.setUser("pa.signedCopy", name);
            in.setUser("pa.signed", "Signed");
            in.setUser("pa.signedHow", Forms.SIGNED_HOW[2]);
            in.setUser("pa.signedDate", Agreement.today());
            changed = true;
        }
        in.setUser("pa.esign.state", e.state);
        Store.save(in);
        return changed;
    }

    /** Short status for the agreement card when an e-signature request exists. */
    public static String statusText(Inspection in) {
        String st = state(in);
        if (DECLINED.equals(st)) return "E-signature declined by client";
        if (EXPIRED.equals(st)) return "E-signature request expired";
        if (VOIDED.equals(st)) return "E-signature request canceled";
        if (VIEWED.equals(st)) return "E-signature sent " + in.str("pa.sentDate") + ", opened by client";
        return "E-signature sent " + in.str("pa.sentDate") + ", waiting for signature";
    }

    // ------------------------------------------------------------ relay

    static final class RelayService implements Service {
        private final RelayClient relay;

        RelayService(RelayClient relay) { this.relay = relay; }

        @Override
        public Envelope send(Request r) throws Exception {
            return Envelope.fromJson(relay.post("/v1/envelopes", r.toJson()));
        }

        @Override
        public Envelope status(String id) throws Exception {
            return Envelope.fromJson(relay.get("/v1/envelopes/" + id));
        }

        @Override
        public void download(String id, File out) throws Exception {
            relay.download("/v1/envelopes/" + id + "/document", out);
        }
    }

    // ------------------------------------------------------------ demo

    /** Simulated provider: the client "signs" {@link Config#DEMO_SIGN_DELAY_SECONDS} after sending. */
    static final class DemoService implements Service {
        private final File dir;

        DemoService(Context c) {
            dir = new File(c.getApplicationContext().getFilesDir(), "esign-demo");
            dir.mkdirs();
        }

        @Override
        public Envelope send(Request r) throws Exception {
            if (r.signerEmail == null || !r.signerEmail.contains("@")) throw new Exception("No client email on file");
            String id = "demo-" + UUID.randomUUID().toString().substring(0, 8);
            copy(r.pdf, new File(dir, id + ".pdf"));
            JSONObject rec = new JSONObject().put("sentAt", System.currentTimeMillis()).put("request", stripPdf(r));
            write(new File(dir, id + ".json"), rec.toString().getBytes("UTF-8"));
            return new Envelope(id, "demo", SENT);
        }

        @Override
        public Envelope status(String id) throws Exception {
            File f = new File(dir, id + ".json");
            if (!f.exists()) return new Envelope(id, "demo", VOIDED);
            long sent = new JSONObject(new String(readBytes(f), "UTF-8")).getLong("sentAt");
            long age = System.currentTimeMillis() - sent;
            String st = age >= Config.DEMO_SIGN_DELAY_SECONDS * 1000L ? COMPLETED
                    : age >= Config.DEMO_SIGN_DELAY_SECONDS * 500L ? VIEWED : SENT;
            return new Envelope(id, "demo", st);
        }

        @Override
        public void download(String id, File out) throws Exception {
            copy(new File(dir, id + ".pdf"), out);
        }

        private static JSONObject stripPdf(Request r) throws Exception {
            JSONObject o = new JSONObject().put("title", r.title).put("signer", r.signerEmail);
            JSONArray f = new JSONArray();
            for (Field x : r.fields) f.put(x.toJson());
            return o.put("fields", f);
        }
    }

    // ------------------------------------------------------------ files

    static byte[] readBytes(File f) throws Exception {
        InputStream is = new FileInputStream(f);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
        is.close();
        return bo.toByteArray();
    }

    static void write(File f, byte[] b) throws Exception {
        OutputStream os = new FileOutputStream(f);
        os.write(b);
        os.close();
    }

    static void copy(File from, File to) throws Exception { write(to, readBytes(from)); }

}
