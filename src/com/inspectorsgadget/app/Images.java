package com.inspectorsgadget.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bitmap decoding (respecting camera EXIF rotation) and a small thumbnail cache. */
public final class Images {
    private Images() {}

    private static final ExecutorService pool = Executors.newFixedThreadPool(2);
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> thumbs = new LruCache<String, Bitmap>(24 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
    };

    /** Decodes so the longest side is at most maxDim, rotated upright. */
    public static Bitmap decode(File f, int maxDim) {
        if (f == null || !f.exists()) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        if (o.outWidth <= 0 || o.outHeight <= 0) return null;
        int sample = 1;
        while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= maxDim) sample *= 2;
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = sample;
        Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath(), o2);
        if (b == null) return null;
        int rot = rotation(f);
        float scale = Math.min(1f, (float) maxDim / Math.max(b.getWidth(), b.getHeight()));
        if (rot != 0 || scale < 0.999f) {
            Matrix m = new Matrix();
            m.postScale(scale, scale);
            m.postRotate(rot);
            Bitmap r = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            if (r != b) b.recycle();
            b = r;
        }
        return b;
    }

    static int rotation(File f) {
        try {
            ExifInterface e = new ExifInterface(f.getAbsolutePath());
            switch (e.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (Exception ex) {
            return 0;
        }
    }

    public static void writeJpeg(Bitmap b, File out, int quality) throws Exception {
        File tmp = new File(out.getPath() + ".tmp");
        FileOutputStream os = new FileOutputStream(tmp);
        b.compress(Bitmap.CompressFormat.JPEG, quality, os);
        os.close();
        if (!tmp.renameTo(out)) throw new RuntimeException("rename failed");
    }

    public static void writePng(Bitmap b, File out) throws Exception {
        FileOutputStream os = new FileOutputStream(out);
        b.compress(Bitmap.CompressFormat.PNG, 100, os);
        os.close();
    }

    /** Loads a thumbnail into an ImageView off the main thread. */
    public static void loadThumb(final ImageView iv, final File f, final int size) {
        final String key = f.getAbsolutePath() + "@" + f.lastModified() + "#" + size;
        iv.setTag(key);
        Bitmap cached = thumbs.get(key);
        if (cached != null) {
            iv.setImageBitmap(cached);
            return;
        }
        iv.setImageDrawable(null);
        pool.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap b = decode(f, size);
                if (b == null) return;
                thumbs.put(key, b);
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        if (key.equals(iv.getTag())) iv.setImageBitmap(b);
                    }
                });
            }
        });
    }
}
