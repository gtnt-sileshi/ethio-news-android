package com.ethionews.app.executor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import com.ethionews.app.callback.Callback;
import com.ethionews.app.interfaces.LiveIDListener;
import com.ethionews.app.item.ItemData;
import com.ethionews.app.item.ItemLiveTv;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.AsyncTaskExecutor;
import okhttp3.RequestBody;

public class LoadLiveID extends AsyncTaskExecutor<String, String, String> {

    private final RequestBody requestBody;
    private final LiveIDListener listener;
    private final ArrayList<ItemLiveTv> arrayListLive = new ArrayList<>();
    private final ArrayList<ItemData> arrayListRelated = new ArrayList<>();

    public LoadLiveID(LiveIDListener listener, RequestBody requestBody) {
        this.listener = listener;
        this.requestBody = requestBody;
    }

    @Override
    protected void onPreExecute() {
        listener.onStart();
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String strings) {
        try {
            String json = ApplicationUtil.responsePost(Callback.API_URL, requestBody);
            JSONObject mainJson = new JSONObject(json);
            JSONObject jsonObject = mainJson.getJSONObject(Callback.TAG_ROOT);

            JSONArray jsonArrayMovie = jsonObject.getJSONArray("live_data");
            for (int i = 0; i < jsonArrayMovie.length(); i++) {
                JSONObject objJson = jsonArrayMovie.getJSONObject(i);

                String id = objJson.getString("id");
                String catID = objJson.getString("cat_id");
                String title = objJson.getString("live_title");
                String liveUrl = objJson.getString("live_url");
                String image = objJson.getString("image").replace(" ", "%20");
                if (image.isEmpty()) {
                    image = "null";
                }
                String liveType = objJson.getString("live_type");
                String description = objJson.getString("live_description");
                String rateAvg = objJson.getString("rate_avg");
                String totalRate = objJson.getString("total_rate");
                String totalViews = objJson.getString("total_views");
                String totalShare = objJson.getString("total_share");
                boolean isFav = objJson.optBoolean("is_favorite", false);
                boolean isUserAgent = objJson.getBoolean("user_agent");
                String userAgentName = objJson.getString("user_agent_name");
                String playerType    = objJson.getString("player_type");
                String userRating    = objJson.optString("user_rating", "0");
                String userMessage   = objJson.optString("user_message", "");

                ItemLiveTv item = new ItemLiveTv(id, catID, title, liveUrl, image,
                        liveType, description, rateAvg, totalRate, totalViews, totalShare,
                        isFav, isUserAgent, userAgentName, playerType);

                item.setUserRating(userRating);
                item.setUserMessage(userMessage);

                arrayListLive.add(item);
            }

            JSONArray jsonArray = jsonObject.getJSONArray("related");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject objJson = jsonArray.getJSONObject(i);

                String id    = objJson.getString("id");
                String title = objJson.getString("live_title");
                String image = objJson.getString("image").replace(" ", "%20");
                if (image.isEmpty()) image = "null";

                ItemData objItem = new ItemData(id, title, image);
                arrayListRelated.add(objItem);
            }

            return "1";
        } catch (Exception e) {
            return "0";
        }
    }

    @Override
    protected void onPostExecute(String s) {
        listener.onEnd(s, arrayListLive, arrayListRelated);
    }
}