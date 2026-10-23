package com.ethionews.app.interfaces;

public interface LoginListener {
    void onStart();
    void onEnd(String success, String loginSuccess, String message, String userID, String userName,
               String userGender, String userPhone, String profile);
}