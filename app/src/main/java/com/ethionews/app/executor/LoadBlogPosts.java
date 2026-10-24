package com.ethionews.app.executor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import com.ethionews.app.callback.Callback;
import com.ethionews.app.interfaces.BlogPostsListener;
import com.ethionews.app.item.ItemBlogPost;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.AsyncTaskExecutor;
import okhttp3.RequestBody;

/**
 * Loads a paginated list of blog posts. Handles the flat-array response from
 * get_blog_posts, get_blog_by_cat, get_blog_trending, get_blog_by_tag, etc.
 */
public class LoadBlogPosts extends AsyncTaskExecutor<String, String, String> {

    private final BlogPostsListener listener;
    private final RequestBody requestBody;
    private final ArrayList<ItemBlogPost> list = new ArrayList<>();
    private String verifyStatus = "0";
    private String message = "";

    public LoadBlogPosts(BlogPostsListener listener, RequestBody requestBody) {
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
            JSONArray arr  = root.getJSONArray(Callback.TAG_ROOT);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);

                // Each post has success="1" AND an id field — treat it as a post.
                // Only skip items that are pure error/status objects (no id).
                if (obj.has("id")) {
                    list.add(parsePost(obj));
                    verifyStatus = "1";
                } else if (obj.has(Callback.TAG_SUCCESS)) {
                    verifyStatus = obj.getString(Callback.TAG_SUCCESS);
                    message      = obj.getString(Callback.TAG_MSG);
                }
            }
            return "1";
        } catch (Exception e) {
            return "0";
        }
    }

    @Override
    protected void onPostExecute(String s) {
        listener.onEnd(s, message, list);
    }

    static ItemBlogPost parsePost(JSONObject obj) throws Exception {
        return new ItemBlogPost(
            obj.optString("id", ""),
            obj.optString("title", ""),
            obj.optString("slug", ""),
            obj.optString("excerpt", ""),
            obj.optString("featured_image", "").replace(" ", "%20"),
            obj.optString("category_name", ""),
            obj.optString("author_name", ""),
            obj.optString("reading_time", "0"),
            obj.optString("total_views", "0"),
            obj.optBoolean("featured", false),
            obj.optString("published_at", "")
        );
    }
}
