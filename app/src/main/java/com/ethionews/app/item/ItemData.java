package com.ethionews.app.item;

import java.io.Serializable;

public class ItemData implements Serializable {

    private final String id;
    private final String title;
    private final String thumb;

    public ItemData(String id, String title, String thumb) {
        this.id    = id;
        this.title = title;
        this.thumb = thumb;
    }

    public String getId()    { return id; }
    public String getTitle() { return title; }
    public String getThumb() { return thumb; }
}
