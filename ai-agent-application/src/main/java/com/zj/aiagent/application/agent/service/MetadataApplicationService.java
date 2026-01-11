package com.zj.aiagent.application.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.application.agent.dto.ConfigFieldDTO;
import com.zj.aiagent.application.agent.dto.NodeTemplateDTO;
import com.zj.aiagent.infrastructure.meta.mapper.NodeTemplateConfigMappingMapper;
import com.zj.aiagent.infrastructure.meta.mapper.NodeTemplateMapper;
import com.zj.aiagent.infrastructure.meta.mapper.SysConfigFieldDefMapper;
import com.zj.aiagent.infrastructure.meta.po.NodeTemplateConfigMappingPO;
import com.zj.aiagent.infrastructure.meta.po.NodeTemplatePO;
import com.zj.aiagent.infrastructure.meta.po.SysConfigFieldDefPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataApplicationService {

    private final NodeTemplateMapper nodeTemplateMapper;
    private final SysConfigFieldDefMapper fieldDefMapper;
    private final NodeTemplateConfigMappingMapper mappingMapper;

    /**
     * Get all node templates with their configuration definitions
     */
    public List<NodeTemplateDTO> getAllNodeTemplates() {
        // 1. Fetch all enabled templates
        List<NodeTemplatePO> templates = nodeTemplateMapper.selectList(
                new LambdaQueryWrapper<NodeTemplatePO>()
                        .eq(NodeTemplatePO::getStatus, 1)
                        .orderByAsc(NodeTemplatePO::getSortOrder));

        if (templates.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Fetch all mappings and fields
        // Optimization: Fetch all fields once (Metadata is usually small)
        // Or fetch only related to enabled templates. Metadata is small enough to fetch
        // all.
        List<SysConfigFieldDefPO> allFields = fieldDefMapper.selectList(null);
        Map<Long, SysConfigFieldDefPO> fieldMap = allFields.stream()
                .collect(Collectors.toMap(SysConfigFieldDefPO::getId, Function.identity()));

        List<NodeTemplateConfigMappingPO> allMappings = mappingMapper.selectList(null);
        Map<Long, List<NodeTemplateConfigMappingPO>> mappingMap = allMappings.stream()
                .collect(Collectors.groupingBy(NodeTemplateConfigMappingPO::getNodeTemplateId));

        // 3. Assemble DTOs
        return templates.stream().map(tpl -> {
            NodeTemplateDTO dto = new NodeTemplateDTO();
            dto.setId(tpl.getId());
            dto.setTypeCode(tpl.getTypeCode());
            dto.setName(tpl.getName());
            dto.setDescription(tpl.getDescription());
            dto.setIcon(tpl.getIcon());
            dto.setCategory(tpl.getCategory());
            dto.setSortOrder(tpl.getSortOrder());
            dto.setDefaultSchemaPolicy(tpl.getDefaultSchemaPolicy());
            dto.setInitialSchema(tpl.getInitialSchema());

            List<NodeTemplateConfigMappingPO> mappings = mappingMap.getOrDefault(tpl.getId(), new ArrayList<>());
            List<ConfigFieldDTO> fieldDTOs = mappings.stream()
                    .sorted(Comparator.comparingInt(NodeTemplateConfigMappingPO::getSortOrder))
                    .map(mapping -> {
                        SysConfigFieldDefPO fieldDef = fieldMap.get(mapping.getFieldDefId());
                        if (fieldDef == null)
                            return null;

                        ConfigFieldDTO fieldDTO = new ConfigFieldDTO();
                        // Base Field Def
                        fieldDTO.setFieldId(fieldDef.getId());
                        fieldDTO.setFieldKey(fieldDef.getFieldKey());
                        fieldDTO.setFieldLabel(fieldDef.getFieldLabel());
                        fieldDTO.setFieldType(fieldDef.getFieldType());
                        fieldDTO.setOptions(fieldDef.getOptions());
                        fieldDTO.setPlaceholder(fieldDef.getPlaceholder());
                        fieldDTO.setDescription(fieldDef.getDescription());
                        fieldDTO.setValidationRules(fieldDef.getValidationRules());
                        fieldDTO.setDefaultValue(fieldDef.getDefaultValue());

                        // Mapping Overrides
                        fieldDTO.setGroupName(mapping.getGroupName());
                        fieldDTO.setSortOrder(mapping.getSortOrder());
                        fieldDTO.setIsRequired(mapping.getIsRequired());
                        if (mapping.getOverrideDefault() != null) {
                            fieldDTO.setDefaultValue(mapping.getOverrideDefault());
                        }

                        return fieldDTO;
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            dto.setConfigFields(fieldDTOs);
            return dto;
        }).collect(Collectors.toList());
    }
}
