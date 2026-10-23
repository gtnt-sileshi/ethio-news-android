package com.ethionews.app.item;


import java.io.Serializable;

public class ItemEvent implements Serializable {

    private final String id;
    private final String postID;
    private final String title;
    private final String time;
    private final String date;
    private final String titleOne;
    private final String thumbOne;
    private final String titleTwo;
    private final String thumbTwo;

    public ItemEvent(String id, String postID, String title, String time, String date,
                     String titleOne, String thumbOne, String titleTwo, String thumbTwo) {
        this.id = id;
        this.postID = postID;
        this.title = title;
        this.time = time;
        this.date = date;
        this.titleOne = titleOne;
        this.thumbOne = thumbOne;
        this.titleTwo = titleTwo;
        this.thumbTwo = thumbTwo;
    }

    public String getId() {
        return id;
    }

    public String getPostId() {
        return postID;
    }

    public String getTitle() {
        return title;
    }

    public String getTime() {
        return time;
    }

    public String getDate() {
        return date;
    }

    public String getTitleOne() {
        return titleOne;
    }

    public String getThumbOne() {
        return thumbOne;
    }

    public String getTitleTwo() {
        return titleTwo;
    }

    public String getThumbTwo() {
        return thumbTwo;
    }
}