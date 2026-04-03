package com.zj.aiagent.interfaces.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.mcp.cmd.CreateMcpServerCmd;
import com.zj.aiagent.application.mcp.cmd.UpdateMcpServerCmd;
import com.zj.aiagent.application.mcp.dto.McpServerDTO;
import com.zj.aiagent.application.mcp.dto.McpToolDTO;
import com.zj.aiagent.application.mcp.service.McpServerService;
import com.zj.aiagent.application.mcp.service.McpToolService;
import com.zj.aiagent.domain.mcp.valobj.McpServerStatus;
import com.zj.aiagent.shared.context.UserContext;
import com.zj.aiagent.shared.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MCP 服务器 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class McpServerController {

    private final McpServerService serverService;
    private final McpToolService toolService;

    /**
     * 获取当前用户所有服务器
     */
    @GetMapping("/servers")
    public Response<List<McpServerDTO>> listServers() {
        Long userId = getUserIdOrThrow();
        return Response.success(serverService.listServers(userId));
    }

    /**
     * 获取服务器详情
     */
    @GetMapping("/servers/{id}")
    public Response<McpServerDTO> getServer(@PathVariable Long id) {
        Long userId = getUserIdOrThrow();
        return Response.success(serverService.getServer(id, userId));
    }

    /**
     * 创建服务器
     */
    @PostMapping("/servers")
    public Response<Long> createServer(@RequestBody CreateServerRequest req) {
        Long userId = getUserIdOrThrow();
        CreateMcpServerCmd cmd = new CreateMcpServerCmd();
        cmd.setUserId(userId);
        cmd.setName(req.getName());
        cmd.setServerType(req.getServerType());
        cmd.setConfigJson(req.getConfigJson());
        cmd.setEnabled(req.getEnabled());
        cmd.setDescription(req.getDescription());
        Long id = serverService.createServer(cmd);
        return Response.success(id);
    }

    /**
     * 更新服务器
     */
    @PostMapping("/servers/{id}")
    public Response<Void> updateServer(@PathVariable Long id, @RequestBody UpdateServerRequest req) {
        Long userId = getUserIdOrThrow();
        UpdateMcpServerCmd cmd = new UpdateMcpServerCmd();
        cmd.setId(id);
        cmd.setUserId(userId);
        cmd.setName(req.getName());
        cmd.setServerType(req.getServerType());
        cmd.setConfigJson(req.getConfigJson());
        cmd.setEnabled(req.getEnabled());
        cmd.setDescription(req.getDescription());
        serverService.updateServer(cmd);
        return Response.success();
    }

    /**
     * 删除服务器
     */
    @DeleteMapping("/servers/{id}")
    public Response<Void> deleteServer(@PathVariable Long id) {
        Long userId = getUserIdOrThrow();
        serverService.deleteServer(id, userId);
        return Response.success();
    }

    /**
     * 删除服务器（POST 方式，前端 HttpClient delete 可选时的降级方案）
     */
    @PostMapping("/servers/{id}/delete")
    public Response<Void> deleteServerByPost(@PathVariable Long id) {
        Long userId = getUserIdOrThrow();
        serverService.deleteServer(id, userId);
        return Response.success();
    }

    /**
     * 手动触发连接
     */
    @PostMapping("/servers/{id}/connect")
    public Response<Void> connectServer(@PathVariable Long id) {
        Long userId = getUserIdOrThrow();
        serverService.connectServer(id, userId);
        return Response.success();
    }

    /**
     * 断开连接
     */
    @PostMapping("/servers/{id}/disconnect")
    public Response<Void> disconnectServer(@PathVariable Long id) {
        Long userId = getUserIdOrThrow();
        serverService.disconnectServer(id, userId);
        return Response.success();
    }

    /**
     * 获取服务器状态（用于轮询）
     */
    @GetMapping("/servers/{id}/status")
    public Response<McpServerStatus> getServerStatus(@PathVariable Long id) {
        return Response.success(serverService.getServerStatus(id));
    }

    /**
     * 获取指定服务器的工具列表
     */
    @GetMapping("/servers/{id}/tools")
    public Response<List<McpToolDTO>> getServerTools(@PathVariable Long id) {
        return Response.success(toolService.getToolsByServer(id));
    }

    /**
     * 获取所有已连接服务器的工具汇总
     */
    @GetMapping("/tools")
    public Response<List<McpToolDTO>> getAllTools() {
        return Response.success(toolService.getAllTools());
    }

    // --- Request DTOs ---

    @lombok.Data
    public static class CreateServerRequest {
        private String name;
        private String serverType;
        private String configJson;
        private Boolean enabled;
        private String description;
    }

    @lombok.Data
    public static class UpdateServerRequest {
        private String name;
        private String serverType;
        private String configJson;
        private Boolean enabled;
        private String description;
    }

    // --- Helper ---

    private Long getUserIdOrThrow() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new SecurityException("Unauthorized: no user context");
        }
        return userId;
    }
}
