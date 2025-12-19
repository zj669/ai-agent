package com.zj.aiagemt.service.dag.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAG日志服务
 * 记录和管理DAG执行日志
 */
@Slf4j
@Service
public class DagLoggingService {

    // 内存中存储最近的日志（实际应该存储到数据库）
    private final Map<String, List<DagExecutionLog>> executionLogs = new ConcurrentHashMap<>();

    /**
     * 记录DAG开始执行
     */
    public void logDagStart(String executionId, String conversationId, String dagId) {
        DagExecutionLog log1 = DagExecutionLog.builder()
                .logId(UUID.randomUUID().toString())
                .executionId(executionId)
                .conversationId(conversationId)
                .dagId(dagId)
                .level("INFO")
                .logType("DAG_START")
                .message("DAG执行开始")
                .createTime(LocalDateTime.now())
                .build();

        addLog(executionId, log1);
        log.info("【DAG开始】executionId={}, dagId={}", executionId, dagId);
    }

    /**
     * 记录DAG执行完成
     */
    public void logDagEnd(String executionId, String conversationId, String dagId,
            String status, long durationMs) {
        DagExecutionLog log1 = DagExecutionLog.builder()
                .logId(UUID.randomUUID().toString())
                .executionId(executionId)
                .conversationId(conversationId)
                .dagId(dagId)
                .level("INFO")
                .logType("DAG_END")
                .message("DAG执行完成: " + status)
                .durationMs(durationMs)
                .details(Map.of("status", status))
                .createTime(LocalDateTime.now())
                .build();

        addLog(executionId, log1);
        log.info("【DAG完成】executionId={}, status={}, 耗时={}ms", executionId, status, durationMs);
    }

    /**
     * 记录节点开始执行
     */
    public void logNodeStart(String executionId, String conversationId,
            String nodeId, String nodeName) {
        DagExecutionLog log1 = DagExecutionLog.builder()
                .logId(UUID.randomUUID().toString())
                .executionId(executionId)
                .conversationId(conversationId)
                .nodeId(nodeId)
                .nodeName(nodeName)
                .level("INFO")
                .logType("NODE_START")
                .message("节点开始执行: " + nodeName)
                .createTime(LocalDateTime.now())
                .build();

        addLog(executionId, log1);
        log.info("【节点开始】executionId={}, nodeId={}, nodeName={}",
                executionId, nodeId, nodeName);
    }

    /**
     * 记录节点执行完成
     */
    public void logNodeEnd(String executionId, String conversationId,
            String nodeId, String nodeName, boolean success, long durationMs) {
        DagExecutionLog log1 = DagExecutionLog.builder()
                .logId(UUID.randomUUID().toString())
                .executionId(executionId)
                .conversationId(conversationId)
                .nodeId(nodeId)
                .nodeName(nodeName)
                .level(success ? "INFO" : "ERROR")
                .logType("NODE_END")
                .message("节点执行" + (success ? "成功" : "失败") + ": " + nodeName)
                .durationMs(durationMs)
                .details(Map.of("success", success))
                .createTime(LocalDateTime.now())
                .build();

        addLog(executionId, log1);

        if (success) {
            log.info("【节点完成】executionId={}, nodeId={}, 耗时={}ms",
                    executionId, nodeId, durationMs);
        } else {
            log.error("【节点失败】executionId={}, nodeId={}, 耗时={}ms",
                    executionId, nodeId, durationMs);
        }
    }

    /**
     * 记录节点执行错误
     */
    public void logNodeError(String executionId, String conversationId,
            String nodeId, String nodeName, Exception exception) {
        String exceptionMessage = exception.getMessage();
        String exceptionStack = getStackTrace(exception);

        DagExecutionLog log1 = DagExecutionLog.builder()
                .logId(UUID.randomUUID().toString())
                .executionId(executionId)
                .conversationId(conversationId)
                .nodeId(nodeId)
                .nodeName(nodeName)
                .level("ERROR")
                .logType("NODE_ERROR")
                .message("节点执行异常: " + nodeName)
                .exceptionMessage(exceptionMessage)
                .exceptionStack(exceptionStack)
                .createTime(LocalDateTime.now())
                .build();

        addLog(executionId, log1);
        log.error("【节点异常】executionId={}, nodeId={}, error={}",
                executionId, nodeId, exceptionMessage, exception);
    }

    /**
     * 记录人工介入
     */
    public void logHumanIntervention(String executionId, String conversationId,
            String nodeId, String nodeName) {
        DagExecutionLog log1 = DagExecutionLog.builder()
                .logId(UUID.randomUUID().toString())
                .executionId(executionId)
                .conversationId(conversationId)
                .nodeId(nodeId)
                .nodeName(nodeName)
                .level("WARN")
                .logType("HUMAN_INTERVENTION")
                .message("等待人工介入: " + nodeName)
                .createTime(LocalDateTime.now())
                .build();

        addLog(executionId, log1);
        log.warn("【人工介入】executionId={}, nodeId={}", executionId, nodeId);
    }

    /**
     * 获取执行的所有日志
     */
    public List<DagExecutionLog> getExecutionLogs(String executionId) {
        return executionLogs.getOrDefault(executionId, new ArrayList<>());
    }

    /**
     * 获取会话的所有日志
     */
    public List<DagExecutionLog> getConversationLogs(String conversationId) {
        List<DagExecutionLog> result = new ArrayList<>();
        for (List<DagExecutionLog> logs : executionLogs.values()) {
            logs.stream()
                    .filter(log -> conversationId.equals(log.getConversationId()))
                    .forEach(result::add);
        }
        result.sort(Comparator.comparing(DagExecutionLog::getCreateTime));
        return result;
    }

    /**
     * 清理旧日志
     */
    public void cleanOldLogs(int maxExecutions) {
        if (executionLogs.size() > maxExecutions) {
            // 保留最近的日志
            List<String> allKeys = new ArrayList<>(executionLogs.keySet());
            if (allKeys.size() > maxExecutions) {
                int toRemove = allKeys.size() - maxExecutions;
                for (int i = 0; i < toRemove; i++) {
                    executionLogs.remove(allKeys.get(i));
                }
                log.info("清理旧日志，删除 {} 个执行记录", toRemove);
            }
        }
    }

    /**
     * 添加日志到内存
     */
    private void addLog(String executionId, DagExecutionLog log) {
        executionLogs.computeIfAbsent(executionId, k -> new ArrayList<>()).add(log);
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception e) {
        if (e == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");

        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (sb.length() > 2000) { // 限制长度
                sb.append("\t... (truncated)");
                break;
            }
        }

        return sb.toString();
    }
}
