package com.ethionews.app.interfaces;

public interface SocialLoginListener {
    void onStart();
    void onEnd(String success, String registerSuccess, String message, String userID, String userName,
               String email, String authID);
}