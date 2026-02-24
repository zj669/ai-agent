package com.zj.aiagent.infrastructure.swarm.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.swarm.entity.SwarmGroup;
import com.zj.aiagent.domain.swarm.repository.SwarmGroupRepository;
import com.zj.aiagent.infrastructure.swarm.mapper.SwarmGroupMapper;
import com.zj.aiagent.infrastructure.swarm.mapper.SwarmGroupMemberMapper;
import com.zj.aiagent.infrastructure.swarm.po.SwarmGroupMemberPO;
import com.zj.aiagent.infrastructure.swarm.po.SwarmGroupPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SwarmGroupRepositoryImpl implements SwarmGroupRepository {

    private final SwarmGroupMapper groupMapper;
    private final SwarmGroupMemberMapper memberMapper;

    @Override
    public void save(SwarmGroup group) {
        SwarmGroupPO po = toPO(group);
        groupMapper.insert(po);
        group.setId(po.getId());
    }

    @Override
    public Optional<SwarmGroup> findById(Long id) {
        return Optional.ofNullable(groupMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<SwarmGroup> findByWorkspaceId(Long workspaceId) {
        LambdaQueryWrapper<SwarmGroupPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SwarmGroupPO::getWorkspaceId, workspaceId);
        return groupMapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<SwarmGroup> findByAgentId(Long agentId) {
        // 先查 member 表拿到 groupId 列表，再查 group 表
        LambdaQueryWrapper<SwarmGroupMemberPO> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(SwarmGroupMemberPO::getAgentId, agentId);
        List<Long> groupIds = memberMapper.selectList(memberWrapper).stream()
                .map(SwarmGroupMemberPO::getGroupId).collect(Collectors.toList());
        if (groupIds.isEmpty()) return List.of();
        return groupMapper.selectBatchIds(groupIds).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteByWorkspaceId(Long workspaceId) {
        // 先删成员，再删群
        List<SwarmGroupPO> groups = groupMapper.selectList(
                new LambdaQueryWrapper<SwarmGroupPO>().eq(SwarmGroupPO::getWorkspaceId, workspaceId));
        for (SwarmGroupPO g : groups) {
            LambdaQueryWrapper<SwarmGroupMemberPO> mw = new LambdaQueryWrapper<>();
            mw.eq(SwarmGroupMemberPO::getGroupId, g.getId());
            memberMapper.delete(mw);
        }
        LambdaQueryWrapper<SwarmGroupPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SwarmGroupPO::getWorkspaceId, workspaceId);
        groupMapper.delete(wrapper);
    }

    @Override
    public void addMember(Long groupId, Long agentId) {
        SwarmGroupMemberPO po = new SwarmGroupMemberPO();
        po.setGroupId(groupId);
        po.setAgentId(agentId);
        po.setLastReadMessageId(0L);
        po.setJoinedAt(LocalDateTime.now());
        memberMapper.insert(po);
    }

    @Override
    public void removeMember(Long groupId, Long agentId) {
        LambdaQueryWrapper<SwarmGroupMemberPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SwarmGroupMemberPO::getGroupId, groupId)
                .eq(SwarmGroupMemberPO::getAgentId, agentId);
        memberMapper.delete(wrapper);
    }

    @Override
    public List<Long> findMemberIds(Long groupId) {
        return memberMapper.selectMemberIds(groupId);
    }

    @Override
    public void updateLastReadMessageId(Long groupId, Long agentId, Long messageId) {
        memberMapper.updateLastReadMessageId(groupId, agentId, messageId);
    }

    @Override
    public Long getLastReadMessageId(Long groupId, Long agentId) {
        Long result = memberMapper.selectLastReadMessageId(groupId, agentId);
        return result != null ? result : 0L;
    }

    private SwarmGroupPO toPO(SwarmGroup domain) {
        SwarmGroupPO po = new SwarmGroupPO();
        po.setId(domain.getId());
        po.setWorkspaceId(domain.getWorkspaceId());
        po.setName(domain.getName());
        po.setContextTokens(domain.getContextTokens());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private SwarmGroup toDomain(SwarmGroupPO po) {
        return SwarmGroup.builder()
                .id(po.getId())
                .workspaceId(po.getWorkspaceId())
                .name(po.getName())
                .contextTokens(po.getContextTokens())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
