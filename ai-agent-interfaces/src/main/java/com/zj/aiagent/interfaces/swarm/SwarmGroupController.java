package com.zj.aiagent.interfaces.swarm;

import com.zj.aiagent.application.swarm.SwarmMessageService;
import com.zj.aiagent.application.swarm.dto.SendMessageRequest;
import com.zj.aiagent.application.swarm.dto.SwarmGroupDTO;
import com.zj.aiagent.application.swarm.dto.SwarmMessageDTO;
import com.zj.aiagent.shared.response.Response;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SwarmGroupController {

    private final SwarmMessageService messageService;

    @GetMapping("/api/swarm/workspace/{wid}/groups")
    public Response<List<SwarmGroupDTO>> listGroups(
        @PathVariable Long wid,
        @RequestParam(required = false) Long agentId
    ) {
        return Response.success(messageService.listGroups(wid, agentId));
    }

    @GetMapping("/api/swarm/group/{gid}/messages")
    public Response<List<SwarmMessageDTO>> getMessages(
        @PathVariable Long gid,
        @RequestParam(defaultValue = "false") boolean markRead,
        @RequestParam(required = false) Long readerId
    ) {
        return Response.success(
            messageService.getMessages(gid, markRead, readerId)
        );
    }

    @PostMapping("/api/swarm/workspace/{wid}/groups/p2p")
    public Response<SwarmGroupDTO> createP2PGroup(
        @PathVariable Long wid,
        @RequestBody Map<String, Long> body
    ) {
        Long agentId1 = body.get("agentId1");
        Long agentId2 = body.get("agentId2");
        SwarmGroupDTO group = messageService.createP2PGroup(
            wid,
            agentId1,
            agentId2
        );
        return Response.success(group);
    }

    @PostMapping("/api/swarm/group/{gid}/messages")
    public Response<SwarmMessageDTO> sendMessage(
        @PathVariable Long gid,
        @RequestBody SendMessageRequest request
    ) {
        log.info(
            "[SwarmAPI] Send message request received: group={}, sender={}, contentType={}, preview={}",
            gid,
            request.getSenderId(),
            request.getContentType(),
            preview(request.getContent())
        );
        return Response.success(messageService.sendMessage(gid, request));
    }

    private String preview(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120
            ? normalized
            : normalized.substring(0, 120) + "...";
    }
}
