package com.kob.backend.service.impl.user.account;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import com.kob.backend.service.user.account.EmailCodeService;
import com.kob.backend.service.user.account.RegisterService;
import com.kob.backend.utils.ForumConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class RegisterServiceImpl implements RegisterService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,30}$");

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailCodeService emailCodeService;

    @Override
    public Map<String, String> Register(String username, String account, String email, String password, String confirmedPassword, String emailCode) {
        Map<String, String> map = new HashMap<>();
        String nextUsername = username == null ? "" : username.trim();
        String nextAccount = account == null ? "" : account.trim();
        String nextEmail = email == null ? "" : email.trim().toLowerCase();
        if (nextUsername.isEmpty()) {
            map.put("error_message", "用户名不能为空");
            return map;
        }
        if (!isValidAccount(nextAccount)) {
            map.put("error_message", "账号只能包含 4-30 位字母、数字或下划线");
            return map;
        }
        if (!isValidEmail(nextEmail)) {
            map.put("error_message", "邮箱格式不正确");
            return map;
        }
        if (password == null || password.isEmpty() || confirmedPassword == null || confirmedPassword.isEmpty()) {
            map.put("error_message", "密码不能为空");
            return map;
        }
        if (!password.equals(confirmedPassword)) {
            map.put("error_message", "两次密码不一致");
            return map;
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", nextUsername);
        List<User> list = userMapper.selectList(queryWrapper);
        if (!list.isEmpty()) {
            map.put("error_message", "用户名已存在");
            return map;
        }

        QueryWrapper<User> accountWrapper = new QueryWrapper<>();
        accountWrapper.eq("account", nextAccount);
        List<User> accountUsers = userMapper.selectList(accountWrapper);
        if (!accountUsers.isEmpty()) {
            map.put("error_message", "账号已存在");
            return map;
        }

        QueryWrapper<User> emailWrapper = new QueryWrapper<>();
        emailWrapper.eq("email", nextEmail);
        List<User> emailUsers = userMapper.selectList(emailWrapper);
        if (!emailUsers.isEmpty()) {
            map.put("error_message", "邮箱已被注册");
            return map;
        }

        if (!emailCodeService.verifyAndConsume(nextEmail, emailCode)) {
            map.put("error_message", "邮箱验证码错误或已过期");
            return map;
        }

        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(null, nextUsername, nextAccount, nextEmail, encodedPassword, ForumConstants.DEFAULT_AVATAR, "USER", "ACTIVE", "", null);
        userMapper.insert(user);
        map.put("error_message", "success");
        return map;
    }

    private boolean isValidAccount(String account) {
        return account != null && ACCOUNT_PATTERN.matcher(account).matches();
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches() && email.length() <= 255;
    }
}
