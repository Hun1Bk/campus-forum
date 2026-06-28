package com.kob.backend.controller.user.notification;

import com.kob.backend.service.user.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @GetMapping("/user/notification/list/")
    public List<Map<String, String>> list() {
        return notificationService.list();
    }

    @GetMapping("/user/notification/unread-count/")
    public Map<String, Integer> unreadCount() {
        return notificationService.unreadCount();
    }

    @PostMapping("/user/notification/read/")
    public Map<String, String> read(@RequestParam Map<String, String> map) {
        Integer nid = Integer.parseInt(map.get("nid"));
        return notificationService.read(nid);
    }

    @PostMapping("/user/notification/readAll/")
    public Map<String, String> readAll() {
        return notificationService.readAll();
    }
}
