package com.zj.aiagent.domain.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Domain Service for validating Graph structure.
 * Pure structural validation without DB dependencies.
 */
@Slf4j
@Service
public class GraphValidator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validates the graph JSON structure.
     * Checks: Schema, Entry Point, Connectivity, Cycles.
     *
     * @param graphJson the graph definition
     * @throws IllegalArgumentException if invalid
     */
    public void validate(String graphJson) {
        if (graphJson == null || graphJson.trim().isEmpty()) {
            throw new IllegalArgumentException("Graph definition cannot be empty");
        }

        try {
            JsonNode root = objectMapper.readTree(graphJson);
            JsonNode nodes = root.get("nodes");
            JsonNode edges = root.get("edges");

            if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
                throw new IllegalArgumentException("Graph must contain nodes");
            }
            // edges can be empty if single node? Usually fine.

            // 1. Check Entry Point (Start Node)
            String startNodeId = findStartNodeId(nodes);

            // Build Adjacency List for Graph checks
            Map<String, List<String>> adjList = buildAdjacencyList(nodes, edges);

            // 2. Connectivity: All nodes reachable from Start
            checkConnectivity(startNodeId, nodes, adjList);

            // 3. No Cycles
            checkCycles(nodes, adjList);

        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage(), e);
        }
    }

    private String findStartNodeId(JsonNode nodes) {
        String startId = null;
        for (JsonNode node : nodes) {
            // Frontend convention for Start Node: "type": "START" or "input"
            // We assume "type" == "start" (case insensitive) or specific key logic
            String type = node.has("type") ? node.get("type").asText() : "";
            if ("start".equalsIgnoreCase(type) || "input".equalsIgnoreCase(type)) {
                if (startId != null) {
                    throw new IllegalArgumentException("Multiple start nodes found");
                }
                startId = node.get("id").asText();
            }
        }
        if (startId == null) {
            throw new IllegalArgumentException("No start node found");
        }
        return startId;
    }

    private Map<String, List<String>> buildAdjacencyList(JsonNode nodes, JsonNode edges) {
        Map<String, List<String>> adj = new HashMap<>();
        // Init all nodes
        for (JsonNode node : nodes) {
            adj.put(node.get("id").asText(), new ArrayList<>());
        }

        if (edges != null && edges.isArray()) {
            for (JsonNode edge : edges) {
                String source = edge.get("source").asText();
                String target = edge.get("target").asText();
                if (adj.containsKey(source) && adj.containsKey(target)) {
                    adj.get(source).add(target);
                }
            }
        }
        return adj;
    }

    private void checkConnectivity(String startId, JsonNode nodes, Map<String, List<String>> adj) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(startId);
        visited.add(startId);

        while (!queue.isEmpty()) {
            String curr = queue.poll();
            for (String neighbor : adj.getOrDefault(curr, Collections.emptyList())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        if (visited.size() != nodes.size()) {
            throw new IllegalArgumentException("Graph is not fully connected. Unreachable nodes exist.");
        }
    }

    // Cycle detection using DFS
    private void checkCycles(JsonNode nodes, Map<String, List<String>> adj) {
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();

        for (JsonNode node : nodes) {
            String nodeId = node.get("id").asText();
            if (hasCycle(nodeId, adj, visited, recStack)) {
                throw new IllegalArgumentException("Graph contains a cycle involving node: " + nodeId);
            }
        }
    }

    private boolean hasCycle(String curr, Map<String, List<String>> adj, Set<String> visited, Set<String> recStack) {
        if (recStack.contains(curr)) {
            return true;
        }
        if (visited.contains(curr)) {
            return false;
        }

        visited.add(curr);
        recStack.add(curr);

        for (String neighbor : adj.getOrDefault(curr, Collections.emptyList())) {
            if (hasCycle(neighbor, adj, visited, recStack)) {
                return true;
            }
        }

        recStack.remove(curr);
        return false;
    }
}
