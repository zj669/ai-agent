package com.zj.aiagemt.controller;


import com.alibaba.fastjson.JSON;

import com.zj.aiagemt.model.bo.ExecuteCommandEntity;
import com.zj.aiagemt.model.common.Response;
import com.zj.aiagemt.model.dto.AgentInfoDTO;
import com.zj.aiagemt.model.dto.AutoAgentRequestDTO;
import com.zj.aiagemt.service.agent.execute.IExecuteStrategy;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AutoAgent 自动智能对话体
 *
 * @author xiaofuge bugstack.cn @小傅哥
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class AiAgentControlle {

    @Resource(name = "autoAgentExecuteStrategy")
    private IExecuteStrategy autoAgentExecuteStrategy;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @RequestMapping(value = "auto_agent", method = RequestMethod.POST)
    public ResponseBodyEmitter autoAgent(@RequestBody AutoAgentRequestDTO request, HttpServletResponse response) {
        log.info("AutoAgent流式执行请求开始，请求信息：{}", JSON.toJSONString(request));
        
        try {
            // 设置SSE响应头
            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");

            // 1. 创建流式输出对象
            ResponseBodyEmitter emitter = new ResponseBodyEmitter(Long.MAX_VALUE);
            
            // 2. 构建执行命令实体
            ExecuteCommandEntity executeCommandEntity = ExecuteCommandEntity.builder()
                    .aiAgentId(request.getAiAgentId())
                    .userMessage(request.getMessage())
                    .sessionId(request.getSessionId())
                    .maxStep(request.getMaxStep())
                    .build();
            
            // 3. 异步执行AutoAgent
            threadPoolExecutor.execute(() -> {
                try {
                    autoAgentExecuteStrategy.execute(executeCommandEntity, emitter);
                } catch (Exception e) {
                    log.error("AutoAgent执行异常：{}", e.getMessage(), e);
                    try {
                        emitter.send("执行异常：" + e.getMessage());
                    } catch (Exception ex) {
                        log.error("发送异常信息失败：{}", ex.getMessage(), ex);
                    }
                } finally {
                    try {
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("完成流式输出失败：{}", e.getMessage(), e);
                    }
                }
            });
            
            return emitter;

        } catch (Exception e) {
            log.error("AutoAgent请求处理异常：{}", e.getMessage(), e);
            ResponseBodyEmitter errorEmitter = new ResponseBodyEmitter();
            try {
                errorEmitter.send("请求处理异常：" + e.getMessage());
                errorEmitter.complete();
            } catch (Exception ex) {
                log.error("发送错误信息失败：{}", ex.getMessage(), ex);
            }
            return errorEmitter;
        }
    }

    @RequestMapping(value = "list", method = RequestMethod.GET)
    public Response<List<AgentInfoDTO>> getAvailableAgents() {
        log.info("获取可用智能体列表请求");
        
        try {
            // 目前返回固定的智能体列表，后续可以从数据库查询
            List<AgentInfoDTO> agents = new ArrayList<>();
            
            AgentInfoDTO agent3001 = AgentInfoDTO.builder()
                    .id("3")
                    .name("默认智能体")
                    .description("支持多轮对话的通用AI智能体")
                    .available(true)
                    .type("GENERAL")
                    .createTime("2024-01-01 00:00:00")
                    .updateTime("2024-01-01 00:00:00")
                    .build();
            
            agents.add(agent3001);
            
            // 可以添加更多智能体
            // agents.add(AgentInfoDTO.builder()
            //         .id("3002")
            //         .name("编程助手")
            //         .description("专门用于编程问答的智能体")
            //         .available(false)
            //         .type("CODING")
            //         .build());
            
            log.info("成功获取到{}个智能体", agents.size());
            return Response.<List<AgentInfoDTO>>builder()
                    .code("0000")
                    .info("成功")
                    .data(agents)
                    .build();
                    
        } catch (Exception e) {
            log.error("获取智能体列表异常：{}", e.getMessage(), e);
            return Response.<List<AgentInfoDTO>>builder()
                    .code("0001")
                    .info("获取智能体列表失败：" + e.getMessage())
                    .build();
        }
    }

    @RequestMapping(value = "detail/{agentId}", method = RequestMethod.GET)
    public Response<AgentInfoDTO> getAgentDetail(@PathVariable String agentId) {
        log.info("获取智能体详情请求, agentId: {}", agentId);
        
        try {
            // TODO: 实现智能体详情查询逻辑
            // 这里可以调用相应的服务方法获取智能体详细信息
            
            AgentInfoDTO agentDetail = AgentInfoDTO.builder()
                    .id(agentId)
                    .name("智能体 " + agentId)
                    .description("这是智能体 " + agentId + " 的详细描述")
                    .available(true)
                    .type("GENERAL")
                    .createTime("2024-01-01 00:00:00")
                    .updateTime("2024-01-01 00:00:00")
                    .build();
            
            return Response.<AgentInfoDTO>builder()
                    .code("0000")
                    .info("成功")
                    .data(agentDetail)
                    .build();
                    
        } catch (Exception e) {
            log.error("获取智能体详情异常：{}", e.getMessage(), e);
            return Response.<AgentInfoDTO>builder()
                    .code("0001")
                    .info("获取智能体详情失败：" + e.getMessage())
                    .build();
        }
    }

    @RequestMapping(value = "session/history", method = RequestMethod.GET)
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
                    .info("获取会话历史失败：" + e.getMessage())
                    .build();
        }
    }

    @RequestMapping(value = "export", method = RequestMethod.GET)
    public Response<Object> exportChat(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "json") String format) {
        log.info("导出聊天记录请求, sessionId: {}, format: {}", sessionId, format);
        
        try {
            // TODO: 实现聊天记录导出逻辑
            // 这里可以调用相应的服务方法导出聊天记录
            
            // 示例返回数据结构
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("format", format);
            result.put("downloadUrl", "/api/v1/agent/download/" + sessionId + "." + format);
            result.put("exportTime", new Date());
            
            return Response.<Object>builder()
                    .code("0000")
                    .info("成功")
                    .data(result)
                    .build();
                    
        } catch (Exception e) {
            log.error("导出聊天记录异常：{}", e.getMessage(), e);
            return Response.<Object>builder()
                    .code("0001")
                    .info("导出聊天记录失败：" + e.getMessage())
                    .build();
        }
    }

}
