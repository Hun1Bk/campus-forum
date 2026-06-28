package com.kob.backend.controller.user.account;

import com.kob.backend.service.user.account.AccountUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AccountUpdateController {
    @Autowired
    private AccountUpdateService accountUpdateService;

    @PostMapping("/user/account/account/code/")
    public Map<String, String> sendCode() {
        return accountUpdateService.sendCode();
    }

    @PostMapping("/user/account/account/update/")
    public Map<String, String> update(@RequestParam Map<String, String> map) {
        return accountUpdateService.update(map.get("account"), map.get("emailCode"));
    }
}
