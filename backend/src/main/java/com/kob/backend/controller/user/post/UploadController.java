package com.kob.backend.controller.user.post;


import com.kob.backend.service.user.post.UploadPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class UploadController {
    @Autowired
    private UploadPostService uploadPostService;
    @PostMapping("/user/post/write/")
    public Map<String,String> write(@RequestParam Map<String,String> map,
                                    @RequestParam(value = "images", required = false) MultipartFile[] images){
        String title = map.get("title");
        String content = map.get("content");
        String create_time = map.get("timer");
        Integer id = Integer.parseInt(map.get("id"));
        Integer sectionId = map.get("sectionId") == null ? null : Integer.parseInt(map.get("sectionId"));
        return uploadPostService.upload_a_post(title, content,create_time,id, sectionId, map.get("visibility"), images);
    }
}
