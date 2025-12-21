package com.zj.aiagemt.controller.client;


import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;

import com.zj.aiagemt.model.common.Response;
import com.zj.aiagemt.model.dto.AutoAgentRequestDTO;
import com.zj.aiagemt.model.entity.AiAgent;
import com.zj.aiagemt.service.AiAgentService;
import com.zj.aiagemt.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AutoAgent 自动智能对话体
 *
 */
@Slf4j
@RestController
@RequestMapping("/client/agent")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class AiAgentControlle {
    @Resource
    private AiAgentService aiAgentService;

    @PostMapping(value = "/auto_agent")
    public ResponseBodyEmitter autoAgent(@RequestBody AutoAgentRequestDTO request, HttpServletResponse response) {
        log.info("AutoAgent流式执行请求开始，请求信息：{}", JSON.toJSONString(request));
        // 设置SSE响应头
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        // 1. 创建流式输出对象
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(Long.MAX_VALUE);
        Long userId = UserContext.getUserId();
        return  aiAgentService.autoAgent(request, emitter, userId);
    }


    @GetMapping(value = "/list")
    public Response<List<AiAgent>> getAvailableAgents() {
        log.info("获取可用智能体列表请求");
        Long userId = UserContext.getUserId();
        try {
            List<AiAgent> agents = aiAgentService.queryAgentDtoList(userId);
            log.info("成功获取到{}个智能体", agents.size());
            return Response.<List<AiAgent>>builder()
                    .code("0000")
                    .info("成功")
                    .data(agents)
                    .build();
                    
        } catch (Exception e) {
            log.error("获取智能体列表异常：{}", e.getMessage(), e);
            return Response.<List<AiAgent>>builder()
                    .code("0001")
                    .info("获取智能体列表失败：" + e.getMessage())
                    .build();
        }
    }

    @GetMapping(value = "session/history")
    public Response<Object> getSessionHistory(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        log.info("获取会话历史请求, sessionId: {}, page: {}, size: {}", sessionId, page, size);
        
        try {
            // TODO: 实现会话历史查询逻辑
            // 这里可以调用相应的服务方法获取会话历史记录
            
            // 示例返回数据结构
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("messages", new ArrayList<>());
            result.put("total", 0);
            result.put("page", page);
            result.put("size", size);
            
            return Response.<Object>builder()
                    .code("0000")
                    .info("成功")
                    .data(result)
                    .build();
                    
        } catch (Exception e) {
            log.error("获取会话历史异常：{}", e.getMessage(), e);
            return Response.<Object>builder()
                    .code("0001")
                    .info("获取会话历史失败")
                    .build();
        }
    }

    @GetMapping("newChat")
    @Operation(summary = "发起流式问答前生成会话ID")
    public Response<String> newChat(){
        return Response.<String>builder()
                .code("0000")
                .info("成功")
                .data(String.valueOf(IdUtil.getSnowflake(1, 1).nextId()))
                .build();
    }

    @GetMapping("oldChat")
    @Operation(summary = "历史会话ID")
    public Response<List<String>> oldChat(){
        return Response.<List<String>>builder()
                .code("0000")
                .info("成功")
                .data(List.of("1", "2", "3"))
                .build();
    }

}