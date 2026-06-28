package com.kob.backend.service.user.section;

import java.util.List;
import java.util.Map;

public interface SectionService {
    List<Map<String, String>> list();

    List<Map<String, String>> hot();
}
