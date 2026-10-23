package com.ethionews.app.item;

import java.io.Serializable;

public class ItemRadio implements Serializable {

    private final String id;
    private final String catID;
    private final String name;
    private final String streamUrl;
    private final String logo;
    private final String genre;
    private final String description;
    private final boolean isPremium;
    private boolean isFavourite;

    public ItemRadio(String id, String catID, String name, String streamUrl,
                     String logo, String genre, String description, boolean isPremium) {
        this(id, catID, name, streamUrl, logo, genre, description, isPremium, false);
    }

    public ItemRadio(String id, String catID, String name, String streamUrl,
                     String logo, String genre, String description, boolean isPremium,
                     boolean isFavourite) {
        this.id = id;
        this.catID = catID;
        this.name = name;
        this.streamUrl = streamUrl;
        this.logo = logo;
        this.genre = genre;
        this.description = description;
        this.isPremium = isPremium;
        this.isFavourite = isFavourite;
    }

    public String getId()          { return id; }
    public String getCatID()       { return catID; }
    public String getName()        { return name; }
    public String getStreamUrl()   { return streamUrl; }
    public String getLogo()        { return logo; }
    public String getGenre()       { return genre; }
    public String getDescription() { return description; }
    public boolean getIsPremium()  { return isPremium; }

    public boolean getIsFav()      { return isFavourite; }
    public void setIsFav(boolean favourite) { this.isFavourite = favourite; }
}
