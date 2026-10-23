package com.ethionews.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Stores a theme page index (0=Classic/Light, 1=Dark, 2=DarkGrey, 3=DarkBlue)
 * and maps it to AppCompatDelegate night mode flags.
 */
public class ThemeManager {

    private static final String PREFS = "theme_prefs";
    private static final String KEY_PAGE  = "theme_page";
    private static final String KEY_MODE  = "theme_mode";

    public static final int THEME_CLASSIC   = 0;
    public static final int THEME_DARK      = 1;
    public static final int THEME_DARK_GREY = 2;
    public static final int THEME_DARK_BLUE = 3;

    private final SharedPreferences prefs;

    public ThemeManager(Context ctx) {
        prefs = ctx.getApplicationContext()
                   .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getThemePage() {
        return prefs.getInt(KEY_PAGE, THEME_CLASSIC);
    }

    public boolean getIsThemeMode() {
        return prefs.getBoolean(KEY_MODE, false);
    }

    public void setThemePage(int page) {
        prefs.edit().putInt(KEY_PAGE, page).apply();
    }

    public void setThemeMode(boolean isDark) {
        prefs.edit().putBoolean(KEY_MODE, isDark).apply();
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES
                       : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /** Call once in Application.onCreate() to restore the saved night mode. */
    public static void applyTheme(Context ctx) {
        ThemeManager tm = new ThemeManager(ctx);
        AppCompatDelegate.setDefaultNightMode(
                tm.getIsThemeMode() ? AppCompatDelegate.MODE_NIGHT_YES
                                    : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /** Returns the theme style resource for the current theme page. */
    public static int getThemeStyleRes(Context ctx) {
        int page = new ThemeManager(ctx).getThemePage();
        switch (page) {
            case THEME_DARK:      return com.ethionews.app.R.style.AppTheme_Dark;
            case THEME_DARK_GREY: return com.ethionews.app.R.style.AppTheme_DarkGrey;
            case THEME_DARK_BLUE: return com.ethionews.app.R.style.AppTheme_DarkBlue;
            default:              return com.ethionews.app.R.style.AppTheme;
        }
    }
}
