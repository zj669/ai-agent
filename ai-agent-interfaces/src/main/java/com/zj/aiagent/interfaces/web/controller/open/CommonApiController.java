package com.zj.aiagent.interfaces.web.controller.open;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/public")
public class CommonApiController {
    @GetMapping("/health")
    public String health() {
        log.info("health");
        return "ok";
    }
}
