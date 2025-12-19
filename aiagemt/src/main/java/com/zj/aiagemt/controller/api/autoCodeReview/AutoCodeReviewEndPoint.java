package com.zj.aiagemt.controller.api.autoCodeReview;


import com.zj.aiagemt.model.bo.AutoCodeCommandEntity;
import com.zj.aiagemt.service.agent.impl.execute.codereview.AutoCodeReviewExecuteStrategy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auto/review")
public class AutoCodeReviewEndPoint {
    @Resource
    private AutoCodeReviewExecuteStrategy autoCodeReviewExecuteStrategy;

    @PostMapping("/codeReview")
    public String codeReview(@RequestBody String diff) {
        List<String> strings = splitDiff(diff, 5000);
        AutoCodeCommandEntity build = AutoCodeCommandEntity.builder().aiAgentId("6").diff(strings).build();
        String execute = null;
        try {
            execute = autoCodeReviewExecuteStrategy.execute(build);
        } catch (Exception e) {
            log.error("自动审计执行失败:{}", e.getMessage());
        }
        log.info("自动审计执行结果: {}", execute);
        return execute;
    }


    public List<String> splitDiff(String diff, int maxChunkSize) {

        return List.of(diff);
    }
}
