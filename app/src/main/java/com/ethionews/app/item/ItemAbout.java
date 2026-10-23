package com.ethionews.app.item;

import java.io.Serializable;

public class ItemAbout implements Serializable {

	private final String email;
	private final String author;
	private final String contact;
	private final String website;
	private final String description;
	private final String developedBy;
	private final String moreApps;

	public ItemAbout(String email, String author, String contact, String website, String description,
					 String developedBy, String moreApps) {
		this.email = email;
		this.author = author;
		this.contact = contact;
		this.website = website;
		this.description = description;
		this.developedBy = developedBy;
		this.moreApps = moreApps;
	}

	public String getEmail() {
		return email;
	}

	public String getAuthor() {
		return author;
	}

	public String getContact() {
		return contact;
	}

	public String getWebsite() {
		return website;
	}

	public String getAppDesc() {
		return description;
	}

	public String getDevelopedBY() {
		return developedBy;
	}

	public String getMoreApps() {
		return moreApps;
	}
}