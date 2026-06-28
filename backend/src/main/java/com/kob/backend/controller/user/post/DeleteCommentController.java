package com.kob.backend.controller.user.post;

import com.kob.backend.service.user.post.DeleteCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DeleteCommentController {
    @Autowired
    private DeleteCommentService deleteCommentService;

    @PostMapping("/user/post/comment/delete/")
    public Map<String, String> delete(@RequestParam Integer cid) {
        return deleteCommentService.delete(cid);
    }
}
