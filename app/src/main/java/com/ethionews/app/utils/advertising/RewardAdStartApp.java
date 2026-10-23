package com.ethionews.app.utils.advertising;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

public class RewardAdStartApp {

    @SuppressLint("StaticFieldLeak")
    private static StartAppAd startAppAd;
    private final Context ctx;

    public RewardAdStartApp(Context ctx) {
        this.ctx = ctx;
    }

    public void createAd() {
        RewardAdStartApp.setAd(new StartAppAd(ctx));
        startAppAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new AdEventListener() {
            @Override
            public void onReceiveAd(@NonNull Ad ad) {
                // this method is empty
            }

            @Override
            public void onFailedToReceiveAd(@Nullable Ad ad) {
                // this method is empty
            }
        });
    }

    public StartAppAd getAd() {
        return startAppAd;
    }

    public static void setAd(StartAppAd startAppAdInter) {
        startAppAd = startAppAdInter;
    }
}