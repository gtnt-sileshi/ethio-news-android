package com.ethionews.app.executor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import com.ethionews.app.callback.Callback;
import com.ethionews.app.interfaces.BlogCommentsListener;
import com.ethionews.app.item.ItemBlogComment;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.AsyncTaskExecutor;
import okhttp3.RequestBody;

public class LoadBlogComments extends AsyncTaskExecutor<String, String, String> {

    private final BlogCommentsListener listener;
    private final RequestBody requestBody;
    private final ArrayList<ItemBlogComment> list = new ArrayList<>();
    private String verifyStatus = "-1";
    private String message = "";

    public LoadBlogComments(BlogCommentsListener listener, RequestBody requestBody) {
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
                if (!obj.has(Callback.TAG_SUCCESS)) {
                    ItemBlogComment comment = new ItemBlogComment(
                        obj.optString("id", ""),
                        obj.optString("author_name", ""),
                        obj.optString("content", ""),
                        obj.optString("created_at", "")
                    );
                    // Parse replies
                    if (obj.has("replies")) {
                        JSONArray repliesArr = obj.getJSONArray("replies");
                        List<ItemBlogComment> replies = new ArrayList<>();
                        for (int j = 0; j < repliesArr.length(); j++) {
                            JSONObject r = repliesArr.getJSONObject(j);
                            replies.add(new ItemBlogComment(
                                r.optString("id", ""),
                                r.optString("author_name", ""),
                                r.optString("content", ""),
                                r.optString("created_at", "")
                            ));
                        }
                        comment.setReplies(replies);
                    }
                    list.add(comment);
                } else {
                    verifyStatus = obj.getString(Callback.TAG_SUCCESS);
                    message = obj.getString(Callback.TAG_MSG);
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
}
