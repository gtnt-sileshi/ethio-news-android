package com.ethionews.app.executor;

import android.annotation.SuppressLint;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import com.ethionews.app.R;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.interfaces.HomeListener;
import com.ethionews.app.item.ItemBlogPost;
import com.ethionews.app.item.ItemCat;
import com.ethionews.app.item.ItemData;
import com.ethionews.app.item.ItemEvent;
import com.ethionews.app.item.ItemHomeSlider;
import com.ethionews.app.item.ItemPost;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.AsyncTaskExecutor;
import okhttp3.RequestBody;

/**
 * Loads the home screen payload from the blog API.
 *
 * Response shape:
 * {
 *   "slider":        [...banner objects],
 *   "latest_posts":  [...blog post objects],
 *   "home_sections": [
 *     { "home_id", "home_title", "home_type": "blog"|"live"|"category", "home_content": [...] }
 *   ]
 * }
 */
public class LoadHome extends AsyncTaskExecutor<String, String, String> {

    @SuppressLint("StaticFieldLeak")
    private final Context ctx;
    private final RequestBody requestBody;
    private final HomeListener homeListener;
    private final ArrayList<ItemPost> arrayListPost = new ArrayList<>();
    private String message = "";

    public LoadHome(Context ctx, HomeListener homeListener, RequestBody requestBody) {
        this.ctx          = ctx;
        this.homeListener = homeListener;
        this.requestBody  = requestBody;
    }

    @Override
    protected void onPreExecute() {
        homeListener.onStart();
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String strings) {
        try {
            String json     = ApplicationUtil.responsePost(Callback.API_URL, requestBody);
            JSONObject root = new JSONObject(json);

            // The blog API returns an object (not an array) under TAG_ROOT
            JSONObject payload;
            try {
                payload = root.getJSONObject(Callback.TAG_ROOT);
            } catch (Exception e) {
                // Fallback: error array response
                JSONArray arr = root.getJSONArray(Callback.TAG_ROOT);
                if (arr.length() > 0) {
                    message = arr.getJSONObject(0).optString(Callback.TAG_MSG, "");
                }
                return "1";
            }

            // ── Banner / featured slider ──────────────────────────────────
            if (payload.has("slider")) {
                JSONArray banners = payload.getJSONArray("slider");
                if (banners.length() > 0) {
                    ItemPost sliderPost = new ItemPost("", ctx.getString(R.string.nav_home), "slider", "banners");
                    ArrayList<ItemHomeSlider> sliderList = new ArrayList<>();
                    for (int i = 0; i < banners.length(); i++) {
                        JSONObject b = banners.getJSONObject(i);
                        String bannerImage = b.optString("banner_image", "").replace(" ", "%20");
                        if (bannerImage.isEmpty()) bannerImage = "null";
                        sliderList.add(new ItemHomeSlider(
                            b.optString("bid", ""),
                            b.optString("banner_title", ""),
                            b.optString("banner_info", ""),
                            bannerImage
                        ));
                    }
                    sliderPost.setArrayListBanner(sliderList);
                    arrayListPost.add(sliderPost);
                }
            }

            // ── Latest posts feed (primary blog content on home) ──────────
            if (payload.has("latest_posts")) {
                JSONArray latestArr = payload.getJSONArray("latest_posts");
                if (latestArr.length() > 0) {
                    ItemPost latestPost = new ItemPost("", ctx.getString(R.string.latest_posts), "blog", "latest");
                    ArrayList<ItemData> blogItems = new ArrayList<>();
                    for (int i = 0; i < latestArr.length(); i++) {
                        JSONObject p = latestArr.getJSONObject(i);
                        String img = p.optString("featured_image", "").replace(" ", "%20");
                        if (img.isEmpty()) img = "null";
                        blogItems.add(new ItemData(
                            p.optString("id", ""),
                            p.optString("title", ""),
                            img
                        ));
                    }
                    latestPost.setArrayListLive(blogItems);
                    arrayListPost.add(latestPost);
                }
            }

            // ── Home sections ─────────────────────────────────────────────
            if (payload.has("home_sections")) {
                JSONArray sections = payload.getJSONArray("home_sections");
                for (int j = 0; j < sections.length(); j++) {
                    JSONObject sec    = sections.getJSONObject(j);
                    String secId      = sec.optString("home_id", "");
                    String secTitle   = sec.optString("home_title", "");
                    String secType    = sec.optString("home_type", "blog");
                    JSONArray content = sec.getJSONArray("home_content");

                    ItemPost sectionPost = new ItemPost(secId, secTitle, secType, "sections");

                    switch (secType) {
                        case "category": {
                            ArrayList<ItemCat> cats = new ArrayList<>();
                            for (int i = 0; i < content.length(); i++) {
                                JSONObject c = content.getJSONObject(i);
                                String img = c.optString("category_image", "").replace(" ", "%20");
                                if (img.isEmpty()) img = "null";
                                cats.add(new ItemCat(c.optString("cid", ""), c.optString("category_name", ""), img));
                            }
                            sectionPost.setArrayListCategories(cats);
                            break;
                        }
                        case "event": {
                            ArrayList<ItemEvent> events = new ArrayList<>();
                            for (int i = 0; i < content.length(); i++) {
                                JSONObject e = content.getJSONObject(i);
                                String thumbOne = e.optString("team_one_thumbnail", "").replace(" ", "%20");
                                String thumbTwo = e.optString("team_two_thumbnail", "").replace(" ", "%20");
                                if (thumbOne.isEmpty()) thumbOne = "null";
                                if (thumbTwo.isEmpty()) thumbTwo = "null";
                                events.add(new ItemEvent(
                                    e.optString("id", ""), e.optString("post_id", ""),
                                    e.optString("event_title", ""), e.optString("event_time", ""),
                                    e.optString("event_date", ""),
                                    e.optString("team_title_one", ""), thumbOne,
                                    e.optString("team_title_two", ""), thumbTwo
                                ));
                            }
                            sectionPost.setArrayListEvent(events);
                            break;
                        }
                        case "live": {
                            ArrayList<ItemData> channels = new ArrayList<>();
                            for (int i = 0; i < content.length(); i++) {
                                JSONObject c = content.getJSONObject(i);
                                String img = c.optString("image", "").replace(" ", "%20");
                                if (img.isEmpty()) img = "null";
                                channels.add(new ItemData(
                                    c.optString("id", ""), c.optString("live_title", ""), img
                                ));
                            }
                            sectionPost.setArrayListLive(channels);
                            break;
                        }
                        default: {
                            // "blog" or any future type — map to ItemData using title + featured_image
                            ArrayList<ItemData> blogItems = new ArrayList<>();
                            for (int i = 0; i < content.length(); i++) {
                                JSONObject p = content.getJSONObject(i);
                                String img = p.optString("image", p.optString("featured_image", "")).replace(" ", "%20");
                                if (img.isEmpty()) img = "null";
                                String title = p.optString("title", p.optString("live_title", ""));
                                blogItems.add(new ItemData(p.optString("id", ""), title, img));
                            }
                            sectionPost.setArrayListLive(blogItems);
                            break;
                        }
                    }
                    arrayListPost.add(sectionPost);
                }
            }

            return "1";
        } catch (Exception e) {
            return "0";
        }
    }

    @Override
    protected void onPostExecute(String s) {
        homeListener.onEnd(s, message, arrayListPost);
    }
}
