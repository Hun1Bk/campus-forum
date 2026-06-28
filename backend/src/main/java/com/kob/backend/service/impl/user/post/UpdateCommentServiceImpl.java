package com.kob.backend.service.impl.user.post;

import com.kob.backend.mapper.CommentMapper;
import com.kob.backend.mapper.NotificationMapper;
import com.kob.backend.mapper.PostMapper;
import com.kob.backend.pojo.Comment;
import com.kob.backend.pojo.Notification;
import com.kob.backend.pojo.Post;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.post.UpdateCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class UpdateCommentServiceImpl implements UpdateCommentService {
    @Autowired
    private CommentMapper commentMapper;

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
    public Map<String, String> update_a_comment(String content, Integer pid, Integer id, Integer parentId) {
        Map<String, String> map = new HashMap<>();
        if (content == null || content.trim().isEmpty()) {
            map.put("error_message", "评论不能为空");
            return map;
        }

        Integer actorId = getCurrentUserId(id);
        Post post = postMapper.selectById(pid);
        if (post == null) {
            map.put("error_message", "帖子不存在");
            return map;
        }
        Integer normalizedParentId = null;
        if (parentId != null && parentId > 0) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null || !pid.equals(parent.getPid())) {
                map.put("error_message", "回复的评论不存在");
                return map;
            }
            normalizedParentId = parentId;
        }

        commentMapper.insert(new Comment(null, content, pid, actorId, normalizedParentId));
        if (!actorId.equals(post.getId())) {
            Notification notification = new Notification(
                    null,
                    post.getId(),
                    actorId,
                    pid,
                    "comment",
                    "评论了你的帖子：" + content,
                    false,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            notificationMapper.insert(notification);
        }

        map.put("error_message", "success");
        return map;
    }
}
