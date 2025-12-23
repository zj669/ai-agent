package com.zj.aiagent.infrastructure.persistence.repository;

import com.alibaba.fastjson.JSON;
import com.zj.aiagent.domain.agent.dag.context.ExecutionContextSnapshot;
import com.zj.aiagent.domain.agent.dag.context.HumanInterventionRequest;
import com.zj.aiagent.domain.agent.dag.entity.DagExecutionInstance;
import com.zj.aiagent.domain.agent.dag.repository.IDagExecutionRepository;
import com.zj.aiagent.domain.agent.dag.repository.IHumanInterventionRepository;
import com.zj.aiagent.infrastructure.redis.IRedisService;
import com.zj.aiagent.shared.constants.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * 人工介入仓储实现（基础设施层）
 * 使用 IRedisService + 数据库双写策略
 *
 * @author zj
 * @since 2025-12-23
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class HumanInterventionRepositoryImpl implements IHumanInterventionRepository {

    private final IRedisService redisService;
    private final IDagExecutionRepository dagExecutionRepository;

    @Override
    public void savePauseState(HumanInterventionRequest request) {
        String key = buildPauseKey(request.getConversationId(), request.getNodeId());
        String json = JSON.toJSONString(request);

        // 写入 Redis（主存储）
        redisService.setValue(key, json, RedisKeyConstants.HumanIntervention.DEFAULT_TTL_SECONDS);
        log.info("保存人工介入暂停状态到 Redis: conversationId={}, nodeId={}",
                request.getConversationId(), request.getNodeId());

        // 写入数据库（备份）
        saveToDatabaseBackup(request);
    }

    @Override
    public void saveReviewResult(String conversationId, String nodeId,
            boolean approved, String comments, String modifiedOutput) {
        String key = buildPauseKey(conversationId, nodeId);

        // 从 Redis 加载现有状态
        HumanInterventionRequest request = loadState(conversationId, nodeId);
        if (request == null) {
            log.warn("未找到暂停状态: conversationId={}, nodeId={}", conversationId, nodeId);
            return;
        }

        // 更新审核结果
        request.setReviewed(true);
        request.setApproved(approved);
        request.setComments(comments);
        request.setModifiedOutput(modifiedOutput);
        request.setReviewTime(System.currentTimeMillis());

        String json = JSON.toJSONString(request);

        // 更新 Redis，缩短 TTL
        redisService.setValue(key, json, RedisKeyConstants.HumanIntervention.COMPLETED_TTL_SECONDS);
        log.info("保存审核结果到 Redis: conversationId={}, nodeId={}, approved={}",
                conversationId, nodeId, approved);

        // 更新数据库备份
        saveToDatabaseBackup(request);
    }

    @Override
    public HumanInterventionRequest loadState(String conversationId, String nodeId) {
        String key = buildPauseKey(conversationId, nodeId);

        // 优先从 Redis 读取
        String json = redisService.getValue(key);
        if (json != null) {
            log.debug("从 Redis 加载人工介入状态: conversationId={}, nodeId={}", conversationId, nodeId);
            return JSON.parseObject(json, HumanInterventionRequest.class);
        }

        // Redis miss，从数据库加载
        log.info("Redis miss，从数据库加载人工介入状态: conversationId={}, nodeId={}", conversationId, nodeId);
        return loadFromDatabase(conversationId, nodeId);
    }

    @Override
    public void cleanupCompletedState(String conversationId, String nodeId) {
        String key = buildPauseKey(conversationId, nodeId);
        redisService.remove(key);
        log.info("清理已完成的审核状态: conversationId={}, nodeId={}", conversationId, nodeId);
    }

    @Override
    public HumanInterventionRequest findPausedState(String conversationId) {
        try {
            // 从数据库加载执行实例，其中包含暂停状态信息
            DagExecutionInstance instance = dagExecutionRepository.findByConversationId(conversationId);
            if (instance != null && instance.getRuntimeContextJson() != null) {
                com.alibaba.fastjson.JSONObject context = JSON.parseObject(instance.getRuntimeContextJson());
                com.alibaba.fastjson.JSONObject intervention = context.getJSONObject("humanIntervention");

                if (intervention != null) {
                    return intervention.toJavaObject(HumanInterventionRequest.class);
                }
            }
        } catch (Exception e) {
            log.error("查询暂停状态失败: conversationId={}", conversationId, e);
        }
        return null;
    }

    @Override
    public Map<String, Object> loadFullContext(String conversationId) {
        String contextKey = buildContextKey(conversationId);

        // 优先从 Redis 读取
        String json = redisService.getValue(contextKey);
        if (json != null) {
            log.debug("从 Redis 加载完整上下文: conversationId={}", conversationId);
            return JSON.parseObject(json, Map.class);
        }

        // Redis miss，从数据库加载
        log.info("Redis miss，从数据库加载完整上下文: conversationId={}", conversationId);
        try {
            DagExecutionInstance instance = dagExecutionRepository.findByConversationId(conversationId);
            if (instance != null && instance.getRuntimeContextJson() != null) {
                Map<String, Object> context = JSON.parseObject(instance.getRuntimeContextJson(), Map.class);

                // 回写到 Redis
                redisService.setValue(contextKey, JSON.toJSONString(context),
                        RedisKeyConstants.HumanIntervention.DEFAULT_TTL_SECONDS);

                return context;
            }
        } catch (Exception e) {
            log.error("从数据库加载完整上下文失败: conversationId={}", conversationId, e);
        }

        return new HashMap<>();
    }

    @Override
    public void updateContext(String conversationId, Map<String, Object> modifications) {
        // 加载现有上下文
        Map<String, Object> context = loadFullContext(conversationId);

        // 应用修改
        context.putAll(modifications);

        String contextKey = buildContextKey(conversationId);
        String json = JSON.toJSONString(context);

        // 更新 Redis
        redisService.setValue(contextKey, json, RedisKeyConstants.HumanIntervention.DEFAULT_TTL_SECONDS);
        log.info("更新上下文到 Redis: conversationId={}, modifiedKeys={}", conversationId, modifications.keySet());

        // 更新数据库
        try {
            DagExecutionInstance instance = dagExecutionRepository.findByConversationId(conversationId);
            if (instance != null) {
                instance.setRuntimeContextJson(json);
                instance.setUpdateTime(java.time.LocalDateTime.now());
                dagExecutionRepository.update(instance);
                log.debug("更新上下文到数据库: conversationId={}", conversationId);
            }
        } catch (Exception e) {
            log.warn("更新上下文到数据库失败，不影响主流程", e);
        }
    }

    /**
     * 构建暂停状态 Redis Key
     */
    private String buildPauseKey(String conversationId, String nodeId) {
        return RedisKeyConstants.HumanIntervention.PAUSE_PREFIX + conversationId + ":" + nodeId;
    }

    /**
     * 构建上下文 Redis Key
     */
    private String buildContextKey(String conversationId) {
        return RedisKeyConstants.HumanIntervention.CONTEXT_PREFIX + conversationId;
    }

    /**
     * 保存到数据库备份
     */
    private void saveToDatabaseBackup(HumanInterventionRequest request) {
        try {
            DagExecutionInstance instance = dagExecutionRepository.findByConversationId(request.getConversationId());
            if (instance != null) {
                // 更新 runtimeContextJson，添加 humanIntervention 字段
                String contextJson = instance.getRuntimeContextJson();
                com.alibaba.fastjson.JSONObject context = contextJson != null
                        ? JSON.parseObject(contextJson)
                        : new com.alibaba.fastjson.JSONObject();

                context.put("humanIntervention", request);
                instance.setRuntimeContextJson(context.toJSONString());
                instance.setUpdateTime(java.time.LocalDateTime.now());

                dagExecutionRepository.update(instance);
                log.debug("保存人工介入状态到数据库备份: conversationId={}", request.getConversationId());
            }
        } catch (Exception e) {
            log.warn("保存到数据库备份失败，不影响主流程", e);
        }
    }

    /**
     * 从数据库加载
     */
    private HumanInterventionRequest loadFromDatabase(String conversationId, String nodeId) {
        try {
            DagExecutionInstance instance = dagExecutionRepository.findByConversationId(conversationId);
            if (instance != null && instance.getRuntimeContextJson() != null) {
                com.alibaba.fastjson.JSONObject context = JSON.parseObject(instance.getRuntimeContextJson());
                com.alibaba.fastjson.JSONObject intervention = context.getJSONObject("humanIntervention");

                if (intervention != null) {
                    HumanInterventionRequest request = intervention.toJavaObject(HumanInterventionRequest.class);

                    // 恢复到 Redis
                    String key = buildPauseKey(conversationId, nodeId);
                    String json = JSON.toJSONString(request);
                    redisService.setValue(key, json, RedisKeyConstants.HumanIntervention.DEFAULT_TTL_SECONDS);

                    return request;
                }
            }
        } catch (Exception e) {
            log.error("从数据库加载人工介入状态失败", e);
        }
        return null;
    }

    // ==================== 快照相关方法 ====================

    private static final String SNAPSHOT_PREFIX = "dag:snapshot:";

    @Override
    public void saveContextSnapshot(ExecutionContextSnapshot snapshot) {
        String key = SNAPSHOT_PREFIX + snapshot.getConversationId();
        String json = JSON.toJSONString(snapshot);

        // 写入 Redis
        redisService.setValue(key, json, RedisKeyConstants.HumanIntervention.DEFAULT_TTL_SECONDS);
        log.info("保存执行上下文快照到 Redis: conversationId={}, pausedNodeId={}",
                snapshot.getConversationId(), snapshot.getPausedNodeId());

        // 同步写入数据库备份
        syncSnapshotToDatabase(snapshot);
    }

    @Override
    public ExecutionContextSnapshot loadContextSnapshot(String conversationId) {
        String key = SNAPSHOT_PREFIX + conversationId;

        // 优先从 Redis 读取
        String json = redisService.getValue(key);
        if (json != null) {
            log.debug("从 Redis 加载执行上下文快照: conversationId={}", conversationId);
            return JSON.parseObject(json, ExecutionContextSnapshot.class);
        }

        // Redis miss，从数据库恢复
        log.info("Redis miss，从数据库加载执行上下文快照: conversationId={}", conversationId);
        return loadSnapshotFromDatabase(conversationId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateSnapshotEditableFields(String conversationId, Map<String, Object> modifications) {
        ExecutionContextSnapshot snapshot = loadContextSnapshot(conversationId);
        if (snapshot == null) {
            throw new RuntimeException("未找到快照: " + conversationId);
        }

        // 仅允许更新可编辑字段
        if (modifications.containsKey("nodeResults")) {
            snapshot.setNodeResults((Map<String, Object>) modifications.get("nodeResults"));
        }
        if (modifications.containsKey("userInput")) {
            snapshot.setUserInput((String) modifications.get("userInput"));
        }
        if (modifications.containsKey("customVariables")) {
            snapshot.setCustomVariables((Map<String, Object>) modifications.get("customVariables"));
        }
        if (modifications.containsKey("messageHistory")) {
            // messageHistory 需要转换为 List<ChatMessage>
            Object msgHistory = modifications.get("messageHistory");
            if (msgHistory instanceof java.util.List) {
                snapshot.setMessageHistory(
                        (java.util.List<com.zj.aiagent.domain.agent.dag.context.ChatMessage>) msgHistory);
            }
        }

        // 重新保存快照
        saveContextSnapshot(snapshot);
        log.info("更新快照可编辑字段: conversationId={}, modifiedKeys={}", conversationId, modifications.keySet());
    }

    @Override
    public void deleteContextSnapshot(String conversationId) {
        String key = SNAPSHOT_PREFIX + conversationId;
        redisService.remove(key);
        log.info("删除执行上下文快照: conversationId={}", conversationId);
    }

    /**
     * 同步快照到数据库备份
     */
    private void syncSnapshotToDatabase(ExecutionContextSnapshot snapshot) {
        try {
            DagExecutionInstance instance = dagExecutionRepository.findByConversationId(snapshot.getConversationId());
            if (instance != null) {
                // 将快照存储到 runtimeContextJson
                String snapshotJson = JSON.toJSONString(snapshot);
                instance.setRuntimeContextJson(snapshotJson);
                instance.setCurrentNodeId(snapshot.getPausedNodeId());
                instance.setStatus("PAUSED");
                instance.setUpdateTime(java.time.LocalDateTime.now());

                dagExecutionRepository.update(instance);
                log.debug("同步快照到数据库: conversationId={}", snapshot.getConversationId());
            }
        } catch (Exception e) {
            log.warn("同步快照到数据库失败，不影响主流程", e);
        }
    }

    /**
     * 从数据库加载快照
     */
    private ExecutionContextSnapshot loadSnapshotFromDatabase(String conversationId) {
        try {
            DagExecutionInstance instance = dagExecutionRepository.findByConversationId(conversationId);
            if (instance != null && instance.getRuntimeContextJson() != null) {
                ExecutionContextSnapshot snapshot = JSON.parseObject(
                        instance.getRuntimeContextJson(),
                        ExecutionContextSnapshot.class);

                // 回写到 Redis
                String key = SNAPSHOT_PREFIX + conversationId;
                redisService.setValue(key, instance.getRuntimeContextJson(),
                        RedisKeyConstants.HumanIntervention.DEFAULT_TTL_SECONDS);

                return snapshot;
            }
        } catch (Exception e) {
            log.error("从数据库加载快照失败: conversationId={}", conversationId, e);
        }
        return null;
    }
}
