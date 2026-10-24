package com.ethionews.app.interfaces;

import com.ethionews.app.item.ItemBlogPost;

public interface BlogPostDetailListener {
    void onStart();
    void onEnd(String success, String message, ItemBlogPost post);
}
