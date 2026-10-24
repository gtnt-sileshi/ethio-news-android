package com.ethionews.app.interfaces;

import com.ethionews.app.item.ItemBlogPost;
import java.util.ArrayList;

public interface BlogPostsListener {
    void onStart();
    void onEnd(String success, String message, ArrayList<ItemBlogPost> posts);
}
