package com.ethionews.app.interfaces;

import java.util.ArrayList;

import com.ethionews.app.item.ItemNotify;


public interface NotifyListener {
    void onStart();
    void onEnd(String success, ArrayList<ItemNotify> notificationArrayList);
}