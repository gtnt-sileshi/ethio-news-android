package com.ethionews.app.utils.advertising;

import android.annotation.SuppressLint;
import android.content.Context;

import com.startapp.sdk.adsbase.StartAppAd;

public class AdManagerInterStartApp {

    @SuppressLint("StaticFieldLeak")
    private static StartAppAd startAppAd;
    private final Context ctx;

    public AdManagerInterStartApp(Context ctx) {
        this.ctx = ctx;
    }

    public void createAd() {
        AdManagerInterStartApp.setAd(new StartAppAd(ctx));
        startAppAd.loadAd();
    }

    public StartAppAd getAd() {
        return startAppAd;
    }

    public static void setAd(StartAppAd startAppAdInter) {
        startAppAd = startAppAdInter;
    }
}