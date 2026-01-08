package com.zj.aiagent.domain.toolbox.parse.entity;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class McpConfigResult {
    private List<McpSyncClient> mcpClients;
}
