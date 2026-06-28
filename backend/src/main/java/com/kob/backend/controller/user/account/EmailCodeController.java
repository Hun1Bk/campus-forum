package com.kob.backend.controller.user.account;

import com.kob.backend.service.user.account.EmailCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class EmailCodeController {
    @Autowired
    private EmailCodeService emailCodeService;

    @PostMapping("/user/account/email/code/")
    public Map<String, String> sendCode(@RequestParam Map<String, String> map) {
        return emailCodeService.sendCode(map.get("email"));
    }
}
