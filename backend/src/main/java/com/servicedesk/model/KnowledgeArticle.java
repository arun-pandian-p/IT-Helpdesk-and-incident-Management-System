package com.servicedesk.model;

import java.time.Instant;

public class KnowledgeArticle {
    private String id;
    private String title;
    private String category;
    private String osTarget;
    private String content;
    private String tags;
    private int viewCount;
    private int helpfulCount;
    private int notHelpfulCount;
    private String createdBy;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;

    public KnowledgeArticle() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getOsTarget() { return osTarget; }
    public void setOsTarget(String osTarget) { this.osTarget = osTarget; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getHelpfulCount() { return helpfulCount; }
    public void setHelpfulCount(int helpfulCount) { this.helpfulCount = helpfulCount; }

    public int getNotHelpfulCount() { return notHelpfulCount; }
    public void setNotHelpfulCount(int notHelpfulCount) { this.notHelpfulCount = notHelpfulCount; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
