package com.kob.backend.service.impl.user.account;

import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.account.InfoService;
import com.kob.backend.utils.RoleTitleUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class InfoServiceImpl implements InfoService {
    @Override
    public Map<String, String> getInfo() {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl loginUser = (UserDetailsImpl) authentication.getPrincipal();
        User user = loginUser.getUser();

        Map<String, String> map = new HashMap<>();
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
