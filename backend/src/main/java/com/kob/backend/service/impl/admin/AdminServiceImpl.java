package com.kob.backend.service.impl.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kob.backend.mapper.AgreeMapper;
import com.kob.backend.mapper.CommentMapper;
import com.kob.backend.mapper.NotificationMapper;
import com.kob.backend.mapper.PostMapper;
import com.kob.backend.mapper.SectionMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Agree;
import com.kob.backend.pojo.Comment;
import com.kob.backend.pojo.Notification;
import com.kob.backend.pojo.Post;
import com.kob.backend.pojo.Section;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.PostCascadeDeleteHelper;
import com.kob.backend.service.admin.AdminService;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.utils.ForumConstants;
import com.kob.backend.utils.RoleTitleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TIMER_FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d H:m:s");

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private AgreeMapper agreeMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private SectionMapper sectionMapper;

    @Autowired
    private PostCascadeDeleteHelper postCascadeDeleteHelper;

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return null;
        }
        return ((UserDetailsImpl) authentication.getPrincipal()).getUser();
    }

    private String roleOf(User user) {
        return user == null || user.getRole() == null ? "USER" : user.getRole();
    }

    private boolean isOwner(User user) {
        return "OWNER".equals(roleOf(user)) && !"DISABLED".equals(user.getStatus());
    }

    private boolean isSuperAdmin(User user) {
        return "SUPER_ADMIN".equals(roleOf(user)) && !"DISABLED".equals(user.getStatus());
    }

    private boolean isManager(User user) {
        String role = roleOf(user);
        return ("ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "OWNER".equals(role))
                && !"DISABLED".equals(user.getStatus());
    }

    private void ensureManager() {
        User user = currentUser();
        if (user == null || !isManager(user)) {
            throw new AccessDeniedException("无管理员权限");
        }
    }

    private void ensureOwner() {
        User user = currentUser();
        if (user == null || !isOwner(user)) {
            throw new AccessDeniedException("仅站长可以执行该操作");
        }
    }

    private void ensureUserAdmin() {
        User user = currentUser();
        if (user == null || !(isSuperAdmin(user) || isOwner(user))) {
            throw new AccessDeniedException("仅高级管理员或站长可以执行该操作");
        }
    }

    private boolean canManageUser(User operator, User target) {
        if (operator == null || target == null || operator.getId().equals(target.getId())) {
            return false;
        }
        if (isOwner(operator)) {
            return !"OWNER".equals(roleOf(target));
        }
        return isSuperAdmin(operator) && "USER".equals(roleOf(target));
    }

    @Override
    public List<Map<String, String>> users() {
        ensureUserAdmin();
        List<Map<String, String>> res = new ArrayList<>();
        List<User> users = userMapper.selectList(null);
        for (User user : users) {
            String role = roleOf(user);
            Map<String, String> map = new HashMap<>();
            map.put("id", user.getId().toString());
            map.put("username", user.getUsername());
            map.put("photo", user.getPhoto());
            map.put("role", role);
            map.put("title", RoleTitleUtil.displayTitle(user));
            map.put("customTitle", user.getCustomTitle() == null ? "" : user.getCustomTitle());
            map.put("status", user.getStatus() == null ? "ACTIVE" : user.getStatus());
            res.add(map);
        }
        return res;
    }

    @Override
    public Map<String, String> updateUserStatus(Integer id, String status) {
        ensureUserAdmin();
        Map<String, String> map = new HashMap<>();
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            map.put("error_message", "状态不合法");
            return map;
        }
        User operator = currentUser();
        User user = userMapper.selectById(id);
        if (user == null) {
            map.put("error_message", "用户不存在");
            return map;
        }
        if (!canManageUser(operator, user)) {
            map.put("error_message", "无权管理该用户");
            return map;
        }
        user.setStatus(status);
        userMapper.updateById(user);
        map.put("error_message", "success");
        return map;
    }

    @Override
    public Map<String, String> updateUserCustomTitle(Integer id, String customTitle) {
        ensureUserAdmin();
        Map<String, String> map = new HashMap<>();
        User operator = currentUser();
        User user = userMapper.selectById(id);
        if (user == null) {
            map.put("error_message", "用户不存在");
            return map;
        }
        if (!canManageUser(operator, user) && !(isOwner(operator) && operator.getId().equals(user.getId()))) {
            map.put("error_message", "无权设置该用户头衔");
            return map;
        }
        String value = customTitle == null ? "" : customTitle.trim();
        if (value.length() > 20) {
            map.put("error_message", "专属头衔不能超过 20 个字符");
            return map;
        }
        user.setCustomTitle(value);
        userMapper.updateById(user);
        map.put("error_message", "success");
        map.put("title", RoleTitleUtil.displayTitle(user));
        map.put("customTitle", user.getCustomTitle() == null ? "" : user.getCustomTitle());
        return map;
    }

    @Override
    public Map<String, String> resetUserProfile(Integer id) {
        ensureUserAdmin();
        Map<String, String> map = new HashMap<>();
        User operator = currentUser();
        User user = userMapper.selectById(id);
        if (user == null) {
            map.put("error_message", "用户不存在");
            return map;
        }
        if (!canManageUser(operator, user)) {
            map.put("error_message", "无权重置该用户资料");
            return map;
        }
        user.setUsername("用户" + user.getId());
        user.setPhoto(ForumConstants.DEFAULT_AVATAR);
        userMapper.updateById(user);
        map.put("error_message", "success");
        return map;
    }

    @Override
    public Map<String, String> updateUserRole(Integer id, String role) {
        ensureOwner();
        Map<String, String> map = new HashMap<>();
        User owner = currentUser();
        if (owner.getId().equals(id)) {
            map.put("error_message", "不能修改自己的站长身份");
            return map;
        }
        if (!"USER".equals(role) && !"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
            map.put("error_message", "角色不合法");
            return map;
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            map.put("error_message", "用户不存在");
            return map;
        }
        if ("OWNER".equals(roleOf(user))) {
            map.put("error_message", "不能修改站长身份");
            return map;
        }
        user.setRole(role);
        userMapper.updateById(user);
        map.put("error_message", "success");
        return map;
    }

    @Override
    public List<Map<String, String>> posts() {
        ensureManager();
        List<Map<String, String>> res = new ArrayList<>();
        List<Post> posts = postMapper.selectList(null);
        posts.sort(postComparator());
        for (Post post : posts) {
            Map<String, String> map = new HashMap<>();
            map.put("pid", post.getPid().toString());
            map.put("id", post.getId().toString());
            map.put("title", post.getTitle() == null ? "" : post.getTitle());
            map.put("content", post.getContent());
            map.put("visibility", "PRIVATE".equals(post.getVisibility()) ? "PRIVATE" : "PUBLIC");
            map.put("timer", post.getTimer());
            map.put("viewCount", String.valueOf(post.getViewCount() == null ? 1 : post.getViewCount()));
            map.put("hotScore", String.format("%.2f", calculateHotScore(post)));
            map.put("isTop", Boolean.TRUE.equals(post.getIsTop()) ? "true" : "false");
            map.put("topTime", post.getTopTime() == null ? "" : post.getTopTime());
            Section section = post.getSectionId() == null ? null : sectionMapper.selectById(post.getSectionId());
            map.put("sectionId", section == null ? "" : section.getSid().toString());
            map.put("sectionName", section == null ? "综合讨论" : section.getName());
            User user = userMapper.selectById(post.getId());
            map.put("username", user == null ? "未知用户" : user.getUsername());
            map.put("authorTitle", RoleTitleUtil.displayTitle(user));
            map.put("agreeCount", countAgree(post.getPid()).toString());
            map.put("commentCount", countComment(post.getPid()).toString());
            map.put("imageCount", String.valueOf(countImages(post.getImageUrls())));
            res.add(map);
        }
        return res;
    }

    @Override
    public Map<String, String> deletePost(Integer pid) {
        ensureManager();
        Map<String, String> map = new HashMap<>();
        Post post = postMapper.selectById(pid);
        User operator = currentUser();
        postCascadeDeleteHelper.delete(pid);
        if (post != null && operator != null && !operator.getId().equals(post.getId())) {
            String title = post.getTitle() == null || post.getTitle().trim().isEmpty()
                    ? preview(post.getContent())
                    : post.getTitle().trim();
            Notification notification = new Notification(
                    null,
                    post.getId(),
                    operator.getId(),
                    pid,
                    "post_deleted",
                    "你的帖子已被管理员删除：" + title,
                    false,
                    LocalDateTime.now().format(TIMER_FORMATTER)
            );
            notificationMapper.insert(notification);
        }
        map.put("error_message", "success");
        return map;
    }

    @Override
    public Map<String, String> pinPost(Integer pid, Boolean isTop) {
        ensureManager();
        Map<String, String> map = new HashMap<>();
        Post post = postMapper.selectById(pid);
        if (post == null) {
            map.put("error_message", "帖子不存在");
            return map;
        }
        UpdateWrapper<Post> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("pid", pid)
                .set("is_top", Boolean.TRUE.equals(isTop))
                .set("top_time", Boolean.TRUE.equals(isTop) ? LocalDateTime.now().format(TIMER_FORMATTER) : null);
        postMapper.update(null, updateWrapper);
        map.put("error_message", "success");
        return map;
    }

    @Override
    public List<Map<String, String>> comments() {
        ensureUserAdmin();
        List<Map<String, String>> res = new ArrayList<>();
        List<Comment> comments = commentMapper.selectList(null);
        for (Comment comment : comments) {
            Map<String, String> map = new HashMap<>();
            map.put("cid", comment.getCid().toString());
            map.put("pid", comment.getPid().toString());
            map.put("id", comment.getId().toString());
            map.put("content", comment.getContent());
            User user = userMapper.selectById(comment.getId());
            Post post = postMapper.selectById(comment.getPid());
            map.put("username", user == null ? "未知用户" : user.getUsername());
            map.put("authorTitle", RoleTitleUtil.displayTitle(user));
            map.put("postContent", post == null ? "帖子已删除" : post.getContent());
            res.add(map);
        }
        return res;
    }

    @Override
    public Map<String, String> deleteComment(Integer cid) {
        ensureUserAdmin();
        Map<String, String> map = new HashMap<>();
        QueryWrapper<Comment> replyWrapper = new QueryWrapper<>();
        replyWrapper.eq("parent_id", cid);
        commentMapper.delete(replyWrapper);
        commentMapper.deleteById(cid);
        map.put("error_message", "success");
        return map;
    }

    @Override
    public Map<String, String> createSection(String name, String description) {
        ensureUserAdmin();
        Map<String, String> map = new HashMap<>();
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            map.put("error_message", "分区名称不能为空");
            return map;
        }
        QueryWrapper<Section> existWrapper = new QueryWrapper<>();
        existWrapper.eq("name", value);
        if (sectionMapper.selectCount(existWrapper) > 0) {
            map.put("error_message", "分区已存在");
            return map;
        }
        int sortOrder = sectionMapper.selectCount(null).intValue() + 1;
        String desc = description == null ? "" : description.trim();
        Section section = new Section(null, value, desc, sortOrder, LocalDateTime.now().format(TIMER_FORMATTER));
        sectionMapper.insert(section);
        map.put("error_message", "success");
        return map;
    }

    @Override
    public Map<String, String> deleteSection(Integer sid) {
        ensureUserAdmin();
        Map<String, String> map = new HashMap<>();
        if (sid == null || sid == 1) {
            map.put("error_message", "默认分区不能删除");
            return map;
        }
        Section section = sectionMapper.selectById(sid);
        if (section == null) {
            map.put("error_message", "分区不存在");
            return map;
        }
        UpdateWrapper<Post> postUpdateWrapper = new UpdateWrapper<>();
        postUpdateWrapper.eq("section_id", sid).set("section_id", 1);
        postMapper.update(null, postUpdateWrapper);
        sectionMapper.deleteById(sid);
        map.put("error_message", "success");
        return map;
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

    private int countImages(String imageUrls) {
        if (imageUrls == null || imageUrls.trim().isEmpty()) {
            return 0;
        }
        try {
            List<String> urls = OBJECT_MAPPER.readValue(imageUrls, new TypeReference<List<String>>() {});
            return urls.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private String preview(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "无标题";
        }
        String text = value.trim();
        return text.length() > 30 ? text.substring(0, 30) : text;
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
}
