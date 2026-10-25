package com.ethionews.app.executor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import com.ethionews.app.callback.Callback;
import com.ethionews.app.interfaces.CategoryListener;
import com.ethionews.app.item.ItemCat;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.AsyncTaskExecutor;
import okhttp3.RequestBody;

public class LoadCat extends AsyncTaskExecutor<String, String, String> {

    private final RequestBody requestBody;
    private final CategoryListener catListener;
    private final ArrayList<ItemCat> arrayList = new ArrayList<>();
    private String verifyStatus = "0";
    private String message = "";

    public LoadCat(CategoryListener catListener, RequestBody requestBody) {
        this.catListener = catListener;
        this.requestBody = requestBody;
    }

    @Override
    protected void onPreExecute() {
        catListener.onStart();
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String strings) {
        try {
            String json = ApplicationUtil.responsePost(Callback.API_URL, requestBody);
            JSONObject mainJson = new JSONObject(json);
            JSONArray jsonArray = mainJson.getJSONArray(Callback.TAG_ROOT);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                if (!obj.has(Callback.TAG_SUCCESS)) {
                    String id    = obj.getString("cid");
                    String name  = obj.getString("category_name");
                    String type  = obj.optString("category_type", ItemCat.TYPE_ALL);
                    String image = obj.getString("category_image").replace(" ", "%20");
                    if (image.isEmpty()) image = "null";
                    arrayList.add(new ItemCat(id, name, image, type));
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
        catListener.onEnd(s, verifyStatus, message, arrayList);
    }
}