package com.inspectorsgadget.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Photo markup screen. Also used to capture signatures (mode = signature). */
public class AnnotateActivity extends Activity {
    public static final String EXTRA_INSPECTION = "insp", EXTRA_PHOTO = "photo", EXTRA_SIGNATURE = "sigKey";
    private static final int REQ_VOICE = 41;

    /** High-contrast colors that stand out on most backgrounds. */
    static final int[] COLORS = {0xFFFF1744, 0xFFFFEA00, 0xFF00E676, 0xFF00E5FF, 0xFFFFFFFF};
    static final String[] COLOR_NAMES = {"Red", "Yellow", "Green", "Cyan", "White"};
    static final int[] SIG_COLORS = {0xFF111111, 0xFF0D47A1};

    private Inspection insp;
    private String photoId, sigKey;
    private DrawingView canvas;
    private EditText caption;
    private final List<TextView> toolButtons = new ArrayList<>();
    private final List<View> swatches = new ArrayList<>();
    private TextView sizeButton;
    private boolean thick;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        insp = Store.get(this, getIntent().getStringExtra(EXTRA_INSPECTION));
        photoId = getIntent().getStringExtra(EXTRA_PHOTO);
        sigKey = getIntent().getStringExtra(EXTRA_SIGNATURE);
        if (insp == null) {
            finish();
            return;
        }
        boolean sig = sigKey != null;
        setTitle(sig ? "Sign" : "Mark up photo");

        LinearLayout root = Ui.vbox(this);
        root.setBackgroundColor(0xFF121212);

        canvas = new DrawingView(this);
        if (sig) {
            Bitmap white = Bitmap.createBitmap(1200, 450, Bitmap.Config.ARGB_8888);
            white.eraseColor(Color.WHITE);
            canvas.setImage(white);
            canvas.setHalo(false);
            canvas.setColor(SIG_COLORS[0]);
            canvas.setStrokeWidth(0.006f);
        } else {
            Bitmap img = Images.decode(insp.originalFile(photoId), 2048);
            if (img == null) {
                Toast.makeText(this, "Photo could not be opened", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            canvas.setImage(img);
            canvas.loadStrokes(insp.meta(photoId).optJSONArray("strokes"));
        }
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        if (sig) {
            clp.setMargins(Ui.dp(this, 12), Ui.dp(this, 24), Ui.dp(this, 12), Ui.dp(this, 24));
        }
        root.addView(canvas, clp);

        LinearLayout panel = Ui.vbox(this);
        int p = Ui.dp(this, 10);
        panel.setPadding(p, p, p, p);
        panel.setBackgroundColor(0xFF1E1E1E);
        root.addView(panel);

        if (!sig) {
            LinearLayout capRow = Ui.hbox(this);
            caption = new EditText(this);
            caption.setHint("Caption, e.g. \"Double tap at breaker 14\"");
            caption.setTextColor(Color.WHITE);
            caption.setHintTextColor(0xFF9E9E9E);
            caption.setText(insp.caption(photoId));
            caption.setSingleLine(false);
            caption.setMaxLines(3);
            capRow.addView(caption, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Button mic = Ui.button(this, "🎤", Ui.ACCENT, true);
            mic.setContentDescription("Dictate caption");
            mic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { dictate(); }
            });
            capRow.addView(mic, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 46)));
            panel.addView(capRow);

