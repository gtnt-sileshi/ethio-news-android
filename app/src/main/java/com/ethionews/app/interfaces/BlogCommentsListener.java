package com.ethionews.app.interfaces;

import com.ethionews.app.item.ItemBlogComment;
import java.util.ArrayList;

public interface BlogCommentsListener {
    void onStart();
    void onEnd(String success, String message, ArrayList<ItemBlogComment> comments);
}
