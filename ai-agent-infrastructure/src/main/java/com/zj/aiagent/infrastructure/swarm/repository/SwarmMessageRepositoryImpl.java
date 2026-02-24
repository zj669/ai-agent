package com.zj.aiagent.infrastructure.swarm.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.swarm.entity.SwarmMessage;
import com.zj.aiagent.domain.swarm.repository.SwarmMessageRepository;
import com.zj.aiagent.infrastructure.swarm.mapper.SwarmMessageMapper;
import com.zj.aiagent.infrastructure.swarm.po.SwarmMessagePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SwarmMessageRepositoryImpl implements SwarmMessageRepository {

    private final SwarmMessageMapper mapper;

    @Override
    public void save(SwarmMessage message) {
        SwarmMessagePO po = toPO(message);
        mapper.insert(po);
        message.setId(po.getId());
    }

    @Override
    public List<SwarmMessage> findByGroupId(Long groupId) {
        LambdaQueryWrapper<SwarmMessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SwarmMessagePO::getGroupId, groupId).orderByAsc(SwarmMessagePO::getSendTime);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<SwarmMessage> findByGroupIdAfter(Long groupId, Long afterId) {
        LambdaQueryWrapper<SwarmMessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SwarmMessagePO::getGroupId, groupId)
                .gt(SwarmMessagePO::getId, afterId)
                .orderByAsc(SwarmMessagePO::getSendTime);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public int countMessagesBetween(Long workspaceId, Long agentId1, Long agentId2) {
        return mapper.countMessagesBetween(workspaceId, agentId1, agentId2);
    }

    @Override
    public void deleteByWorkspaceId(Long workspaceId) {
        LambdaQueryWrapper<SwarmMessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SwarmMessagePO::getWorkspaceId, workspaceId);
        mapper.delete(wrapper);
    }

    @Override
    public void deleteByGroupId(Long groupId) {
        LambdaQueryWrapper<SwarmMessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SwarmMessagePO::getGroupId, groupId);
        mapper.delete(wrapper);
    }

    private SwarmMessagePO toPO(SwarmMessage domain) {
        SwarmMessagePO po = new SwarmMessagePO();
        po.setId(domain.getId());
        po.setWorkspaceId(domain.getWorkspaceId());
        po.setGroupId(domain.getGroupId());
        po.setSenderId(domain.getSenderId());
        po.setContentType(domain.getContentType());
        po.setContent(domain.getContent());
        po.setSendTime(domain.getSendTime());
        return po;
    }

    private SwarmMessage toDomain(SwarmMessagePO po) {
        return SwarmMessage.builder()
                .id(po.getId())
                .workspaceId(po.getWorkspaceId())
                .groupId(po.getGroupId())
                .senderId(po.getSenderId())
                .contentType(po.getContentType())
                .content(po.getContent())
                .sendTime(po.getSendTime())
                .build();
    }
}
