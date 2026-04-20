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

    @GetMapping("/overview")
    public Result overview(@RequestParam(value = "limit", defaultValue = "5") Integer limit) {
        return Result.ok(mqGovernanceService.getOverview(limit));
    }

    @GetMapping("/records")
    public Result listRecords(@RequestParam(value = "scene", required = false) String scene,
                              @RequestParam(value = "stage", required = false) String stage,
                              @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return Result.ok(mqGovernanceService.listRecords(scene, stage, limit));
    }

    @PostMapping("/records/{recordId}/replay")
    public Result replayDeadLetter(@PathVariable("recordId") String recordId) {
        return mqGovernanceService.replayDeadLetter(recordId);
    }
}
