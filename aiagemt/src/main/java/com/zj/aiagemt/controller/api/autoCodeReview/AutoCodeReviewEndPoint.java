package com.zj.aiagemt.controller.api.autoCodeReview;


import com.zj.aiagemt.model.bo.AutoCodeCommandEntity;
import com.zj.aiagemt.service.agent.execute.codereview.AutoCodeReviewExecuteStrategy;
import com.zj.aiagemt.service.rag.split.RegularSplit;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
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
        RegularSplit splitter = new RegularSplit("(?=^diff --git)");
        RegularSplit hunkSplitter = new RegularSplit("(?=^@@ )");

        // 首先按文件分割
        List<String> fileDiffs = splitter.splitText(diff);
        List<String> result = new ArrayList<>();

        for (String fileDiff : fileDiffs) {
            if (fileDiff.length() <= maxChunkSize) {
                result.add(fileDiff);
            } else {
                // 文件太大，按hunk分割
                result.addAll(hunkSplitter.splitText(fileDiff));
            }
        }

        return result;
    }
}
