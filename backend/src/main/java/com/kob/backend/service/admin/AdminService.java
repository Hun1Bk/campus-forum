package com.kob.backend.service.admin;

import java.util.List;
import java.util.Map;

public interface AdminService {
    List<Map<String, String>> users();

    Map<String, String> updateUserStatus(Integer id, String status);

    Map<String, String> updateUserRole(Integer id, String role);

    Map<String, String> updateUserCustomTitle(Integer id, String customTitle);

    Map<String, String> resetUserProfile(Integer id);

    List<Map<String, String>> posts();

    Map<String, String> deletePost(Integer pid);

    Map<String, String> pinPost(Integer pid, Boolean isTop);

    List<Map<String, String>> comments();

    Map<String, String> deleteComment(Integer cid);

    Map<String, String> createSection(String name, String description);

    Map<String, String> deleteSection(Integer sid);
}
