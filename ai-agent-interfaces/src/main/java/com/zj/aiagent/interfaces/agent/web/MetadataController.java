package com.zj.aiagent.interfaces.agent.web;

import com.zj.aiagent.application.agent.dto.NodeTemplateDTO;
import com.zj.aiagent.application.agent.service.MetadataApplicationService;
import com.zj.aiagent.shared.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetadataController {

    private final MetadataApplicationService metadataService;

    @GetMapping("/node-templates")
    public Response<List<NodeTemplateDTO>> getNodeTemplates() {
        return Response.success(metadataService.getAllNodeTemplates());
    }
}
