package com.inspectorsgadget.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** One inspection section: photos, then every field, with dictation on notes. */
public class SectionActivity extends Activity {
    public static final String EXTRA_INSPECTION = "insp", EXTRA_SECTION = "section";
    private static final int REQ_CAMERA = 11, REQ_GALLERY = 12, REQ_VOICE = 13, REQ_ANNOTATE = 14, REQ_SIGN = 15;

    private Inspection insp;
    private Forms.Section section;
    private Forms.Form form;
    private boolean isGeneral;

    private LinearLayout photoStrip;
    private TextView photoTitle;
    private LinearLayout importBanner;
    private String pendingPhoto, pendingVoiceKey, photoTarget;
    private final Map<String, LinearLayout> itemStrips = new HashMap<>();
    private final Map<String, View> commentBoxes = new HashMap<>();
    private final Map<String, EditText> notesViews = new HashMap<>();
    private final Map<String, TextView> prefillBadges = new HashMap<>();
    private final Map<String, TextView> findingBadges = new HashMap<>();
    private final Map<String, ImageView> signatureViews = new HashMap<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        insp = Store.get(this, getIntent().getStringExtra(EXTRA_INSPECTION));
        section = Forms.section(getIntent().getStringExtra(EXTRA_SECTION));
        if (insp == null || section == null) {
            finish();
            return;
        }
        if (b != null) {
            pendingPhoto = b.getString("pendingPhoto");
            pendingVoiceKey = b.getString("pendingVoice");
            photoTarget = b.getString("photoTarget");
        }
        form = Forms.form(section.formId);
        // "isGeneral" = forms that do not reuse general-inspection photos/answers
        isGeneral = !(Forms.FOUR_POINT.equals(form.id) || Forms.WIND.equals(form.id) || Forms.AGREEMENT.equals(form.id));
        boolean prefillForm = !Forms.GENERAL.equals(form.id) && !Forms.INFO.equals(form.id)
                && !insp.id.startsWith("_");
        setTitle(section.title);
        if (getActionBar() != null) {
            getActionBar().setSubtitle(insp.id.startsWith("_") ? form.shortTitle : form.shortTitle + " \u00B7 " + insp.title());
            getActionBar().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(form.color));
        }
        if (prefillForm && Logic.prefill(insp, section) > 0) Store.save(insp);
        build();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("pendingPhoto", pendingPhoto);
        out.putString("pendingVoice", pendingVoiceKey);
        out.putString("photoTarget", photoTarget);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (insp != null) Store.save(insp);
    }

    // ------------------------------------------------------------------ layout

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = Ui.vbox(this);
        int pad = Ui.dp(this, 14);
        content.setPadding(pad, pad, pad, Ui.dp(this, 40));
        scroll.addView(content);

        LinearLayout head = Ui.card(this);
        TextView t = Ui.title(this, section.title);
        head.addView(t);
        int pre = 0;
        for (Forms.Field f : section.fields) if (f.inputs() && insp.isPrefilled(f.key)) pre++;
        if (pre > 0) {
            head.addView(Ui.pill(this, "↺ " + pre + " prefilled", Ui.TEAL, Ui.TEAL_LIGHT), wrapLp(8));
        }
        content.addView(head);

        boolean itemPhotos = Forms.GENERAL.equals(form.id);
        if (section.photos && !itemPhotos) content.addView(buildPhotoCard());

        LinearLayout card = Ui.card(this);
        content.addView(card);
        boolean first = true;
        for (Forms.Field f : section.fields) {
            if (f.type == Forms.HEADER) {
                if (!first) {
                    card = Ui.card(this);
                    content.addView(card);
                }
                TextView h = Ui.text(this, f.label, 16, form.color, true);
                card.addView(h);
                first = false;
                continue;
            }
            card.addView(buildField(f), Ui.matchWrap(this, first ? 0 : 14));
            first = false;
        }

        // Next section button for walking through the house in order.
        int idx = form.sections.indexOf(section);
        if (idx >= 0 && idx + 1 < form.sections.size()) {
            final Forms.Section next = form.sections.get(idx + 1);
            Button nb = Ui.button(this, "Next: " + next.title + "  →", form.color, true);
            nb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Store.save(insp);
                    Intent i = new Intent(SectionActivity.this, SectionActivity.class);
                    i.putExtra(EXTRA_INSPECTION, insp.id);
                    i.putExtra(EXTRA_SECTION, next.key);
                    startActivity(i);
                    finish();
                }
            });
            content.addView(nb, Ui.matchWrap(this, 4));
        }
        if (section.photos && itemPhotos) content.addView(buildPhotoCard(), content.getChildCount() - (idx >= 0 && idx + 1 < form.sections.size() ? 1 : 0));
        refreshPhotos();
        setContentView(scroll);
    }

    private View buildField(final Forms.Field f) {
        LinearLayout box = Ui.vbox(this);
        LinearLayout labelRow = Ui.hbox(this);
        TextView label = Ui.text(this, f.label, 14, Ui.TEXT, true);
        labelRow.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView pre = Ui.pill(this, "↺ Prefilled", Ui.TEAL, Ui.TEAL_LIGHT);
        pre.setVisibility(insp.isPrefilled(f.key) ? View.VISIBLE : View.GONE);
        prefillBadges.put(f.key, pre);
        labelRow.addView(pre);
        TextView finding = Ui.pill(this, "Finding", Ui.RED, Ui.RED_LIGHT);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.leftMargin = Ui.dp(this, 6);
        finding.setVisibility(Logic.isFinding(insp, f) ? View.VISIBLE : View.GONE);
        findingBadges.put(f.key, finding);
        labelRow.addView(finding, flp);
        box.addView(labelRow);

        View control;
        switch (f.type) {
            case Forms.TEXT:
            case Forms.NUMBER:
                control = textInput(f, false);
                break;
            case Forms.NOTES:
                control = notesInput(f);
                break;
            case Forms.DATE:
                control = dateInput(f);
                break;
            case Forms.CHOICE:
            case Forms.MULTI:
                control = choiceInput(f);
                break;
            case Forms.PICK_INSPECTOR:
                control = inspectorPicker(f);
                break;
            case Forms.SIGNATURE:
                control = signatureInput(f);
                break;
            default:
                control = new View(this);
        }
        box.addView(control, Ui.matchWrap(this, 6));
        if (f.takesPhotos()) itemExtras(box, f);
        return box;
    }

    private LinearLayout.LayoutParams wrapLp(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, top);
        return lp;
    }

    /** Photos and a dictated comment attached to one inspection item. */
    private void itemExtras(LinearLayout box, final Forms.Field f) {
        final String ck = Forms.commentKey(f.key);
        LinearLayout actions = Ui.hbox(this);
        TextView cam = Ui.pill(this, "📷  Photo", form.color, Ui.PRIMARY_LIGHT);
        cam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { takePhoto(f.key); }
        });
        actions.addView(cam);
        TextView mic = Ui.pill(this, "🎤  Comment", form.color, Ui.PRIMARY_LIGHT);
        LinearLayout.LayoutParams mlp = wrapLp(0);
        mlp.leftMargin = Ui.dp(this, 8);
        actions.addView(mic, mlp);
        box.addView(actions, wrapLp(8));

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout strip = Ui.hbox(this);
        hs.addView(strip);
        itemStrips.put(f.key, strip);
        box.addView(hs, Ui.matchWrap(this, 8));

        final LinearLayout cbox = Ui.hbox(this);
        final EditText e = styledEdit();
        e.setHint("Comments");
        e.setMinLines(2);
        e.setGravity(Gravity.TOP | Gravity.START);
        e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setText(insp.str(ck));
        e.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence x, int a, int b, int c) {}

            @Override
            public void onTextChanged(CharSequence x, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable x) {
                insp.setUser(ck, x.toString().trim());
                Store.saveSoon(insp);
            }
        });
        notesViews.put(ck, e);
        cbox.addView(e, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button dm = Ui.button(this, "🎤", form.color, false);
        dm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { dictate(ck); }
        });
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Ui.dp(this, 52), ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.leftMargin = Ui.dp(this, 6);
        cbox.addView(dm, dlp);
        cbox.setVisibility(insp.str(ck).length() > 0 ? View.VISIBLE : View.GONE);
        commentBoxes.put(ck, cbox);
        box.addView(cbox, Ui.matchWrap(this, 8));
        mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cbox.setVisibility(View.VISIBLE);
                dictate(ck);
            }
        });
    }

    private void changed(Forms.Field f, Object value) {
        insp.setUser(f.key, value);
        Store.saveSoon(insp);
        TextView pre = prefillBadges.get(f.key);
        if (pre != null) pre.setVisibility(View.GONE);
        TextView fb = findingBadges.get(f.key);
        if (fb != null) fb.setVisibility(Logic.isFinding(insp, f) ? View.VISIBLE : View.GONE);
    }

    private EditText styledEdit() {
        EditText e = new EditText(this);
        e.setBackground(Ui.rounded(this, 0xFFF8FAFC, Ui.BORDER, 8));
        int p = Ui.dp(this, 10);
        e.setPadding(p, p, p, p);
        e.setTextSize(15);
        e.setTextColor(Ui.TEXT);
        return e;
    }

    private View textInput(final Forms.Field f, boolean multiline) {
        EditText e = styledEdit();
        if (f.type == Forms.NUMBER) e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        else if (multiline) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(3);
            e.setGravity(Gravity.TOP | Gravity.START);
        } else e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        e.setText(insp.str(f.key));
        if (f.hint != null) e.setHint(f.hint);
        e.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(insp.str(f.key))) changed(f, s.toString());
            }
        });
        return e;
    }

    private View notesInput(final Forms.Field f) {
        LinearLayout box = Ui.vbox(this);
        EditText e = (EditText) textInput(f, true);
        e.setHint("Type or tap Dictate and speak your findings");
        notesViews.put(f.key, e);
        box.addView(e);
        Button mic = Ui.button(this, "🎤  Dictate", Ui.ACCENT, true);
        mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { dictate(f.key); }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 8);
        box.addView(mic, lp);
        return box;
    }

    private View dateInput(final Forms.Field f) {
        final TextView t = Ui.text(this, "", 15, Ui.TEXT, false);
        t.setBackground(Ui.rounded(this, 0xFFF8FAFC, Ui.BORDER, 8));
        int p = Ui.dp(this, 10);
        t.setPadding(p, Ui.dp(this, 12), p, Ui.dp(this, 12));
        final Runnable refresh = new Runnable() {
            @Override
            public void run() {
                String v = insp.str(f.key);
                t.setText(v.length() > 0 ? "📅  " + v : "📅  Tap to pick a date");
                t.setTextColor(v.length() > 0 ? Ui.TEXT : Ui.MUTED);
            }
        };
        refresh.run();
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                String cur = insp.str(f.key);
                String[] parts = cur.split("/");
                if (parts.length == 3) {
                    try {
                        c.set(Integer.parseInt(parts[2]), Integer.parseInt(parts[0]) - 1, Integer.parseInt(parts[1]));
                    } catch (Exception ignored) {
                    }
                }
                DatePickerDialog d = new DatePickerDialog(SectionActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int y, int m, int day) {
                        changed(f, String.format(Locale.US, "%02d/%02d/%04d", m + 1, day, y));
                        refresh.run();
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                d.setButton(DialogInterface.BUTTON_NEUTRAL, "Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        changed(f, "");
                        refresh.run();
                    }
                });
                d.show();
            }
        });
        return t;
    }

    private View choiceInput(final Forms.Field f) {
        boolean longOptions = false;
        for (String o : f.options) if (o.length() > 28) longOptions = true;
        final ViewGroup group;
        if (longOptions) group = Ui.vbox(this);
        else group = new FlowLayout(this);
        final List<TextView> chips = new ArrayList<>();
        final boolean multi = f.type == Forms.MULTI;
        final int color = form.color;
        for (final String opt : f.options) {
            final TextView chip = Ui.chip(this, (multi ? "☐ " : "") + opt);
            chips.add(chip);
            if (longOptions) {
                LinearLayout.LayoutParams lp = Ui.matchWrap(this, chips.size() == 1 ? 0 : 6);
                group.addView(chip, lp);
            } else {
                group.addView(chip);
            }
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (multi) {
                        List<String> cur = insp.list(f.key);
                        if (cur.contains(opt)) cur.remove(opt);
                        else cur.add(opt);
                        // keep form order
                        List<String> ordered = new ArrayList<>();
                        for (String o : f.options) if (cur.contains(o)) ordered.add(o);
                        changed(f, new JSONArray(ordered));
                    } else {
                        changed(f, opt.equals(insp.str(f.key)) ? "" : opt);
                    }
                    paintChips(f, chips, color);
                }
            });
        }
        paintChips(f, chips, color);
        return group;
    }

    private void paintChips(Forms.Field f, List<TextView> chips, int color) {
        List<String> sel = insp.list(f.key);
        boolean multi = f.type == Forms.MULTI;
        for (int i = 0; i < chips.size(); i++) {
            String opt = f.options[i];
            boolean on = sel.contains(opt);
            TextView c = chips.get(i);
            if (multi) c.setText((on ? "☑ " : "☐ ") + opt);
            boolean bad = on && f.flags != null && (Arrays.asList(f.flags).contains(opt)
                    || Arrays.asList(f.flags).contains("*"));
            Ui.styleChip(this, c, on, bad ? Ui.RED : color);
        }
    }

    private View signatureInput(final Forms.Field f) {
        LinearLayout box = Ui.vbox(this);
        ImageView iv = new ImageView(this);
        iv.setBackground(Ui.rounded(this, Color.WHITE, Ui.BORDER, 8));
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        box.addView(iv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 100)));
        signatureViews.put(f.key, iv);
        loadSignature(f.key);
        Button sign = Ui.button(this, "✍  Sign", form.color, false);
        sign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SectionActivity.this, AnnotateActivity.class);
                i.putExtra(AnnotateActivity.EXTRA_INSPECTION, insp.id);
                i.putExtra(AnnotateActivity.EXTRA_SIGNATURE, f.key);
                startActivityForResult(i, REQ_SIGN);
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 8);
        box.addView(sign, lp);
        return box;
    }

    private View inspectorPicker(final Forms.Field f) {
        LinearLayout box = Ui.vbox(this);
        FlowLayout chips = new FlowLayout(this);
        String current = insp.str(f.key);
        java.util.List<Inspection> roster = Company.inspectors(this);
        for (final Inspection ip : roster) {
            TextView chip = Ui.chip(this, "\uD83D\uDC64 " + Company.inspectorName(ip)
                    + (ip.str("ip.license").length() > 0 ? "  \u00B7  " + ip.str("ip.license") : ""));
            Ui.styleChip(this, chip, ip.id.equals(current), form.color);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Company.applyInspector(SectionActivity.this, insp, ip.id);
                    Store.save(insp);
                    recreate();
                }
            });
            chips.addView(chip);
        }
        box.addView(chips);
        if (current.length() > 0) {
            String details = Forms.joinNonEmpty(insp.str("info.licenseType"), insp.str("info.license"),
                    insp.str("info.issuer"));
            if (details.length() > 0) box.addView(Ui.muted(this, details), Ui.matchWrap(this, 6));
        }
        Button manage = Ui.button(this, roster.isEmpty() ? "+ Add an inspector" : "Manage inspectors", form.color, false);
        manage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { startActivity(new Intent(SectionActivity.this, CompanyActivity.class)); }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 8);
        box.addView(manage, lp);
        return box;
    }

    private void loadSignature(String key) {
        ImageView iv = signatureViews.get(key);
        if (iv == null) return;
        String name = insp.str(key);
        if (name.length() == 0) {
            iv.setImageDrawable(null);
            return;
        }
        Images.loadThumb(iv, new File(new File(insp.dir, "signatures"), name), 800);
    }

    // ------------------------------------------------------------------ photos

    private View buildPhotoCard() {
        LinearLayout card = Ui.card(this);
        photoTitle = Ui.text(this, photoLabel(), 16, form.color, true);
        card.addView(photoTitle);

        if (section.shotList != null) {
            StringBuilder sb = new StringBuilder("Required shots:");
            for (String s : section.shotList) sb.append("\n• ").append(s);
            TextView tv = Ui.muted(this, sb.toString());
            card.addView(tv, Ui.matchWrap(this, 6));
        }

        importBanner = Ui.vbox(this);
        card.addView(importBanner);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        photoStrip = Ui.hbox(this);
        hs.addView(photoStrip);
        card.addView(hs, Ui.matchWrap(this, 10));

        LinearLayout actions = Ui.hbox(this);
        Button cam = Ui.button(this, "📷  Take photo", form.color, Forms.GENERAL.equals(form.id) || insp.id.startsWith("_"));
        cam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { takePhoto(section.key); }
        });
        actions.addView(cam, Ui.weight(this, 1.2f, 3));
        if (insp.id.startsWith("_")) {
            // company logo: the only place a file from the phone is used
            Button gal = Ui.button(this, "🖼  Choose file", form.color, false);
            gal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { pickFromGallery(section.key); }
            });
            actions.addView(gal, Ui.weight(this, 1f, 3));
        } else if (!isGeneral) {
            // 4-Point and Wind pull from this inspection's own photos, never the phone's files
            Button reuse = Ui.button(this, "♻  Inspection photos", form.color, true);
            reuse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { showReusePicker(); }
            });
            actions.addView(reuse, Ui.weight(this, 1.2f, 3));
        }
        card.addView(actions, Ui.matchWrap(this, 10));
        return card;
    }

    private String photoLabel() {
        return Forms.GENERAL.equals(form.id) ? "Other photos" : section.photoLabel;
    }

    private void refreshPhotos() {
        for (Map.Entry<String, LinearLayout> en : itemStrips.entrySet()) {
            fillStrip(en.getValue(), en.getKey(), Ui.dp(this, 84));
            ((View) en.getValue().getParent()).setVisibility(en.getValue().getChildCount() > 0 ? View.VISIBLE : View.GONE);
        }
        if (photoStrip == null) return;
        int n = fillStrip(photoStrip, section.key, Ui.dp(this, 112));
        String lbl = photoLabel();
        photoTitle.setText(lbl + (lbl.endsWith("hotos") ? " (" + n + ")" : ""));
        if (n == 0) {
            TextView empty = Ui.muted(this, "No photos");
            photoStrip.addView(empty);
        }
        refreshImportBanner();
    }

    /** Fills a thumbnail strip with the photos in one list; returns how many. */
    private int fillStrip(LinearLayout strip, final String bucket, int size) {
        strip.removeAllViews();
        List<String> ids = new ArrayList<>();
        for (String p : insp.sectionPhotos(bucket)) if (insp.photoExists(p)) ids.add(p);
        for (final String pid : ids) {
            FrameLayout fl = new FrameLayout(this);
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackgroundColor(0xFFE0E0E0);
            fl.addView(iv, new FrameLayout.LayoutParams(size, size));
            Images.loadThumb(iv, insp.displayFile(pid), 320);
            String cap = insp.caption(pid);
            if (cap.length() > 0) {
                TextView ct = Ui.text(this, cap, 11, Color.WHITE, false);
                ct.setMaxLines(2);
                ct.setBackgroundColor(0xAA000000);
                ct.setPadding(Ui.dp(this, 4), Ui.dp(this, 2), Ui.dp(this, 4), Ui.dp(this, 2));
                fl.addView(ct, new FrameLayout.LayoutParams(size, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
            }
            LinearLayout badges = Ui.hbox(this);
            if (insp.meta(pid).optString("annotated", "").length() > 0)
                badges.addView(Ui.pill(this, "✎", Color.WHITE, 0xCCC9A04A));
            String origin = insp.meta(pid).optString("origin", "");
            if (!isGeneral && origin.startsWith("g.")) {
                TextView g = Ui.pill(this, "↺", Color.WHITE, 0xCC0E8C7F);
                badges.addView(g);
            }
            fl.addView(badges, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END));
            fl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { photoOptions(bucket, pid); }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.rightMargin = Ui.dp(this, 8);
            strip.addView(fl, lp);
        }
        return ids.size();
    }

    private void refreshImportBanner() {
        importBanner.removeAllViews();
        if (!isGeneral) {
            final List<String> importable = Logic.importablePhotos(insp, section);
            if (!importable.isEmpty()) {
                LinearLayout row = Ui.hbox(this);
                row.setBackground(Ui.rounded(this, Ui.TEAL_LIGHT, 0, 8));
                row.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6));
                StringBuilder names = new StringBuilder();
                for (String k : section.photoSources) {
                    if (Logic.sectionPhotoIds(insp, Forms.section(k)).isEmpty()) continue;
                    if (names.length() > 0) names.append(", ");
                    names.append(Forms.section(k).title);
                }
                TextView tv = Ui.text(this, importable.size() + " photo" + (importable.size() == 1 ? "" : "s")
                        + " from General › " + names, 13, Ui.TEAL, false);
                row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                Button imp = Ui.button(this, "Import all", Ui.TEAL, true);
                imp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (String p : importable) insp.addPhotoToSection(section.key, p);
                        Store.save(insp);
                        refreshPhotos();
                    }
                });
                row.addView(imp);
                importBanner.addView(row, Ui.matchWrap(this, 8));
            }
        }
    }

    private void photoOptions(final String bucket, final String pid) {
        final boolean shared = insp.usageCount(pid) > 1;
        String[] items = {"✏  Mark up / draw on photo", "💬  Edit caption",
                "✖  Remove"};
        new AlertDialog.Builder(this)
                .setTitle(insp.caption(pid).length() > 0 ? insp.caption(pid) : "Photo")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) annotate(pid);
                        else if (which == 1) editCaption(pid);
                        else {
                            insp.removePhotoFromSection(bucket, pid);
                            if (insp.usageCount(pid) == 0) insp.deletePhotoEverywhere(pid);
                            Store.save(insp);
                            refreshPhotos();
                            if (shared) Toast.makeText(SectionActivity.this,
                                    "Removed here. Still used in another section.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).show();
    }

    private void editCaption(final String pid) {
        final EditText e = styledEdit();
        e.setText(insp.caption(pid));
        FrameLayout wrap = new FrameLayout(this);
        int p = Ui.dp(this, 20);
        wrap.setPadding(p, Ui.dp(this, 8), p, 0);
        wrap.addView(e);
        new AlertDialog.Builder(this).setTitle("Caption").setView(wrap)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        insp.setCaption(pid, e.getText().toString().trim());
                        Store.save(insp);
                        refreshPhotos();
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void annotate(String pid) {
        Store.save(insp);
        Intent i = new Intent(this, AnnotateActivity.class);
        i.putExtra(AnnotateActivity.EXTRA_INSPECTION, insp.id);
        i.putExtra(AnnotateActivity.EXTRA_PHOTO, pid);
        startActivityForResult(i, REQ_ANNOTATE);
    }

    private void takePhoto(String bucket) {
        photoTarget = bucket;
        pendingPhoto = insp.newPhoto(bucket);
        File f = insp.originalFile(pendingPhoto);
        Uri uri = FileShareProvider.uriFor(this, f);
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ask for the rear camera. Camera apps read different extras, so set the common ones.
        i.putExtra("android.intent.extras.CAMERA_FACING", 0);
        i.putExtra("android.intent.extras.LENS_FACING_BACK", 1);
        i.putExtra("android.intent.extra.USE_FRONT_CAMERA", false);
        i.putExtra("com.google.assistant.extra.USE_FRONT_CAMERA", false);
        i.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        i.setClipData(ClipData.newRawUri("photo", uri));
        i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Store.save(insp);
        try {
            startActivityForResult(i, REQ_CAMERA);
        } catch (ActivityNotFoundException e) {
            insp.deletePhotoEverywhere(pendingPhoto);
            pendingPhoto = null;
            Toast.makeText(this, "No camera app found", Toast.LENGTH_LONG).show();
        }
    }

    private void pickFromGallery(String bucket) {
        photoTarget = bucket;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
            startActivityForResult(Intent.createChooser(i, "Add photos"), REQ_GALLERY);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No gallery app found", Toast.LENGTH_LONG).show();
        }
    }

    private void importUri(Uri uri) {
        String bucket = target();
        String pid = insp.newPhoto(bucket);
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            OutputStream os = new FileOutputStream(insp.originalFile(pid));
            byte[] buf = new byte[16384];
            int n;
            while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
            os.close();
            is.close();
            insp.addPhotoToSection(bucket, pid);
            defaultCaption(bucket, pid);
        } catch (Exception e) {
            insp.deletePhotoEverywhere(pid);
            Toast.makeText(this, "Could not import photo", Toast.LENGTH_SHORT).show();
        }
    }

    private String target() { return photoTarget != null ? photoTarget : section.key; }

    /** Item photos are captioned with the item name until the inspector writes a caption. */
    private void defaultCaption(String bucket, String pid) {
        if (bucket.equals(section.key) || insp.caption(pid).length() > 0) return;
        for (Forms.Field f : section.fields) if (f.key.equals(bucket)) insp.setCaption(pid, f.label);
    }

    private void showReusePicker() {
        final List<String> current = insp.sectionPhotos(section.key);
        final Set<String> selected = new HashSet<>(current);
        final Set<String> offered = new HashSet<>();
        ScrollView sv = new ScrollView(this);
        LinearLayout box = Ui.vbox(this);
        int p = Ui.dp(this, 16);
        box.setPadding(p, Ui.dp(this, 8), p, p);
        sv.addView(box);

        // Suggested sections first, then the rest of the general inspection.
        List<Forms.Section> order = new ArrayList<>();
        for (String k : section.photoSources) order.add(Forms.section(k));
        for (Forms.Section s : Forms.form(Forms.GENERAL).sections) if (!order.contains(s)) order.add(s);

        int size = Ui.dp(this, 92);
        for (Forms.Section gs : order) {
            List<String> ids = new ArrayList<>();
            for (String pid : Logic.sectionPhotoIds(insp, gs)) if (!offered.contains(pid)) ids.add(pid);
            if (ids.isEmpty()) continue;
            boolean suggested = Arrays.asList(section.photoSources).contains(gs.key);
            box.addView(Ui.text(this, gs.title + (suggested ? "  ★ suggested" : ""), 14, Ui.TEXT, true),
                    Ui.matchWrap(this, 10));
            FlowLayout grid = new FlowLayout(this);
            for (final String pid : ids) {
                offered.add(pid);
                final FrameLayout fl = new FrameLayout(this);
                ImageView iv = new ImageView(this);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Images.loadThumb(iv, insp.displayFile(pid), 280);
                fl.addView(iv, new FrameLayout.LayoutParams(size, size));
                final TextView check = Ui.text(this, "✔", 18, Color.WHITE, true);
                check.setGravity(Gravity.CENTER);
                check.setBackground(Ui.rounded(this, 0xCC0E8C7F, 0, 14));
                fl.addView(check, new FrameLayout.LayoutParams(Ui.dp(this, 28), Ui.dp(this, 28), Gravity.TOP | Gravity.END));
                final Runnable paint = new Runnable() {
                    @Override
                    public void run() {
                        boolean on = selected.contains(pid);
                        check.setVisibility(on ? View.VISIBLE : View.GONE);
                        fl.setAlpha(on ? 1f : 0.55f);
                    }
                };
                paint.run();
                fl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (selected.contains(pid)) selected.remove(pid);
                        else selected.add(pid);
                        paint.run();
                    }
                });
                grid.addView(fl, new ViewGroup.LayoutParams(size, size));
            }
            box.addView(grid, Ui.matchWrap(this, 6));
        }
        if (offered.isEmpty()) {
            box.addView(Ui.body(this, "No General Inspection photos."));
        }
        new AlertDialog.Builder(this).setTitle("Inspection photos")
                .setView(sv)
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        for (String pid : offered) {
                            if (selected.contains(pid)) insp.addPhotoToSection(section.key, pid);
                            else if (current.contains(pid)) insp.removePhotoFromSection(section.key, pid);
                        }
                        Store.save(insp);
                        refreshPhotos();
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    // ------------------------------------------------------------------ voice

    private void dictate(String key) {
        pendingVoiceKey = key;
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, section.title);
        try {
            startActivityForResult(i, REQ_VOICE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech recognition isn't available on this device", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        switch (req) {
            case REQ_CAMERA:
                if (pendingPhoto != null) {
                    File f = insp.originalFile(pendingPhoto);
                    if (res == RESULT_OK && f.exists() && f.length() > 0) {
                        insp.addPhotoToSection(target(), pendingPhoto);
                        defaultCaption(target(), pendingPhoto);
                        Store.save(insp);
                        String pid = pendingPhoto;
                        pendingPhoto = null;
                        refreshPhotos();
                        annotate(pid); // go straight to markup
                    } else {
                        insp.deletePhotoEverywhere(pendingPhoto);
                        pendingPhoto = null;
                        Store.save(insp);
                    }
                }
                break;
            case REQ_GALLERY:
                if (res == RESULT_OK && data != null) {
                    if (data.getClipData() != null) {
                        ClipData cd = data.getClipData();
                        for (int i = 0; i < cd.getItemCount(); i++) importUri(cd.getItemAt(i).getUri());
                    } else if (data.getData() != null) {
                        importUri(data.getData());
                    }
                    Store.save(insp);
                    refreshPhotos();
                }
                break;
            case REQ_VOICE:
                if (res == RESULT_OK && data != null && pendingVoiceKey != null) {
                    ArrayList<String> r = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    EditText e = notesViews.get(pendingVoiceKey);
                    if (r != null && !r.isEmpty() && e != null) {
                        String cur = e.getText().toString().trim();
                        String add = AnnotateActivity.capitalize(r.get(0));
                        if (!add.endsWith(".")) add = add + ".";
                        e.setText(cur.length() == 0 ? add : cur + " " + add);
                        e.setSelection(e.getText().length());
                    }
                }
                pendingVoiceKey = null;
                break;
            case REQ_ANNOTATE:
                refreshPhotos();
                break;
            case REQ_SIGN:
                for (String k : signatureViews.keySet()) loadSignature(k);
                TextView pre;
                for (String k : signatureViews.keySet()) {
                    pre = prefillBadges.get(k);
                    if (pre != null) pre.setVisibility(View.GONE);
                }
                break;
        }
    }
}
