package com.inspectorsgadget.app;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;

/**
 * Shrinks PDFs written by android.graphics.pdf.PdfDocument. That writer stores photos as
 * lossless Flate-compressed RGB, which makes reports with photos too large to email. This
 * rewrites each such image as a JPEG (DCTDecode), optionally downscaled, and rebuilds the
 * cross-reference table. Text and vector content are left untouched. If anything about the
 * file is unexpected, the original bytes are returned unchanged.
 */
public final class PdfCompressor {
    private PdfCompressor() {}

    public static class Result {
        public final byte[] data;
        public final int imagesCompressed;

        Result(byte[] d, int n) {
            data = d;
            imagesCompressed = n;
        }
    }

    private static final Pattern OBJ = Pattern.compile("(\\d+) (\\d+) obj\\b");

    public static Result compress(byte[] pdf, int maxDim, int quality) {
        try {
            return compressOrThrow(pdf, maxDim, quality);
        } catch (Throwable t) {
            android.util.Log.w("InspectorsGadget", "PDF compression skipped", t);
            return new Result(pdf, 0);
        }
    }

    public static int compressFile(File f, int maxDim, int quality) throws Exception {
        byte[] in = new byte[(int) f.length()];
        java.io.DataInputStream is = new java.io.DataInputStream(new java.io.FileInputStream(f));
        is.readFully(in);
        is.close();
        Result r = compress(in, maxDim, quality);
        if (r.imagesCompressed > 0 && r.data.length < in.length) {
            FileOutputStream os = new FileOutputStream(f);
            os.write(r.data);
            os.close();
        }
        return r.imagesCompressed;
    }

    static Result compressOrThrow(byte[] pdf, int maxDim, int quality) throws Exception {
        // ISO-8859-1 maps bytes 1:1 to chars so offsets match.
        String s = new String(pdf, StandardCharsets.ISO_8859_1);
        int trailerAt = s.lastIndexOf("trailer");
        int xrefAt = s.lastIndexOf("\nxref");
        if (trailerAt < 0 || xrefAt < 0) return new Result(pdf, 0);
        int trailerEnd = s.indexOf("startxref", trailerAt);
        String trailer = s.substring(trailerAt, trailerEnd);

        ByteArrayOutputStream out = new ByteArrayOutputStream(pdf.length);
        int firstObj = -1;
        Matcher m = OBJ.matcher(s);
        List<long[]> offsets = new ArrayList<>(); // {objNum, offset}
        int pos = 0, count = 0, maxNum = 0;
        while (m.find(pos)) {
            if (m.start() > xrefAt) break;
            // must be at the start of a line
            if (m.start() > 0 && s.charAt(m.start() - 1) != '\n' && s.charAt(m.start() - 1) != '\r') {
                pos = m.end();
                continue;
            }
            if (firstObj < 0) {
                firstObj = m.start();
                out.write(pdf, 0, firstObj); // header
            }
            int num = Integer.parseInt(m.group(1));
            maxNum = Math.max(maxNum, num);
            int end = findEndObj(s, m.end());
            if (end < 0) return new Result(pdf, 0);
            int objEnd = end + "endobj".length();
            offsets.add(new long[]{num, out.size()});
            byte[] replaced = maybeCompressImage(s, pdf, m.start(), objEnd, maxDim, quality);
            if (replaced != null) {
                out.write(replaced);
                count++;
            } else {
                out.write(pdf, m.start(), objEnd - m.start());
            }
            out.write('\n');
            pos = objEnd;
        }
        if (firstObj < 0 || count == 0) return new Result(pdf, 0);

        long[] table = new long[maxNum + 1];
        for (long[] o : offsets) table[(int) o[0]] = o[1];
        long xrefPos = out.size();
        StringBuilder x = new StringBuilder();
        x.append("xref\n0 ").append(maxNum + 1).append('\n');
        x.append("0000000000 65535 f \n");
        for (int i = 1; i <= maxNum; i++) {
            if (table[i] == 0) x.append("0000000000 65535 f \n");
            else x.append(String.format(java.util.Locale.US, "%010d 00000 n \n", table[i]));
        }
        x.append(trailer.trim()).append('\n');
        x.append("startxref\n").append(xrefPos).append("\n%%EOF\n");
        out.write(x.toString().getBytes(StandardCharsets.ISO_8859_1));
        return new Result(out.toByteArray(), count);
    }

