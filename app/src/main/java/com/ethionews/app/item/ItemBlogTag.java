package com.ethionews.app.item;

import java.io.Serializable;

public class ItemBlogTag implements Serializable {
    private final String id;
    private final String name;
    private final String slug;

    public ItemBlogTag(String id, String name, String slug) {
        this.id = id;
        this.name = name;
        this.slug = slug;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
}
