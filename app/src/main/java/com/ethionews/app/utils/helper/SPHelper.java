package com.ethionews.app.utils.helper;

import android.content.Context;
import android.content.SharedPreferences;

import com.ethionews.app.utils.EncryptData;

public class SPHelper {

    private final EncryptData encryptData;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    private static final String TAG_FIRST_OPEN = "firstopen";
    private static final String TAG_IS_LOGGED = "islogged";
    private static final String TAG_UID = "uid";
    private static final String TAG_USERNAME = "name";
    private static final String TAG_EMAIL = "email";
    private static final String TAG_MOBILE = "mobile";
    private static final String TAG_GENDER = "gender";
    private static final String TAG_REMEMBER = "rem";
    private static final String TAG_PASSWORD = "pass";
    private static final String SHARED_PREF_AUTOLOGIN = "autologin";
    private static final String TAG_LOGIN_TYPE = "loginType";
    private static final String TAG_AUTH_ID = "auth_id";
    private static final String TAG_IMAGES = "profile";

    private static final String TAG_IS_SUPP_RTL = "is_rtl";
    private static final String TAG_IS_SUPP_MAINTENANCE = "is_maintenance";
    private static final String TAG_IS_SUPP_SCREEN = "is_screenshot";
    private static final String TAG_IS_SUPP_APK = "is_apk";
    private static final String TAG_IS_SUPP_VPN = "is_vpn";
    private static final String TAG_IS_LOGGED_IN = "is_login";
    private static final String TAG_IS_GOOGLE_LOGIN = "is_google";
    private static final String TAG_IS_REWARD_AD_WARNED = "reward_ad_credit";

