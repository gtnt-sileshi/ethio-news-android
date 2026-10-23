package com.ethionews.app.interfaces;

import java.util.ArrayList;

import com.ethionews.app.item.ItemCat;

public interface CategoryListener {
    void onStart();
    void onEnd(String success, String verifyStatus, String message, ArrayList<ItemCat> arrayListCat);
}