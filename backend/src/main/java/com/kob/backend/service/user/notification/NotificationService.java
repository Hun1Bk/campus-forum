package com.kob.backend.service.user.notification;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    List<Map<String, String>> list();

    Map<String, Integer> unreadCount();

    Map<String, String> read(Integer nid);

    Map<String, String> readAll();
}
