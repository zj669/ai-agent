package com.zj.aiagent.application.swarm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SwarmGraphDTO {

    private List<GraphNode> nodes;
    private List<GraphEdge> edges;

    @Data
    @Builder
    public static class GraphNode {
        private Long id;
        private String role;
        private Long parentId;
        private String status;
    }

    @Data
    @Builder
    public static class GraphEdge {
        private Long from;
        private Long to;
        private Integer count;
    }
}
