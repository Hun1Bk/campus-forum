package com.kob.backend.service.impl.user.account;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.account.EmailCodeService;
import com.kob.backend.service.user.account.LoginService;
import com.kob.backend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class LoginServiceImpl implements LoginService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmailCodeService emailCodeService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Map<String,String> getToken(String account, String email, String password) {
        String loginAccount = account == null ? "" : account.trim();
        String loginEmail = normalizeEmail(email);
        if (loginAccount.isEmpty() && !loginEmail.isEmpty()) {
            User user = getUserByEmail(loginEmail);
            if (user == null) {
                return error("邮箱或密码错误");
            }
            if (!isActive(user)) {
                return error("账号已被禁用");
            }
            loginAccount = user.getAccount();
        }
        if (!loginAccount.isEmpty()) {
            User user = getUserByAccount(loginAccount);
            if (user != null && !isActive(user)) {
                return error("账号已被禁用");
            }
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginAccount, password);

        Authentication authenticate = authenticationManager.authenticate(authenticationToken);  // 登录失败，会自动处理
        UserDetailsImpl loginUser = (UserDetailsImpl) authenticate.getPrincipal();
        User user = loginUser.getUser();
        if (!isActive(user)) {
            return error("账号已被禁用");
        }
        return tokenOf(user);
    }

    @Override
    public Map<String, String> sendEmailLoginCode(String email) {
        User user = getUserByEmail(normalizeEmail(email));
        if (user == null) return error("邮箱未注册");
        if (!isActive(user)) return error("账号已被禁用");
        return emailCodeService.sendCode(user.getEmail(), "LOGIN");
    }

    @Override
    public Map<String, String> emailLogin(String email, String emailCode) {
        User user = getUserByEmail(normalizeEmail(email));
        if (user == null) return error("邮箱未注册");
        if (!isActive(user)) return error("账号已被禁用");
        if (!emailCodeService.verifyAndConsume(user.getEmail(), emailCode, "LOGIN")) {
            return error("邮箱验证码错误或已过期");
        }
        return tokenOf(user);
    }

    @Override
    public Map<String, String> sendResetPasswordCode(String email) {
        User user = getUserByEmail(normalizeEmail(email));
        if (user == null) return error("邮箱未注册");
        if (!isActive(user)) return error("账号已被禁用");
        return emailCodeService.sendCode(user.getEmail(), "RESET_PASSWORD");
    }

    @Override
    public Map<String, String> resetPassword(String email, String emailCode, String password, String confirmedPassword) {
        User user = getUserByEmail(normalizeEmail(email));
        if (user == null) return error("邮箱未注册");
        if (!isActive(user)) return error("账号已被禁用");
        if (password == null || password.isEmpty() || confirmedPassword == null || confirmedPassword.isEmpty()) {
            return error("密码不能为空");
        }
        if (!password.equals(confirmedPassword)) {
            return error("两次密码不一致");
        }
        if (!emailCodeService.verifyAndConsume(user.getEmail(), emailCode, "RESET_PASSWORD")) {
            return error("邮箱验证码错误或已过期");
        }
        user.setPassword(passwordEncoder.encode(password));
        userMapper.updateById(user);
        Map<String, String> map = new HashMap<>();
        map.put("error_message", "success");
        return map;
    }

    private Map<String, String> tokenOf(User user) {
        String jwt = JwtUtil.createJWT(user.getId().toString());

        Map<String, String> map = new HashMap<>();
        map.put("error_message", "success");
        map.put("token", jwt);

        return map;
    }

    private User getUserByEmail(String email) {
        if (!isValidEmail(email)) return null;
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        return userMapper.selectOne(queryWrapper);
    }

    private User getUserByAccount(String account) {
        String value = account == null ? "" : account.trim();
        if (value.isEmpty()) return null;
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account", value);
        return userMapper.selectOne(queryWrapper);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches() && email.length() <= 255;
    }

    private boolean isActive(User user) {
        return user != null && !"DISABLED".equals(user.getStatus());
    }

    private Map<String, String> error(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("error_message", message);
        return map;
    }
}
