package com.ethionews.app.executor;

import org.json.JSONArray;
import org.json.JSONObject;

import com.ethionews.app.callback.Callback;
import com.ethionews.app.interfaces.SocialLoginListener;
import com.ethionews.app.utils.ApplicationUtil;
import com.ethionews.app.utils.AsyncTaskExecutor;
import okhttp3.RequestBody;

public class LoadRegister extends AsyncTaskExecutor<String, String, String> {

    private final RequestBody requestBody;
    private final SocialLoginListener socialLoginListener;
    private String success = "0";
    private String message = "";
    private String userID = "";
    private String userName = "";
    private String email = "";
    private String authID = "";

    public LoadRegister(SocialLoginListener socialLoginListener, RequestBody requestBody) {
        this.socialLoginListener = socialLoginListener;
        this.requestBody = requestBody;
    }

    @Override
    protected void onPreExecute() {
        socialLoginListener.onStart();
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String strings) {
        try {
            String json = ApplicationUtil.responsePost(Callback.API_URL, requestBody);
            JSONObject mainJson = new JSONObject(json);
            JSONArray jsonArray = mainJson.getJSONArray(Callback.TAG_ROOT);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject c = jsonArray.getJSONObject(i);

                success = c.getString(Callback.TAG_SUCCESS);
                message = c.getString(Callback.TAG_MSG);
                if(c.has("user_id")) {
                    userID = c.getString("user_id");
                    userName = c.getString("user_name");
                    authID = c.getString("auth_id");
                    email = c.getString( "user_email");
                }
            }
            return "1";
        } catch (Exception e) {
            return "0";
        }
    }

    @Override
    protected void onPostExecute(String s) {
        socialLoginListener.onEnd(s, success, message, userID, userName, email, authID);
    }
}