package com.kob.backend.service.user.block;

import java.util.List;
import java.util.Map;

public interface BlockRuleService {
    List<Map<String, String>> list();

    Map<String, String> add(String targetType, Integer targetId);

    Map<String, String> delete(Integer bid);
}
