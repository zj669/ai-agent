package com.zj.aiagent.interfaces.meta;

import com.zj.aiagent.application.agent.dto.NodeTemplateDTO;
import com.zj.aiagent.application.agent.service.MetadataApplicationService;
import com.zj.aiagent.shared.response.Response;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 元数据控制器
 * 提供工具、节点类型等元数据查询接口
 */
@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetadataController {

    private final MetadataApplicationService metadataService;

    @GetMapping("/node-templates")
    public Response<List<NodeTemplateDTO>> getNodeTemplates() {
        return Response.success(metadataService.getAllNodeTemplates());
    }

    /**
     * 获取节点类型列表（node-templates 的别名）
     */
    @GetMapping("/node-types")
    public Response<List<NodeTemplateDTO>> getNodeTypes() {
        return Response.success(metadataService.getAllNodeTemplates());
    }
}
