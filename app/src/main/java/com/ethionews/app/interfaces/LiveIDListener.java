package com.ethionews.app.interfaces;

import java.util.ArrayList;

import com.ethionews.app.item.ItemData;
import com.ethionews.app.item.ItemLiveTv;

public interface LiveIDListener {
    void onStart();
    void onEnd(String success, ArrayList<ItemLiveTv> arrayListLive, ArrayList<ItemData> arrayListRelated);
}
