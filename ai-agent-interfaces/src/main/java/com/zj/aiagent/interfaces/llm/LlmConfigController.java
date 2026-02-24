package com.zj.aiagent.interfaces.llm;

import com.zj.aiagent.application.llm.LlmConfigService;
import com.zj.aiagent.application.llm.dto.*;
import com.zj.aiagent.shared.context.UserContext;
import com.zj.aiagent.shared.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/llm-config")
@RequiredArgsConstructor
public class LlmConfigController {

    private final LlmConfigService llmConfigService;

    @GetMapping
    public Response<List<LlmConfigDTO>> listConfigs() {
        Long userId = UserContext.getUserId();
        return Response.success(llmConfigService.listConfigs(userId));
    }

    @PostMapping
    public Response<Long> createConfig(@RequestBody CreateLlmConfigRequest request) {
        Long userId = UserContext.getUserId();
        return Response.success(llmConfigService.createConfig(userId, request));
    }

    @PutMapping("/{id}")
    public Response<Void> updateConfig(@PathVariable Long id, @RequestBody UpdateLlmConfigRequest request) {
        llmConfigService.updateConfig(id, request);
        return Response.success();
    }

    @DeleteMapping("/{id}")
    public Response<Void> deleteConfig(@PathVariable Long id) {
        llmConfigService.deleteConfig(id);
        return Response.success();
    }

    @PostMapping("/{id}/test")
    public Response<TestResultDTO> testConfig(@PathVariable Long id) {
        return Response.success(llmConfigService.testConfig(id));
    }
}
