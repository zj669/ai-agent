package com.zj.aiagent.interfaces.swarm;

import com.zj.aiagent.application.swarm.SwarmMessageService;
import com.zj.aiagent.application.swarm.dto.SendMessageRequest;
import com.zj.aiagent.application.swarm.dto.SwarmGroupDTO;
import com.zj.aiagent.application.swarm.dto.SwarmMessageDTO;
import com.zj.aiagent.shared.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SwarmGroupController {

    private final SwarmMessageService messageService;

    @GetMapping("/api/swarm/workspace/{wid}/groups")
    public Response<List<SwarmGroupDTO>> listGroups(
            @PathVariable Long wid,
            @RequestParam(required = false) Long agentId) {
        return Response.success(messageService.listGroups(wid, agentId));
    }

    @GetMapping("/api/swarm/group/{gid}/messages")
    public Response<List<SwarmMessageDTO>> getMessages(
            @PathVariable Long gid,
            @RequestParam(defaultValue = "false") boolean markRead,
            @RequestParam(required = false) Long readerId) {
        return Response.success(messageService.getMessages(gid, markRead, readerId));
    }

    @PostMapping("/api/swarm/group/{gid}/messages")
    public Response<SwarmMessageDTO> sendMessage(
            @PathVariable Long gid,
            @RequestBody SendMessageRequest request) {
        return Response.success(messageService.sendMessage(gid, request));
    }
}
