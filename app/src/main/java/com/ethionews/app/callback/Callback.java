package com.ethionews.app.callback;

import java.io.Serial;
import java.io.Serializable;

import com.ethionews.app.BuildConfig;
import com.ethionews.app.item.ItemAbout;

public class Callback implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // API URL — points to Laravel backend
    public static final String API_URL = BuildConfig.BASE_URL+"api/app";

    // TAG_API
    public static final String TAG_ROOT = BuildConfig.API_NAME;
    public static final String TAG_SUCCESS = "success";
    public static final String TAG_MSG = "MSG";


    private static String searchItem = "";
    public static String getSearchItem() {
        return searchItem;
    }
    public static void setSearchItem(String data) {
        searchItem = data;
    }

    // About Details -------------------------------------------------------------------------------
    private static ItemAbout itemAbout = new ItemAbout("", "", "", "", "", "", "");
    public static ItemAbout getItemAbout() {
        return itemAbout;
    }
    public static void setItemAbout(ItemAbout item) {
        itemAbout = item;
    }

    private static boolean isRecreate = false;
    public static boolean isRecreate() {
        return isRecreate;
    }
    public static void setRecreate(boolean recreate) {
        isRecreate = recreate;
    }

    private static Boolean isProfileUpdate = false;
    public static Boolean getIsProfileUpdate() {
        return isProfileUpdate;
    }
    public static void setIsProfileUpdate(Boolean profileUpdate) {
        isProfileUpdate = profileUpdate;
    }

    private static int recentLimit = 10;
    public static int getRecentLimit() {
        return recentLimit;
    }
    public static void setRecentLimit(int limit) {
        recentLimit = limit;
    }

    // Update and dialog ---------------------------------------------------------------------------
    public static final String DIALOG_TYPE_UPDATE = "upgrade";
    public static final String DIALOG_TYPE_MAINTENANCE = "maintenance";
    public static final String DIALOG_TYPE_DEVELOPER = "developer";
    public static final String DIALOG_TYPE_VPN = "vpn";

    private static Boolean isAppUpdate = false;
    public static Boolean getIsAppUpdate() {
        return isAppUpdate;
    }
    public static void setIsAppUpdate(Boolean appUpdate) {
        isAppUpdate = appUpdate;
    }

    private static int appNewVersion = 1;
    public static int getAppNewVersion() {
        return appNewVersion;
    }
    public static void setAppNewVersion(int newVersion) {
        appNewVersion = newVersion;
    }

    private static String appUpdateDesc = "";
    public static String getAppUpdateDesc() {
        return appUpdateDesc;
    }
    public static void setAppUpdateDesc(String updateDesc) {
        appUpdateDesc = updateDesc;
    }

    private static String appRedirectUrl = "";
    public static String getAppRedirectUrl() {
        return appRedirectUrl;
    }
    public static void setAppRedirectUrl(String redirectUrl) {
        appRedirectUrl = redirectUrl;
    }

    // Advertising ---------------------------------------------------------------------------------
    public static final String PAGE_HOME = "banner_home";
    public static final String PAGE_POST_DETAILS = "post_details";
    public static final String PAGE_CAT_DETAILS = "category_details";
    public static final String PAGE_SEARCH = "search_page";
    public static final String PAGE_NATIVE_POST = "post_native";
    public static final String PAGE_NATIVE_CAT = "category_native";

    public static final String AD_TYPE_ADMOB = "admob";
    public static final String AD_TYPE_STARTAPP = "startapp";
    public static final String AD_TYPE_UNITY = "unity";
    public static final String AD_TYPE_APPLOVIN = "applovin";
    public static final String AD_TYPE_IRONSOURCE = "ironsource";
    public static final String AD_TYPE_META = "meta";
    public static final String AD_TYPE_YANDEX = "yandex";
    public static final String AD_TYPE_WORTISE = "wortise";

    private static String adNetwork = AD_TYPE_ADMOB;
    public static String getAdNetwork() {
        return adNetwork;
    }
    public static void setAdNetwork(String network) {
        adNetwork = network;
    }

    private static Boolean isAdsStatus = false;
    public static Boolean getIsAdsStatus() {
        return isAdsStatus;
    }
    public static void setIsAdsStatus(Boolean adsStatus) {
        isAdsStatus = adsStatus;
    }

    private static int adCount = 0;
    public static int getAdCount() {
        return adCount;
    }
    public static void setAdCount(int count) {
        adCount = count;
    }

    private static int interstitialAdShow = 5;
    public static int getInterstitialAdShow() {
        return interstitialAdShow;
    }
    public static void setInterstitialAdShow(int count) {
        interstitialAdShow = count;
    }

    private static int nativeAdShow = 6;
    public static int getNativeAdShow() {
        return nativeAdShow;
    }
    public static void setNativeAdShow(int count) {
        nativeAdShow = count;
    }

    private static int rewardCredit = 5;
    public static int getRewardCredit() {
        return rewardCredit;
    }
    public static void setRewardCredit(int count) {
        rewardCredit = count;
    }

    // ADMOB ---------------------------------------------------------------------------------------
    private static String admobPublisherID = "";
    public static String getAdmobPublisherID() {
        return admobPublisherID;
    }
    public static void setAdmobPublisherID(String publisherID) {
        admobPublisherID = publisherID;
    }

    private static String admobBannerAdID = "";
    public static String getAdmobBannerAdID() {
        return admobBannerAdID;
    }
    public static void setAdmobBannerAdID(String bannerAdID) {
        admobBannerAdID = bannerAdID;
    }

    private static String admobInterstitialAdID = "";
    public static String getAdmobInterstitialAdID() {
        return admobInterstitialAdID;
    }
    public static void setAdmobInterstitialAdID(String interstitialAdID) {
        admobInterstitialAdID = interstitialAdID;
    }

    private static String admobRewardAdID = "";
    public static String getAdmobRewardAdID() {
        return admobRewardAdID;
    }
    public static void setAdmobRewardAdID(String rewardAdID) {
        admobRewardAdID = rewardAdID;
    }

    private static String admobNativeAdID = "";
    public static String getAdmobNativeAdID() {
        return admobNativeAdID;
    }
    public static void setAdmobNativeAdID(String nativeAdID) {
        admobNativeAdID = nativeAdID;
    }

    private static String admobOpenAdID = "";
    public static String getAdmobOpenAdID() {
        return admobOpenAdID;
    }
    public static void setAdmobOpenAdID(String openAdID) {
        admobOpenAdID = openAdID;
    }

    // STARTAPP ------------------------------------------------------------------------------------
    private static String startappAppID = "";
    public static String getStartappAppID() {
        return startappAppID;
    }
    public static void setStartappAppID(String appID) {
        startappAppID = appID;
    }

    // UNITY ---------------------------------------------------------------------------------------
    private static String unityGameID = "";
    public static String getUnityGameID() {
        return unityGameID;
    }
    public static void setUnityGameID(String gameID) {
        unityGameID = gameID;
    }

    private static String unityBannerAdID = "";
    public static String getUnityBannerAdID() {
        return unityBannerAdID;
    }
    public static void setUnityBannerAdID(String bannerAdID) {
        unityBannerAdID = bannerAdID;
    }

    private static String unityInterstitialAdID = "";
    public static String getUnityInterstitialAdID() {
        return unityInterstitialAdID;
    }
    public static void setUnityInterstitialAdID(String interstitialAdID) {
        unityInterstitialAdID = interstitialAdID;
    }

    private static String unityRewardAdID = "";
    public static String getUnityRewardAdID() {
        return unityRewardAdID;
    }
    public static void setUnityRewardAdID(String rewardAdID) {
        unityRewardAdID = rewardAdID;
    }

    // IRONSOURCE ----------------------------------------------------------------------------------
    private static String ironsourceAppKey = "";
    public static String getIronsourceAppKey() {
        return ironsourceAppKey;
    }
    public static void setIronsourceAppKey(String appKey) {
        ironsourceAppKey = appKey;
    }

    // WORTISE -------------------------------------------------------------------------------------
    private static String wortiseAppID = "";
    public static String getWortiseAppID() {
        return wortiseAppID;
    }
    public static void setWortiseAppID(String appID) {
        wortiseAppID = appID;
    }

    private static String wortiseBannerAdID = "";
    public static String getWortiseBannerAdID() {
        return wortiseBannerAdID;
    }
    public static void setWortiseBannerAdID(String bannerAdID) {
        wortiseBannerAdID = bannerAdID;
    }

    private static String wortiseInterstitialAdID = "";
    public static String getWortiseInterstitialAdID() {
        return wortiseInterstitialAdID;
    }
    public static void setWortiseInterstitialAdID(String interstitialAdID) {
        wortiseInterstitialAdID = interstitialAdID;
    }

    private static String wortiseNativeAdID = "";
    public static String getWortiseNativeAdID() {
        return wortiseNativeAdID;
    }
    public static void setWortiseNativeAdID(String nativeAdID) {
        wortiseNativeAdID = nativeAdID;
    }

    private static String wortiseOpenAdID = "";
    public static String getWortiseOpenAdID() {
        return wortiseOpenAdID;
    }
    public static void setWortiseOpenAdID(String openAdID) {
        wortiseOpenAdID = openAdID;
    }

    private static String wortiseRewardAdID = "";
    public static String getWortiseRewardAdID() {
        return wortiseRewardAdID;
    }
    public static void setWortiseRewardAdID(String rewardAdID) {
        wortiseRewardAdID = rewardAdID;
    }

    //APPLOVIN -------------------------------------------------------------------------------------
    private static String applovinBannerAdID = "";
    public static String getApplovinBannerAdID() {
        return applovinBannerAdID;
    }
    public static void setApplovinBannerAdID(String bannerAdID) {
        applovinBannerAdID = bannerAdID;
    }

    private static String applovinInterstitialAdID = "";
    public static String getApplovinInterstitialAdID() {
        return applovinInterstitialAdID;
    }
    public static void setApplovinInterstitialAdID(String interstitialAdID) {
        applovinInterstitialAdID = interstitialAdID;
    }

    private static String applovinNativeAdID = "";
    public static String getApplovinNativeAdID() {
        return applovinNativeAdID;
    }
    public static void setApplovinNativeAdID(String nativeAdID) {
        applovinNativeAdID = nativeAdID;
    }

    private static String applovinOpenAdID = "";
    public static String getApplovinOpenAdID() {
        return applovinOpenAdID;
    }
    public static void setApplovinOpenAdID(String openAdID) {
        applovinOpenAdID = openAdID;
    }

    private static String applovinRewardAdID = "";
    public static String getApplovinRewardAdID() {
        return applovinRewardAdID;
    }
    public static void setApplovinRewardAdID(String rewardAdID) {
        applovinRewardAdID = rewardAdID;
    }

    // YANDEX --------------------------------------------------------------------------------------
    private static String yandexBannerAdID = "";
    public static String getYandexBannerAdID() {
        return yandexBannerAdID;
    }
    public static void setYandexBannerAdID(String bannerAdID) {
        yandexBannerAdID = bannerAdID;
    }

    private static String yandexInterstitialAdID = "";
    public static String getYandexInterstitialAdID() {
        return yandexInterstitialAdID;
    }
    public static void setYandexInterstitialAdID(String interstitialAdID) {
        yandexInterstitialAdID = interstitialAdID;
    }

    private static String yandexNativeAdID = "";
    public static String getYandexNativeAdID() {
        return yandexNativeAdID;
    }
    public static void setYandexNativeAdID(String nativeAdID) {
        yandexNativeAdID = nativeAdID;
    }

    private static String yandexOpenAdID = "";
    public static String getYandexOpenAdID() {
        return yandexOpenAdID;
    }
    public static void setYandexOpenAdID(String openAdID) {
        yandexOpenAdID = openAdID;
    }

    //Advertising isSupported ----------------------------------------------------------------------
    private static Boolean isOpenAd = false;
    public static Boolean getIsOpenAd() {
        return isOpenAd;
    }
    public static void setIsOpenAd(Boolean openAd) {
        isOpenAd = openAd;
    }

    private static Boolean isAppOpenAdShown = false;
    public static Boolean getIsAppOpenAdShown() {
        return isAppOpenAdShown;
    }
    public static void setIsAppOpenAdShown(Boolean appOpenAdShown) {
        isAppOpenAdShown = appOpenAdShown;
    }

    private static Boolean isBannerAdHome = false;
    public static Boolean getIsBannerAdHome() {
        return isBannerAdHome;
    }
    public static void setIsBannerAdHome(Boolean bannerAdHome) {
        isBannerAdHome = bannerAdHome;
    }

    private static Boolean isBannerAdPostDetails = false;
    public static Boolean getIsBannerAdPostDetails() {
        return isBannerAdPostDetails;
    }
    public static void setIsBannerAdPostDetails(Boolean bannerAdPostDetails) {
        isBannerAdPostDetails = bannerAdPostDetails;
    }

    private static Boolean isBannerAdCatDetails = false;
    public static Boolean getIsBannerAdCatDetails() {
        return isBannerAdCatDetails;
    }
    public static void setIsBannerAdCatDetails(Boolean bannerAdCatDetails) {
        isBannerAdCatDetails = bannerAdCatDetails;
    }

    private static Boolean isBannerAdSearch = false;
    public static Boolean getIsBannerAdSearch() {
        return isBannerAdSearch;
    }
    public static void setIsBannerAdSearch(Boolean bannerAdSearch) {
        isBannerAdSearch = bannerAdSearch;
    }

    private static Boolean isInterAd = false;
    public static Boolean getIsInterAd() {
        return isInterAd;
    }
    public static void setIsInterAd(Boolean interAd) {
        isInterAd = interAd;
    }

    private static Boolean isNativeAdPost = false;
    public static Boolean getIsNativeAdPost() {
        return isNativeAdPost;
    }
    public static void setIsNativeAdPost(Boolean nativeAdPost) {
        isNativeAdPost = nativeAdPost;
    }

    private static Boolean isNativeAdCat = false;
    public static Boolean getIsNativeAdCat() {
        return isNativeAdCat;
    }
    public static void setIsNativeAdCat(Boolean nativeAdCat) {
        isNativeAdCat = nativeAdCat;
    }

    private static Boolean isRewardAd = false;
    public static Boolean getIsRewardAd() {
        return isRewardAd;
    }
    public static void setIsRewardAd(Boolean rewardAd) {
        isRewardAd = rewardAd;
    }

}