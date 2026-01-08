package com.zj.aiagent.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.infrastructure.persistence.entity.AiNodeTemplatePO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiNodeTemplateMapper;
import com.zj.aiagent.infrastructure.persistence.repository.INodeTemplateRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 节点模板仓储实现
 *
 * @author zj
 * @since 2025-12-27
 */
@Slf4j
@Repository
public class NodeTemplateRepository implements INodeTemplateRepository {

    @Resource
    private AiNodeTemplateMapper nodeTemplateMapper;

    @Override
    public List<AiNodeTemplatePO> findAll() {
        LambdaQueryWrapper<AiNodeTemplatePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiNodeTemplatePO::getIsDeprecated, false)
                .orderByAsc(AiNodeTemplatePO::getNodeType);
        return nodeTemplateMapper.selectList(wrapper);
    }

    @Override
    public AiNodeTemplatePO findByNodeType(String nodeType) {
        if (nodeType == null || nodeType.isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<AiNodeTemplatePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiNodeTemplatePO::getNodeType, nodeType);
        return nodeTemplateMapper.selectOne(wrapper);
    }
}
