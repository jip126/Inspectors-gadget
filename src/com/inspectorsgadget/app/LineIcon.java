package com.inspectorsgadget.app;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Outline icons for the login screen, drawn on a 24 by 24 grid. */
public class LineIcon extends Drawable {
    public static final int MAIL = 0, LOCK = 1, EYE = 2, EYE_OFF = 3, PERSON = 4, FINGERPRINT = 5;

    private final int type;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int sizePx;

    public LineIcon(int type, int color, int sizePx, float strokePx) {
        this.type = type;
        this.sizePx = sizePx;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(color);
        paint.setStrokeWidth(strokePx);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(color);
    }

    @Override
    public int getIntrinsicWidth() { return sizePx; }

    @Override
    public int getIntrinsicHeight() { return sizePx; }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        float s = Math.min(b.width(), b.height()) / 24f;
        c.save();
        c.translate(b.left + (b.width() - 24 * s) / 2f, b.top + (b.height() - 24 * s) / 2f);
        c.scale(s, s);
        float sw = paint.getStrokeWidth();
        paint.setStrokeWidth(sw / s);
        Path p = new Path();
        switch (type) {
            case MAIL:
                c.drawRoundRect(new RectF(2.5f, 5f, 21.5f, 19f), 1.8f, 1.8f, paint);
                p.moveTo(3.2f, 6.2f);
                p.lineTo(12f, 13f);
                p.lineTo(20.8f, 6.2f);
                c.drawPath(p, paint);
                break;
            case LOCK:
                c.drawRoundRect(new RectF(5f, 10.5f, 19f, 21f), 2f, 2f, paint);
                p.moveTo(8f, 10.5f);
                p.lineTo(8f, 7.5f);
                p.arcTo(new RectF(8f, 3.5f, 16f, 11.5f), 180, 180, false);
                p.lineTo(16f, 10.5f);
                c.drawPath(p, paint);
                c.drawCircle(12f, 15f, 1.3f, fill);
                c.drawLine(12f, 15f, 12f, 17.6f, paint);
                break;
            case EYE:
            case EYE_OFF:
                p.moveTo(2f, 12f);
                p.quadTo(12f, 2.5f, 22f, 12f);
                p.quadTo(12f, 21.5f, 2f, 12f);
                p.close();
                c.drawPath(p, paint);
                c.drawCircle(12f, 12f, 3.2f, paint);
                if (type == EYE_OFF) c.drawLine(4f, 4f, 20f, 20f, paint);
                break;
            case PERSON:
                c.drawCircle(12f, 8f, 4f, paint);
                c.drawArc(new RectF(4f, 14f, 20f, 30f), 180, 180, false, paint);
                break;
            case FINGERPRINT:
                RectF o = new RectF();
                float cx = 12f, cy = 12.5f;
                float[][] arcs = {{4.6f, 150, 300}, {7.4f, 195, 230}, {10.2f, 215, 110}};
                for (float[] a : arcs) {
                    o.set(cx - a[0], cy - a[0] * 1.08f, cx + a[0], cy + a[0] * 1.08f);
                    c.drawArc(o, a[1], a[2], false, paint);
                }
                c.drawLine(cx, cy - 1.8f, cx, cy + 4.5f, paint);
                o.set(cx - 2f, cy - 2f, cx + 2f, cy + 2f);
                c.drawArc(o, 180, 180, false, paint);
                break;
            default:
                break;
        }
        paint.setStrokeWidth(sw);
        c.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        fill.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
        fill.setColorFilter(cf);
    }

    @Override
    public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
