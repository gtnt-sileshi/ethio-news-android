package com.ethionews.app.item;

import java.io.Serializable;
import java.util.ArrayList;

public class ItemPost implements Serializable {

	String id;
	String title;
	String type;
	String page;
	ArrayList<ItemHomeSlider> arrayListBanner = new ArrayList<>();
	ArrayList<ItemCat> arrayListCategories = new ArrayList<>();
	ArrayList<ItemEvent> arrayListEvent = new ArrayList<>();
	ArrayList<ItemData> arrayListLive = new ArrayList<>();

	public ItemPost(String id, String title, String type, String page) {
		this.id = id;
		this.type = type;
		this.title = title;
		this.page = page;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getType() {
		return type;
	}

	public String getSections() {
		return page;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ArrayList<ItemHomeSlider> getArrayListBanner() {
		return arrayListBanner;
	}
	public void setArrayListBanner(ArrayList<ItemHomeSlider> arrayListBanner) {
		this.arrayListBanner.addAll(arrayListBanner);
	}

	public ArrayList<ItemCat> getArrayListCategories() {
		return arrayListCategories;
	}
	public void setArrayListCategories(ArrayList<ItemCat> arrayListCategories) {
		this.arrayListCategories.addAll(arrayListCategories);
	}

	public ArrayList<ItemEvent> getArrayListEvent() {
		return arrayListEvent;
	}
	public void setArrayListEvent(ArrayList<ItemEvent> arrayListEvent) {
		this.arrayListEvent.addAll(arrayListEvent);
	}

	public ArrayList<ItemData> getArrayListLive() {
		return arrayListLive;
	}
	public void setArrayListLive(ArrayList<ItemData> arrayListLive) {
		this.arrayListLive.addAll(arrayListLive);
	}
}