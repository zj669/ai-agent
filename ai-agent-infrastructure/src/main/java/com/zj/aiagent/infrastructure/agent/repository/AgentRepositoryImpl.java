package com.zj.aiagent.infrastructure.agent.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.agent.entity.Agent;
import com.zj.aiagent.domain.agent.entity.AgentVersion;
import com.zj.aiagent.domain.agent.repository.AgentRepository;
import com.zj.aiagent.domain.agent.valobj.AgentStatus;
import com.zj.aiagent.domain.agent.valobj.AgentSummary;
import com.zj.aiagent.infrastructure.agent.mapper.AgentMapper;
import com.zj.aiagent.infrastructure.agent.mapper.AgentVersionMapper;
import com.zj.aiagent.infrastructure.agent.po.AgentPO;
import com.zj.aiagent.infrastructure.agent.po.AgentVersionPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AgentRepositoryImpl implements AgentRepository {

    private final AgentMapper agentMapper;
    private final AgentVersionMapper agentVersionMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Agent agent) {
        AgentPO po = toPO(agent);
        if (po.getId() == null) {
            agentMapper.insert(po);
            agent.setId(po.getId()); // Write back ID
        } else {
            // Update with Optimistic Lock
            int rows = agentMapper.updateById(po);
            if (rows == 0) {
                // If ID exists but no rows updated, it means version mismatch or deleted
                throw new OptimisticLockingFailureException(
                        "Update failed: Agent modified by another user (Optimistic Lock)");
            }
        }
        // Update domain object with new version/times from PO if needed,
        // usually MyBatis updates PO objects. sync back:
        agent.setVersion(po.getVersion());
        agent.setUpdateTime(po.getUpdateTime());
    }

    @Override
    public void deleteById(Long id) {
        agentMapper.deleteById(id);
    }

    @Override
    public Optional<Agent> findById(Long id) {
        AgentPO po = agentMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<AgentSummary> findSummaryByUserId(Long userId) {
        List<AgentPO> pos = agentMapper.selectSummaryByUserId(userId);
        return pos.stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Override
    public void saveVersion(AgentVersion version) {
        AgentVersionPO po = toVersionPO(version);
        agentVersionMapper.insert(po);
        version.setId(po.getId());
    }

    @Override
    public Optional<AgentVersion> findVersion(Long agentId, Integer version) {
        LambdaQueryWrapper<AgentVersionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentVersionPO::getAgentId, agentId)
                .eq(AgentVersionPO::getVersion, version);
        AgentVersionPO po = agentVersionMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toVersionDomain);
    }

    @Override
    public Optional<Integer> findMaxVersion(Long agentId) {
        return Optional.ofNullable(agentVersionMapper.selectMaxVersion(agentId));
    }

    @Override
    public List<AgentVersion> findVersionHistory(Long agentId) {
        List<AgentVersionPO> pos = agentVersionMapper.selectHistory(agentId);
        return pos.stream().map(this::toVersionDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteVersion(Long agentId, Integer version) {
        LambdaQueryWrapper<AgentVersionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentVersionPO::getAgentId, agentId)
                .eq(AgentVersionPO::getVersion, version);
        agentVersionMapper.delete(wrapper);
    }

    @Override
    public void deleteAllVersions(Long agentId) {
        LambdaQueryWrapper<AgentVersionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentVersionPO::getAgentId, agentId);
        agentVersionMapper.delete(wrapper);
    }

    // --- Converters ---

    private AgentPO toPO(Agent domain) {
        if (domain == null)
            return null;
        AgentPO po = new AgentPO();
        po.setId(domain.getId());
        po.setUserId(domain.getUserId());
        po.setName(domain.getName());
        po.setDescription(domain.getDescription());
        po.setIcon(domain.getIcon());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().getCode() : 0);
        po.setPublishedVersionId(domain.getPublishedVersionId());
        po.setVersion(domain.getVersion());
        po.setDeleted(domain.getDeleted());

        // Convert String graphJson to JsonNode
        if (domain.getGraphJson() != null) {
            try {
                po.setGraphJson(objectMapper.readTree(domain.getGraphJson()));
            } catch (JsonProcessingException e) {
                log.error("Failed to parse graphJson for Agent ID {}", domain.getId(), e);
                // Depending on policy: throw error or set null?
                // Domain usually enforces valid JSON string.
                throw new IllegalArgumentException("Invalid graphJson string", e);
            }
        }
        return po;
    }

    private Agent toDomain(AgentPO po) {
        if (po == null)
            return null;
        return Agent.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .name(po.getName())
                .description(po.getDescription())
                .icon(po.getIcon())
                .graphJson(po.getGraphJson() != null ? po.getGraphJson().toString() : null)
                .status(AgentStatus.fromCode(po.getStatus()))
                .publishedVersionId(po.getPublishedVersionId())
                .version(po.getVersion())
                .deleted(po.getDeleted())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }

    private AgentSummary toSummary(AgentPO po) {
        if (po == null)
            return null;
        return AgentSummary.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .name(po.getName())
                .description(po.getDescription())
                .icon(po.getIcon())
                .status(AgentStatus.fromCode(po.getStatus()))
                .publishedVersionId(po.getPublishedVersionId())
                .updateTime(po.getUpdateTime())
                .build();
    }

    private AgentVersionPO toVersionPO(AgentVersion domain) {
        if (domain == null)
            return null;
        AgentVersionPO po = new AgentVersionPO();
        po.setId(domain.getId());
        po.setAgentId(domain.getAgentId());
        po.setVersion(domain.getVersion());
        po.setDescription(domain.getDescription());
        if (domain.getGraphSnapshot() != null) {
            try {
                po.setGraphSnapshot(objectMapper.readTree(domain.getGraphSnapshot()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid graphSnapshot string", e);
            }
        }
        return po;
    }

    private AgentVersion toVersionDomain(AgentVersionPO po) {
        if (po == null)
            return null;
        return AgentVersion.builder()
                .id(po.getId())
                .agentId(po.getAgentId())
                .version(po.getVersion())
                .graphSnapshot(po.getGraphSnapshot() != null ? po.getGraphSnapshot().toString() : null)
                .description(po.getDescription())
                .createTime(po.getCreateTime())
                .build();
    }
}
