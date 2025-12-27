package com.zj.aiagent.interfaces.web.controller.client;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/client/agent/config")
@Tag(name = "agent表单配置", description = "agent表单配置")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
                RequestMethod.GET,
                RequestMethod.POST,
                RequestMethod.OPTIONS
})
public class AgentConfigController {

        @GetMapping("/node-templates")
        @Operation(summary = "获取所有可用的节点模板")
        public Response<String> getConfig() {
                return Response.success("success");
        }

        @GetMapping("/config-schema/{module}")
        @Operation(summary = "获取节点模板")
        public Response<String> getConfig() {
                return Response.success("success");
        }
}
