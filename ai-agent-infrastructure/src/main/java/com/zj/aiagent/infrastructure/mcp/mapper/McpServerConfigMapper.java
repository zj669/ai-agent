package com.zj.aiagent.infrastructure.mcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.mcp.po.McpServerConfigPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MCP 服务器配置 Mapper
 */
@Mapper
public interface McpServerConfigMapper extends BaseMapper<McpServerConfigPO> {
}
