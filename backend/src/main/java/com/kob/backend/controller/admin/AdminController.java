package com.kob.backend.controller.admin;

import com.kob.backend.service.admin.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AdminController {
    @Autowired
    private AdminService adminService;

    @GetMapping("/admin/users/")
    public List<Map<String, String>> users() {
        return adminService.users();
    }

    @PostMapping("/admin/users/status/")
    public Map<String, String> updateUserStatus(@RequestParam Map<String, String> map) {
        return adminService.updateUserStatus(Integer.parseInt(map.get("id")), map.get("status"));
    }

    @PostMapping("/admin/users/role/")
    public Map<String, String> updateUserRole(@RequestParam Map<String, String> map) {
        return adminService.updateUserRole(Integer.parseInt(map.get("id")), map.get("role"));
    }

    @PostMapping("/admin/users/custom-title/")
    public Map<String, String> updateUserCustomTitle(@RequestParam Map<String, String> map) {
        return adminService.updateUserCustomTitle(Integer.parseInt(map.get("id")), map.get("customTitle"));
    }

    @PostMapping("/admin/users/reset-profile/")
    public Map<String, String> resetUserProfile(@RequestParam Map<String, String> map) {
        return adminService.resetUserProfile(Integer.parseInt(map.get("id")));
    }

    @GetMapping("/admin/posts/")
    public List<Map<String, String>> posts() {
        return adminService.posts();
    }

    @PostMapping("/admin/posts/delete/")
    public Map<String, String> deletePost(@RequestParam Map<String, String> map) {
        return adminService.deletePost(Integer.parseInt(map.get("pid")));
    }

    @PostMapping("/admin/posts/pin/")
    public Map<String, String> pinPost(@RequestParam Map<String, String> map) {
        return adminService.pinPost(Integer.parseInt(map.get("pid")), Boolean.parseBoolean(map.get("isTop")));
    }

    @GetMapping("/admin/comments/")
    public List<Map<String, String>> comments() {
        return adminService.comments();
    }

    @PostMapping("/admin/comments/delete/")
    public Map<String, String> deleteComment(@RequestParam Map<String, String> map) {
        return adminService.deleteComment(Integer.parseInt(map.get("cid")));
    }

    @PostMapping("/admin/sections/create/")
    public Map<String, String> createSection(@RequestParam Map<String, String> map) {
        return adminService.createSection(map.get("name"), map.get("description"));
    }

    @PostMapping("/admin/sections/delete/")
    public Map<String, String> deleteSection(@RequestParam Map<String, String> map) {
        return adminService.deleteSection(Integer.parseInt(map.get("sid")));
    }
}
