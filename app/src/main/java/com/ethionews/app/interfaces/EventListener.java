package com.ethionews.app.interfaces;

import java.util.ArrayList;

import com.ethionews.app.item.ItemEvent;

public interface EventListener {
    void onStart();
    void onEnd(String success, String verifyStatus, String message, ArrayList<ItemEvent> arrayListData);
}