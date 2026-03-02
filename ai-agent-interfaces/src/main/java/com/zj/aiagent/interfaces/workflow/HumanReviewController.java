package com.zj.aiagent.interfaces.workflow;

import com.zj.aiagent.application.workflow.SchedulerService;
import com.zj.aiagent.domain.workflow.config.HumanReviewConfig;
import com.zj.aiagent.domain.workflow.entity.Execution;
import com.zj.aiagent.domain.workflow.entity.HumanReviewRecord;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.ExecutionRepository;
import com.zj.aiagent.domain.workflow.port.ExpressionResolverPort;
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
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/workflow/reviews")
@RequiredArgsConstructor
public class HumanReviewController {

    private final SchedulerService schedulerService;
    private final RedissonClient redissonClient;
    private final HumanReviewRepository humanReviewRepository;
    private final ExecutionRepository executionRepository;
    private final ExpressionResolverPort expressionResolver;

    /**
     * 获取待审核列表
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
        }

        String nodeId = execution.getPausedNodeId();
        Node node = execution.getGraph().getNode(nodeId);
        final TriggerPhase phase = execution.getPausedPhase() != null 
            ? execution.getPausedPhase() 
            : TriggerPhase.AFTER_EXECUTION;

        HumanReviewDTO.ReviewDetailDTO dto = new HumanReviewDTO.ReviewDetailDTO();
        dto.setExecutionId(executionId);
        dto.setNodeId(nodeId);
        dto.setNodeName(node.getName());
        dto.setTriggerPhase(phase);

        // 收集所有需要展示的节点：上游已成功节点 + 当前暂停节点
        List<HumanReviewDTO.NodeContextDTO> nodes = execution.getGraph().getNodes().values().stream()
                .filter(n -> {
                    ExecutionStatus ns = execution.getNodeStatuses().get(n.getNodeId());
                    // 包含已成功的节点 + 当前暂停的节点
                    return ns == ExecutionStatus.SUCCEEDED || n.getNodeId().equals(nodeId);
                })
                .map(n -> {
                    boolean isCurrentNode = n.getNodeId().equals(nodeId);
                    Map<String, Object> nodeInputs = null;
                    Map<String, Object> nodeOutputs = null;

                    try {
                        // 所有节点都展示输入
                        nodeInputs = expressionResolver.resolveInputs(n.getInputs(), execution.getContext());
                    } catch (Exception e) {
                        log.debug("Failed to resolve inputs for node {}: {}", n.getNodeId(), e.getMessage());
                    }

                    if (isCurrentNode) {
                        // 当前暂停节点：根据 phase 决定是否展示输出
                        if (phase == TriggerPhase.AFTER_EXECUTION) {
                            // 执行后暂停：展示输出
                            nodeOutputs = execution.getContext().getNodeOutput(n.getNodeId());
                        }
                        // BEFORE_EXECUTION：不展示输出（还没执行）
                    } else {
                        // 上游节点：展示输出
                        nodeOutputs = execution.getContext().getNodeOutput(n.getNodeId());
                    }

                    return HumanReviewDTO.NodeContextDTO.builder()
                            .nodeId(n.getNodeId())
                            .nodeName(n.getName())
                            .nodeType(n.getType().name())
                            .status(execution.getNodeStatuses().getOrDefault(n.getNodeId(), ExecutionStatus.PENDING).name())
                            .inputs(nodeInputs)
                            .outputs(nodeOutputs)
                            .build();
                })
                .collect(Collectors.toList());
        dto.setNodes(nodes);

        return ResponseEntity.ok(dto);
    }

    /**
     * 提交审核（恢复执行）
     */
    @PostMapping("/resume")
    public ResponseEntity<Void> resumeExecution(@RequestBody HumanReviewDTO.ResumeExecutionRequest request) {
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