    /** Finds the "endobj" that closes an object, skipping over stream data. */
    private static int findEndObj(String s, int from) {
        int stream = s.indexOf("stream", from);
        int endobj = s.indexOf("endobj", from);
        if (stream >= 0 && stream < endobj) {
            int endstream = s.indexOf("endstream", stream + 6);
            if (endstream < 0) return -1;
            return s.indexOf("endobj", endstream);
        }
        return endobj;
    }

    private static final Pattern W = Pattern.compile("/Width\\s+(\\d+)");
    private static final Pattern H = Pattern.compile("/Height\\s+(\\d+)");
    private static final Pattern LEN = Pattern.compile("/Length\\s+(\\d+)(\\s+\\d+\\s+R)?");

    private static byte[] maybeCompressImage(String s, byte[] pdf, int start, int end, int maxDim, int quality)
            throws Exception {
        int dictStart = s.indexOf("<<", start);
        int streamKw = s.indexOf("stream", start);
        if (dictStart < 0 || streamKw < 0 || streamKw > end) return null;
        String dict = s.substring(dictStart, streamKw);
        if (!dict.contains("/Subtype /Image") && !dict.contains("/Subtype/Image")) return null;
        if (!dict.contains("/FlateDecode")) return null;
        // 3-channel color only (DeviceRGB, or the sRGB ICC profile newer Skia writes)
        if (!dict.contains("/DeviceRGB") && !dict.contains("/ICCBased")) return null;
        if (dict.contains("/SMask") || dict.contains("/DecodeParms") || dict.contains("/Decode ")) return null;
        if (!dict.matches("(?s).*/BitsPerComponent\\s+8\\b.*")) return null;
        Matcher wm = W.matcher(dict), hm = H.matcher(dict), lm = LEN.matcher(dict);
        if (!wm.find() || !hm.find() || !lm.find() || lm.group(2) != null) return null;
        int w = Integer.parseInt(wm.group(1)), h = Integer.parseInt(hm.group(1));
        int len = Integer.parseInt(lm.group(1));
        int dataStart = streamKw + "stream".length();
        if (s.charAt(dataStart) == '\r') dataStart++;
        if (s.charAt(dataStart) == '\n') dataStart++;
        if (dataStart + len > end) return null;
        if ((long) w * h > 40_000_000L || w < 8 || h < 8) return null;

        Inflater inf = new Inflater();
        inf.setInput(pdf, dataStart, len);
        byte[] raw = new byte[w * h * 3];
        int got = 0;
        while (got < raw.length && !inf.finished()) {
            int n = inf.inflate(raw, got, raw.length - got);
            if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break;
            got += n;
        }
        boolean exact = got == raw.length && (inf.finished() || inf.getRemaining() == 0);
        if (exact && !inf.finished()) {
            byte[] probe = new byte[1];
            exact = inf.inflate(probe) == 0;
        }
        inf.end();
        if (!exact) return null;

        int[] px = new int[w * h];
        for (int i = 0, j = 0; i < px.length; i++, j += 3) {
            px[i] = 0xFF000000 | ((raw[j] & 0xFF) << 16) | ((raw[j + 1] & 0xFF) << 8) | (raw[j + 2] & 0xFF);
        }
        Bitmap b = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888);
        float scale = Math.min(1f, (float) maxDim / Math.max(w, h));
        if (scale < 0.999f) {
            Bitmap sb = Bitmap.createScaledBitmap(b, Math.max(1, Math.round(w * scale)),
                    Math.max(1, Math.round(h * scale)), true);
            b.recycle();
            b = sb;
        }
        ByteArrayOutputStream jpg = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, quality, jpg);
        int nw = b.getWidth(), nh = b.getHeight();
        b.recycle();
        byte[] jpeg = jpg.toByteArray();
        if (jpeg.length >= len) return null; // not worth it

        String header = s.substring(start, dictStart);
        String newDict = dict.trim();
        newDict = W.matcher(newDict).replaceFirst("/Width " + nw);
        newDict = H.matcher(newDict).replaceFirst("/Height " + nh);
        newDict = LEN.matcher(newDict).replaceFirst("/Length " + jpeg.length);
        newDict = newDict.replace("/FlateDecode", "/DCTDecode");
        ByteArrayOutputStream o = new ByteArrayOutputStream(jpeg.length + 300);
        o.write((header + newDict + "\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
        o.write(jpeg);
        o.write("\nendstream\nendobj".getBytes(StandardCharsets.ISO_8859_1));
        return o.toByteArray();
    }
}
