package com.ethionews.app.interfaces;

public interface ProfileListener {
    void onStart();
    void onEnd(String success, String isApiSuccess, String message, String userID, String userName,
               String email, String mobile, String gender , String profile);
}