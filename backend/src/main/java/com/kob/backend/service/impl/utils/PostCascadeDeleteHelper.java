package com.kob.backend.service.impl.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.AgreeMapper;
import com.kob.backend.mapper.CommentMapper;
import com.kob.backend.mapper.NotificationMapper;
import com.kob.backend.mapper.PostMapper;
import com.kob.backend.pojo.Agree;
import com.kob.backend.pojo.Comment;
import com.kob.backend.pojo.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostCascadeDeleteHelper {
    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private AgreeMapper agreeMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void delete(Integer pid) {
        QueryWrapper<Comment> commentQueryWrapper = new QueryWrapper<>();
        commentQueryWrapper.eq("pid", pid);
        commentMapper.delete(commentQueryWrapper);

        QueryWrapper<Agree> agreeQueryWrapper = new QueryWrapper<>();
        agreeQueryWrapper.eq("pid", pid);
        agreeMapper.delete(agreeQueryWrapper);

        QueryWrapper<Notification> notificationQueryWrapper = new QueryWrapper<>();
        notificationQueryWrapper.eq("pid", pid);
        notificationMapper.delete(notificationQueryWrapper);

        jdbcTemplate.update("DELETE FROM post_view_record WHERE pid = ?", pid);

        postMapper.deleteById(pid);
    }
}