    public SPHelper(Context ctx) {
        encryptData = new EncryptData(ctx);
        sharedPreferences = ctx.getSharedPreferences("setting_app", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void setIsFirst(Boolean flag) {
        editor.putBoolean(TAG_FIRST_OPEN, flag);
        editor.apply();
    }

    public Boolean getIsFirst() {
        return sharedPreferences.getBoolean(TAG_FIRST_OPEN, true);
    }

    public void setIsLogged(Boolean isLogged) {
        editor.putBoolean(TAG_IS_LOGGED, isLogged);
        editor.apply();
    }

    public boolean isLogged() {
        return sharedPreferences.getBoolean(TAG_IS_LOGGED, false);
    }

    public void setLoginDetails(String id, String name, String mobile, String email,
                                String gender, String profilePic, String authID,
                                Boolean isRemember, String password, String loginType) {
        editor.putBoolean(TAG_REMEMBER, isRemember);
        editor.putString(TAG_UID, encryptData.encrypt(id));
        editor.putString(TAG_USERNAME, encryptData.encrypt(name));
        editor.putString(TAG_MOBILE, encryptData.encrypt(mobile));
        editor.putString(TAG_EMAIL, encryptData.encrypt(email));
        editor.putString(TAG_GENDER, encryptData.encrypt(gender));
        editor.putString(TAG_PASSWORD, encryptData.encrypt(password));
        editor.putString(TAG_LOGIN_TYPE, encryptData.encrypt(loginType));
        editor.putString(TAG_AUTH_ID, encryptData.encrypt(authID));
        if (profilePic != null) {
            editor.putString(TAG_IMAGES, encryptData.encrypt(profilePic.replace(" ", "%20")));
        }
        editor.apply();
    }

    public void setRemember(Boolean isRemember) {
        editor.putBoolean(TAG_REMEMBER, isRemember);
        editor.putString(TAG_PASSWORD, "");
        editor.apply();
    }

    public String getUserId() {
        return encryptData.decrypt(sharedPreferences.getString(TAG_UID, ""));
    }

    public void setUserName(String userName) {
        editor.putString(TAG_USERNAME, encryptData.encrypt(userName));
        editor.apply();
    }

    public String getUserName() {
        return encryptData.decrypt(sharedPreferences.getString(TAG_USERNAME, ""));
    }

    public void setEmail(String email) {
        editor.putString(TAG_EMAIL, encryptData.encrypt(email));
        editor.apply();
    }

    public String getEmail() {
        return encryptData.decrypt(sharedPreferences.getString(TAG_EMAIL,""));
    }

    public void setUserMobile(String mobile) {
        editor.putString(TAG_MOBILE, encryptData.encrypt(mobile));
        editor.apply();
    }

    public String getUserMobile() {
        return encryptData.decrypt(sharedPreferences.getString(TAG_MOBILE, ""));
    }

    public String getPassword() {
        return encryptData.decrypt(sharedPreferences.getString(TAG_PASSWORD,""));
    }

    public Boolean getIsAutoLogin() { return sharedPreferences.getBoolean(SHARED_PREF_AUTOLOGIN, false); }

    public void setIsAutoLogin(Boolean isAutoLogin) {
        editor.putBoolean(SHARED_PREF_AUTOLOGIN, isAutoLogin);
        editor.apply();
    }

    public Boolean getIsRemember() {
        return sharedPreferences.getBoolean(TAG_REMEMBER, false);
    }


    public String getLoginType() {
        return encryptData.decrypt(sharedPreferences.getString(TAG_LOGIN_TYPE,""));
    }

    public String getAuthID() {
        return encryptData.decrypt(sharedPreferences.getString(TAG_AUTH_ID,""));
    }
    public String getProfileImages() {
        return encryptData.decrypt(sharedPreferences.getString(TAG_IMAGES,""));
    }
    public void setProfileImages(String profilePic) {
        if (profilePic != null) {
            editor.putString(TAG_IMAGES, encryptData.encrypt(profilePic.replace(" ", "%20")));
            editor.apply();
        }
    }

    public int getTextSize() {
        return sharedPreferences.getInt("text_size", 2);
    }

    public void setTextSize(int state) {
        editor.putInt("text_size", state);
        editor.apply();
    }

    public String getHLSVideoPlayer() {
        return encryptData.decrypt(sharedPreferences.getString("player",""));
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

    public boolean getIsSubscribed() {
        return sharedPreferences.getBoolean("isSubscribed", false);
    }

    public boolean getIsAdOn() {
        return sharedPreferences.getBoolean("isAds",true);
    }

    public void setIsSubscribed(Boolean isPurchases) {
        editor.putBoolean("isSubscribed", isPurchases);
        editor.apply();
    }

    public void setIsads(Boolean isAds) {
        editor.putBoolean("isAds", isAds);
        editor.apply();
    }

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
        int credit = getRewardCredit() + rewardCredit;
        editor.putInt(TAG_IS_REWARD_AD_WARNED, credit);
        editor.apply();
    }

    public void useRewardCredit(int rewardCredit) {
        int credit = getRewardCredit() - rewardCredit;
        editor.putInt(TAG_IS_REWARD_AD_WARNED, credit);
        editor.apply();
    }

    public void setIsSupported(Boolean isRtl, Boolean isMaintenance, Boolean isScreenshot,
                               Boolean isApk, Boolean isVpn, Boolean isLogin, Boolean isGoogleLogin) {
        editor.putBoolean(TAG_IS_SUPP_RTL, isRtl);
        editor.putBoolean(TAG_IS_SUPP_MAINTENANCE, isMaintenance);
        editor.putBoolean(TAG_IS_SUPP_SCREEN, isScreenshot);
        editor.putBoolean(TAG_IS_SUPP_APK, isApk);
        editor.putBoolean(TAG_IS_SUPP_VPN, isVpn);
        editor.putBoolean(TAG_IS_LOGGED_IN, isLogin);
        editor.putBoolean(TAG_IS_GOOGLE_LOGIN, isGoogleLogin);
        editor.apply();
    }

    public Boolean getIsRTL() {
        return sharedPreferences.getBoolean(TAG_IS_SUPP_RTL, false);
    }
    public Boolean getIsMaintenance() {
        return sharedPreferences.getBoolean(TAG_IS_SUPP_MAINTENANCE, false);
    }
    public Boolean getIsScreenshot() {
        return sharedPreferences.getBoolean(TAG_IS_SUPP_SCREEN, false);
    }
    public Boolean getIsAPK() {
        return sharedPreferences.getBoolean(TAG_IS_SUPP_APK, false);
    }
    public Boolean getIsVPN() {
        return sharedPreferences.getBoolean(TAG_IS_SUPP_VPN, false);
    }
    public Boolean getIsLogin(){
        return sharedPreferences.getBoolean(TAG_IS_LOGGED_IN, false);
    }
    public Boolean getIsGoogleLogin(){
        return sharedPreferences.getBoolean(TAG_IS_GOOGLE_LOGIN, false);
    }
}