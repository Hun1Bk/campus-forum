package com.kob.backend.utils;

import com.kob.backend.pojo.User;

public class RoleTitleUtil {
    public static String displayTitle(User user) {
        if (user == null) {
            return roleTitle("USER");
        }
        String customTitle = user.getCustomTitle();
        if (customTitle != null && !customTitle.trim().isEmpty()) {
            return customTitle.trim();
        }
        return roleTitle(user.getRole());
    }

    public static String roleTitle(String role) {
        if ("OWNER".equals(role)) return "站长";
        if ("SUPER_ADMIN".equals(role)) return "高级管理员";
        if ("ADMIN".equals(role)) return "管理员";
        return "普通用户";
    }

    private RoleTitleUtil() {
    }
}
