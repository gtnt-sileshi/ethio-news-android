package com.ethionews.app.item;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ItemBlogComment implements Serializable {
    private final String id;
    private final String authorName;
    private final String content;
    private final String createdAt;
    private List<ItemBlogComment> replies;

    public ItemBlogComment(String id, String authorName, String content, String createdAt) {
        this.id = id;
        this.authorName = authorName;
        this.content = content;
        this.createdAt = createdAt;
        this.replies = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getAuthorName() { return authorName; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }
    public List<ItemBlogComment> getReplies() { return replies; }
    public void setReplies(List<ItemBlogComment> replies) { this.replies = replies; }
}
