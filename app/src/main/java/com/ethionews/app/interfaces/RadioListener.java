package com.ethionews.app.interfaces;

import java.util.ArrayList;

import com.ethionews.app.item.ItemRadio;

public interface RadioListener {
    void onStart();
    void onEnd(String success, String verifyStatus, String message, ArrayList<ItemRadio> arrayListData);
}
