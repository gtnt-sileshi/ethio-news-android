package com.ethionews.app.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDex;

import com.google.firebase.analytics.FirebaseAnalytics;

import com.ethionews.app.BuildConfig;
import com.ethionews.app.R;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.utils.ThemeManager;
import com.ethionews.app.utils.advertising.AppOpenAdManager;
import com.ethionews.app.utils.helper.DBHelper;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;

public class MyApplication extends android.app.Application implements android.app.Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    DBHelper dbHelper;
    private SPHelper spHelper;
    private AppOpenAdManager appOpenAdManager;
    private Activity currentActivity;

    @Override
    public void onCreate() {
        super.onCreate();

        this.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        appOpenAdManager = new AppOpenAdManager(getApplicationContext());

        spHelper = new SPHelper(getApplicationContext());

        FirebaseAnalytics.getInstance(getApplicationContext());

        // Enable StrictMode in debug builds to catch file URI exposure and other VM violations.
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectFileUriExposure()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .penaltyLog()
                    .build());
        }

        try {
            dbHelper = new DBHelper(getApplicationContext());
            dbHelper.onCreate(dbHelper.getWritableDatabase());
            dbHelper.getAbout();
        } catch (Exception e) {
            Log.e("MyApplication", "Error db", e);
        }

        // Create FCM notification channel (required for Android 8+)
        createNotificationChannel();

        // Ad initialisation creates a WebView and must only run in the main process.
        // Secondary processes (e.g. the :AppMetrica background process) share this
        // Application class but cannot share WebView's data directory — doing so
        // crashes with "Using WebView from more than one process at once".
        if (isMainProcess()) {
            new Helper(getApplicationContext()).initializeAds();
        }

        setThemeEngine();
    }

    private boolean isMainProcess() {
        String processName = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            processName = android.app.Application.getProcessName();
        } else {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                int pid = android.os.Process.myPid();
                for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
                    if (info.pid == pid) {
                        processName = info.processName;
                        break;
                    }
                }
            }
        }
        return processName == null || processName.equals(getPackageName());
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
        // Show the app open ad only when:
        //   1. The open ad feature is enabled AND it hasn't been shown yet this session
        //   2. The user is NOT subscribed (subscribers are ad-free)
        //   3. The "ads on" toggle is enabled
        if (Boolean.TRUE.equals(Callback.getIsOpenAd())
                && Boolean.TRUE.equals(!Callback.getIsAppOpenAdShown())
                && !spHelper.getIsSubscribed()
                && spHelper.getIsAdOn()) {
            appOpenAdManager.showAdIfAvailable(currentActivity);
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        // this method is empty
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (Boolean.FALSE.equals(appOpenAdManager.getIsShowingAd())) {
            currentActivity = activity;
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        // this method is empty
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        // this method is empty
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        // this method is empty
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        // this method is empty
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        // this method is empty
    }

    public void loadAd(@NonNull Activity activity) {
        if (Boolean.TRUE.equals(Callback.getIsOpenAd() && spHelper != null)
                && (!spHelper.getIsSubscribed() || spHelper.getIsAdOn())) {
            appOpenAdManager.loadAd(activity);
        }
    }

    public AppOpenAdManager getAppOpenAdManager() {
        return appOpenAdManager;
    }

    private void setThemeEngine() {
        try {
            // Restore saved night mode on startup
            ThemeManager.applyTheme(getApplicationContext());
            // On first launch, auto-detect system dark mode
            if (Boolean.TRUE.equals(new SPHelper(getApplicationContext()).getIsFirst())) {
                ThemeManager tm = new ThemeManager(getApplicationContext());
                int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                    if (tm.getThemePage() != ThemeManager.THEME_DARK_GREY) {
                        tm.setThemeMode(true);
                        tm.setThemePage(ThemeManager.THEME_DARK_GREY);
                    }
                } else if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO
                        && tm.getThemePage() != ThemeManager.THEME_CLASSIC) {
                    tm.setThemeMode(false);
                    tm.setThemePage(ThemeManager.THEME_CLASSIC);
                }
            }
        } catch (Exception e) {
            Log.e("MyApplication", "Error set theme engine", e);
        }
    }

    /**
     * Creates the default FCM notification channel for Android 8.0+ (API 26+).
     * Without this, notifications are silently dropped on Oreo and above.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) return;

            // FCM push notifications
            NotificationChannel fcmChannel = new NotificationChannel(
                    "fcm_default_channel",
                    "Push Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            fcmChannel.setDescription("General push notifications from Ethio TV");
            manager.createNotificationChannel(fcmChannel);

            // Background media playback (Radio / Live TV / Events)
            NotificationChannel mediaChannel = new NotificationChannel(
                    com.ethionews.app.service.MediaPlaybackService.CHANNEL_ID,
                    getString(R.string.media_playback_channel_name),
                    NotificationManager.IMPORTANCE_LOW   // no sound for media controls
            );
            mediaChannel.setDescription(getString(R.string.media_playback_channel_desc));
            manager.createNotificationChannel(mediaChannel);

            // Media3's DefaultMediaNotificationProvider uses this hardcoded channel ID.
            // We create it here so the placeholder notification (posted in
            // MediaPlaybackService.onStartCommand before Media3 is ready) can use
            // the same channel and be seamlessly replaced by the full MediaStyle
            // notification with play/pause controls.
            NotificationChannel media3Channel = new NotificationChannel(
                    "default_channel_id",
                    getString(R.string.media_playback_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            media3Channel.setDescription(getString(R.string.media_playback_channel_desc));
            manager.createNotificationChannel(media3Channel);
        }
    }
}