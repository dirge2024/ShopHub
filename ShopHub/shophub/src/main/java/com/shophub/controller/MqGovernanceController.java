package com.shophub.controller;

import com.shophub.dto.Result;
import com.shophub.service.MqGovernanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/mq-governance")
public class MqGovernanceController {

    @Resource
    private MqGovernanceService mqGovernanceService;

    /**
     * 查询 MQ 治理总览，返回计数、最近重试和最近死信记录。
     */
    @GetMapping("/overview")
    public Result overview(@RequestParam(value = "limit", defaultValue = "5") Integer limit) {
        return Result.ok(mqGovernanceService.getOverview(limit));
    }

    /**
     * 按场景和阶段查询治理记录，便于联调时定位具体失败消息。
     */
    @GetMapping("/records")
    public Result listRecords(@RequestParam(value = "scene", required = false) String scene,
                              @RequestParam(value = "stage", required = false) String stage,
                              @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return Result.ok(mqGovernanceService.listRecords(scene, stage, limit));
    }

    /**
     * 手动重放指定死信记录，主要用于联调和人工补偿。
     */
    @PostMapping("/records/{recordId}/replay")
    public Result replayDeadLetter(@PathVariable("recordId") String recordId) {
        return mqGovernanceService.replayDeadLetter(recordId);
    }
}
