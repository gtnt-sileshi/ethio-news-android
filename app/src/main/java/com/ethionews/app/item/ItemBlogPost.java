package com.ethionews.app.item;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ItemBlogPost implements Serializable {

    private final String id;
    private final String title;
    private final String slug;
    private final String excerpt;
    private final String content;
    private final String featuredImage;
    private final String categoryName;
    private final String authorName;
    private final String readingTime;
    private final String totalViews;
    private final String totalShare;
    private final boolean featured;
    private final boolean isSaved;
    private final String publishedAt;
    private final String createdAt;
    private List<ItemBlogTag> tags;
    private List<ItemBlogPost> related;

    // Compact constructor for list views (no content/related)
    public ItemBlogPost(String id, String title, String slug, String excerpt,
                        String featuredImage, String categoryName, String authorName,
                        String readingTime, String totalViews, boolean featured,
                        String publishedAt) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.excerpt = excerpt;
        this.content = "";
        this.featuredImage = featuredImage;
        this.categoryName = categoryName;
        this.authorName = authorName;
        this.readingTime = readingTime;
        this.totalViews = totalViews;
        this.totalShare = "0";
        this.featured = featured;
        this.isSaved = false;
        this.publishedAt = publishedAt;
        this.createdAt = publishedAt;
        this.tags = new ArrayList<>();
        this.related = new ArrayList<>();
    }

    // Full constructor for detail view (includes content, saved status, tags, related)
    public ItemBlogPost(String id, String title, String slug, String excerpt, String content,
                        String featuredImage, String categoryName, String authorName,
                        String readingTime, String totalViews, String totalShare,
                        boolean featured, boolean isSaved, String publishedAt) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.excerpt = excerpt;
        this.content = content;
        this.featuredImage = featuredImage;
        this.categoryName = categoryName;
        this.authorName = authorName;
        this.readingTime = readingTime;
        this.totalViews = totalViews;
        this.totalShare = totalShare;
        this.featured = featured;
        this.isSaved = isSaved;
        this.publishedAt = publishedAt;
        this.createdAt = publishedAt;
        this.tags = new ArrayList<>();
        this.related = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSlug() { return slug; }
    public String getExcerpt() { return excerpt; }
    public String getContent() { return content; }
    public String getFeaturedImage() { return featuredImage; }
    public String getCategoryName() { return categoryName; }
    public String getAuthorName() { return authorName; }
    public String getReadingTime() { return readingTime; }
    public String getTotalViews() { return totalViews; }
    public String getTotalShare() { return totalShare; }
    public boolean isFeatured() { return featured; }
    public boolean isSaved() { return isSaved; }
    public String getPublishedAt() { return publishedAt; }
    public String getCreatedAt() { return createdAt; }
    public List<ItemBlogTag> getTags() { return tags; }
    public void setTags(List<ItemBlogTag> tags) { this.tags = tags; }
    public List<ItemBlogPost> getRelated() { return related; }
    public void setRelated(List<ItemBlogPost> related) { this.related = related; }
}