            LinearLayout tools = Ui.hbox(this);
            tools.setPadding(0, Ui.dp(this, 8), 0, 0);
            String[] names = {"✏ Pen", "↗ Arrow", "◯ Circle", "▭ Box"};
            for (int i = 0; i < names.length; i++) {
                final int t = i;
                TextView tb = darkChip(names[i]);
                tb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { selectTool(t); }
                });
                toolButtons.add(tb);
                tools.addView(tb, Ui.weight(this, 1, 3));
            }
            panel.addView(tools);
        }

        LinearLayout colors = Ui.hbox(this);
        colors.setPadding(0, Ui.dp(this, 10), 0, 0);
        final int[] palette = sig ? SIG_COLORS : COLORS;
        for (int i = 0; i < palette.length; i++) {
            final int idx = i;
            View sw = new View(this);
            sw.setContentDescription(sig ? (i == 0 ? "Black" : "Blue") : COLOR_NAMES[i]);
            sw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { selectColor(idx, palette); }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40));
            lp.rightMargin = Ui.dp(this, 10);
            colors.addView(sw, lp);
            swatches.add(sw);
        }
        if (!sig) {
            sizeButton = darkChip("Thin");
            sizeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    thick = !thick;
                    sizeButton.setText(thick ? "Thick" : "Thin");
                    canvas.setStrokeWidth(thick ? 0.013f : 0.007f);
                }
            });
            colors.addView(sizeButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 40)));
        }
        panel.addView(colors);

        LinearLayout actions = Ui.hbox(this);
        actions.setPadding(0, Ui.dp(this, 10), 0, 0);
        Button undo = Ui.button(this, "Undo", 0xFFBDBDBD, false);
        undo.setBackground(Ui.rounded(this, 0xFF2C2C2C, 0xFF555555, 10));
        undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { canvas.undo(); }
        });
        Button clear = Ui.button(this, "Clear", 0xFFBDBDBD, false);
        clear.setBackground(Ui.rounded(this, 0xFF2C2C2C, 0xFF555555, 10));
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { canvas.clear(); }
        });
        Button save = Ui.button(this, sig ? "Save signature" : "Save", Ui.ACCENT, true);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { save(); }
        });
        actions.addView(undo, Ui.weight(this, 1, 3));
        actions.addView(clear, Ui.weight(this, 1, 3));
        actions.addView(save, Ui.weight(this, 1.4f, 3));
        panel.addView(actions);

        setContentView(root);
        selectColor(0, palette);
        if (!sig) selectTool(DrawingView.PEN);
    }

    private TextView darkChip(String s) {
        TextView t = Ui.text(this, s, 14, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8));
        t.setBackground(Ui.rounded(this, 0xFF2C2C2C, 0xFF555555, 10));
        return t;
    }

    private void selectTool(int t) {
        canvas.setTool(t);
        for (int i = 0; i < toolButtons.size(); i++) {
            toolButtons.get(i).setBackground(i == t ? Ui.rounded(this, Ui.ACCENT, 0, 10)
                    : Ui.rounded(this, 0xFF2C2C2C, 0xFF555555, 10));
        }
    }

    private void selectColor(int idx, int[] palette) {
        canvas.setColor(palette[idx]);
        for (int i = 0; i < swatches.size(); i++) {
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(palette[i]);
            g.setStroke(Ui.dp(this, i == idx ? 4 : 1), i == idx ? Color.WHITE : 0xFF777777);
            swatches.get(i).setBackground(g);
        }
    }

    private void dictate() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe this finding");
        try {
            startActivityForResult(i, REQ_VOICE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech recognition isn't available on this device", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_VOICE && res == RESULT_OK && data != null) {
            ArrayList<String> r = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (r != null && !r.isEmpty()) {
                String cur = caption.getText().toString().trim();
                caption.setText(cur.length() == 0 ? capitalize(r.get(0)) : cur + " " + r.get(0));
                caption.setSelection(caption.getText().length());
            }
        }
    }

    static String capitalize(String s) {
        if (s == null || s.length() == 0) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void save() {
        try {
            if (sigKey != null) {
                if (canvas.isEmpty()) {
                    Toast.makeText(this, "Sign in the white box first", Toast.LENGTH_SHORT).show();
                    return;
                }
                Bitmap out = canvas.getImage().copy(Bitmap.Config.ARGB_8888, true);
                Canvas c = new Canvas(out);
                DrawingView.renderJson(c, canvas.strokesJson(), out.getWidth(), out.getHeight(), false);
                File dir = new File(insp.dir, "signatures");
                dir.mkdirs();
                String name = "sig_" + sigKey.replace('.', '_') + ".png";
                Images.writePng(out, new File(dir, name));
                insp.setUser(sigKey, name);
            } else {
                JSONObject m = insp.meta(photoId);
                JSONArray strokes = canvas.strokesJson();
                if (strokes.length() == 0) {
                    String a = m.optString("annotated", "");
                    if (a.length() > 0) new File(insp.photoDir(), a).delete();
                    m.remove("annotated");
                    m.remove("strokes");
                } else {
                    Bitmap out = canvas.getImage().copy(Bitmap.Config.ARGB_8888, true);
                    Canvas c = new Canvas(out);
                    DrawingView.renderJson(c, strokes, out.getWidth(), out.getHeight(), true);
                    String name = photoId + "_a.jpg";
                    Images.writeJpeg(out, new File(insp.photoDir(), name), 88);
                    m.put("annotated", name);
                    m.put("strokes", strokes);
                    out.recycle();
                }
                insp.setCaption(photoId, caption.getText().toString().trim());
            }
            Store.save(insp);
            setResult(RESULT_OK);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Could not save: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
