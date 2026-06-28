package com.kob.backend.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("post")
public class Post {
    @TableId(type = IdType.AUTO)
    private Integer pid;
    private String title;
    private String content;
    private String timer;
    private Integer id;
    private boolean seen;
    private String imageUrls;
    private Integer sectionId;
    private Integer viewCount;
    private Boolean isTop;
    private String topTime;
    private String visibility;

    public Post() {
    }

    public Post(Integer pid, String title, String content, String timer, Integer id, boolean seen, String imageUrls, Integer sectionId, Integer viewCount, Boolean isTop, String topTime, String visibility) {
        this.pid = pid;
        this.title = title;
        this.content = content;
        this.timer = timer;
        this.id = id;
        this.seen = seen;
        this.imageUrls = imageUrls;
        this.sectionId = sectionId;
        this.viewCount = viewCount;
        this.isTop = isTop;
        this.topTime = topTime;
        this.visibility = visibility;
    }

    public Integer getPid() { return pid; }
    public void setPid(Integer pid) { this.pid = pid; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTimer() { return timer; }
    public void setTimer(String timer) { this.timer = timer; }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public boolean isSeen() { return seen; }
    public boolean getSeen() { return seen; }
    public void setSeen(boolean seen) { this.seen = seen; }
    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
    public Integer getSectionId() { return sectionId; }
    public void setSectionId(Integer sectionId) { this.sectionId = sectionId; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public Boolean getIsTop() { return isTop; }
    public void setIsTop(Boolean isTop) { this.isTop = isTop; }
    public String getTopTime() { return topTime; }
    public void setTopTime(String topTime) { this.topTime = topTime; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
}
