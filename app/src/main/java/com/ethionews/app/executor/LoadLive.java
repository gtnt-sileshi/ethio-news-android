package com.ethionews.app.executor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import com.ethionews.app.callback.Callback;
import com.ethionews.app.interfaces.LiveListener;
import com.ethionews.app.item.ItemData;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.AsyncTaskExecutor;
import okhttp3.RequestBody;

public class LoadLive extends AsyncTaskExecutor<String, String, String> {

    private final LiveListener listener;
    private final ArrayList<ItemData> arrayList = new ArrayList<>();
    private final RequestBody requestBody;
    private String verifyStatus = "0";
    private String message = "";

    public LoadLive(LiveListener listener, RequestBody requestBody) {
        this.listener = listener;
        this.requestBody = requestBody;
    }

    @Override
    protected void onPreExecute() {
        listener.onStart();
        super.onPreExecute();
    }

    @Override
    protected  String doInBackground(String strings)  {
        String json = ApplicationUtil.responsePost(Callback.API_URL, requestBody);
        try {
            JSONObject jOb = new JSONObject(json);
            JSONArray jsonArray = jOb.getJSONArray(Callback.TAG_ROOT);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject objJson = jsonArray.getJSONObject(i);

                if (!objJson.has(Callback.TAG_SUCCESS)) {

                    String id = objJson.getString("id");
                    String title = objJson.getString("live_title");
                    String image = objJson.getString("image").replace(" ", "%20");
                    if (image.isEmpty()) {
                        image = "null";
                    }
                    boolean isPremium = objJson.getBoolean("is_premium");

                    ItemData objItem = new ItemData(id,title,image,isPremium);
                    arrayList.add(objItem);

                } else {
                    verifyStatus = objJson.getString(Callback.TAG_SUCCESS);
                    message = objJson.getString(Callback.TAG_MSG);
                }
            }
            return "1";
        } catch (Exception e) {
            return "0";
        }
    }

    @Override
    protected void onPostExecute(String s) {
        listener.onEnd(s, verifyStatus, message, arrayList);
    }
}

