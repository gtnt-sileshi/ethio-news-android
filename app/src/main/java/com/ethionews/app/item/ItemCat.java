package com.ethionews.app.item;

import java.io.Serializable;

public class ItemCat implements Serializable {

    // category_type values from the API: "all" | "blog" | "live" | "radio"
    public static final String TYPE_ALL   = "all";
    public static final String TYPE_BLOG  = "blog";
    public static final String TYPE_LIVE  = "live";
    public static final String TYPE_RADIO = "radio";

    private final String id;
    private final String name;
    private final String image;
    private final String type;   // category_type from API

    public ItemCat(String id, String name, String image, String type) {
        this.id    = id;
        this.name  = name;
        this.image = image;
        this.type  = type != null ? type : TYPE_ALL;
    }

    /** Legacy constructor — defaults type to "all" */
    public ItemCat(String id, String name, String image) {
        this(id, name, image, TYPE_ALL);
    }

    public String getId()    { return id; }
    public String getName()  { return name; }
    public String getImage() { return image; }
    public String getType()  { return type; }

    public boolean isForBlog()  { return TYPE_BLOG.equals(type) || TYPE_ALL.equals(type); }
    public boolean isForLive()  { return TYPE_LIVE.equals(type) || TYPE_ALL.equals(type); }
    public boolean isForRadio() { return TYPE_RADIO.equals(type) || TYPE_ALL.equals(type); }
}
