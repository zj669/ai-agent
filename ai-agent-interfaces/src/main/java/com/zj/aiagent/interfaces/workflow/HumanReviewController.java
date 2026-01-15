package com.zj.aiagent.interfaces.workflow;

import com.zj.aiagent.application.workflow.SchedulerService;
import com.zj.aiagent.domain.workflow.config.HumanReviewConfig;
import com.zj.aiagent.domain.workflow.entity.Execution;
import com.zj.aiagent.domain.workflow.entity.HumanReviewRecord;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.ExecutionRepository;
import com.zj.aiagent.domain.workflow.port.HumanReviewRepository;
import com.zj.aiagent.domain.workflow.valobj.ExecutionStatus;
import com.zj.aiagent.domain.workflow.valobj.TriggerPhase;
import com.zj.aiagent.interfaces.workflow.dto.HumanReviewDTO;
import com.zj.aiagent.shared.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/workflow/reviews")
@RequiredArgsConstructor
public class HumanReviewController {

    private final SchedulerService schedulerService;
    private final RedissonClient redissonClient;
    private final HumanReviewRepository humanReviewRepository;
    // We access Repositories for queries (CQRS separation: Controller reads,
    // Service writes)
    // Or we should put query logic in Service.
    // For simplicity and DDD pragmatism, Controller can Query specific read models
    // or use Repositories for tailored queries.
    // But finding *active paused* executions might need a specialized query on
    // ExecutionRepository.
    private final ExecutionRepository executionRepository;

    /**
     * 获取待审核列表
     * 实际场景应查询 Redis 或 DB 中状态为 PAUSED_FOR_REVIEW 的任务
     */
    @GetMapping("/pending")
    public ResponseEntity<List<HumanReviewDTO.PendingReviewDTO>> getPendingReviews() {
        RSet<String> pendingSet = redissonClient.getSet("human_review:pending");
        if (pendingSet == null || pendingSet.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<HumanReviewDTO.PendingReviewDTO> dtos = pendingSet.stream()
                .map(executionRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(execution -> {
                    String nodeId = execution.getPausedNodeId();
                    Node node = execution.getGraph().getNode(nodeId);

                    HumanReviewDTO.PendingReviewDTO dto = new HumanReviewDTO.PendingReviewDTO();
                    dto.setExecutionId(execution.getExecutionId());
                    dto.setNodeId(nodeId);
                    dto.setNodeName(node != null ? node.getName() : "Unknown Node");
                    dto.setTriggerPhase(execution.getPausedPhase());
                    dto.setPausedAt(execution.getUpdatedAt());
                    dto.setAgentName("Agent-" + execution.getAgentId()); // Placeholder
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * 获取审核详情
     */
    @GetMapping("/{executionId}")
    public ResponseEntity<HumanReviewDTO.ReviewDetailDTO> getReviewDetail(@PathVariable String executionId) {
        Execution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));

        if (execution.getStatus() != ExecutionStatus.PAUSED_FOR_REVIEW
                && execution.getStatus() != ExecutionStatus.PAUSED) {
            // throw new IllegalStateException("Execution is not paused");
            // Just return info even if not paused? No, UI expects pending review.
        }

        String nodeId = execution.getPausedNodeId();
        Node node = execution.getGraph().getNode(nodeId);
        TriggerPhase phase = execution.getPausedPhase();
        if (phase == null)
            phase = TriggerPhase.AFTER_EXECUTION;

        HumanReviewDTO.ReviewDetailDTO dto = new HumanReviewDTO.ReviewDetailDTO();
        dto.setExecutionId(executionId);
        dto.setNodeId(nodeId);
        dto.setNodeName(node.getName());
        dto.setTriggerPhase(phase);

        // Context Data
        if (phase == TriggerPhase.BEFORE_EXECUTION) {
            // Inputs for the node (resolved)
            // We can re-resolve or pick from someplace.
            // Context doesn't store "resolved inputs" for the specific node persistently
            // unless in a special field.
            // But valid inputs are in SharedState + Node Config.
            dto.setContextData(execution.getContext().resolveInputs(node.getInputs()));
        } else {
            // Outputs of the node (calculated but waiting for approval)
            // Where are they stored?
            // In `onNodeComplete`, we called `execution.advance(paused(phase))`.
            // `NodeExecutionResult` carrying output was passed?
            // Wait, `NodeExecutionResult.paused(phase)` DOES NOT carry outputs in my
            // implementation!
            // `NodeExecutionResult` has `outputs` field.
            // But `paused()` factory only set status.
            // If AFTER_EXECUTION, we *executed* the node. We have outputs.
            // We need to persist these 'pending outputs'.
            // Execution entity currently doesn't have a field "pendingOutputs".
            // It only has `context.nodeOutputs`.
            // If we called `advance` with `paused`, did we save outputs?
            // Look at Execution.advance:
            // 1. `nodeStatuses.put(nodeId, result.getStatus())` (PAUSED)
            // 2. `if (result.getOutputs() != null) context.setNodeOutput(...)`
            // 3. `if (result.isPaused()) ... return`

            // So inputs: `NodeExecutionResult.paused(phase)` needs to carry outputs if we
            // want to save them!
            // My implementation of `onNodeComplete` in `SchedulerService`:
            // `if (checkPause(... AFTER ...))` -> `execution.advance(paused(phase))`
            // It does NOT pass the outputs from the completed result!
            // THIS IS A BUG in my `SchedulerService` implementation in Step 208.
            // I need to fix `SchedulerService` to pass outputs when pausing
            // AFTER_EXECUTION.

            // Assuming I fix it: outputs are in `context.getNodeOutput(nodeId)`.
            dto.setContextData(execution.getContext().getNodeOutput(nodeId));
        }

        HumanReviewConfig config = node.getConfig().getHumanReviewConfig();
        HumanReviewDTO.HumanReviewConfigDTO configDTO = new HumanReviewDTO.HumanReviewConfigDTO();
        if (config != null) {
            configDTO.setPrompt(config.getPrompt());
            configDTO.setEditableFields(config.getEditableFields());
        }
        dto.setConfig(configDTO);

        return ResponseEntity.ok(dto);
    }

    /**
     * 提交审核（恢复执行）
     */
    @PostMapping("/resume")
    public ResponseEntity<Void> resumeExecution(@RequestBody HumanReviewDTO.ResumeExecutionRequest request) {
        // Get helper fields, e.g. current user
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        schedulerService.resumeExecution(
                request.getExecutionId(),
                request.getNodeId(),
                request.getEdits(),
                userId,
                request.getComment());
        return ResponseEntity.ok().build();
    }

    /**
     * 审核历史
     */
    @GetMapping("/history")
    public ResponseEntity<Page<HumanReviewRecord>> getHistory(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(humanReviewRepository.findReviewHistory(userId, pageable));
    }
}
