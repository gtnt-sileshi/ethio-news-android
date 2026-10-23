package com.ethionews.app.utils.advertising;

import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.UnityAds;

import com.ethionews.app.callback.Callback;

public class AdManagerInterUnity {

    private static boolean isAdLoaded = false;

    public AdManagerInterUnity() {
        // no
    }

    public void createAd() {
        UnityAds.load(Callback.getUnityInterstitialAdID(), new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                AdManagerInterUnity.setAdLoaded(true);
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                AdManagerInterUnity.setAdLoaded(false);
            }
        });
    }

    private static void setAdLoaded(boolean isLoaded) {
        isAdLoaded = isLoaded;
    }

    public static boolean getAd() {
        return isAdLoaded;
    }

    public static void setAd() {
        isAdLoaded = false;
    }
}