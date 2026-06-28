package com.kob.backend.service.impl.user.post;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.CommentMapper;
import com.kob.backend.mapper.PostMapper;
import com.kob.backend.pojo.Comment;
import com.kob.backend.pojo.Post;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.post.DeleteCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DeleteCommentServiceImpl implements DeleteCommentService {
    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PostMapper postMapper;

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return null;
        }
        return ((UserDetailsImpl) authentication.getPrincipal()).getUser();
    }

    @Override
    public Map<String, String> delete(Integer cid) {
        Map<String, String> map = new HashMap<>();
        User current = currentUser();
        if (current == null) {
            map.put("error_message", "请先登录");
            return map;
        }
        Comment comment = commentMapper.selectById(cid);
        if (comment == null) {
            map.put("error_message", "评论不存在");
            return map;
        }
        Post post = postMapper.selectById(comment.getPid());
        if (post == null) {
            map.put("error_message", "帖子不存在");
            return map;
        }
        if (!canDelete(current, post, comment)) {
            map.put("error_message", "无权删除该评论");
            return map;
        }
        QueryWrapper<Comment> replyWrapper = new QueryWrapper<>();
        replyWrapper.eq("parent_id", cid);
        commentMapper.delete(replyWrapper);
        commentMapper.deleteById(cid);
        map.put("error_message", "success");
        return map;
    }

    private boolean canDelete(User current, Post post, Comment comment) {
        if (current.getId().equals(comment.getId()) || current.getId().equals(post.getId())) {
            return true;
        }
        String role = current.getRole();
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "OWNER".equals(role);
    }
}
