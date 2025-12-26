package com.zj.aiagent.infrastructure.workflow;

import com.alibaba.fastjson.JSON;
import com.zj.aiagent.domain.workflow.interfaces.Checkpointer;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentInstancePO;
import com.zj.aiagent.infrastructure.persistence.repository.IAiAgentInstanceRepository;
import com.zj.aiagent.infrastructure.redis.IRedisService;
import com.zj.aiagent.shared.constants.RedisKeyConstants;
import com.zj.aiagent.shared.design.workflow.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流检查点实现
 */
@Slf4j
@Component
public class WorkflowCheckpointer implements Checkpointer {

    /**
     * 最后节点 ID 的特殊字段名
     */
    private static final String LAST_NODE_FIELD = "_lastNodeId";

    @Resource
    private IRedisService redisService;

    @Resource
    private IAiAgentInstanceRepository agentInstanceRepository;

    @Override
    public void save(String executionId, String nodeId, WorkflowState state) {
        if (executionId == null || nodeId == null || state == null) {
            log.warn("保存检查点参数为空，跳过: executionId={}, nodeId={}", executionId, nodeId);
            return;
        }

        try {
            // 1. 获取 Redis Map
            String mapKey = buildCheckpointMapKey(executionId);
            RMap<String, String> checkpointMap = redisService.getMap(mapKey);

            // 2. 序列化状态
            ConcurrentHashMap<String, Object> stateData = state.getAll();
            String stateJson = JSON.toJSONString(stateData);

            // 3. 同步写入 Redis Map（主存储）
            checkpointMap.put(nodeId, stateJson);
            checkpointMap.put(LAST_NODE_FIELD, nodeId);

            // 4. 设置过期时间
            checkpointMap.expireAsync(
                    java.time.Duration.ofSeconds(RedisKeyConstants.WorkflowCheckpoint.DEFAULT_TTL_SECONDS));

            log.debug("保存检查点到 Redis Map: executionId={}, nodeId={}, mapSize={}",
                    executionId, nodeId, checkpointMap.size());

            // 5. 异步写入数据库（备份）
            asyncSaveToDatabase(executionId, nodeId, stateJson);

        } catch (Exception e) {
            log.error("保存检查点失败: executionId={}, nodeId={}", executionId, nodeId, e);
        }
    }

    @Override
    public WorkflowState load(String executionId) {
        if (executionId == null) {
            log.warn("加载检查点 executionId 为空");
            return null;
        }

        try {
            // 1. 获取最后执行的节点 ID
            String lastNodeId = getLastNodeId(executionId);
            if (lastNodeId == null || lastNodeId.isEmpty()) {
                log.debug("未找到最后节点 ID: executionId={}", executionId);
                return null;
            }

            // 2. 加载该节点的检查点
            return loadAt(executionId, lastNodeId);

        } catch (Exception e) {
            log.error("加载检查点失败: executionId={}", executionId, e);
            return null;
        }
    }

    @Override
    public WorkflowState loadAt(String executionId, String nodeId) {
        if (executionId == null || nodeId == null) {
            log.warn("加载检查点参数为空: executionId={}, nodeId={}", executionId, nodeId);
            return null;
        }

        try {
            // 1. 优先从 Redis Map 加载
            String mapKey = buildCheckpointMapKey(executionId);
            RMap<String, String> checkpointMap = redisService.getMap(mapKey);
            String stateJson = checkpointMap.get(nodeId);

            if (stateJson != null && !stateJson.isEmpty()) {
                log.debug("从 Redis Map 加载检查点: executionId={}, nodeId={}", executionId, nodeId);
                return deserializeState(stateJson);
            }

            // 2. Redis miss，从数据库加载
            log.info("Redis miss，从数据库加载检查点: executionId={}, nodeId={}", executionId, nodeId);
            return loadFromDatabase(executionId, nodeId);

        } catch (Exception e) {
            log.error("加载检查点失败: executionId={}, nodeId={}", executionId, nodeId, e);
            return null;
        }
    }

    @Override
    public String getLastNodeId(String executionId) {
        if (executionId == null) {
            return "";
        }

        try {
            // 1. 优先从 Redis Map 获取
            String mapKey = buildCheckpointMapKey(executionId);
            RMap<String, String> checkpointMap = redisService.getMap(mapKey);
            String lastNodeId = checkpointMap.get(LAST_NODE_FIELD);

            if (lastNodeId != null && !lastNodeId.isEmpty()) {
                return lastNodeId;
            }

            // 2. Redis miss，从数据库获取
            log.debug("从数据库获取最后节点 ID: executionId={}", executionId);
            return loadLastNodeIdFromDatabase(executionId);

        } catch (Exception e) {
            log.error("获取最后节点 ID 失败: executionId={}", executionId, e);
            return "";
        }
    }

