package com.kob.backend.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("section")
public class Section {
    @TableId(type = IdType.AUTO)
    private Integer sid;
    private String name;
    private String description;
    private Integer sortOrder;
    private String createTime;

    public Section() {
    }

    public Section(Integer sid, String name, String description, Integer sortOrder, String createTime) {
        this.sid = sid;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.createTime = createTime;
    }

    public Integer getSid() { return sid; }
    public void setSid(Integer sid) { this.sid = sid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
