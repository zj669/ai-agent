package com.zj.aiagent.interfaces.web.controller.client;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import com.zj.aiagent.application.agent.IAgentApplicationService;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentPO;
import com.zj.aiagent.interfaces.common.Response;
import com.zj.aiagent.interfaces.web.dto.request.agent.SaveAgentRequest;
import com.zj.aiagent.interfaces.web.dto.response.agent.AgentResponse;
import com.zj.aiagent.shared.utils.UserContext;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/client/agent")
@Tag(name = "agent管理", description = "agent管理")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
                RequestMethod.GET,
                RequestMethod.POST,
                RequestMethod.OPTIONS
})
public class AgentController {

        @Resource
        private IAgentApplicationService agentApplicationService;

        @GetMapping("/list")
        @Operation(summary = "获取agent列表")
        public Response<List<AgentResponse>> list() {
                Long userId = UserContext.getUserId();
                List<AiAgentPO> agents = agentApplicationService.getUserAgentList(userId);
                List<AgentResponse> responses = agents.stream()
                                .map(this::toAgentResponse)
                                .collect(Collectors.toList());
                return Response.success(responses);
        }

        @GetMapping("/detail/{agentId}")
        @Operation(summary = "获取agent详情")
        public Response<AgentResponse> detail(@PathVariable("agentId") Long agentId) {
                Long userId = UserContext.getUserId();
                AiAgentPO agent = agentApplicationService.getAgentDetail(userId, agentId);
                return Response.success(toAgentResponse(agent));
        }

        @PostMapping("/save")
        @Operation(summary = "更新或保存agent")
        public Response<String> save(@RequestBody SaveAgentRequest agentRequest) {
                Long userId = UserContext.getUserId();
                String agentId = agentApplicationService.saveAgent(userId,
                                agentRequest.getAgentId(),
                                agentRequest.getAgentName(),
                                agentRequest.getDescription(),
                                agentRequest.getGraphJson(),
                                agentRequest.getStatus());
                return Response.success(agentId);
        }

        @PostMapping("/delete/{agentId}")
        @Operation(summary = "删除agent")
        public Response<Void> delete(@PathVariable("agentId") Long agentId) {
                Long userId = UserContext.getUserId();
                agentApplicationService.deleteAgent(userId, agentId);
                return Response.success();
        }

        @PostMapping("/publish/{agentId}")
        @Operation(summary = "发布agent")
        public Response<Void> publish(@PathVariable("agentId") Long agentId) {
                Long userId = UserContext.getUserId();
                agentApplicationService.publishAgent(userId, agentId);
                return Response.success();
        }

        /**
         * PO 转 DTO
         */
        private AgentResponse toAgentResponse(AiAgentPO po) {
                return AgentResponse.builder()
                                .agentId(String.valueOf(po.getId()))
                                .agentName(po.getAgentName())
                                .description(po.getDescription())
                                .status(po.getStatus())
                                .statusDesc(getStatusDesc(po.getStatus()))
                                .createTime(po.getCreateTime())
                                .updateTime(po.getUpdateTime())
                                .build();
        }

        /**
         * 获取状态描述
         */
        private String getStatusDesc(Integer status) {
                if (status == null) {
                        return "未知";
                }
                return switch (status) {
                        case 0 -> "草稿";
                        case 1 -> "已发布";
                        case 2 -> "已停用";
                        default -> "未知";
                };
        }
}
