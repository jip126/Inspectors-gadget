package com.inspectorsgadget.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Branded sign-in: email and password, biometric login, password reset and account creation. */
public class LoginActivity extends Activity {
    /** Set after logging out: the biometric prompt is not opened automatically. */
    public static final String EXTRA_LOGGED_OUT = "loggedOut";

    static final int BG_TOP = 0xFF16233A, BG_EDGE = 0xFF060D18;
    static final int GOLD_LIGHT = 0xFFF2CF8C, GOLD = 0xFFD3A45C, GOLD_DARK = 0xFFA87632;
    static final int FIELD_TEXT = 0xFFF1F3F6, FIELD_HINT = 0xFF97A2B1, FIELD_STROKE = 0x99B5BFCC;
    static final int ERROR = 0xFFFF9C8F;

    private static final int REQ_CONFIRM_DEVICE = 41;

    public static void open(Activity a, boolean loggedOut) {
        Intent i = new Intent(a, LoginActivity.class).putExtra(EXTRA_LOGGED_OUT, loggedOut)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
        a.finish();
    }

    private boolean creating;
    private boolean busy;
    private String resetEmail = "";

    private LinearLayout content;
    private EditText name, email, password, confirm;
    private TextView error;
    private Button primary;
    private View fingerprint;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (getActionBar() != null) getActionBar().hide();
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(BG_EDGE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        if (b != null) {
            creating = b.getBoolean("creating");
            resetEmail = b.getString("resetEmail", "");
        }
        setContentView(buildRoot());
        build();
        boolean loggedOut = getIntent().getBooleanExtra(EXTRA_LOGGED_OUT, false);
        if (b == null && !loggedOut && !creating && Biometrics.isEnabled(this)) {
            content.post(new Runnable() {
                @Override
                public void run() { biometricLogin(); }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean("creating", creating);
        out.putString("resetEmail", resetEmail);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Auth.isUnlocked()) goHome();
    }

    // ------------------------------------------------------------ layout

    private int dp(float v) { return Ui.dp(this, v); }

    private View buildRoot() {
        FrameLayout root = new FrameLayout(this);
        root.setBackground(new Backdrop());

        int screenW = getResources().getDisplayMetrics().widthPixels;
        ImageView tl = new ImageView(this);
        tl.setImageResource(R.drawable.login_swoosh_tl);
        tl.setScaleType(ImageView.ScaleType.FIT_XY);
        int tlw = Math.round(screenW * 470f / 941f);
        root.addView(tl, new FrameLayout.LayoutParams(tlw, Math.round(tlw * 205f / 470f), Gravity.TOP | Gravity.START));
        ImageView br = new ImageView(this);
        br.setImageResource(R.drawable.login_swoosh_br);
        br.setScaleType(ImageView.ScaleType.FIT_XY);
        int brw = Math.round(screenW * 381f / 941f);
        root.addView(br, new FrameLayout.LayoutParams(brw, Math.round(brw * 252f / 381f), Gravity.BOTTOM | Gravity.END));

        final ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        content = Ui.vbox(this);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        scroll.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets in) {
                v.setPadding(0, in.getSystemWindowInsetTop(), 0, in.getSystemWindowInsetBottom());
                return in;
            }
        });
        return root;
    }

    private void build() {
        content.removeAllViews();
        int side = dp(30);
        content.setPadding(side, dp(creating ? 20 : 44), side, dp(28));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.login_logo);
        logo.setAdjustViewBounds(true);
        logo.setContentDescription("The Inspector's Gadget");
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        if (creating) {
            llp = new LinearLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        content.addView(logo, llp);
        content.addView(tagline(), matchTop(creating ? 8 : 14));

        String last = Auth.lastEmail(this);
        error = Ui.text(this, "", 14, ERROR, false);
        error.setGravity(Gravity.CENTER_HORIZONTAL);
        error.setVisibility(View.GONE);

        if (creating) {
            name = new EditText(this);
            content.addView(field(LineIcon.PERSON, name, "Full name",
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS, false),
                    matchTop(30));
        }
        email = new EditText(this);
        content.addView(field(LineIcon.MAIL, email, "Email",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, false), matchTop(creating ? 16 : 40));
        if (!creating && last.length() > 0) email.setText(last);
        password = new EditText(this);
        content.addView(field(LineIcon.LOCK, password, "Password",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, true), matchTop(16));
        if (creating) {
            confirm = new EditText(this);
            content.addView(field(LineIcon.LOCK, confirm, "Confirm password",
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, true), matchTop(16));
        } else {
            confirm = null;
            name = null;
        }
        EditText lastField = creating ? confirm : password;
        lastField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        lastField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent e) {
                if (id == EditorInfo.IME_ACTION_DONE) {
                    submit();
                    return true;
                }
                return false;
            }
        });

        content.addView(error, matchTop(12));

        LinearLayout row = Ui.hbox(this);
        primary = goldButton(creating ? "Create Account" : "Log In");
        primary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { submit(); }
        });
        row.addView(primary, new LinearLayout.LayoutParams(0, dp(60), 1f));
        fingerprint = null;
        if (!creating && Biometrics.isEnabled(this) && Biometrics.available(this) != Biometrics.NONE) {
            ImageView fp = new ImageView(this);
            fp.setImageDrawable(new LineIcon(LineIcon.FINGERPRINT, GOLD_LIGHT, dp(30), dp(1.8f)));
            fp.setScaleType(ImageView.ScaleType.CENTER);
            fp.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33F2CF8C),
                    Ui.rounded(this, 0x22000000, GOLD, 14), null));
            fp.setContentDescription("Biometric login");
            fp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { biometricLogin(); }
            });
            LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(dp(60), dp(60));
            flp.leftMargin = dp(12);
            row.addView(fp, flp);
            fingerprint = fp;
        }
        content.addView(row, matchTop(creating ? 20 : 26));

        if (!creating) {
            TextView forgot = Ui.text(this, "Forgot Password?", 17, 0xFFE2E6EB, false);
            forgot.setPaintFlags(forgot.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            forgot.setPadding(dp(12), dp(10), dp(12), dp(10));
            forgot.setLetterSpacing(0.03f);
            forgot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { forgotPassword(); }
            });
            LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            fl.topMargin = dp(18);
            content.addView(forgot, fl);
        }

        content.addView(divider(), new LinearLayout.LayoutParams(dp(230), dp(2)) {{
            topMargin = dp(creating ? 26 : 22);
        }});

        Button secondary = outlineButton(creating ? "Log In" : "Create Account");
        secondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (busy) return;
                creating = !creating;
                build();
            }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(220), dp(54));
        slp.topMargin = dp(26);
        content.addView(secondary, slp);
    }

    private LinearLayout.LayoutParams matchTop(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(top);
        return lp;
    }

    private View tagline() {
        LinearLayout row = Ui.hbox(this);
        row.setGravity(Gravity.CENTER);
        row.addView(goldRule(false), new LinearLayout.LayoutParams(dp(26), dp(1.5f)));
        TextView t = Ui.text(this, "Home Inspection Reports & Tools", 15, 0xFFE3E7EC, false);
        t.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
        t.setLetterSpacing(0.05f);
        t.setSingleLine(true);
        t.setPadding(dp(10), 0, dp(10), 0);
        row.addView(t);
        row.addView(goldRule(true), new LinearLayout.LayoutParams(dp(26), dp(1.5f)));
        return row;
    }

    private View goldRule(boolean fadeRight) {
        View v = new View(this);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                fadeRight ? new int[]{GOLD, 0x00C9A04A} : new int[]{0x00C9A04A, GOLD});
        v.setBackground(g);
        return v;
    }

    private View divider() {
        LinearLayout row = Ui.hbox(this);
        View l = new View(this), m = new View(this), r = new View(this);
        l.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0x00C0C7D0, 0x55C0C7D0}));
        r.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0x55C0C7D0, 0x00C0C7D0}));
        m.setBackground(Ui.rounded(this, GOLD, 0, 1));
        row.addView(l, new LinearLayout.LayoutParams(0, dp(1), 1f));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(40), dp(2));
        mp.leftMargin = dp(8);
        mp.rightMargin = dp(8);
        row.addView(m, mp);
        row.addView(r, new LinearLayout.LayoutParams(0, dp(1), 1f));
        return row;
    }

    private View field(int icon, final EditText edit, String hint, int inputType, boolean secret) {
        LinearLayout box = Ui.hbox(this);
        box.setBackground(Ui.rounded(this, 0x400A1423, FIELD_STROKE, 14));
        box.setPadding(dp(18), 0, dp(10), 0);
        box.setMinimumHeight(dp(58));

        ImageView ic = new ImageView(this);
        ic.setImageDrawable(new LineIcon(icon, 0xFFE8ECF0, dp(26), dp(1.6f)));
        box.addView(ic, new LinearLayout.LayoutParams(dp(26), dp(26)));
        View bar = new View(this);
        bar.setBackgroundColor(0x66C0C7D0);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(1), dp(28));
        bp.leftMargin = dp(14);
        bp.rightMargin = dp(14);
        box.addView(bar, bp);

        edit.setHint(hint);
        edit.setInputType(inputType);
        edit.setBackground(null);
        edit.setTextColor(FIELD_TEXT);
        edit.setHintTextColor(FIELD_HINT);
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        edit.setLetterSpacing(0.02f);
        edit.setSingleLine(true);
        edit.setPadding(0, dp(14), 0, dp(14));
        edit.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        if (secret) edit.setTransformationMethod(PasswordTransformationMethod.getInstance());
        edit.setTypeface(Typeface.DEFAULT);
        box.addView(edit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (secret) {
            final ImageView eye = new ImageView(this);
            eye.setImageDrawable(new LineIcon(LineIcon.EYE, 0xFFD5DBE2, dp(26), dp(1.6f)));
            eye.setScaleType(ImageView.ScaleType.CENTER);
            eye.setContentDescription("Show password");
            eye.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean hidden = edit.getTransformationMethod() instanceof PasswordTransformationMethod;
                    int at = edit.getSelectionEnd();
                    edit.setTransformationMethod(hidden ? null : PasswordTransformationMethod.getInstance());
                    edit.setTypeface(Typeface.DEFAULT);
                    if (at >= 0) edit.setSelection(at);
                    eye.setImageDrawable(new LineIcon(hidden ? LineIcon.EYE_OFF : LineIcon.EYE, 0xFFD5DBE2, dp(26), dp(1.6f)));
                    eye.setContentDescription(hidden ? "Hide password" : "Show password");
                }
            });
            box.addView(eye, new LinearLayout.LayoutParams(dp(44), dp(44)));
        }
        return box;
    }

    private Button goldButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        b.setTextColor(0xFF111821);
        GradientDrawable face = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFC48E48, GOLD_LIGHT, GOLD, GOLD_DARK});
        face.setCornerRadius(dp(14));
        face.setStroke(dp(1), 0xCCF7DFA8);
        GradientDrawable sheen = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x40FFFFFF, 0x00FFFFFF, 0x00000000, 0x30000000});
        sheen.setCornerRadius(dp(14));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000),
                new LayerDrawable(new Drawable[]{face, sheen}), null));
        b.setStateListAnimator(null);
        b.setElevation(dp(6));
        if (android.os.Build.VERSION.SDK_INT >= 28) b.setOutlineSpotShadowColor(GOLD);
        return b;
    }

    private Button outlineButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setLetterSpacing(0.03f);
        b.setTextColor(0xFFF1F3F6);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22FFFFFF),
                Ui.rounded(this, 0x200A1423, 0xCCC0C7D0, 14), null));
        b.setStateListAnimator(null);
        return b;
    }

    /** Navy backdrop with the soft center glow from the brand artwork. */
    static final class Backdrop extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        @Override
        protected void onBoundsChange(android.graphics.Rect b) {
            float r = Math.max(b.width(), b.height()) * 0.62f;
            paint.setShader(new RadialGradient(b.exactCenterX(), b.top + b.height() * 0.42f, r,
                    new int[]{0xFF1B2A40, 0xFF0F1A2B, BG_EDGE}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        }

        @Override
        public void draw(Canvas c) { c.drawRect(getBounds(), paint); }

        @Override
        public void setAlpha(int a) { paint.setAlpha(a); }

        @Override
        public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); }

        @Override
        public int getOpacity() { return PixelFormat.OPAQUE; }
    }

    // ------------------------------------------------------------ actions

    private void showError(String msg) {
        error.setText(msg);
        error.setVisibility(msg.length() == 0 ? View.GONE : View.VISIBLE);
    }

    private void setBusy(boolean on) {
        busy = on;
        primary.setEnabled(!on);
        primary.setAlpha(on ? 0.6f : 1f);
        if (fingerprint != null) fingerprint.setEnabled(!on);
    }

    private static String text(EditText e) { return e == null ? "" : e.getText().toString(); }

    private void submit() {
        if (busy) return;
        final String em = Auth.normalizeEmail(text(email));
        final String pw = text(password);
        if (creating && text(name).trim().length() == 0) { showError("Enter your name"); return; }
        if (em.length() == 0) { showError("Enter your email"); return; }
        if (!Auth.validEmail(em)) { showError("Enter a valid email"); return; }
        if (pw.length() == 0) { showError("Enter your password"); return; }
        if (creating) {
            if (pw.length() < Auth.MIN_PASSWORD) { showError("Password must be at least " + Auth.MIN_PASSWORD + " characters"); return; }
            if (!pw.equals(text(confirm))) { showError("Passwords do not match"); return; }
        }
        showError("");
        setBusy(true);
        final Auth.Provider p = Auth.provider(this);
        final String nm = text(name).trim();
        Auth.run(new Auth.Call() {
            @Override
            public Auth.Result call() { return creating ? p.register(nm, em, pw) : p.signIn(em, pw); }
        }, new Auth.Callback() {
            @Override
            public void done(Auth.Result r) {
                setBusy(false);
                if (!r.ok) {
                    showError(r.error);
                    return;
                }
                Auth.begin(LoginActivity.this, r);
                offerBiometrics();
            }
        });
    }

    private void biometricLogin() {
        if (busy || !Biometrics.isEnabled(this)) return;
        final String em = Biometrics.enabledEmail(this);
        Biometrics.unlock(this, new Biometrics.Listener() {
            @Override
            public void success(final String token) {
                setBusy(true);
                final Auth.Provider p = Auth.provider(LoginActivity.this);
                Auth.run(new Auth.Call() {
                    @Override
                    public Auth.Result call() { return p.refresh(em, token); }
                }, new Auth.Callback() {
                    @Override
                    public void done(Auth.Result r) {
                        setBusy(false);
                        if (!r.ok) {
                            Biometrics.disable(LoginActivity.this);
                            build();
                            showError(r.error);
                            return;
                        }
                        Auth.begin(LoginActivity.this, r);
                        goHome();
                    }
                });
            }

            @Override
            public void cancelled() {}

            @Override
            public void failed(String message) {
                build();
                showError(message);
            }
        });
    }

    /** After a password sign-in: offer biometric login once per account. */
    private void offerBiometrics() {
        final String em = Auth.email(this);
        boolean on = Biometrics.isEnabled(this) && Biometrics.enabledEmail(this).equals(em);
        if (on || Biometrics.available(this) == Biometrics.NONE || Biometrics.declined(this, em)) {
            goHome();
            return;
        }
        new AlertDialog.Builder(this).setTitle("Use fingerprint or face to log in?")
                .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        Biometrics.enable(LoginActivity.this, new Biometrics.Listener() {
                            @Override
                            public void success(String token) {
                                Toast.makeText(LoginActivity.this, "Biometric login on", Toast.LENGTH_SHORT).show();
                                goHome();
                            }

                            @Override
                            public void cancelled() { goHome(); }

                            @Override
                            public void failed(String message) {
                                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                                goHome();
                            }
                        });
                    }
                })
                .setNegativeButton("Not now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        Biometrics.setDeclined(LoginActivity.this, em);
                        goHome();
                    }
                })
                .setCancelable(false).show();
    }

    private void goHome() {
        if (isFinishing()) return;
        Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    // ------------------------------------------------------------ password reset

    private void forgotPassword() {
        if (busy) return;
        final EditText e = dialogField("Email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        e.setText(Auth.normalizeEmail(text(email)));
        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle("Reset password").setView(wrap(e))
                .setPositiveButton("Continue", null).setNegativeButton("Cancel", null).create();
        dlg.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String em = Auth.normalizeEmail(e.getText().toString());
                        if (!Auth.validEmail(em)) {
                            e.setError("Enter a valid email");
                            return;
                        }
                        final Auth.Provider p = Auth.provider(LoginActivity.this);
                        if (!p.resetsOnDevice()) {
                            dlg.dismiss();
                            Auth.run(new Auth.Call() {
                                @Override
                                public Auth.Result call() { return p.resetPassword(em, null); }
                            }, new Auth.Callback() {
                                @Override
                                public void done(Auth.Result r) {
                                    Toast.makeText(LoginActivity.this, r.ok ? "Reset link sent to " + em : r.error,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                            return;
                        }
                        if (!p.exists(em)) {
                            e.setError("No account found for this email");
                            return;
                        }
                        dlg.dismiss();
                        resetEmail = em;
                        KeyguardManager km = getSystemService(KeyguardManager.class);
                        Intent confirmIntent = km != null && km.isDeviceSecure()
                                ? km.createConfirmDeviceCredentialIntent("Reset password", em) : null;
                        if (confirmIntent != null) startActivityForResult(confirmIntent, REQ_CONFIRM_DEVICE);
                        else newPassword(em);
                    }
                });
            }
        });
        dlg.show();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_CONFIRM_DEVICE && res == RESULT_OK && resetEmail.length() > 0) newPassword(resetEmail);
    }

    private void newPassword(final String em) {
        LinearLayout box = Ui.vbox(this);
        final EditText pw = dialogField("New password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        final EditText pw2 = dialogField("Confirm password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(pw);
        box.addView(pw2);
        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle("New password").setMessage(em).setView(wrap(box))
                .setPositiveButton("Save", null).setNegativeButton("Cancel", null).create();
        dlg.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String a = pw.getText().toString();
                        if (a.length() < Auth.MIN_PASSWORD) {
                            pw.setError("At least " + Auth.MIN_PASSWORD + " characters");
                            return;
                        }
                        if (!a.equals(pw2.getText().toString())) {
                            pw2.setError("Passwords do not match");
                            return;
                        }
                        dlg.dismiss();
                        setBusy(true);
                        final Auth.Provider p = Auth.provider(LoginActivity.this);
                        Auth.run(new Auth.Call() {
                            @Override
                            public Auth.Result call() { return p.resetPassword(em, a); }
                        }, new Auth.Callback() {
                            @Override
                            public void done(Auth.Result r) {
                                setBusy(false);
                                resetEmail = "";
                                if (!r.ok) {
                                    showError(r.error);
                                    return;
                                }
                                if (Biometrics.enabledEmail(LoginActivity.this).equals(em)) Biometrics.disable(LoginActivity.this);
                                Toast.makeText(LoginActivity.this, "Password updated", Toast.LENGTH_SHORT).show();
                                Auth.begin(LoginActivity.this, r);
                                offerBiometrics();
                            }
                        });
                    }
                });
            }
        });
        dlg.show();
    }

    private EditText dialogField(String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(type);
        e.setSingleLine(true);
        return e;
    }

    private View wrap(View v) {
        FrameLayout f = new FrameLayout(this);
        int p = dp(20);
        f.setPadding(p, dp(8), p, 0);
        f.addView(v);
        return f;
    }
}
