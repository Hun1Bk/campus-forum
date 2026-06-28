package com.kob.backend.controller.user.block;

import com.kob.backend.service.user.block.BlockRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class BlockRuleController {
    @Autowired
    private BlockRuleService blockRuleService;

    @GetMapping("/user/block/list/")
    public List<Map<String, String>> list() {
        return blockRuleService.list();
    }

    @PostMapping("/user/block/add/")
    public Map<String, String> add(@RequestParam Map<String, String> map) {
        return blockRuleService.add(map.get("targetType"), Integer.parseInt(map.get("targetId")));
    }

    @PostMapping("/user/block/delete/")
    public Map<String, String> delete(@RequestParam Integer bid) {
        return blockRuleService.delete(bid);
    }
}
