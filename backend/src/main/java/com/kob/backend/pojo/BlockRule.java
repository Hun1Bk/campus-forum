package com.kob.backend.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("block_rule")
public class BlockRule {
    @TableId(type = IdType.AUTO)
    private Integer bid;
    private Integer userId;
    private String targetType;
    private Integer targetId;
    private String createTime;

    public BlockRule() {
    }

    public BlockRule(Integer bid, Integer userId, String targetType, Integer targetId, String createTime) {
        this.bid = bid;
        this.userId = userId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.createTime = createTime;
    }

    public Integer getBid() { return bid; }
    public void setBid(Integer bid) { this.bid = bid; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Integer getTargetId() { return targetId; }
    public void setTargetId(Integer targetId) { this.targetId = targetId; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
