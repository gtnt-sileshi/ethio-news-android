package com.ethionews.app.callback;

public class Method {

    private Method() {
        throw new IllegalStateException("Utility class");
    }

    public static final String LOGIN_TYPE_NORMAL = "Normal";
    public static final String LOGIN_TYPE_GOOGLE = "Google";

    public static final String METHOD_APP_DETAILS = "app_details";
    public static final String METHOD_LOGIN = "user_login";
    public static final String METHOD_REGISTER = "user_register";
    public static final String METHOD_PROFILE = "user_profile";
    public static final String METHOD_ACCOUNT_DELETE = "account_delete";
    public static final String METHOD_EDIT_PROFILE = "edit_profile";
    public static final String METHOD_USER_IMAGES_UPDATE = "user_images_update";
    public static final String METHOD_FORGOT_PASSWORD = "forgot_pass";
    public static final String METHOD_NOTIFICATION = "get_notification";
    public static final String METHOD_REMOVE_NOTIFICATION = "remove_notification";
    public static final String METHOD_DO_FAV = "favourite_post";
    public static final String METHOD_SEARCH_LIVE = "get_search_live";
    public static final String METHOD_SEARCH = "get_search";

    public static final String METHOD_REPORT = "post_report";
    public static final String METHOD_GET_RATINGS = "get_rating";
    public static final String METHOD_RATINGS = "post_rating";
    public static final String METHOD_SUGGESTION = "post_suggest";
    public static final String METHOD_LIVE_ID = "get_live_id";
    public static final String METHOD_CAT_ID = "get_cat_by";
    public static final String METHOD_POST_BY_FAV = "get_favourite";
    public static final String METHOD_POST_BY_BANNER = "get_banner_by";
    public static final String METHOD_EVENT = "get_event";

    public static final String METHOD_PLAN = "subscription_list";
    public static final String TRANSACTION_URL = "transaction";

    public static final String METHOD_HOME = "get_home";
    public static final String METHOD_HOME_DETAILS = "home_collections";
    public static final String METHOD_LATEST = "get_latest";
    public static final String METHOD_MOST_VIEWED = "get_trending";
    public static final String METHOD_CAT = "cat_list";
    public static final String METHOD_LIVE_RECENT = "get_recent";

    // IP Radio
    public static final String METHOD_RADIO_LIST   = "get_radio_list";
    public static final String METHOD_RADIO_BY_CAT = "get_radio_by_cat";
    public static final String METHOD_RADIO_ID     = "get_radio_id";
    public static final String METHOD_RADIO_FAV    = "get_radio_favourite";
    public static final String METHOD_SEARCH_RADIO = "get_search_radio";

    // FCM token registration
    public static final String METHOD_UPDATE_FCM_TOKEN = "update_fcm_token";

    // Email verification
    public static final String METHOD_VERIFY_EMAIL = "verify_email";
    public static final String METHOD_RESEND_OTP   = "resend_otp";
}
