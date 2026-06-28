package com.kob.backend.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Integer nid;
    private Integer receiverId;
    private Integer actorId;
    private Integer pid;
    private String type;
    private String content;
    @TableField("is_read")
    private Boolean isRead;
    private String createTime;

    public Notification() {
    }

    public Notification(Integer nid, Integer receiverId, Integer actorId, Integer pid, String type, String content, Boolean isRead, String createTime) {
        this.nid = nid;
        this.receiverId = receiverId;
        this.actorId = actorId;
        this.pid = pid;
        this.type = type;
        this.content = content;
        this.isRead = isRead;
        this.createTime = createTime;
    }

    public Integer getNid() { return nid; }
    public void setNid(Integer nid) { this.nid = nid; }
    public Integer getReceiverId() { return receiverId; }
    public void setReceiverId(Integer receiverId) { this.receiverId = receiverId; }
    public Integer getActorId() { return actorId; }
    public void setActorId(Integer actorId) { this.actorId = actorId; }
    public Integer getPid() { return pid; }
    public void setPid(Integer pid) { this.pid = pid; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
