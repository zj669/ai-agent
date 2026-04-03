import { useState } from "react";
import { Drawer, Typography } from "antd";
import { LoadingOutlined } from "@ant-design/icons";
import MarkdownRenderer from "./MarkdownRenderer";
import ToolCallBadge from "./ToolCallBadge";
import type { SwarmMessage, SwarmAgent, LiveToolCallStep } from "../../types/swarm";
import { parseToolCallMessage } from "./toolCallMessage";
import { AGENT_GRADIENTS, SWARM_COLORS } from "../../styles/swarm-colors";

const { Text } = Typography;

/** Strip markdown thinking fences from content before rendering */
function stripThinkingFences(raw: string): string {
  return raw
    .replace(/^```thinking\s*\n?/gim, "")
    .replace(/^```\s*\n?/gim, "")
    .trim();
}

interface Props {
  message: SwarmMessage;
  agents: SwarmAgent[];
  userId?: number;
  thinkingTitle?: string;
  /** Live tool call step shown while streaming */
  activeToolCall?: LiveToolCallStep | null;
  /** True when this bubble is the currently streaming message */
  isStreaming?: boolean;
}

export default function SwarmMessageBubble({
  message,
  agents,
  userId,
  thinkingTitle,
  activeToolCall,
  isStreaming = false,
}: Props) {
  const [toolDrawerOpen, setToolDrawerOpen] = useState(false);
  // userId 异步加载；加载前（userId == null）时默认当前用户发送的消息为人类消息
  const isHuman = userId != null ? message.senderId === userId : true;
  const sender = agents.find((a) => a.id === message.senderId);
  const colorIndex = agents.findIndex((a) => a.id === message.senderId);
  const gradient = AGENT_GRADIENTS[colorIndex % AGENT_GRADIENTS.length];
  const avatarLetter = isHuman ? "我" : (sender?.role?.charAt(0).toUpperCase() ?? "?");

  const isThinking = message.contentType === "thinking";
  const toolData = parseToolCallMessage(message);

  const bubbleBg = isHuman
    ? SWARM_COLORS.humanBubble
    : isThinking
      ? SWARM_COLORS.thinkingBubble
      : SWARM_COLORS.agentBubble;

  const bubbleColor = isHuman
    ? SWARM_COLORS.humanBubbleText
    : SWARM_COLORS.agentBubbleText;

  const bubbleStyle: React.CSSProperties = {
    borderRadius: isHuman ? "16px 16px 4px 16px" : "16px 16px 16px 4px",
    background: bubbleBg,
    color: bubbleColor,
    padding: isThinking ? "10px 14px" : "10px 14px",
    boxShadow: isHuman
      ? SWARM_COLORS.humanShadow
      : SWARM_COLORS.agentShadow,
    border: isHuman ? "none" : "1px solid #f0f0f0",
    maxWidth: "72%",
    width: "fit-content",
  };

  // ── Tool call message ────────────────────────────────────────────────────
  if (toolData) {
    const isRunning =
      activeToolCall?.toolCallId === `tool_${message.id}` ||
      activeToolCall?.status === "running";

    return (
      <>
        <div
          className="swarm-msg-enter"
          style={{
            display: "flex",
            flexDirection: isHuman ? "row-reverse" : "row",
            marginBottom: 10,
            gap: 8,
            alignItems: "flex-start",
          }}
        >
          {/* Avatar */}
          <div
            style={{
              width: 32,
              height: 32,
              borderRadius: "50%",
              background: gradient,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              color: "#fff",
              fontSize: 12,
              fontWeight: 600,
              flexShrink: 0,
            }}
          >
            {avatarLetter}
          </div>
          <div style={{ maxWidth: "70%" }}>
            {!isHuman && (
              <Text
                type="secondary"
                style={{ fontSize: 12, marginBottom: 4, display: "block" }}
              >
                {sender?.role ?? `agent_${message.senderId}`}
              </Text>
            )}
            {/* Inline tool call compact card */}
            <div
              style={{
                borderRadius: 12,
                border: `1px solid ${SWARM_COLORS.toolCardBorder}`,
                background: SWARM_COLORS.toolCardBg,
                overflow: "hidden",
              }}
            >
              {/* Header row */}
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 8,
                  padding: "8px 12px",
                  borderBottom: `1px solid ${SWARM_COLORS.toolCardBorder}`,
                }}
              >
                {isRunning ? (
                  <LoadingOutlined style={{ color: "#1677ff" }} />
                ) : (
                  <ToolCallBadge toolName={toolData.tool} status="done" />
                )}
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {isRunning ? "正在调用..." : "已调用"}
                </Text>
                <button
                  type="button"
                  onClick={() => setToolDrawerOpen(true)}
                  style={{
                    marginLeft: "auto",
                    background: "none",
                    border: "none",
                    cursor: "pointer",
                    color: "#1677ff",
                    fontSize: 12,
                    padding: 0,
                  }}
                >
                  查看详情 →
                </button>
              </div>
              {/* Args preview */}
              <div style={{ padding: "8px 12px" }}>
                <Text
                  strong
                  style={{
                    fontSize: 11,
                    color: "#8c8c8c",
                    display: "block",
                    marginBottom: 4,
                  }}
                >
                  调用参数
                </Text>
                <div
                  style={{
                    background: SWARM_COLORS.codeBg,
                    borderRadius: 6,
                    padding: "6px 10px",
                    fontSize: 12,
                    lineHeight: 1.5,
                    whiteSpace: "pre-wrap",
                    wordBreak: "break-word",
                    color: "#a5d6ff",
                    maxHeight: 80,
                    overflow: "hidden",
                    position: "relative",
                  }}
                >
                  {toolData.args || "无参数"}
                  <div
                    style={{
                      position: "absolute",
                      bottom: 0,
                      left: 0,
                      right: 0,
                      height: 24,
                      background: "linear-gradient(transparent, #0f172a)",
                    }}
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <Drawer
          title={`工具调用详情 · ${toolData.tool}`}
          open={toolDrawerOpen}
          width={480}
          onClose={() => setToolDrawerOpen(false)}
        >
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            <div>
              <Text strong style={{ fontSize: 13 }}>
                调用参数
              </Text>
              <div
                style={{
                  background: "#0f172a",
                  borderRadius: 8,
                  padding: 12,
                  marginTop: 6,
                  color: "#e6edf3",
                  fontSize: 13,
                  lineHeight: 1.6,
                  whiteSpace: "pre-wrap",
                  wordBreak: "break-word",
                }}
              >
                {toolData.args || "无参数"}
              </div>
            </div>
            <div>
              <Text strong style={{ fontSize: 13 }}>
                执行结果
              </Text>
              <div
                style={{
                  background: "#f6f8fa",
                  borderRadius: 8,
                  padding: 12,
                  marginTop: 6,
                  color: "#1a1a2e",
                  fontSize: 13,
                  lineHeight: 1.6,
                  whiteSpace: "pre-wrap",
                  wordBreak: "break-word",
                  border: "1px solid #d0d7de",
                }}
              >
                {toolData.result || "无返回结果"}
              </div>
            </div>
          </div>
        </Drawer>
      </>
    );
  }

  // ── Thinking / processing placeholder ────────────────────────────────────
  if (isThinking) {
    return (
      <div
        className="swarm-msg-enter"
        style={{
          display: "flex",
          flexDirection: isHuman ? "row-reverse" : "row",
          marginBottom: 10,
          gap: 8,
          alignItems: "flex-start",
        }}
      >
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: "50%",
            background: gradient,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "#fff",
            fontSize: 12,
            fontWeight: 600,
            flexShrink: 0,
          }}
        >
          {avatarLetter}
        </div>
        <div style={{ maxWidth: "72%" }}>
          {!isHuman && (
            <Text
              type="secondary"
              style={{ fontSize: 12, marginBottom: 4, display: "block" }}
            >
              {sender?.role ?? `agent_${message.senderId}`}
            </Text>
          )}
          <div
            style={{
              borderRadius: 16,
              background: SWARM_COLORS.thinkingBubble,
              border: `1px solid ${SWARM_COLORS.thinkingBorder}`,
              padding: "10px 14px",
              boxShadow: "0 1px 4px rgba(0,0,0,0.06)",
            }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8,
                marginBottom: 6,
              }}
            >
              <LoadingOutlined style={{ color: "#1677ff", fontSize: 13 }} />
              <Text
                style={{ fontSize: 13, color: "#1677ff", fontWeight: 600 }}
              >
                {thinkingTitle || "正在思考..."}
              </Text>
            </div>
            {message.content && (
              <MarkdownRenderer
                content={stripThinkingFences(message.content)}
                style={{ fontSize: 13, color: "#475569" }}
              />
            )}
          </div>
        </div>
      </div>
    );
  }

  // ── Regular text message ──────────────────────────────────────────────────
  return (
    <div
      className="swarm-msg-enter"
      style={{
        display: "flex",
        flexDirection: isHuman ? "row-reverse" : "row",
        marginBottom: 10,
        gap: 8,
        alignItems: "flex-start",
      }}
    >
      {/* Avatar */}
      <div
        style={{
          width: 32,
          height: 32,
          borderRadius: "50%",
          background: gradient,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          color: "#fff",
          fontSize: 12,
          fontWeight: 600,
          flexShrink: 0,
        }}
      >
        {avatarLetter}
      </div>
      <div style={{ maxWidth: "72%" }}>
        {!isHuman && (
          <Text
            type="secondary"
            style={{ fontSize: 12, marginBottom: 4, display: "block" }}
          >
            {sender?.role ?? `agent_${message.senderId}`}
          </Text>
        )}
        <div style={bubbleStyle}>
          <MarkdownRenderer content={message.content} streaming={isStreaming} />
        </div>
      </div>
    </div>
  );
}
