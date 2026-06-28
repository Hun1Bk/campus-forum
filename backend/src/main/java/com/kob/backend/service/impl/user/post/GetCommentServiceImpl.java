package com.kob.backend.service.impl.user.post;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.CommentMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Comment;
import com.kob.backend.pojo.User;
import com.kob.backend.service.user.post.GetCommentService;
import com.kob.backend.utils.RoleTitleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GetCommentServiceImpl implements GetCommentService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private UserMapper userMapper;
    @Override
    public List<Map<String, String>> get_comment(int pid) {
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pid",pid);
        queryWrapper.orderByAsc("cid");
        List<Comment> list = commentMapper.selectList(queryWrapper);
        List<Map<String,String>> res = new ArrayList<>();
        for(Comment comment:list){
            Map<String,String> map = new HashMap<>();
            map.put("cid", comment.getCid().toString());
            map.put("id", comment.getId().toString());
            map.put("content",comment.getContent());
            map.put("pid",comment.getPid().toString());
            map.put("parentId", comment.getParentId() == null ? "" : comment.getParentId().toString());
            if (comment.getParentId() != null) {
                Comment parent = commentMapper.selectById(comment.getParentId());
                User parentUser = parent == null ? null : userMapper.selectById(parent.getId());
                map.put("replyToUsername", parentUser == null ? "" : parentUser.getUsername());
            } else {
                map.put("replyToUsername", "");
            }
            User user = userMapper.selectById(comment.getId());
            String role = user == null || user.getRole() == null ? "USER" : user.getRole();
            map.put("username", user == null ? "未知用户" : user.getUsername());
            map.put("photo", user == null ? "" : user.getPhoto());
            map.put("authorRole", role);
            map.put("authorTitle", RoleTitleUtil.displayTitle(user));
            res.add(map);
        }
        return res;
    }
}
