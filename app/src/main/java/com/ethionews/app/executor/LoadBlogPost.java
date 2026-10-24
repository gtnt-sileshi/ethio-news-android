package com.ethionews.app.executor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import com.ethionews.app.callback.Callback;
import com.ethionews.app.interfaces.BlogPostDetailListener;
import com.ethionews.app.item.ItemBlogPost;
import com.ethionews.app.item.ItemBlogTag;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.AsyncTaskExecutor;
import okhttp3.RequestBody;

/**
 * Loads a single blog post detail. Response is a JSON object with
 * "post" (detail) and "related" (list) fields.
 */
public class LoadBlogPost extends AsyncTaskExecutor<String, String, String> {

    private final BlogPostDetailListener listener;
    private final RequestBody requestBody;
    private ItemBlogPost post;
    private String verifyStatus = "0";
    private String message = "";

    public LoadBlogPost(BlogPostDetailListener listener, RequestBody requestBody) {
        this.listener = listener;
        this.requestBody = requestBody;
    }

    @Override
    protected void onPreExecute() {
        listener.onStart();
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String s) {
        String json = ApplicationUtil.responsePost(Callback.API_URL, requestBody);
        try {
            JSONObject root = new JSONObject(json);

            // API returns: {"ETHIONEWS_APP": [{"post": {...}, "related": [...]}]}
            JSONArray rootArr = root.getJSONArray(Callback.TAG_ROOT);
            if (rootArr.length() == 0) return "0";
            JSONObject wrapper = rootArr.getJSONObject(0);

            // Check for error response
            if (!wrapper.has("post")) {
                verifyStatus = wrapper.optString(Callback.TAG_SUCCESS, "-1");
                message      = wrapper.optString(Callback.TAG_MSG, "");
                return "1";
            }

            JSONObject postObj = wrapper.getJSONObject("post");

            post = new ItemBlogPost(
                postObj.optString("id", ""),
                postObj.optString("title", ""),
                postObj.optString("slug", ""),
                postObj.optString("excerpt", ""),
                postObj.optString("content", ""),
                postObj.optString("featured_image", "").replace(" ", "%20"),
                postObj.optString("category_name", ""),
                postObj.optString("author_name", ""),
                postObj.optString("reading_time", "0"),
                postObj.optString("total_views", "0"),
                postObj.optString("total_share", "0"),
                postObj.optBoolean("featured", false),
                postObj.optBoolean("is_saved", false),
                postObj.optString("published_at", "")
            );

            // Parse tags
            if (postObj.has("tags")) {
                JSONArray tagsArr = postObj.getJSONArray("tags");
                List<ItemBlogTag> tags = new ArrayList<>();
                for (int i = 0; i < tagsArr.length(); i++) {
                    JSONObject t = tagsArr.getJSONObject(i);
                    tags.add(new ItemBlogTag(
                        t.optString("id", ""),
                        t.optString("name", ""),
                        t.optString("slug", "")
                    ));
                }
                post.setTags(tags);
            }

            // Parse related posts
            if (wrapper.has("related")) {
                JSONArray relArr = wrapper.getJSONArray("related");
                List<ItemBlogPost> related = new ArrayList<>();
                for (int i = 0; i < relArr.length(); i++) {
                    JSONObject r = relArr.getJSONObject(i);
                    related.add(new ItemBlogPost(
                        r.optString("id", ""),
                        r.optString("title", ""),
                        r.optString("slug", ""),
                        "",
                        r.optString("featured_image", "").replace(" ", "%20"),
                        r.optString("category_name", ""),
                        "",
                        "0", "0", false,
                        r.optString("published_at", "")
                    ));
                }
                post.setRelated(related);
            }

            return "1";
        } catch (Exception e) {
            return "0";
        }
    }

    @Override
    protected void onPostExecute(String s) {
        listener.onEnd(s, message, post);
    }
}
