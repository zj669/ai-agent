package com.zj.aiagemt.controller;

import com.zj.aiagemt.integretion.task.CSDNPublishTask;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class CommonApiController {
    @Resource
    private CSDNPublishTask mcpServerCSDNJob;

    @GetMapping("/health")
    public String health() {
        log.info("health");
        return "ok";
    }

    @GetMapping("/run")
    public String run() {
        mcpServerCSDNJob.run1();
        return "ok";
    }
}
