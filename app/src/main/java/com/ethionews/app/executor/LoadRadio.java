package com.ethionews.app.executor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import com.ethionews.app.callback.Callback;
import com.ethionews.app.interfaces.RadioListener;
import com.ethionews.app.item.ItemRadio;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.AsyncTaskExecutor;
import okhttp3.RequestBody;

public class LoadRadio extends AsyncTaskExecutor<String, String, String> {

    private final RadioListener listener;
    private final ArrayList<ItemRadio> arrayList = new ArrayList<>();
    private final RequestBody requestBody;
    private String verifyStatus = "0";
    private String message = "";

    public LoadRadio(RadioListener listener, RequestBody requestBody) {
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
        String json = ApplicationUtil.responsePost(Callback.API_URL, requestBody);
        try {
            JSONObject jOb = new JSONObject(json);
            JSONArray jsonArray = jOb.getJSONArray(Callback.TAG_ROOT);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject objJson = jsonArray.getJSONObject(i);

                if (!objJson.has(Callback.TAG_SUCCESS)) {
                    // Should not happen with our API shape, but guard anyway
                    continue;
                }

                String success = objJson.getString(Callback.TAG_SUCCESS);
                message = objJson.getString(Callback.TAG_MSG);

                if (!success.equals("1")) {
                    verifyStatus = success;
                    continue;
                }

                verifyStatus = success;

                String id          = objJson.getString("id");
                String catID       = objJson.getString("cat_id");
                String name        = objJson.getString("radio_name");
                String streamUrl   = objJson.getString("stream_url");
                String logo        = objJson.getString("logo").replace(" ", "%20");
                if (logo.isEmpty()) logo = "null";
                String genre       = objJson.optString("genre", "");
                String description = objJson.optString("description", "");
                boolean isPremium  = "true".equalsIgnoreCase(objJson.optString("isPremium", "false"));
                boolean isFavourite = "true".equalsIgnoreCase(objJson.optString("is_favorite", "false"));

                arrayList.add(new ItemRadio(id, catID, name, streamUrl, logo, genre, description, isPremium, isFavourite));
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
