package com.kob.backend.service.impl.user.post;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kob.backend.mapper.AgreeMapper;
import com.kob.backend.mapper.BlockRuleMapper;
import com.kob.backend.mapper.CommentMapper;
import com.kob.backend.mapper.PostMapper;
import com.kob.backend.mapper.SectionMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Agree;
import com.kob.backend.pojo.BlockRule;
import com.kob.backend.pojo.Comment;
import com.kob.backend.pojo.Post;
import com.kob.backend.pojo.Section;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.PostCascadeDeleteHelper;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.post.GetPostService;
import com.kob.backend.utils.RoleTitleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GetPostServiceImpl implements GetPostService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TIMER_FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d H:m:s");
    private static final DateTimeFormatter VIEW_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long VIEW_DEDUP_MINUTES = 30;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SectionMapper sectionMapper;

    @Autowired
    private AgreeMapper agreeMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PostCascadeDeleteHelper postCascadeDeleteHelper;

    @Autowired
    private BlockRuleMapper blockRuleMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> get_post(Integer sectionId, String keyword) {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        if (sectionId != null && sectionId > 0) {
            queryWrapper.eq("section_id", sectionId);
        }
        String key = keyword == null ? "" : keyword.trim();
        if (!key.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper.like("title", key).or().like("content", key));
        }
        List<Post> list = postMapper.selectList(queryWrapper);
        list.sort(postComparator());
        List<Map<String, Object>> res = new ArrayList<>();
        User current = currentUser();
        boolean manager = isCurrentManager();
        BlockFilter blockFilter = loadBlockFilter(current);
        for (Post post : list) {
            if (canViewPost(post, current) && !blockFilter.isBlocked(post)) {
                res.add(buildPostMap(post, manager));
            }
        }
        return res;
    }

    @Override
    public List<Map<String, Object>> hot() {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("visibility", "PUBLIC");
        List<Post> posts = postMapper.selectList(queryWrapper);
        posts.sort((left, right) -> Double.compare(calculateHotScore(right), calculateHotScore(left)));
        User current = currentUser();
        boolean manager = isCurrentManager();
        BlockFilter blockFilter = loadBlockFilter(current);
        List<Map<String, Object>> res = new ArrayList<>();
        for (Post post : posts) {
            if (!canViewPost(post, current) || blockFilter.isBlocked(post)) {
                continue;
            }
            res.add(buildPostMap(post, manager));
            if (res.size() >= 10) {
                break;
            }
        }
        return res;
    }

    @Override
    public List<Map<String, Object>> pinned() {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_top", true);
        List<Post> posts = postMapper.selectList(queryWrapper);
        posts.sort(postComparator());
        User current = currentUser();
        boolean manager = isCurrentManager();
        List<Map<String, Object>> res = new ArrayList<>();
        for (Post post : posts) {
            if (canViewPost(post, current)) {
                res.add(buildPostMap(post, manager));
            }
        }
        return res;
    }

    @Override
    public List<Map<String, Object>> mine() {
        List<Map<String, Object>> res = new ArrayList<>();
        User current = currentUser();
        if (current == null) {
            return res;
        }
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", current.getId());
        List<Post> posts = postMapper.selectList(queryWrapper);
        posts.sort(postComparator());
        boolean manager = isCurrentManager();
        for (Post post : posts) {
            res.add(buildPostMap(post, manager));
        }
        return res;
    }

    @Override
    public List<Map<String, Object>> userPosts(Integer userId) {
        List<Map<String, Object>> res = new ArrayList<>();
        if (userId == null) {
            return res;
        }
        User current = currentUser();
        boolean manager = isCurrentManager();
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", userId);
        List<Post> posts = postMapper.selectList(queryWrapper);
        posts.sort(postComparator());
        for (Post post : posts) {
            if (canViewPost(post, current)) {
                res.add(buildPostMap(post, manager));
            }
        }
        return res;
    }

    @Override
    public Map<String, Object> detail(Integer pid) {
        Map<String, Object> map = new HashMap<>();
        Post post = postMapper.selectById(pid);
        User current = currentUser();
        if (post == null) {
            map.put("error_message", "帖子不存在");
            return map;
        }
        if (!canViewPost(post, current)) {
            map.put("error_message", "无权查看该帖子");
            return map;
        }
        recordViewIfNeeded(post, current);
        map.putAll(buildPostMap(post, isCurrentManager()));
        map.put("error_message", "success");
        return map;
    }

    @Override
    public Map<String, Object> userInfo(Integer userId) {
        Map<String, Object> map = new HashMap<>();
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            map.put("error_message", "用户不存在");
            return map;
        }
        User current = currentUser();
        boolean self = current != null && current.getId().equals(user.getId());
        map.put("error_message", "success");
        map.put("id", user.getId().toString());
        map.put("username", user.getUsername());
        map.put("photo", user.getPhoto());
        map.put("role", user.getRole() == null ? "USER" : user.getRole());
        map.put("title", RoleTitleUtil.displayTitle(user));
        map.put("isSelf", self);
        if (self) {
            map.put("account", user.getAccount() == null ? "" : user.getAccount());
            map.put("email", user.getEmail() == null ? "" : user.getEmail());
        }
        return map;
    }

    @Override
    public Map<String, String> view(Integer pid) {
        Map<String, String> map = new HashMap<>();
        Post post = postMapper.selectById(pid);
        User current = currentUser();
        if (post == null) {
            map.put("error_message", "帖子不存在");
            return map;
        }
        if (!canViewPost(post, current)) {
            map.put("error_message", "无权查看该帖子");
            return map;
        }
        recordViewIfNeeded(post, current);
        map.put("error_message", "success");
        map.put("viewCount", post.getViewCount().toString());
        if (isCurrentManager()) {
            map.put("hotScore", String.format("%.2f", calculateHotScore(post)));
        }
        return map;
    }

    private void recordViewIfNeeded(Post post, User current) {
        if (post == null || current == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<String> lastViewTimes = jdbcTemplate.query(
                "SELECT last_view_time FROM post_view_record WHERE pid = ? AND user_id = ?",
                (rs, rowNum) -> rs.getString("last_view_time"),
                post.getPid(),
                current.getId()
        );
        if (lastViewTimes.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO post_view_record(pid, user_id, last_view_time) VALUES (?, ?, ?)",
                    post.getPid(),
                    current.getId(),
                    now.format(VIEW_TIME_FORMATTER)
            );
            incrementViewCount(post);
            return;
        }
        LocalDateTime lastViewTime = parseViewTime(lastViewTimes.get(0));
        if (lastViewTime == null || Duration.between(lastViewTime, now).toMinutes() >= VIEW_DEDUP_MINUTES) {
            jdbcTemplate.update(
                    "UPDATE post_view_record SET last_view_time = ? WHERE pid = ? AND user_id = ?",
                    now.format(VIEW_TIME_FORMATTER),
                    post.getPid(),
                    current.getId()
            );
            incrementViewCount(post);
        }
    }

    private void incrementViewCount(Post post) {
        int viewCount = post.getViewCount() == null ? 1 : post.getViewCount();
        post.setViewCount(viewCount + 1);
        postMapper.updateById(post);
    }

    private LocalDateTime parseViewTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), VIEW_TIME_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, String> delete(Integer pid) {
        Map<String, String> map = new HashMap<>();
        User current = currentUser();
        if (current == null) {
            map.put("error_message", "请先登录");
            return map;
        }
        Post post = postMapper.selectById(pid);
        if (post == null) {
            map.put("error_message", "帖子不存在");
            return map;
        }
        if (!current.getId().equals(post.getId())) {
            map.put("error_message", "只能删除自己发布的帖子");
            return map;
        }
        postCascadeDeleteHelper.delete(pid);
        map.put("error_message", "success");
        return map;
    }

    private Map<String, Object> buildPostMap(Post post, boolean includeHotScore) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", post.getId().toString());
        map.put("pid", post.getPid().toString());
        map.put("title", post.getTitle() == null ? "" : post.getTitle());
        map.put("content", post.getContent());
        map.put("timer", post.getTimer());
        map.put("visibility", normalizeVisibility(post.getVisibility()));
        map.put("imageUrls", parseImageUrls(post.getImageUrls()));
        map.put("viewCount", post.getViewCount() == null ? 1 : post.getViewCount());
        map.put("agreeCount", countAgree(post.getPid()));
        map.put("commentCount", countComment(post.getPid()));
        map.put("isTop", Boolean.TRUE.equals(post.getIsTop()));
        map.put("topTime", post.getTopTime() == null ? "" : post.getTopTime());
        if (includeHotScore) {
            map.put("hotScore", calculateHotScore(post));
        }

        Section section = post.getSectionId() == null ? null : sectionMapper.selectById(post.getSectionId());
        map.put("sectionId", section == null ? "" : section.getSid().toString());
        map.put("sectionName", section == null ? "综合讨论" : section.getName());

        User user = userMapper.selectById(post.getId());
        String role = user == null || user.getRole() == null ? "USER" : user.getRole();
        map.put("photo", user == null ? "" : user.getPhoto());
        map.put("username", user == null ? "未知用户" : user.getUsername());
        map.put("authorRole", role);
        map.put("authorTitle", RoleTitleUtil.displayTitle(user));
        return map;
    }

    private boolean canViewPost(Post post, User current) {
        if (post == null) {
            return false;
        }
        if (!"PRIVATE".equals(normalizeVisibility(post.getVisibility()))) {
            return true;
        }
        return current != null && (current.getId().equals(post.getId()) || isManager(current));
    }

    private String normalizeVisibility(String visibility) {
        return "PRIVATE".equals(visibility) ? "PRIVATE" : "PUBLIC";
    }

    private boolean isCurrentManager() {
        return isManager(currentUser());
    }

    private boolean isManager(User user) {
        if (user == null || "DISABLED".equals(user.getStatus())) {
            return false;
        }
        String role = user.getRole();
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "OWNER".equals(role);
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) authentication.getPrincipal()).getUser();
        }
        return null;
    }

    private List<String> parseImageUrls(String imageUrls) {
        if (imageUrls == null || imageUrls.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(imageUrls, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Comparator<Post> postComparator() {
        return (left, right) -> {
            boolean leftTop = Boolean.TRUE.equals(left.getIsTop());
            boolean rightTop = Boolean.TRUE.equals(right.getIsTop());
            if (leftTop != rightTop) {
                return leftTop ? -1 : 1;
            }
            if (leftTop) {
                return nullSafe(right.getTopTime()).compareTo(nullSafe(left.getTopTime()));
            }
            return Integer.compare(right.getPid(), left.getPid());
        };
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private double calculateHotScore(Post post) {
        int viewCount = post.getViewCount() == null ? 1 : post.getViewCount();
        double hours = Math.max(0.0, hoursSince(post.getTimer()));
        return ((viewCount - 1.0) / (hours + 2.0)) * 100.0;
    }

    private double hoursSince(String timer) {
        if (timer == null || timer.trim().isEmpty()) {
            return 0.0;
        }
        try {
            LocalDateTime created = LocalDateTime.parse(timer.trim(), TIMER_FORMATTER);
            return Duration.between(created, LocalDateTime.now()).toMinutes() / 60.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Long countAgree(Integer pid) {
        QueryWrapper<Agree> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pid", pid);
        return agreeMapper.selectCount(queryWrapper);
    }

    private Long countComment(Integer pid) {
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pid", pid);
        return commentMapper.selectCount(queryWrapper);
    }

    private BlockFilter loadBlockFilter(User current) {
        BlockFilter filter = new BlockFilter();
        if (current == null) {
            return filter;
        }
        QueryWrapper<BlockRule> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", current.getId());
        List<BlockRule> rules = blockRuleMapper.selectList(queryWrapper);
        for (BlockRule rule : rules) {
            if ("USER".equals(rule.getTargetType())) {
                filter.userIds.add(rule.getTargetId());
            }
            if ("SECTION".equals(rule.getTargetType())) {
                filter.sectionIds.add(rule.getTargetId());
            }
        }
        return filter;
    }

    private static class BlockFilter {
        private final Set<Integer> userIds = new HashSet<>();
        private final Set<Integer> sectionIds = new HashSet<>();

        private boolean isBlocked(Post post) {
            if (post == null) {
                return false;
            }
            return userIds.contains(post.getId()) || sectionIds.contains(post.getSectionId());
        }
    }
}
