package com.zj.aiagent.domain.agent.repository;

import com.zj.aiagent.domain.agent.entity.Agent;
import com.zj.aiagent.domain.agent.entity.AgentVersion;
import com.zj.aiagent.domain.agent.valobj.AgentSummary;

import java.util.List;
import java.util.Optional;

/**
 * Agent Repository Interface
 * Separates Command (Full Load) and Query (Summary) operations.
 */
public interface AgentRepository {

    // --- Command / Full Detail Operations ---

    /**
     * Save or Update Agent
     */
    void save(Agent agent);

    /**
     * Delete Agent
     */
    void deleteById(Long id);

    /**
     * Find by ID with FULL details (including graphJson)
     */
    Optional<Agent> findById(Long id);

    // --- Query / Lightweight Operations ---

    /**
     * Find summaries by User ID (Excludes graphJson)
     */
    List<AgentSummary> findSummaryByUserId(Long userId);

    // --- Versioning Operations ---

    void saveVersion(AgentVersion version);

    Optional<AgentVersion> findVersion(Long agentId, Integer version);

    /**
     * Find max version number for agent
     */
    Optional<Integer> findMaxVersion(Long agentId);

    List<AgentVersion> findVersionHistory(Long agentId);
}
