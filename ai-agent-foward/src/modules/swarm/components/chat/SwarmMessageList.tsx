import { useEffect, useRef } from "react";
import { Empty, Space, Typography } from "antd";
import SwarmMessageBubble from "./SwarmMessageBubble";
import ToolCallBadge from "./ToolCallBadge";
import type {
  LiveToolCallStep,
  SwarmMessage,
  SwarmAgent,
} from "../../types/swarm";
import { getToolCallSignature } from "./toolCallMessage";

const { Text } = Typography;

interface Props {
  messages: SwarmMessage[];
  agents: SwarmAgent[];
  humanAgentId?: number;
  streamingContent?: string | null;
  streamingAgentId?: number | null;
  liveToolCalls?: LiveToolCallStep[];
  processingState?: {
    agentId: number | null;
    title: string;
    detail?: string;
  } | null;
}

export default function SwarmMessageList({
  messages,
  agents,
  humanAgentId,
  streamingContent,
  streamingAgentId,
  liveToolCalls = [],
  processingState = null,
}: Props) {
  const bottomRef = useRef<HTMLDivElement>(null);
  const activeStreamAgentId =
    streamingAgentId ?? liveToolCalls[0]?.agentId ?? null;
  const shouldRenderLiveToolCalls =
    liveToolCalls.length > 0 &&
    activeStreamAgentId !== null &&
    (streamingContent === "" ||
      streamingContent === null ||
      streamingContent === undefined);
  const shouldRenderProcessingState =
    !shouldRenderLiveToolCalls &&
    (streamingContent === null || streamingContent === undefined) &&
    processingState?.agentId != null;
  const visibleMessages = messages.filter((message, index) => {
    const signature = getToolCallSignature(message);
    if (!signature || message.contentType === "tool_call") {
      return true;
    }

    const hasStructuredDuplicate = messages.some(
      (candidate, candidateIndex) => {
        if (candidateIndex === index) {
          return false;
        }
        if (
          candidate.groupId !== message.groupId ||
          candidate.senderId !== message.senderId
        ) {
          return false;
        }
        if (candidate.contentType !== "tool_call") {
          return false;
        }

        return getToolCallSignature(candidate) === signature;
      },
    );

    return !hasStructuredDuplicate;
  });

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [visibleMessages.length, streamingContent]);

  if (
    visibleMessages.length === 0 &&
    streamingContent === null &&
    liveToolCalls.length === 0
  ) {
    return <Empty description="暂无消息" style={{ marginTop: 40 }} />;
  }

  return (
    <div style={{ flex: 1, overflow: "auto", padding: "12px 16px" }}>
      {visibleMessages.map((msg) => (
        <SwarmMessageBubble
          key={msg.id}
          message={msg}
          agents={agents}
          humanAgentId={humanAgentId}
        />
      ))}
      {shouldRenderLiveToolCalls ? (
        <div
          style={{
            display: "flex",
            marginBottom: 12,
            gap: 8,
          }}
        >
          <div
            style={{
              width: 32,
              height: 32,
              borderRadius: "50%",
              background: "#722ed1",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              color: "#fff",
              fontSize: 12,
              fontWeight: 600,
              flexShrink: 0,
            }}
          >
            {agents
              .find((agent) => agent.id === activeStreamAgentId)
              ?.role?.charAt(0)
              .toUpperCase() ?? "A"}
          </div>
          <div style={{ maxWidth: "78%" }}>
            <Text
              type="secondary"
              style={{ fontSize: 12, marginBottom: 6, display: "block" }}
            >
              {agents.find((agent) => agent.id === activeStreamAgentId)?.role ??
                `agent_${activeStreamAgentId}`}
            </Text>
            <div
              style={{
                padding: "10px 12px",
                borderRadius: 12,
                background:
                  "linear-gradient(180deg, rgba(248,250,252,0.96) 0%, rgba(241,245,249,0.92) 100%)",
                border: "1px solid #dbeafe",
              }}
            >
              <Text
                strong
                style={{
                  display: "block",
                  marginBottom: 8,
                  color: "#1d4ed8",
                }}
              >
                正在规划与协作
              </Text>
              <Space size={[8, 8]} wrap>
                {liveToolCalls.map((step) => (
                  <ToolCallBadge
                    key={step.toolCallId}
                    toolName={step.tool}
                    status={step.status}
                  />
                ))}
              </Space>
            </div>
          </div>
        </div>
      ) : streamingContent !== null &&
        streamingContent !== undefined &&
        streamingAgentId ? (
        <SwarmMessageBubble
          message={{
            id: -1,
            groupId: 0,
            senderId: streamingAgentId,
            content:
              streamingContent === "" ? "正在思考..." : streamingContent + "▌",
            contentType: streamingContent === "" ? "thinking" : "text",
            sendTime: new Date().toISOString(),
          }}
          agents={agents}
          humanAgentId={humanAgentId}
        />
      ) : shouldRenderProcessingState ? (
        <SwarmMessageBubble
          message={{
            id: -2,
            groupId: 0,
            senderId: processingState.agentId as number,
            content: processingState.detail || processingState.title,
            contentType: "thinking",
            sendTime: new Date().toISOString(),
          }}
          agents={agents}
          humanAgentId={humanAgentId}
          thinkingTitle={processingState.title}
        />
      ) : null}
      <div ref={bottomRef} />
    </div>
  );
}