    @Override
    public void clear(String executionId) {
        if (executionId == null) {
            log.warn("清除检查点 executionId 为空");
            return;
        }

        try {
            // 1. 清除 Redis Map（一次性删除所有节点的检查点）
            String mapKey = buildCheckpointMapKey(executionId);
            RMap<String, String> checkpointMap = redisService.getMap(mapKey);
            int size = checkpointMap.size();
            checkpointMap.delete();

            log.info("清除检查点: executionId={}, 删除了 {} 个节点的数据", executionId, size);

            // 2. 异步清除数据库中的检查点
            asyncClearFromDatabase(executionId);

        } catch (Exception e) {
            log.error("清除检查点失败: executionId={}", executionId, e);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建检查点 Map Redis Key
     */
    private String buildCheckpointMapKey(String executionId) {
        return RedisKeyConstants.WorkflowCheckpoint.CHECKPOINT_MAP_PREFIX + executionId;
    }

    /**
     * 反序列化状态
     */
    private WorkflowState deserializeState(String stateJson) {
        try {
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<String, Object> stateData = JSON.parseObject(
                    stateJson,
                    ConcurrentHashMap.class);
            // 注意：这里创建的 WorkflowState 没有 listener，需要在使用时重新注入
            WorkflowState state = new WorkflowState(null);
            state.putAll(stateData);
            return state;
        } catch (Exception e) {
            log.error("反序列化状态失败", e);
            return null;
        }
    }

    /**
     * 异步保存到数据库
     * 将检查点数据和最后节点ID保存到 AiAgentInstancePO
     */
    @Async
    protected void asyncSaveToDatabase(String executionId, String nodeId, String stateJson) {
        try {
            // 1. 查询现有记录
            AiAgentInstancePO instance = agentInstanceRepository.findByConversationId(executionId);

            if (instance == null) {
                // 2. 不存在，创建新记录
                instance = AiAgentInstancePO.builder()
                        .conversationId(executionId)
                        .currentNodeId(nodeId)
                        .status("RUNNING")
                        .runtimeContextJson(buildRuntimeContextJson(nodeId, stateJson))
                        .build();
            } else {
                // 3. 存在，更新记录
                instance.setCurrentNodeId(nodeId);
                instance.setRuntimeContextJson(buildRuntimeContextJson(nodeId, stateJson));
            }

            agentInstanceRepository.saveOrUpdate(instance);
            log.debug("保存检查点数据库记录: executionId={}, nodeId={}", executionId, nodeId);

        } catch (Exception e) {
            log.warn("异步保存检查点到数据库失败，不影响主流程: executionId={}", executionId, e);
        }
    }

    /**
     * 构建运行时上下文 JSON
     * 包含所有节点的检查点数据
     */
    private String buildRuntimeContextJson(String lastNodeId, String stateJson) {
        Map<String, Object> context = new HashMap<>();
        context.put("lastNodeId", lastNodeId);
        context.put("lastNodeState", JSON.parse(stateJson));
        context.put("timestamp", System.currentTimeMillis());
        return JSON.toJSONString(context);
    }

    /**
     * 从数据库加载检查点
     */
    private WorkflowState loadFromDatabase(String executionId, String nodeId) {
        try {
            AiAgentInstancePO instance = agentInstanceRepository.findByConversationId(executionId);
            if (instance == null || instance.getRuntimeContextJson() == null) {
                log.debug("数据库中未找到检查点: executionId={}", executionId);
                return null;
            }

            // 解析运行时上下文
            @SuppressWarnings("unchecked")
            Map<String, Object> context = JSON.parseObject(
                    instance.getRuntimeContextJson(),
                    Map.class);

            Object lastNodeState = context.get("lastNodeState");
            if (lastNodeState != null) {
                String stateJson = JSON.toJSONString(lastNodeState);

                // 恢复到 Redis Map
                String mapKey = buildCheckpointMapKey(executionId);
                RMap<String, String> checkpointMap = redisService.getMap(mapKey);
                checkpointMap.put(nodeId, stateJson);
                checkpointMap.put(LAST_NODE_FIELD, instance.getCurrentNodeId());
                checkpointMap.expireAsync(
                        java.time.Duration.ofSeconds(RedisKeyConstants.WorkflowCheckpoint.DEFAULT_TTL_SECONDS));

                log.info("从数据库恢复检查点到 Redis: executionId={}, nodeId={}", executionId, nodeId);
                return deserializeState(stateJson);
            }

            return null;
        } catch (Exception e) {
            log.error("从数据库加载检查点失败: executionId={}", executionId, e);
            return null;
        }
    }

    /**
     * 从数据库加载最后节点 ID
     */
    private String loadLastNodeIdFromDatabase(String executionId) {
        try {
            AiAgentInstancePO instance = agentInstanceRepository.findByConversationId(executionId);
            if (instance != null && instance.getCurrentNodeId() != null) {
                String lastNodeId = instance.getCurrentNodeId();

                // 恢复到 Redis Map
                String mapKey = buildCheckpointMapKey(executionId);
                RMap<String, String> checkpointMap = redisService.getMap(mapKey);
                checkpointMap.put(LAST_NODE_FIELD, lastNodeId);
                checkpointMap.expireAsync(
                        java.time.Duration.ofSeconds(RedisKeyConstants.WorkflowCheckpoint.DEFAULT_TTL_SECONDS));

                return lastNodeId;
            }

            return "";
        } catch (Exception e) {
            log.error("从数据库加载最后节点 ID 失败: executionId={}", executionId, e);
            return "";
        }
    }

    /**
     * 异步清除数据库中的检查点
     */
    @Async
    protected void asyncClearFromDatabase(String executionId) {
        try {
            agentInstanceRepository.clearCheckpointData(executionId);
            log.debug("清除数据库检查点: executionId={}", executionId);
        } catch (Exception e) {
            log.warn("异步清除数据库检查点失败，不影响主流程: executionId={}", executionId, e);
        }
    }
}
