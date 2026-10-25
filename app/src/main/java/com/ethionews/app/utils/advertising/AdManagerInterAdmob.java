package com.ethionews.app.utils.advertising;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.ads.mediation.facebook.FacebookMediationAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import com.ethionews.app.callback.Callback;


public class AdManagerInterAdmob {

    private static InterstitialAd interAd;
    private final Context ctx;

    public AdManagerInterAdmob(Context ctx) {
        this.ctx = ctx;
    }

    public void createAd() {
        AdRequest adRequest;
        if (Callback.getAdNetwork().equals(Callback.AD_TYPE_META)) {
            // For Meta (Facebook) mediation, signal the adapter via FacebookMediationAdapter extras.
            // Do NOT set npa=1 here — Meta handles consent independently.
            adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, new Bundle())
                    .addNetworkExtrasBundle(FacebookMediationAdapter.class, new Bundle())
                    .build();
        } else {
            adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, new Bundle())
                    .build();
        }

        InterstitialAd.load(ctx, Callback.getAdmobInterstitialAdID(), adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                AdManagerInterAdmob.setAd(interstitialAd);
            }
        });
    }

    public InterstitialAd getAd() {
        return interAd;
    }

    public static void setAd(InterstitialAd interstitialAd) {
        interAd = interstitialAd;
    }
}