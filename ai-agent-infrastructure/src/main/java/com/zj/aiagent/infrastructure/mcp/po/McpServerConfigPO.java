package com.zj.aiagent.infrastructure.mcp.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

/**
 * MCP 服务器配置 PO
 */
@Data
@TableName(value = "mcp_server_config", autoResultMap = true)
public class McpServerConfigPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    /**
     * 服务器类型: stdio / sse / http
     */
    private String serverType;

    /**
     * MCP 服务器配置（JSON）
     * 使用 JacksonTypeHandler 自动序列化/反序列化
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private McpServerConfigJson configJson;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 连接状态: DISCONNECTED / CONNECTING / CONNECTED / ERROR
     */
    private String status;

    /**
     * 描述
     */
    private String description;

    /**
     * 逻辑删除标记
     */
    private Integer deleted;

    private java.time.LocalDateTime createTime;

    private java.time.LocalDateTime updateTime;

    /**
     * 配置 JSON 内部对象
     */
    @Data
    public static class McpServerConfigJson {
        private String type;
        private String command;
        private java.util.List<String> args;
        private java.util.Map<String, String> env;
        private String url;
        private java.util.Map<String, String> headers;
        private String endpoint;
    }
}
