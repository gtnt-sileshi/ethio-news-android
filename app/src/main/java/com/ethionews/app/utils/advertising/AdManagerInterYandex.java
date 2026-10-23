package com.ethionews.app.utils.advertising;

import android.content.Context;

import androidx.annotation.NonNull;

import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;

import com.ethionews.app.callback.Callback;

public class AdManagerInterYandex {

    private static InterstitialAd mInterstitialAd = null;
    private final Context ctx;

    public AdManagerInterYandex(Context ctx) {
        this.ctx = ctx;
    }

    public void createAd() {
        // SDK 8: AdRequestConfiguration removed — AdRequest now takes the ad unit ID directly.
        // setAdLoadListener() also removed — pass the listener directly to loadAd().
        AdRequest adRequest = new AdRequest.Builder(Callback.getYandexInterstitialAdID()).build();

        InterstitialAdLoader mInterstitialAdLoader = new InterstitialAdLoader(ctx);
        mInterstitialAdLoader.loadAd(adRequest, new InterstitialAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull final InterstitialAd interstitialAd) {
                AdManagerInterYandex.setAd(interstitialAd);
            }

            @Override
            public void onAdFailedToLoad(@NonNull final AdRequestError adRequestError) {
                // Ad failed to load
            }
        });
    }

    public InterstitialAd getAd() {
        return mInterstitialAd;
    }

    public static void setAd(InterstitialAd yandexInter) {
        mInterstitialAd = yandexInter;
    }
}
