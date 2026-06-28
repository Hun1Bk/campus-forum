package com.kob.backend.service.impl.user.account;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.account.UpdateProfileService;
import com.kob.backend.utils.RoleTitleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UpdateProfileServiceImpl implements UpdateProfileService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return null;
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getUser();
    }

    @Override
    public Map<String, String> update(String username, String photo, String password, String confirmedPassword) {
        Map<String, String> map = new HashMap<>();
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            map.put("error_message", "请先登录");
            return map;
        }

        String nextUsername = username == null ? "" : username.trim();
        String nextPhoto = photo == null ? "" : photo.trim();
        if (nextUsername.isEmpty()) {
            map.put("error_message", "用户名不能为空");
            return map;
        }
        if (nextPhoto.isEmpty()) {
            map.put("error_message", "头像地址不能为空");
            return map;
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", nextUsername);
        List<User> users = userMapper.selectList(queryWrapper);
        if (!users.isEmpty() && !users.get(0).getId().equals(currentUser.getId())) {
            map.put("error_message", "用户名已存在");
            return map;
        }

        User user = userMapper.selectById(currentUser.getId());
        if (user == null) {
            map.put("error_message", "用户不存在");
            return map;
        }

        user.setUsername(nextUsername);
        user.setPhoto(nextPhoto);

        boolean shouldUpdatePassword = password != null && !password.isEmpty();
        if (shouldUpdatePassword) {
            if (confirmedPassword == null || confirmedPassword.isEmpty()) {
                map.put("error_message", "请确认新密码");
                return map;
            }
            if (!password.equals(confirmedPassword)) {
                map.put("error_message", "两次密码不一致");
                return map;
            }
            user.setPassword(passwordEncoder.encode(password));
        }

        userMapper.updateById(user);
        currentUser.setUsername(user.getUsername());
        currentUser.setAccount(user.getAccount());
        currentUser.setPhoto(user.getPhoto());
        currentUser.setPassword(user.getPassword());
        currentUser.setEmail(user.getEmail());
        currentUser.setRole(user.getRole());
        currentUser.setStatus(user.getStatus());
        currentUser.setCustomTitle(user.getCustomTitle());
        currentUser.setAccountUpdateTime(user.getAccountUpdateTime());

        map.put("error_message", "success");
        map.put("id", user.getId().toString());
        map.put("username", user.getUsername());
        map.put("account", user.getAccount() == null ? "" : user.getAccount());
        map.put("email", user.getEmail() == null ? "" : user.getEmail());
        map.put("photo", user.getPhoto());
        String role = user.getRole() == null ? "USER" : user.getRole();
        map.put("role", role);
        map.put("title", RoleTitleUtil.displayTitle(user));
        map.put("customTitle", user.getCustomTitle() == null ? "" : user.getCustomTitle());
        map.put("status", user.getStatus() == null ? "ACTIVE" : user.getStatus());
        map.put("accountUpdateTime", user.getAccountUpdateTime() == null ? "" : user.getAccountUpdateTime());
        return map;
    }
}
