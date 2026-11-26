package com.inspectorsgadget.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small helpers for building the UI in code (no XML layouts). */
public final class Ui {
    private Ui() {}

    public static final int PRIMARY = 0xFF0E1C2E;
    public static final int PRIMARY_LIGHT = 0xFFE4E9F0;
    public static final int ACCENT = 0xFFC9A04A;
    public static final int SILVER = 0xFFC0C7D0;
    public static final int LOGO_BG = 0xFF050E18;
    public static final int TEAL = 0xFF0E8C7F;
    public static final int TEAL_LIGHT = 0xFFDDF3F0;
    public static final int RED = 0xFFC62828;
    public static final int RED_LIGHT = 0xFFFDE7E7;
    public static final int AMBER = 0xFFB26A00;
    public static final int TEXT = 0xFF1B1F24;
    public static final int MUTED = 0xFF5F6B7A;
    public static final int BORDER = 0xFFD5DCE5;
    public static final int BG = 0xFFF3F5F8;

    // Module colors
    public static final int GENERAL = 0xFF0E1C2E;
    public static final int FOUR_POINT = 0xFF0E8C7F;
    public static final int WIND = 0xFF6A3FB5;

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    public static GradientDrawable rounded(Context c, int fill, int stroke, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) g.setStroke(dp(c, 1), stroke);
        return g;
    }

    public static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = vbox(c);
        l.setBackground(rounded(c, Color.WHITE, BORDER, 12));
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        l.setElevation(dp(c, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(c, 12));
        l.setLayoutParams(lp);
        return l;
    }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    public static TextView title(Context c, String s) { return text(c, s, 20, TEXT, true); }

    public static TextView body(Context c, String s) { return text(c, s, 15, TEXT, false); }

    public static TextView muted(Context c, String s) { return text(c, s, 13, MUTED, false); }

    public static Button button(Context c, String label, int color, boolean filled) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(filled ? Color.WHITE : color);
        GradientDrawable shape = rounded(c, filled ? color : Color.WHITE, filled ? 0 : color, 10);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000), shape, null));
        b.setStateListAnimator(null);
        int ph = dp(c, 14);
        b.setPadding(ph, 0, ph, 0);
        b.setMinHeight(dp(c, 46));
        b.setMinimumHeight(dp(c, 46));
        return b;
    }

    public static LinearLayout.LayoutParams matchWrap(Context c, int topMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, topMarginDp);
        return lp;
    }

    public static LinearLayout.LayoutParams weight(Context c, float w, int sideMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, w);
        lp.leftMargin = dp(c, sideMarginDp);
        lp.rightMargin = dp(c, sideMarginDp);
        return lp;
    }

    public static TextView pill(Context c, String s, int fg, int bg) {
        TextView t = text(c, s, 12, fg, true);
        t.setBackground(rounded(c, bg, 0, 20));
        t.setPadding(dp(c, 8), dp(c, 2), dp(c, 8), dp(c, 2));
        return t;
    }

    public static View spacer(Context c, int hDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, hDp)));
        return v;
    }

    /** A toggleable chip used for single and multi choice fields. */
    public static TextView chip(Context c, String s) {
        TextView t = text(c, s, 14, TEXT, false);
        t.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
        t.setMinHeight(dp(c, 40));
        t.setGravity(Gravity.CENTER_VERTICAL);
        styleChip(c, t, false, PRIMARY);
        return t;
    }

    public static void styleChip(Context c, TextView t, boolean on, int color) {
        if (on) {
            t.setBackground(rounded(c, color, 0, 20));
            t.setTextColor(Color.WHITE);
        } else {
            t.setBackground(rounded(c, Color.WHITE, BORDER, 20));
            t.setTextColor(TEXT);
        }
    }
}
