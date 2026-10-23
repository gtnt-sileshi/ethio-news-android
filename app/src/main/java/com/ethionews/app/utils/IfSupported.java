package com.ethionews.app.utils;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import com.ethionews.app.utils.ThemeManager;
import com.ethionews.app.utils.helper.SPHelper;


public class IfSupported {

    private static final String TAG = "IfSupported";

    private IfSupported() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Apply the current theme variant (Classic/Dark/DarkGrey/DarkBlue).
     * Must be called before setContentView() in each Activity.
     */
    public static void applyTheme(Activity activity) {
        activity.setTheme(ThemeManager.getThemeStyleRes(activity));
    }

    public static void isRTL(Activity mContext) {
        try {
            if (Boolean.TRUE.equals(new SPHelper(mContext).getIsRTL())) {
                Window window = mContext.getWindow();
                window.getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to isRTL", e);
        }
    }

    public static void isScreenshot(Activity mContext) {
        try {
            if (Boolean.TRUE.equals(new SPHelper(mContext).getIsScreenshot())) {
                Window window = mContext.getWindow();
                window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to isScreenshot", e);
        }
    }

    public static void hideStatusBar(Activity mContext) {
        if (mContext == null) {
            Log.e(TAG, "Activity context is null");
            return;
        }
        try {
            Window window = mContext.getWindow();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);
                WindowInsetsController controller = window.getDecorView().getWindowInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                View decorView = window.getDecorView();
                //noinspection deprecation
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to hide status bar", e);
        }
    }

    public static void keepScreenOn(Activity mContext) {
        if (mContext == null) {
            Log.e(TAG, "Activity context is null");
            return;
        }
        try {
            Window window = mContext.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception e) {
            Log.e(TAG, "Failed to keep screen on", e);
        }
    }
}
