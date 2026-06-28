package com.kob.backend.controller.user.post;

import com.kob.backend.service.user.post.GetPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class GetController {
    @Autowired
    private GetPostService getPostService;
    @GetMapping("/user/post/get/")
    public List<Map<String, Object>> get(
            @RequestParam(value = "sectionId", required = false) Integer sectionId,
            @RequestParam(value = "keyword", required = false) String keyword
    ){
        return getPostService.get_post(sectionId, keyword);
    }

    @GetMapping("/user/post/hot/")
    public List<Map<String, Object>> hot() {
        return getPostService.hot();
    }

    @GetMapping("/user/post/pinned/")
    public List<Map<String, Object>> pinned() {
        return getPostService.pinned();
    }

    @GetMapping("/user/post/mine/")
    public List<Map<String, Object>> mine() {
        return getPostService.mine();
    }

    @GetMapping("/user/post/user/")
    public List<Map<String, Object>> userPosts(@RequestParam Integer userId) {
        return getPostService.userPosts(userId);
    }

    @GetMapping("/user/post/detail/")
    public Map<String, Object> detail(@RequestParam Integer pid) {
        return getPostService.detail(pid);
    }

    @GetMapping("/user/profile/info/")
    public Map<String, Object> userInfo(@RequestParam Integer userId) {
        return getPostService.userInfo(userId);
    }

    @PostMapping("/user/post/view/")
    public Map<String, String> view(@RequestParam Integer pid) {
        return getPostService.view(pid);
    }

    @PostMapping("/user/post/delete/")
    public Map<String, String> delete(@RequestParam Integer pid) {
        return getPostService.delete(pid);
    }
}
