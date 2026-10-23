package com.ethionews.app.item;


import java.io.Serializable;

public class ItemData implements Serializable {

    private final String id;
    private final String title;
    private final String thumb;
    private final boolean isPremium;

    public ItemData(String id, String title, String thumb, boolean isPremium) {
        this.id = id;
        this.title = title;
        this.thumb = thumb;
        this.isPremium = isPremium;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getThumb() {
        return thumb;
    }

    public boolean getIsPremium() {
        return isPremium;
    }
}