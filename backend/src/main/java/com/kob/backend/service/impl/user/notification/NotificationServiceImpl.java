package com.kob.backend.service.impl.user.notification;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.NotificationMapper;
import com.kob.backend.mapper.PostMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Notification;
import com.kob.backend.pojo.Post;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.notification.NotificationService;
import com.kob.backend.utils.RoleTitleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {
    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return null;
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getUser().getId();
    }

    @Override
    public List<Map<String, String>> list() {
        Integer currentUserId = getCurrentUserId();
        List<Map<String, String>> res = new ArrayList<>();
        if (currentUserId == null) {
            return res;
        }

        QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("receiver_id", currentUserId);
        queryWrapper.orderByDesc("nid");
        List<Notification> notifications = notificationMapper.selectList(queryWrapper);

        for (Notification notification : notifications) {
            Map<String, String> map = new HashMap<>();
            map.put("nid", notification.getNid().toString());
            map.put("receiverId", notification.getReceiverId().toString());
            map.put("actorId", notification.getActorId().toString());
            map.put("pid", notification.getPid().toString());
            map.put("type", notification.getType());
            map.put("content", notification.getContent());
            map.put("isRead", notification.getIsRead().toString());
            map.put("createTime", notification.getCreateTime());

            User actor = userMapper.selectById(notification.getActorId());
            if (actor != null) {
                map.put("actorUsername", actor.getUsername());
                map.put("actorPhoto", actor.getPhoto());
                map.put("actorRole", actor.getRole() == null ? "USER" : actor.getRole());
                map.put("actorTitle", RoleTitleUtil.displayTitle(actor));
            }

            Post post = postMapper.selectById(notification.getPid());
            if (post != null) {
                map.put("postContent", post.getContent());
            } else {
                map.put("postContent", notification.getContent() == null ? "帖子已删除" : notification.getContent());
            }
            res.add(map);
        }
        return res;
    }

    @Override
    public Map<String, Integer> unreadCount() {
        Map<String, Integer> map = new HashMap<>();
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            map.put("cnt", 0);
            return map;
        }

        QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("receiver_id", currentUserId);
        queryWrapper.eq("is_read", false);
        map.put("cnt", notificationMapper.selectCount(queryWrapper).intValue());
        return map;
    }

    @Override
    public Map<String, String> read(Integer nid) {
        Map<String, String> map = new HashMap<>();
        Integer currentUserId = getCurrentUserId();
        Notification notification = notificationMapper.selectById(nid);
        if (currentUserId == null || notification == null || !currentUserId.equals(notification.getReceiverId())) {
            map.put("error_message", "通知不存在");
            return map;
        }

        notification.setIsRead(true);
        notificationMapper.updateById(notification);
        map.put("error_message", "success");
        return map;
    }

    @Override
    public Map<String, String> readAll() {
        Map<String, String> map = new HashMap<>();
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            map.put("error_message", "请先登录");
            return map;
        }

        QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("receiver_id", currentUserId);
        queryWrapper.eq("is_read", false);
        List<Notification> notifications = notificationMapper.selectList(queryWrapper);
        for (Notification notification : notifications) {
            notification.setIsRead(true);
            notificationMapper.updateById(notification);
        }
        map.put("error_message", "success");
        return map;
    }

}
