package com.inspectorsgadget.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a photo and lets the inspector draw on it. Strokes are stored in coordinates
 * normalized to the image (0..1) so they can be re-rendered at any resolution and
 * edited again later.
 */
public class DrawingView extends View {
    public static final int PEN = 0, ARROW = 1, CIRCLE = 2, BOX = 3;

    public static class Stroke {
        int tool, color;
        float width; // relative to the longest image side
        float[] pts = new float[64];
        int n; // number of floats used

        void add(float x, float y) {
            if (n + 2 > pts.length) {
                float[] p = new float[pts.length * 2];
                System.arraycopy(pts, 0, p, 0, n);
                pts = p;
            }
            pts[n++] = x;
            pts[n++] = y;
        }

        JSONObject toJson() throws Exception {
            JSONObject o = new JSONObject();
            o.put("t", tool);
            o.put("c", color);
            o.put("w", (double) width);
            JSONArray a = new JSONArray();
            for (int i = 0; i < n; i++) a.put(Math.round(pts[i] * 10000) / 10000.0);
            o.put("p", a);
            return o;
        }

        static Stroke fromJson(JSONObject o) {
            Stroke s = new Stroke();
            s.tool = o.optInt("t");
            s.color = o.optInt("c");
            s.width = (float) o.optDouble("w", 0.006);
            JSONArray a = o.optJSONArray("p");
            if (a != null) for (int i = 0; i + 1 < a.length(); i += 2)
                s.add((float) a.optDouble(i), (float) a.optDouble(i + 1));
            return s;
        }
    }

    private Bitmap image;
    private final List<Stroke> strokes = new ArrayList<>();
    private Stroke current;
    private final Matrix toView = new Matrix(), toImage = new Matrix();
    private final Paint bmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private int color = 0xFFFF1744, tool = PEN;
    private float width = 0.007f;
    private boolean halo = true;
    private Runnable onChange;

    public DrawingView(Context c) {
        super(c);
    }

    public void setImage(Bitmap b) {
        image = b;
        computeMatrix();
        invalidate();
    }

    public Bitmap getImage() { return image; }
    public void setColor(int c) { color = c; }
    public void setTool(int t) { tool = t; }
    public void setStrokeWidth(float w) { width = w; }
    public void setHalo(boolean h) { halo = h; }
    public void setOnChange(Runnable r) { onChange = r; }
    public boolean isEmpty() { return strokes.isEmpty(); }

    public void undo() {
        if (!strokes.isEmpty()) strokes.remove(strokes.size() - 1);
        invalidate();
        changed();
    }

    public void clear() {
        strokes.clear();
        invalidate();
        changed();
    }

    private void changed() { if (onChange != null) onChange.run(); }

    public JSONArray strokesJson() {
        JSONArray a = new JSONArray();
        try {
            for (Stroke s : strokes) a.put(s.toJson());
        } catch (Exception ignored) {
        }
        return a;
    }

    public void loadStrokes(JSONArray a) {
        strokes.clear();
        if (a != null) for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null) strokes.add(Stroke.fromJson(o));
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        computeMatrix();
    }

    private void computeMatrix() {
        toView.reset();
        if (image == null || getWidth() == 0) return;
        float s = Math.min((float) getWidth() / image.getWidth(), (float) getHeight() / image.getHeight());
        float dx = (getWidth() - image.getWidth() * s) / 2f, dy = (getHeight() - image.getHeight() * s) / 2f;
        toView.postScale(s, s);
        toView.postTranslate(dx, dy);
        toView.invert(toImage);
    }

    @Override
    protected void onDraw(Canvas c) {
        if (image == null) return;
        c.drawBitmap(image, toView, bmpPaint);
        c.save();
        c.concat(toView);
        render(c, strokes, image.getWidth(), image.getHeight(), halo);
        if (current != null) {
            List<Stroke> one = new ArrayList<>();
            one.add(current);
            render(c, one, image.getWidth(), image.getHeight(), halo);
        }
        c.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (image == null) return false;
        float[] p = {e.getX(), e.getY()};
        toImage.mapPoints(p);
        float nx = clamp(p[0] / image.getWidth()), ny = clamp(p[1] / image.getHeight());
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                current = new Stroke();
                current.tool = tool;
                current.color = color;
                current.width = width;
                current.add(nx, ny);
                current.add(nx, ny);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (current == null) return false;
                if (current.tool == PEN) {
                    for (int h = 0; h < e.getHistorySize(); h++) {
                        float[] hp = {e.getHistoricalX(h), e.getHistoricalY(h)};
                        toImage.mapPoints(hp);
                        current.add(clamp(hp[0] / image.getWidth()), clamp(hp[1] / image.getHeight()));
                    }
                    current.add(nx, ny);
                } else {
                    current.pts[2] = nx;
                    current.pts[3] = ny;
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (current != null) {
                    strokes.add(current);
                    current = null;
                    changed();
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                current = null;
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    /** Draws strokes on a canvas whose coordinates are image pixels. */
    public static void render(Canvas c, List<Stroke> strokes, int w, int h, boolean halo) {
        float longest = Math.max(w, h);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        for (Stroke s : strokes) {
            Path path = pathFor(s, w, h, s.width * longest);
            float sw = s.width * longest;
            if (halo) {
                // Dark outline keeps bright colors readable on any background.
                p.setColor(0x99000000);
                p.setStrokeWidth(sw * 1.7f);
                c.drawPath(path, p);
            }
            p.setColor(s.color);
            p.setStrokeWidth(sw);
            c.drawPath(path, p);
        }
    }

    public static void renderJson(Canvas c, JSONArray a, int w, int h, boolean halo) {
        List<Stroke> l = new ArrayList<>();
        if (a != null) for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null) l.add(Stroke.fromJson(o));
        }
        render(c, l, w, h, halo);
    }

    private static Path pathFor(Stroke s, int w, int h, float sw) {
        Path path = new Path();
        if (s.n < 4) return path;
        float x0 = s.pts[0] * w, y0 = s.pts[1] * h;
        float x1 = s.pts[2] * w, y1 = s.pts[3] * h;
        switch (s.tool) {
            case ARROW: {
                path.moveTo(x0, y0);
                path.lineTo(x1, y1);
                double ang = Math.atan2(y1 - y0, x1 - x0);
                float len = Math.max(sw * 4f, (float) Math.hypot(x1 - x0, y1 - y0) * 0.18f);
                len = Math.min(len, sw * 9f);
                for (int sign = -1; sign <= 1; sign += 2) {
                    double a = ang + Math.PI - sign * Math.toRadians(28);
                    path.moveTo(x1, y1);
                    path.lineTo(x1 + (float) Math.cos(a) * len, y1 + (float) Math.sin(a) * len);
                }
                break;
            }
            case CIRCLE:
                path.addOval(new RectF(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1), Math.max(y0, y1)),
                        Path.Direction.CW);
                break;
            case BOX:
                path.addRect(new RectF(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1), Math.max(y0, y1)),
                        Path.Direction.CW);
                break;
            default:
                path.moveTo(x0, y0);
                for (int i = 2; i + 1 < s.n; i += 2) {
                    float px = s.pts[i - 2] * w, py = s.pts[i - 1] * h;
                    float cx = s.pts[i] * w, cy = s.pts[i + 1] * h;
                    path.quadTo(px, py, (px + cx) / 2f, (py + cy) / 2f);
                }
                path.lineTo(s.pts[s.n - 2] * w, s.pts[s.n - 1] * h);
        }
        return path;
    }
}
