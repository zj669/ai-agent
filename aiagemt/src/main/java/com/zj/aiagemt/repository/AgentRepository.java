package com.zj.aiagemt.repository;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.zj.aiagemt.repository.base.AiAgentMapper;
import com.zj.aiagemt.repository.base.AiAgentTaskScheduleMapper;
import com.zj.aiagemt.repository.base.AiAdvisorMapper;
import com.zj.aiagemt.repository.base.AiApiMapper;
import com.zj.aiagemt.repository.base.AiModelMapper;
import com.zj.aiagemt.repository.base.AiRagOrderMapper;
import com.zj.aiagemt.repository.base.AiSystemPromptMapper;
import com.zj.aiagemt.repository.base.AiToolMcpMapper;
import com.zj.aiagemt.model.dto.AgentInfoDTO;
import com.zj.aiagemt.model.entity.*;
import com.zj.aiagemt.model.enums.AiAgentEnumVO;
import com.zj.aiagemt.model.vo.*;
import com.zj.aiagemt.service.agent.IAgentRepository;
import com.zj.aiagemt.service.agent.impl.armory.factory.DefaultAgentArmoryFactory;
import com.zj.aiagemt.utils.SpringContextUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.*;

@Slf4j
@Repository
@Primary
public class AgentRepository implements IAgentRepository {

    @Override
    public void queryApiByClientIdS(List<String> commandIdList, DefaultAgentArmoryFactory.DynamicContext context) {

    }

    @Override
    public void queryModelByClientIdS(List<String> commandIdList, DefaultAgentArmoryFactory.DynamicContext context) {

    }

    @Override
    public void queryMcpByClientIdS(List<String> commandIdList, DefaultAgentArmoryFactory.DynamicContext context) {

    }

    @Override
    public void queryAdvisorByClientIdS(List<String> commandIdList, DefaultAgentArmoryFactory.DynamicContext context) {

    }

    @Override
    public void queryPromptByClientIdS(List<String> commandIdList, DefaultAgentArmoryFactory.DynamicContext context) {

    }

    @Override
    public void queryAiClientVOByClientIds(List<String> commandIdList, DefaultAgentArmoryFactory.DynamicContext context) {

    }

    @Override
    public Map<String, AiAgentClientFlowConfigVO> queryAiAgentClientFlowConfig(String aiAgentId) {
        return Map.of();
    }

    @Override
    public List<String> queryClientIdsByAgentId(String aiAgentId) {
        return List.of();
    }

    @Override
    public List<AiAgent> queryAgentDtoList() {
        return List.of();
    }
}
