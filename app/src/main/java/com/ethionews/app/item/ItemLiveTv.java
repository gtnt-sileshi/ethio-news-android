package com.ethionews.app.item;

import java.io.Serializable;

public class ItemLiveTv implements Serializable {

    private final String id;
    private final String catID;
    private final String title;
    private final String liveUrl;
    private final String image;
    private final String type;
    private final String description;
    private String averageRating;
    private String totalRate;
    private final String totalViews;
    private final String totalShare;
    private final boolean isPremium;
    private boolean isFavourite;

    private final boolean isUserAgent;
    private final String userAgentName;
    private final String playerType;

    private String userRating="";
    private String userMessage="" ;

    public ItemLiveTv(String id, String catID, String title, String liveUrl, String image,
                      String type, String description, String averageRating, String totalRate,
                      String totalViews, String totalShare, boolean isPremium, boolean isFavourite,
                      boolean userAgent, String userAgentName, String playerType) {
        this.id = id;
        this.catID = catID;
        this.title = title;
        this.liveUrl = liveUrl;
        this.image = image;
        this.type = type;
        this.description = description;
        this.averageRating = averageRating;
        this.totalRate = totalRate;
        this.totalViews = totalViews;
        this.totalShare = totalShare;
        this.isPremium = isPremium;
        this.isFavourite = isFavourite;
        this.isUserAgent = userAgent;
        this.userAgentName = userAgentName;
        this.playerType = playerType;
    }

    public String getId() {
        return id;
    }

    public String getCatId() {
        return catID;
    }

    public String getLiveTitle() {
        return title;
    }

    public String getLiveURL() {
        return liveUrl;
    }

    public String getImage() {
        return image;
    }

    public String getLiveType() {
        return type;
    }

    public String getLiveDescription() {
        return description;
    }

    public String getTotalViews() {
        return totalViews;
    }

    public String getTotalShare() {
        return totalShare;
    }

    public boolean getIsPremium() {
        return isPremium;
    }

    public Boolean getIsFav() {
        return isFavourite;
    }
    public void setIsFav(Boolean favourite) {
        isFavourite = favourite;
    }

    public String getUserRating() {
        return userRating;
    }
    public void setUserRating(String userRating) {
        this.userRating = userRating;
    }

    public String getUserMessage() {
        return userMessage;
    }
    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getAverageRating() {
        return averageRating;
    }

    public String getTotalRate() {
        return totalRate;
    }

    public void setAverageRating(String averageRating) {
        this.averageRating = averageRating;
    }

    public void setTotalRate(String totalRate) {
        this.totalRate = totalRate;
    }

    public boolean isUserAgent() {
        return isUserAgent;
    }

    public String getUserAgentName() {
        return userAgentName;
    }

    public String getPlayerType() {
        return playerType;
    }
}