package com.ethionews.app.callback;

public class Method {

    private Method() {
        throw new IllegalStateException("Utility class");
    }

    // ── App details ────────────────────────────────────────────────
    public static final String METHOD_APP_DETAILS = "app_details";

    // ── Blog — core feature ────────────────────────────────────────
    public static final String METHOD_BLOG_POSTS        = "get_blog_posts";
    public static final String METHOD_BLOG_POST         = "get_blog_post";
    public static final String METHOD_BLOG_BY_CAT       = "get_blog_by_cat";
    public static final String METHOD_BLOG_FEATURED     = "get_blog_featured";
    public static final String METHOD_BLOG_TRENDING     = "get_blog_trending";
    public static final String METHOD_BLOG_SEARCH       = "get_blog_search";
    public static final String METHOD_BLOG_TAGS         = "get_blog_tags";
    public static final String METHOD_BLOG_BY_TAG       = "get_blog_by_tag";
    public static final String METHOD_BLOG_RECENT       = "get_recent_posts";
    public static final String METHOD_BLOG_COMMENTS     = "get_blog_comments";
    public static final String METHOD_BLOG_COMMENT_POST = "post_blog_comment";
    public static final String METHOD_SAVE_POST         = "save_post";
    public static final String METHOD_SAVED_POSTS       = "get_saved_posts";

    // ── Home & navigation ─────────────────────────────────────────
    public static final String METHOD_HOME         = "get_home";
    public static final String METHOD_HOME_DETAILS = "home_collections";
    public static final String METHOD_CAT          = "cat_list";

    // category_type filter values passed as search_type in METHOD_CAT
    public static final String CAT_TYPE_ALL   = "all";
    public static final String CAT_TYPE_BLOG  = "blog";
    public static final String CAT_TYPE_LIVE  = "live";
    public static final String CAT_TYPE_RADIO = "radio";

    // ── Live TV (supplementary) ────────────────────────────────────
    public static final String METHOD_LATEST         = "get_latest";
    public static final String METHOD_MOST_VIEWED    = "get_trending";
    public static final String METHOD_CAT_ID         = "get_cat_by";
    public static final String METHOD_LIVE_ID        = "get_live_id";
    public static final String METHOD_SEARCH_LIVE    = "get_search_live";
    public static final String METHOD_POST_BY_BANNER = "get_banner_by";

    // ── Events (supplementary) ────────────────────────────────────
    public static final String METHOD_EVENT = "get_event";

    // ── Radio (supplementary) ─────────────────────────────────────
    public static final String METHOD_RADIO_LIST   = "get_radio_list";
    public static final String METHOD_RADIO_BY_CAT = "get_radio_by_cat";
    public static final String METHOD_RADIO_ID     = "get_radio_id";
    public static final String METHOD_RADIO_FAV    = "get_radio_favourite";
    public static final String METHOD_SEARCH_RADIO = "get_search_radio";

    // ── Ratings, reports, suggestions (device-based) ──────────────
    public static final String METHOD_GET_RATINGS = "get_rating";
    public static final String METHOD_RATINGS     = "post_rating";
    public static final String METHOD_REPORT      = "post_report";
    public static final String METHOD_SUGGESTION  = "post_suggest";

    // Favourites (device-based)
    public static final String METHOD_LIVE_FAV        = "toggle_live_favourite";
    public static final String METHOD_LIVE_FAV_LIST   = "get_live_favourite";
    public static final String METHOD_RADIO_FAV_TOG   = "toggle_radio_favourite";
}
