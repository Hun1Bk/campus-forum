package com.kob.backend.service.impl.user.section;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.AgreeMapper;
import com.kob.backend.mapper.CommentMapper;
import com.kob.backend.mapper.PostMapper;
import com.kob.backend.mapper.SectionMapper;
import com.kob.backend.pojo.Agree;
import com.kob.backend.pojo.Comment;
import com.kob.backend.pojo.Post;
import com.kob.backend.pojo.Section;
import com.kob.backend.service.user.section.SectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SectionServiceImpl implements SectionService {
    private static final DateTimeFormatter TIMER_FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d H:m:s");
    private static final double SECTION_HOT_ALPHA = 0.7;

    @Autowired
    private SectionMapper sectionMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private AgreeMapper agreeMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Override
    public List<Map<String, String>> list() {
        QueryWrapper<Section> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("sort_order", "sid");
        List<Section> sections = sectionMapper.selectList(queryWrapper);
        List<Map<String, String>> res = new ArrayList<>();
        for (Section section : sections) {
            Map<String, String> map = new HashMap<>();
            map.put("sid", section.getSid().toString());
            map.put("name", section.getName());
            map.put("description", section.getDescription() == null ? "" : section.getDescription());
            map.put("sortOrder", section.getSortOrder() == null ? "0" : section.getSortOrder().toString());
            map.put("createTime", section.getCreateTime() == null ? "" : section.getCreateTime());
            res.add(map);
        }
        return res;
    }

    @Override
    public List<Map<String, String>> hot() {
        List<Map<String, String>> res = list();
        for (Map<String, String> section : res) {
            Integer sid = Integer.parseInt(section.get("sid"));
            SectionHot hot = calculateSectionHot(sid);
            section.put("hotScore", String.format("%.2f", hot.score));
            section.put("postCount", String.valueOf(hot.postCount));
        }
        res.sort(Comparator
                .comparingDouble((Map<String, String> map) -> Double.parseDouble(map.getOrDefault("hotScore", "0")))
                .reversed()
                .thenComparingInt(map -> Integer.parseInt(map.getOrDefault("sortOrder", "0"))));
        return res;
    }

    private SectionHot calculateSectionHot(Integer sid) {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("section_id", sid).eq("visibility", "PUBLIC");
        List<Post> posts = postMapper.selectList(queryWrapper);
        double score = 0.0;
        for (Post post : posts) {
            long daysAgo = daysAgo(post.getTimer());
            int viewCount = post.getViewCount() == null ? 1 : post.getViewCount();
            long dailyScore = viewCount + countAgree(post.getPid()) * 2 + countComment(post.getPid()) * 3;
            score += dailyScore * Math.pow(SECTION_HOT_ALPHA, daysAgo);
        }
        return new SectionHot(score, posts.size());
    }

    private long daysAgo(String timer) {
        if (timer == null || timer.trim().isEmpty()) {
            return 0;
        }
        try {
            LocalDateTime created = LocalDateTime.parse(timer.trim(), TIMER_FORMATTER);
            return Math.max(0, ChronoUnit.DAYS.between(created.toLocalDate(), LocalDate.now()));
        } catch (Exception e) {
            return 0;
        }
    }

    private long countAgree(Integer pid) {
        QueryWrapper<Agree> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pid", pid);
        return agreeMapper.selectCount(queryWrapper);
    }

    private long countComment(Integer pid) {
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pid", pid);
        return commentMapper.selectCount(queryWrapper);
    }

    private static class SectionHot {
        private final double score;
        private final int postCount;

        private SectionHot(double score, int postCount) {
            this.score = score;
            this.postCount = postCount;
        }
    }
}
