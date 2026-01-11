package com.zj.aiagent.domain.agent.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GraphValidator Unit Test
 * Focus: Edge cases for cycle detection, connectivity, and invalid structures
 */
public class GraphValidatorTest {

    private final GraphValidator validator = new GraphValidator();

    @Test
    @DisplayName("Valid graph with START node and all connected nodes")
    public void testValidGraph() {
        String json = """
                {
                    "nodes": [
                        {"id": "1", "type": "START"},
                        {"id": "2", "type": "LLM"},
                        {"id": "3", "type": "OUTPUT"}
                    ],
                    "edges": [
                        {"source": "1", "target": "2"},
                        {"source": "2", "target": "3"}
                    ]
                }
                """;
        Assertions.assertDoesNotThrow(() -> validator.validate(json));
    }

    @Test
    @DisplayName("Cycle detection: A -> B -> A should fail")
    public void testCycleDetection() {
        String json = """
                {
                    "nodes": [
                        {"id": "1", "type": "START"},
                        {"id": "2", "type": "LLM"}
                    ],
                    "edges": [
                        {"source": "1", "target": "2"},
                        {"source": "2", "target": "1"}
                    ]
                }
                """;
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(json));
        Assertions.assertTrue(ex.getMessage().contains("cycle"));
    }

    @Test
    @DisplayName("Connectivity: Unreachable nodes should fail")
    public void testConnectivity() {
        String json = """
                {
                    "nodes": [
                        {"id": "1", "type": "START"},
                        {"id": "2", "type": "LLM"},
                        {"id": "3", "type": "ISOLATED"}
                    ],
                    "edges": [
                        {"source": "1", "target": "2"}
                    ]
                }
                """;
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(json));
        Assertions.assertTrue(ex.getMessage().contains("not fully connected"));
    }

    @Test
    @DisplayName("Missing START node should fail")
    public void testMissingStartNode() {
        String json = """
                {
                    "nodes": [
                        {"id": "1", "type": "LLM"},
                        {"id": "2", "type": "OUTPUT"}
                    ],
                    "edges": [
                        {"source": "1", "target": "2"}
                    ]
                }
                """;
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(json));
        Assertions.assertTrue(ex.getMessage().contains("start node"));
    }

    @Test
    @DisplayName("Multiple START nodes should fail")
    public void testMultipleStartNodes() {
        String json = """
                {
                    "nodes": [
                        {"id": "1", "type": "START"},
                        {"id": "2", "type": "START"}
                    ],
                    "edges": []
                }
                """;
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(json));
        Assertions.assertTrue(ex.getMessage().contains("Multiple start"));
    }

    @Test
    @DisplayName("Empty graph should fail")
    public void testEmptyGraph() {
        String json = """
                {
                    "nodes": [],
                    "edges": []
                }
                """;
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(json));
    }

    @Test
    @DisplayName("Invalid JSON should fail")
    public void testInvalidJson() {
        String json = "not valid json";
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(json));
    }
}
