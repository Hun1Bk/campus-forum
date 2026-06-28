package com.kob.backend.pojo;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("agree")
public class Agree {
    @TableId(type = IdType.AUTO)
    private Integer aid;
    private Integer pid;
    private Integer id;

    public Agree() {
    }

    public Agree(Integer aid, Integer pid, Integer id) {
        this.aid = aid;
        this.pid = pid;
        this.id = id;
    }

    public Integer getAid() { return aid; }
    public void setAid(Integer aid) { this.aid = aid; }
    public Integer getPid() { return pid; }
    public void setPid(Integer pid) { this.pid = pid; }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
}
