package com.kob.backend.service.impl.user.block;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.BlockRuleMapper;
import com.kob.backend.mapper.SectionMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.BlockRule;
import com.kob.backend.pojo.Section;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.block.BlockRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BlockRuleServiceImpl implements BlockRuleService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private BlockRuleMapper blockRuleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SectionMapper sectionMapper;

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return null;
        }
        return ((UserDetailsImpl) authentication.getPrincipal()).getUser();
    }

    @Override
    public List<Map<String, String>> list() {
        List<Map<String, String>> res = new ArrayList<>();
        User current = currentUser();
        if (current == null) {
            return res;
        }
        QueryWrapper<BlockRule> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", current.getId());
        queryWrapper.orderByDesc("bid");
        List<BlockRule> rules = blockRuleMapper.selectList(queryWrapper);
        for (BlockRule rule : rules) {
            Map<String, String> map = new HashMap<>();
            map.put("bid", rule.getBid().toString());
            map.put("targetType", rule.getTargetType());
            map.put("targetId", rule.getTargetId().toString());
            map.put("createTime", rule.getCreateTime() == null ? "" : rule.getCreateTime());
            map.put("targetName", targetName(rule));
            res.add(map);
        }
        return res;
    }

    @Override
    public Map<String, String> add(String targetType, Integer targetId) {
        Map<String, String> map = new HashMap<>();
        User current = currentUser();
        if (current == null) {
            map.put("error_message", "请先登录");
            return map;
        }
        String type = normalizeType(targetType);
        if (type == null || targetId == null || targetId <= 0) {
            map.put("error_message", "屏蔽目标不合法");
            return map;
        }
        if ("USER".equals(type) && current.getId().equals(targetId)) {
            map.put("error_message", "不能屏蔽自己");
            return map;
        }
        if (!targetExists(type, targetId)) {
            map.put("error_message", "屏蔽目标不存在");
            return map;
        }
        QueryWrapper<BlockRule> existWrapper = new QueryWrapper<>();
        existWrapper.eq("user_id", current.getId())
                .eq("target_type", type)
                .eq("target_id", targetId);
        if (blockRuleMapper.selectCount(existWrapper) > 0) {
            map.put("error_message", "已经屏蔽过该目标");
            return map;
        }
        BlockRule rule = new BlockRule(null, current.getId(), type, targetId, LocalDateTime.now().format(TIME_FORMATTER));
        blockRuleMapper.insert(rule);
        map.put("error_message", "success");
        return map;
    }

    @Override
    public Map<String, String> delete(Integer bid) {
        Map<String, String> map = new HashMap<>();
        User current = currentUser();
        BlockRule rule = bid == null ? null : blockRuleMapper.selectById(bid);
        if (current == null || rule == null || !current.getId().equals(rule.getUserId())) {
            map.put("error_message", "屏蔽规则不存在");
            return map;
        }
        blockRuleMapper.deleteById(bid);
        map.put("error_message", "success");
        return map;
    }

    private String normalizeType(String targetType) {
        if ("USER".equals(targetType)) return "USER";
        if ("SECTION".equals(targetType)) return "SECTION";
        return null;
    }

    private boolean targetExists(String type, Integer targetId) {
        if ("USER".equals(type)) {
            return userMapper.selectById(targetId) != null;
        }
        if ("SECTION".equals(type)) {
            return sectionMapper.selectById(targetId) != null;
        }
        return false;
    }

    private String targetName(BlockRule rule) {
        if ("USER".equals(rule.getTargetType())) {
            User user = userMapper.selectById(rule.getTargetId());
            return user == null ? "用户已不存在" : user.getUsername();
        }
        Section section = sectionMapper.selectById(rule.getTargetId());
        return section == null ? "分区已不存在" : section.getName();
    }
}
