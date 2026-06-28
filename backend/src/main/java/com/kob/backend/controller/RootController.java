package com.kob.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, String> index() {
        Map<String, String> map = new HashMap<>();
        map.put("name", "forum backend");
        map.put("status", "running");
        map.put("frontend", "http://localhost:8081");
        return map;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return index();
    }
}
