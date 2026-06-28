package com.kob.backend.controller.user.section;

import com.kob.backend.service.user.section.SectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SectionController {
    @Autowired
    private SectionService sectionService;

    @GetMapping("/user/section/list/")
    public List<Map<String, String>> list() {
        return sectionService.list();
    }

    @GetMapping("/user/section/hot/")
    public List<Map<String, String>> hot() {
        return sectionService.hot();
    }
}
