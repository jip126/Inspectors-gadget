package com.inspectorsgadget.app;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

/** Sends every screen to the login screen until the inspector has signed in. */
public class App extends Application implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(Activity a, Bundle b) {
        if (a instanceof LoginActivity) return;
        Auth.restore(a);
        if (Auth.isUnlocked()) return;
        a.startActivity(new Intent(a, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        a.finish();
    }

    @Override public void onActivityStarted(Activity a) {}
    @Override public void onActivityResumed(Activity a) {}
    @Override public void onActivityPaused(Activity a) {}
    @Override public void onActivityStopped(Activity a) {}
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
    @Override public void onActivityDestroyed(Activity a) {}
}
