package com.zj.aiagent.interfaces.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.agent.dto.NodeTemplateDTO;
import com.zj.aiagent.application.agent.service.MetadataApplicationService;
import com.zj.aiagent.infrastructure.workflow.executor.NodeExecutorFactory;
import com.zj.aiagent.shared.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 元数据控制器
 * 提供工具、节点类型等元数据查询接口
 */
@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetadataController {

    private final NodeExecutorFactory executorFactory;
    private final ObjectMapper objectMapper;

    private final MetadataApplicationService metadataService;

    @GetMapping("/node-templates")
    public Response<List<NodeTemplateDTO>> getNodeTemplates() {
        return Response.success(metadataService.getAllNodeTemplates());
    }

}
