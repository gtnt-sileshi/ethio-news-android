package com.ethionews.app.executor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import com.ethionews.app.callback.Callback;
import com.ethionews.app.interfaces.HomeListener;
import com.ethionews.app.item.ItemCat;
import com.ethionews.app.item.ItemData;
import com.ethionews.app.item.ItemEvent;
import com.ethionews.app.item.ItemPost;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.AsyncTaskExecutor;
import okhttp3.RequestBody;

public class LoadSearch extends AsyncTaskExecutor<String, String, String> {

    RequestBody requestBody;
    HomeListener homeListener;
    ArrayList<ItemPost> arrayListPost = new ArrayList<>();
    String message = "";

    public LoadSearch(HomeListener homeListener, RequestBody requestBody) {
        this.homeListener = homeListener;
        this.requestBody = requestBody;
    }

    @Override
    protected void onPreExecute() {
        homeListener.onStart();
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String strings) {
        try {
            String json = ApplicationUtil.responsePost(Callback.API_URL, requestBody);
            JSONObject mainJson = new JSONObject(json);
            JSONObject jsonObjectRoot = mainJson.getJSONObject(Callback.TAG_ROOT);

            ItemPost itemPost;
            String postTitle;
            String postType;
            String postId;

            if (jsonObjectRoot.has("live_list")) {
                ArrayList<ItemData> arrayListLive = new ArrayList<>();

                JSONArray jsonArrayCountries = jsonObjectRoot.getJSONArray("live_list");

                if (jsonArrayCountries.length() > 0) {
                    postId = "3";
                    postTitle = "Live";
                    postType = "live";
                    itemPost = new ItemPost(postId, postTitle, postType, "live");

                    for (int i = 0; i < jsonArrayCountries.length(); i++) {
                        JSONObject objJson = jsonArrayCountries.getJSONObject(i);

                        String id = objJson.getString("id");
                        String title = objJson.getString("live_title");
                        String image = objJson.getString("image").replace(" ", "%20");
                        if (image.isEmpty()) {
                            image = "null";
                        }
                        boolean isPremium = objJson.getBoolean("is_premium");

                        ItemData objItem = new ItemData(id,title,image,isPremium);
                        arrayListLive.add(objItem);
                    }
                    itemPost.setArrayListLive(arrayListLive);
                    arrayListPost.add(itemPost);
                }
            }

            if (jsonObjectRoot.has("category_list")) {
                ArrayList<ItemCat> arrayListCat = new ArrayList<>();

                JSONArray jsonArrayAlbums = jsonObjectRoot.getJSONArray("category_list");

                if (jsonArrayAlbums.length() > 0) {
                    postId = "2";
                    postTitle = "Categories";
                    postType = "category";
                    itemPost = new ItemPost(postId, postTitle, postType, "category");

                    for (int i = 0; i < jsonArrayAlbums.length(); i++) {
                        JSONObject objJson = jsonArrayAlbums.getJSONObject(i);

                        String id = objJson.getString("post_id");
                        String name = objJson.getString("post_title");
                        String image = objJson.getString("post_image").replace(" ", "%20");
                        if (image.isEmpty()) {
                            image = "null";
                        }

                        ItemCat itemCat = new ItemCat(id, name, image);
                        arrayListCat.add(itemCat);
                    }
                    itemPost.setArrayListCategories(arrayListCat);
                    arrayListPost.add(itemPost);
                }
            }

            if (jsonObjectRoot.has("event_list")) {
                ArrayList<ItemEvent> arrayListEvent = new ArrayList<>();

                JSONArray jsonArrayAlbums = jsonObjectRoot.getJSONArray("event_list");

                if (jsonArrayAlbums.length() > 0) {
                    postId = "1";
                    postTitle = "Event";
                    postType = "event";
                    itemPost = new ItemPost(postId, postTitle, postType, "event");

                    for (int i = 0; i < jsonArrayAlbums.length(); i++) {
                        JSONObject objJson = jsonArrayAlbums.getJSONObject(i);

                        String id = objJson.getString("id");
                        String postID = objJson.getString("post_id");
                        String title = objJson.getString("event_title");
                        String time = objJson.getString("event_time");
                        String date = objJson.getString("event_date");

                        String titleOne = objJson.getString("team_title_one");
                        String thumbOne = objJson.getString("team_one_thumbnail").replace(" ", "%20");
                        if (thumbOne.isEmpty()) {
                            thumbOne = "null";
                        }
                        String titleTwo = objJson.getString("team_title_two");
                        String thumbTwo = objJson.getString("team_two_thumbnail").replace(" ", "%20");
                        if (thumbTwo.isEmpty()) {
                            thumbTwo = "null";
                        }

                        ItemEvent itemCountries = new ItemEvent(id, postID, title, time, date, titleOne, thumbOne, titleTwo, thumbTwo);
                        arrayListEvent.add(itemCountries);
                    }
                    itemPost.setArrayListEvent(arrayListEvent);
                    arrayListPost.add(itemPost);
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