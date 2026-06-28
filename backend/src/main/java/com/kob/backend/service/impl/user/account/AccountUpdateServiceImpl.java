package com.kob.backend.service.impl.user.account;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.account.AccountUpdateService;
import com.kob.backend.service.user.account.EmailCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class AccountUpdateServiceImpl implements AccountUpdateService {
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,30}$");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmailCodeService emailCodeService;

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return null;
        }
        return ((UserDetailsImpl) authentication.getPrincipal()).getUser();
    }

    @Override
    public Map<String, String> sendCode() {
        Map<String, String> map = new HashMap<>();
        User current = currentUser();
        if (current == null) {
            map.put("error_message", "请先登录");
            return map;
        }
        User user = userMapper.selectById(current.getId());
        String email = user == null ? "" : user.getEmail();
        if (email == null || email.trim().isEmpty()) {
            map.put("error_message", "当前账号未绑定邮箱");
            return map;
        }
        return emailCodeService.sendCode(email);
    }

    @Override
    public Map<String, String> update(String account, String emailCode) {
        Map<String, String> map = new HashMap<>();
        User current = currentUser();
        if (current == null) {
            map.put("error_message", "请先登录");
            return map;
        }
        User user = userMapper.selectById(current.getId());
        if (user == null) {
            map.put("error_message", "用户不存在");
            return map;
        }
        if ("admin".equals(user.getUsername())) {
            map.put("error_message", "默认管理员账号不能修改");
            return map;
        }

        String nextAccount = account == null ? "" : account.trim();
        if (!ACCOUNT_PATTERN.matcher(nextAccount).matches()) {
            map.put("error_message", "账号只能包含 4-30 位字母、数字或下划线");
            return map;
        }
        if (nextAccount.equals(user.getAccount())) {
            map.put("error_message", "新账号不能与当前账号相同");
            return map;
        }
        if (!canUpdateNow(user.getAccountUpdateTime())) {
            map.put("error_message", "账号每半年只能修改一次");
            return map;
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account", nextAccount);
        List<User> users = userMapper.selectList(queryWrapper);
        if (!users.isEmpty() && !users.get(0).getId().equals(user.getId())) {
            map.put("error_message", "账号已存在");
            return map;
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            map.put("error_message", "当前账号未绑定邮箱");
            return map;
        }
        if (!emailCodeService.verifyAndConsume(user.getEmail(), emailCode)) {
            map.put("error_message", "邮箱验证码错误或已过期");
            return map;
        }

        String now = LocalDateTime.now().format(TIME_FORMATTER);
        user.setAccount(nextAccount);
        user.setAccountUpdateTime(now);
        userMapper.updateById(user);
        current.setAccount(user.getAccount());
        current.setAccountUpdateTime(user.getAccountUpdateTime());

        map.put("error_message", "success");
        map.put("account", user.getAccount());
        map.put("accountUpdateTime", user.getAccountUpdateTime());
        return map;
    }

    private boolean canUpdateNow(String accountUpdateTime) {
        if (accountUpdateTime == null || accountUpdateTime.trim().isEmpty()) {
            return true;
        }
        try {
            LocalDateTime last = LocalDateTime.parse(accountUpdateTime.trim(), TIME_FORMATTER);
            return !last.plusMonths(6).isAfter(LocalDateTime.now());
        } catch (Exception e) {
            return true;
        }
    }
}
