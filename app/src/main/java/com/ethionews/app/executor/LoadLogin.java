package com.ethionews.app.executor;


import org.json.JSONArray;
import org.json.JSONObject;

import com.ethionews.app.callback.Callback;
import com.ethionews.app.interfaces.LoginListener;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.AsyncTaskExecutor;
import okhttp3.RequestBody;

public class LoadLogin extends AsyncTaskExecutor<String, String, String> {

    private final RequestBody requestBody;
    private final LoginListener listener;
    private String success = "0";
    private String message = "";
    private String userID="";
    private String userName = "";
    private String userGender = "";
    private String profile = "";
    private String userPhone = "";

    public LoadLogin(LoginListener listener, RequestBody requestBody) {
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
            JSONArray jsonArray = mainJson.getJSONArray(Callback.TAG_ROOT);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                success = jsonObject.getString(Callback.TAG_SUCCESS);
                if(success.equals("1")) {
                    // Server returns "id" not "user_id" — fall back to "id" if "user_id" absent
                    userID = jsonObject.has("user_id")
                            ? jsonObject.getString("user_id")
                            : jsonObject.optString("id", "");
                    userName = jsonObject.optString("user_name", "");
                    userPhone = jsonObject.optString("user_phone", "");
                    userGender = jsonObject.optString("user_gender", "");
                    profile = jsonObject.optString("profile_img", "");
                }
                message = jsonObject.getString(Callback.TAG_MSG);
            }
            return "1";
        } catch (Exception e) {
            return "0";
        }
    }

    @Override
    protected void onPostExecute(String s) {
        listener.onEnd(s, success, message, userID, userName, userGender, userPhone, profile);
    }
}