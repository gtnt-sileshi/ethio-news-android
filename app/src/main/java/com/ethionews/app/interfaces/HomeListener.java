package com.ethionews.app.interfaces;

import java.util.ArrayList;

import com.ethionews.app.item.ItemPost;

public interface HomeListener {
    void onStart();
    void onEnd(String success, String message, ArrayList<ItemPost> arrayListPost);
}
