package com.zj.aiagent.infrastructure.workflow.graph.converter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import com.zj.aiagent.infrastructure.meta.mapper.NodeTemplateConfigMappingMapper;
import com.zj.aiagent.infrastructure.meta.mapper.NodeTemplateMapper;
import com.zj.aiagent.infrastructure.meta.mapper.SysConfigFieldDefMapper;
import com.zj.aiagent.infrastructure.meta.po.NodeTemplateConfigMappingPO;
import com.zj.aiagent.infrastructure.meta.po.NodeTemplatePO;
import com.zj.aiagent.infrastructure.meta.po.SysConfigFieldDefPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * NodeConfig 转换器
 * 根据数据库配置动态解析 userConfig
 * 
 * 工作流程：
 * 1. 根据 nodeType 查询 node_template 表获取模板 ID
 * 2. 根据模板 ID 查询 node_template_config_mapping 表获取配置字段列表
 * 3. 根据字段 ID 查询 sys_config_field_def 表获取字段 key
 * 4. 从 userConfig 中提取对应的值，缺失则记录日志并设为 null
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeConfigConverter {

    private final NodeTemplateMapper nodeTemplateMapper;
    private final NodeTemplateConfigMappingMapper mappingMapper;
    private final SysConfigFieldDefMapper fieldDefMapper;

    /**
     * 转换 userConfig 为 NodeConfig
     *
     * @param nodeType   节点类型
     * @param userConfig 用户配置 Map（扁平结构）
     * @return NodeConfig 实例
     */
    public NodeConfig convert(NodeType nodeType, Map<String, Object> userConfig) {
        if (userConfig == null) {
            userConfig = new HashMap<>();
        }

        // 1. 获取该节点类型的配置字段 key 列表
        Set<String> fieldKeys = getFieldKeysForNodeType(nodeType);

        // 2. 构建配置 Map
        Map<String, Object> properties = new HashMap<>();

        for (String key : fieldKeys) {
            if (userConfig.containsKey(key)) {
                properties.put(key, userConfig.get(key));
            } else {
                log.debug("[NodeConfigConverter] Field '{}' not found in userConfig for nodeType {}", key, nodeType);
                properties.put(key, null);
            }
        }

        // 3. 同时保留 userConfig 中不在字段定义中的额外配置（向前兼容）
        for (Map.Entry<String, Object> entry : userConfig.entrySet()) {
            if (!properties.containsKey(entry.getKey())) {
                properties.put(entry.getKey(), entry.getValue());
            }
        }

        return NodeConfig.builder()
                .properties(properties)
                .build();
    }

    /**
     * 获取节点类型对应的配置字段 key 列表
     */
    private Set<String> getFieldKeysForNodeType(NodeType nodeType) {
        // 1. 查询节点模板
        NodeTemplatePO template = nodeTemplateMapper.selectOne(
                new LambdaQueryWrapper<NodeTemplatePO>()
                        .eq(NodeTemplatePO::getTypeCode, nodeType.name())
                        .eq(NodeTemplatePO::getStatus, 1));

        if (template == null) {
            log.warn("[NodeConfigConverter] No template found for nodeType: {}", nodeType);
            return Set.of();
        }

        // 2. 查询配置字段映射
        List<NodeTemplateConfigMappingPO> mappings = mappingMapper.selectList(
                new LambdaQueryWrapper<NodeTemplateConfigMappingPO>()
                        .eq(NodeTemplateConfigMappingPO::getNodeTemplateId, template.getId()));

        if (mappings.isEmpty()) {
            log.debug("[NodeConfigConverter] No config mappings for template: {}", template.getId());
            return Set.of();
        }

        // 3. 获取字段 ID 列表
        List<Long> fieldIds = mappings.stream()
                .map(NodeTemplateConfigMappingPO::getFieldDefId)
                .collect(Collectors.toList());

        // 4. 查询字段定义获取 key
        List<SysConfigFieldDefPO> fieldDefs = fieldDefMapper.selectBatchIds(fieldIds);

        return fieldDefs.stream()
                .map(SysConfigFieldDefPO::getFieldKey)
                .collect(Collectors.toSet());
    }
}
