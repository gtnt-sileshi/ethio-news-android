package com.ethionews.app.utils.advertising;

import android.content.Context;

import com.applovin.mediation.ads.MaxRewardedAd;

import com.ethionews.app.callback.Callback;

public class RewardAdApplovin {

    private static MaxRewardedAd maxRewardedAd;
    private final Context ctx;

    public RewardAdApplovin(Context ctx) {
        this.ctx = ctx;
    }

    public void createAd() {
        RewardAdApplovin.setAd(MaxRewardedAd.getInstance(Callback.getApplovinRewardAdID(), ctx));
        maxRewardedAd.loadAd();
    }

    public static MaxRewardedAd getAd() {
        return maxRewardedAd;
    }

    public static void setAd(MaxRewardedAd rewardedAd) {
        maxRewardedAd = rewardedAd;
    }
}