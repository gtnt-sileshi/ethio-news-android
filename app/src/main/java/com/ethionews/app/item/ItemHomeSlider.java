package com.ethionews.app.item;

import java.io.Serializable;

public class ItemHomeSlider implements Serializable {

	private final String bid;
	private final String title;
	private final String info;
	private final String image;

	public ItemHomeSlider(String bid, String title, String info, String image) {
		this.bid = bid;
		this.title = title;
		this.info = info;
		this.image = image;
	}

	public String getId() {
		return bid;
	}

	public String getTitle() {
		return title;
	}

	public String getInfo() {
		return info;
	}

	public String getImage() {
		return image;
	}
}