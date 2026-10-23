package com.ethionews.app.utils.advertising;

import android.content.Context;

import com.applovin.mediation.ads.MaxInterstitialAd;

import com.ethionews.app.callback.Callback;


public class AdManagerInterApplovin {

    private static MaxInterstitialAd interstitialAd;
    private final Context ctx;

    public AdManagerInterApplovin(Context ctx) {
        this.ctx = ctx;
    }

    public void createAd() {
        AdManagerInterApplovin.setAd(new MaxInterstitialAd(Callback.getApplovinInterstitialAdID(), ctx));
        interstitialAd.loadAd();
    }

    public MaxInterstitialAd getAd() {
        return interstitialAd;
    }

    public static void setAd(MaxInterstitialAd appLovingInter) {
        interstitialAd = appLovingInter;
    }
}