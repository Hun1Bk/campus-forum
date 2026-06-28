package com.kob.backend.controller.user.account;

import com.kob.backend.service.impl.user.account.LoginServiceImpl;
import com.kob.backend.service.user.account.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class LoginController {
    @Autowired
    LoginService loginService;
    @PostMapping("/user/account/token/")
    public Map<String,String> login(@RequestParam Map<String,String> map){
        String username = map.get("account") == null ? map.get("username") : map.get("account");
        String email = map.get("email");
        String password = map.get("password");
        try {
            return loginService.getToken(username, email, password);
        } catch (Exception e) {
            Map<String, String> result = new HashMap<>();
            result.put("error_message", "账号、邮箱或密码错误");
            return result;
        }
    }

    @PostMapping("/user/account/email-login/code/")
    public Map<String, String> emailLoginCode(@RequestParam Map<String, String> map) {
        return loginService.sendEmailLoginCode(map.get("email"));
    }

    @PostMapping("/user/account/email-login/")
    public Map<String, String> emailLogin(@RequestParam Map<String, String> map) {
        return loginService.emailLogin(map.get("email"), map.get("emailCode"));
    }

    @PostMapping("/user/account/password/reset/code/")
    public Map<String, String> resetPasswordCode(@RequestParam Map<String, String> map) {
        return loginService.sendResetPasswordCode(map.get("email"));
    }

    @PostMapping("/user/account/password/reset/")
    public Map<String, String> resetPassword(@RequestParam Map<String, String> map) {
        return loginService.resetPassword(map.get("email"), map.get("emailCode"), map.get("password"), map.get("confirmedPassword"));
    }
}
