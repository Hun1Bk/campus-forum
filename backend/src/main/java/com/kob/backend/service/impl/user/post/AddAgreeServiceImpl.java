package com.kob.backend.service.impl.user.post;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.AgreeMapper;
import com.kob.backend.mapper.NotificationMapper;
import com.kob.backend.mapper.PostMapper;
import com.kob.backend.pojo.Agree;
import com.kob.backend.pojo.Notification;
import com.kob.backend.pojo.Post;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.post.AddAgreeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class AddAgreeServiceImpl implements AddAgreeService {
    @Autowired
    private AgreeMapper agreeMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    private Integer getCurrentUserId(Integer fallbackId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getUser().getId();
        }
        return fallbackId;
    }

    @Override
    public Map<String, String> AddAgree(int pid, int id) {
        Map<String, String> map = new HashMap<>();
        Integer actorId = getCurrentUserId(id);
        Post post = postMapper.selectById(pid);
        if (post == null) {
            map.put("error_message", "帖子不存在");
            return map;
        }

        QueryWrapper<Agree> agreeQueryWrapper = new QueryWrapper<>();
        agreeQueryWrapper.eq("pid", pid);
        agreeQueryWrapper.eq("id", actorId);
        if (agreeMapper.selectCount(agreeQueryWrapper) > 0) {
            map.put("error_message", "success");
            return map;
        }

        agreeMapper.insert(new Agree(null, pid, actorId));
        if (!actorId.equals(post.getId())) {
            Notification notification = new Notification(
                    null,
                    post.getId(),
                    actorId,
                    pid,
                    "like",
                    "点赞了你的帖子",
                    false,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            notificationMapper.insert(notification);
        }

        map.put("error_message", "success");
        return map;
    }
}
