package com.zj.aiagent.interfaces.web.controller.client;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.zj.aiagent.interfaces.common.Response;
import com.zj.aiagent.interfaces.web.dto.request.agent.SaveAgentRequest;
import com.zj.aiagent.interfaces.web.dto.response.agent.AgentResponse;

import cn.hutool.core.util.IdUtil;

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

        @GetMapping("/list")
        @Operation(summary = "获取agent列表")
        public Response<AgentResponse> newChat() {
                return Response.success(String.valueOf(IdUtil.getSnowflake(1, 1).nextId()));
        }

        @GetMapping("/detail/{agentId}")
        @Operation(summary = "获取agent详情")
        public Response<AgentResponse> detail() {
                return Response.success(String.valueOf(IdUtil.getSnowflake(1, 1).nextId()));
        }

        @PostMapping("/save")
        @Operation(summary = "更新或保存agent")
        public Response<AgentResponse> update(@RequestBody SaveAgentRequest agentRequest) {
                return Response.success(String.valueOf(IdUtil.getSnowflake(1, 1).nextId()));
        }

        @PostMapping("/delete/{agentId}")
        @Operation(summary = "删除agent")
        public Response<AgentResponse> delete() {
                return Response.success(String.valueOf(IdUtil.getSnowflake(1, 1).nextId()));
        }

        @PostMapping("/publish/{agentId}")
        @Operation(summary = "发布agent")
        public Response<AgentResponse> delete() {
                return Response.success(String.valueOf(IdUtil.getSnowflake(1, 1).nextId()));
        }

}
