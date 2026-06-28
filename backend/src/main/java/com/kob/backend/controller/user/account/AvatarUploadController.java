package com.kob.backend.controller.user.account;

import com.kob.backend.service.user.account.AvatarUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class AvatarUploadController {
    @Autowired
    private AvatarUploadService avatarUploadService;

    @PostMapping("/user/account/avatar/upload/")
    public Map<String, String> upload(@RequestParam("avatar") MultipartFile avatar) {
        return avatarUploadService.upload(avatar);
    }
}
