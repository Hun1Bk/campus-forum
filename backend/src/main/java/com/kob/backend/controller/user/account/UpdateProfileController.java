package com.kob.backend.controller.user.account;

import com.kob.backend.service.user.account.UpdateProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UpdateProfileController {
    @Autowired
    private UpdateProfileService updateProfileService;

    @PostMapping("/user/account/update/")
    public Map<String, String> update(@RequestParam Map<String, String> map) {
        return updateProfileService.update(
                map.get("username"),
                map.get("photo"),
                map.get("password"),
                map.get("confirmedPassword")
        );
    }
}
