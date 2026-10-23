package com.ethionews.app.utils.advertising;

import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.UnityAds;

import com.ethionews.app.callback.Callback;

public class RewardAdUnity {

    public static boolean isAdLoaded = false;

    public RewardAdUnity() {
        // this constructor is empty
    }

    public void createAd() {
        UnityAds.load(Callback.getUnityRewardAdID(), new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                RewardAdUnity.setAd(true);
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                RewardAdUnity.setAd(false);
            }
        });
    }

    public boolean getAd() {
        return isAdLoaded;
    }

    public static void setAd(boolean isLoaded) {
        isAdLoaded = isLoaded;
    }
}