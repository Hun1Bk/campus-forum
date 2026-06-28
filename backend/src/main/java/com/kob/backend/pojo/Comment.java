package com.kob.backend.pojo;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Integer cid;
    private String content;
    private Integer pid;
    private Integer id;
    private Integer parentId;

    public Comment() {
    }

    public Comment(Integer cid, String content, Integer pid, Integer id, Integer parentId) {
        this.cid = cid;
        this.content = content;
        this.pid = pid;
        this.id = id;
        this.parentId = parentId;
    }

    public Integer getCid() { return cid; }
    public void setCid(Integer cid) { this.cid = cid; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getPid() { return pid; }
    public void setPid(Integer pid) { this.pid = pid; }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }
}
