package com.ethionews.app.utils.helper;

import android.content.Context;
import android.content.SharedPreferences;

import com.ethionews.app.utils.EncryptData;

/**
 * SharedPreferences wrapper for the blog app.
 * User login, subscription, and profile fields have been removed.
 * Only app-level flags (theme, RTL, maintenance, etc.) and UI preferences remain.
 */
public class SPHelper {

    private final EncryptData encryptData;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    // App feature flags (persisted from app_details response)
    private static final String TAG_IS_SUPP_RTL         = "is_rtl";
    private static final String TAG_IS_SUPP_MAINTENANCE = "is_maintenance";
    private static final String TAG_IS_SUPP_SCREEN      = "is_screenshot";
    private static final String TAG_IS_SUPP_APK         = "is_apk";
    private static final String TAG_IS_SUPP_VPN         = "is_vpn";

    // Reward ad credit (used by ad system)
    private static final String TAG_IS_REWARD_AD_WARNED = "reward_ad_credit";

    public SPHelper(Context ctx) {
        encryptData   = new EncryptData(ctx);
        sharedPreferences = ctx.getSharedPreferences("setting_app", Context.MODE_PRIVATE);
        editor        = sharedPreferences.edit();
    }

    // ── First-open flag ────────────────────────────────────────────
    public void setIsFirst(Boolean flag) {
        editor.putBoolean("firstopen", flag);
        editor.apply();
    }
    public Boolean getIsFirst() {
        return sharedPreferences.getBoolean("firstopen", true);
    }

    // ── App feature flags ─────────────────────────────────────────
    public void setIsSupported(Boolean isRtl, Boolean isMaintenance,
                               Boolean isScreenshot, Boolean isApk, Boolean isVpn) {
        editor.putBoolean(TAG_IS_SUPP_RTL, isRtl);
        editor.putBoolean(TAG_IS_SUPP_MAINTENANCE, isMaintenance);
        editor.putBoolean(TAG_IS_SUPP_SCREEN, isScreenshot);
        editor.putBoolean(TAG_IS_SUPP_APK, isApk);
        editor.putBoolean(TAG_IS_SUPP_VPN, isVpn);
        editor.apply();
    }

    public Boolean getIsRTL()         { return sharedPreferences.getBoolean(TAG_IS_SUPP_RTL, false); }
    public Boolean getIsMaintenance() { return sharedPreferences.getBoolean(TAG_IS_SUPP_MAINTENANCE, false); }
    public Boolean getIsScreenshot()  { return sharedPreferences.getBoolean(TAG_IS_SUPP_SCREEN, false); }
    public Boolean getIsAPK()         { return sharedPreferences.getBoolean(TAG_IS_SUPP_APK, false); }
    public Boolean getIsVPN()         { return sharedPreferences.getBoolean(TAG_IS_SUPP_VPN, false); }

    // ── UI preferences ────────────────────────────────────────────
    public int getTextSize() {
        return sharedPreferences.getInt("text_size", 2);
    }
    public void setTextSize(int state) {
        editor.putInt("text_size", state);
        editor.apply();
    }

    public String getHLSVideoPlayer() {
        return encryptData.decrypt(sharedPreferences.getString("player", ""));
    }
    public void setHLSVideoPlayer(String applicationID) {
        editor.putString("player", encryptData.encrypt(applicationID));
        editor.apply();
    }

    public Boolean getGridSimilar() {
        return sharedPreferences.getBoolean("grid_similar", false);
    }
    public void setGridSimilar(Boolean state) {
        editor.putBoolean("grid_similar", state);
        editor.apply();
    }

    // ── Reward ad credit (ad system) ──────────────────────────────
    public Boolean getIsRewardAdWarned() {
        return sharedPreferences.getBoolean("is_reward_ad_warned", false);
    }
    public void setIsRewardAdWarned(Boolean isRewardAdWarned) {
        editor.putBoolean("is_reward_ad_warned", isRewardAdWarned);
        editor.apply();
    }

    public int getRewardCredit() {
        return sharedPreferences.getInt(TAG_IS_REWARD_AD_WARNED, 0);
    }
    public void addRewardCredit(int rewardCredit) {
        editor.putInt(TAG_IS_REWARD_AD_WARNED, getRewardCredit() + rewardCredit);
        editor.apply();
    }
    public void useRewardCredit(int rewardCredit) {
        editor.putInt(TAG_IS_REWARD_AD_WARNED, getRewardCredit() - rewardCredit);
        editor.apply();
    }

    // ── Ads (no subscription — ads always apply) ──────────────────
    public boolean getIsAdOn() {
        return sharedPreferences.getBoolean("isAds", true);
    }
    public void setIsads(Boolean isAds) {
        editor.putBoolean("isAds", isAds);
        editor.apply();
    }
}
