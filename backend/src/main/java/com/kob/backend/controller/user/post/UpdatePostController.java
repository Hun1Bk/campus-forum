package com.kob.backend.controller.user.post;

import com.kob.backend.service.user.post.UpdatePostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class UpdatePostController {
    @Autowired
    private UpdatePostService updatePostService;

    @PostMapping("/user/post/update/")
    public Map<String, String> update(@RequestParam Map<String, String> map,
                                      @RequestParam(value = "images", required = false) MultipartFile[] images) {
        Integer pid = Integer.parseInt(map.get("pid"));
        Integer sectionId = map.get("sectionId") == null ? null : Integer.parseInt(map.get("sectionId"));
        return updatePostService.update(pid, map.get("title"), map.get("content"), sectionId, map.get("visibility"), map.get("keepImageUrls"), images);
    }
}
