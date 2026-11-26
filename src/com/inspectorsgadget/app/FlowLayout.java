package com.inspectorsgadget.app;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Lays children out left to right, wrapping to new rows. */
public class FlowLayout extends ViewGroup {
    private final int gap;

    public FlowLayout(Context c) {
        super(c);
        gap = Ui.dp(c, 8);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int maxW = MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight();
        if (MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED) maxW = Integer.MAX_VALUE / 2;
        int x = 0, y = 0, rowH = 0, usedW = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View ch = getChildAt(i);
            if (ch.getVisibility() == GONE) continue;
            LayoutParams lp = ch.getLayoutParams();
            int cw = lp != null && lp.width == LayoutParams.MATCH_PARENT
                    ? MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.EXACTLY)
                    : MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.AT_MOST);
            int chh = lp != null && lp.height > 0
                    ? MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY)
                    : MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            if (lp != null && lp.width > 0) cw = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            ch.measure(cw, chh);
            int w = ch.getMeasuredWidth(), h = ch.getMeasuredHeight();
            if (x > 0 && x + w > maxW) {
                x = 0;
                y += rowH + gap;
                rowH = 0;
            }
            x += w + gap;
            usedW = Math.max(usedW, x - gap);
            rowH = Math.max(rowH, h);
        }
        int totalH = y + rowH + getPaddingTop() + getPaddingBottom();
        int width = MeasureSpec.getMode(widthSpec) == MeasureSpec.EXACTLY
                ? MeasureSpec.getSize(widthSpec) : usedW + getPaddingLeft() + getPaddingRight();
        setMeasuredDimension(width, resolveSize(totalH, heightSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int maxW = r - l - getPaddingLeft() - getPaddingRight();
        int x = 0, y = 0, rowH = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View ch = getChildAt(i);
            if (ch.getVisibility() == GONE) continue;
            int w = ch.getMeasuredWidth(), h = ch.getMeasuredHeight();
            if (x > 0 && x + w > maxW) {
                x = 0;
                y += rowH + gap;
                rowH = 0;
            }
            ch.layout(getPaddingLeft() + x, getPaddingTop() + y,
                    getPaddingLeft() + x + w, getPaddingTop() + y + h);
            x += w + gap;
            rowH = Math.max(rowH, h);
        }
    }
}
