import { useState } from "react";
import { Typography } from "antd";
import {
  DownOutlined,
  LoadingOutlined,
  RightOutlined,
} from "@ant-design/icons";
import ToolCallBadge from "./ToolCallBadge";
import type { SwarmMessage, SwarmAgent } from "../../types/swarm";
import { parseToolCallMessage } from "./toolCallMessage";

const { Paragraph, Text } = Typography;

const AGENT_COLORS = [
  "#1677ff",
  "#722ed1",
  "#13c2c2",
  "#eb2f96",
  "#fa8c16",
  "#52c41a",
  "#2f54eb",
];

interface Props {
  message: SwarmMessage;
  agents: SwarmAgent[];
  humanAgentId?: number;
}

export default function SwarmMessageBubble({
  message,
  agents,
  humanAgentId,
}: Props) {
  const [toolExpanded, setToolExpanded] = useState(false);
  const isHuman = message.senderId === humanAgentId;
  const sender = agents.find((a) => a.id === message.senderId);
  const colorIndex = agents.findIndex((a) => a.id === message.senderId);
  const color = AGENT_COLORS[colorIndex % AGENT_COLORS.length];

  const isThinking = message.contentType === "thinking";
  const toolData = parseToolCallMessage(message);

  return (
    <div
      style={{
        display: "flex",
        flexDirection: isHuman ? "row-reverse" : "row",
        marginBottom: 12,
        gap: 8,
      }}
    >
      <div
        style={{
          width: 32,
          height: 32,
          borderRadius: "50%",
          background: isHuman ? "#1677ff" : color,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          color: "#fff",
          fontSize: 12,
          fontWeight: 600,
          flexShrink: 0,
        }}
      >
        {isHuman ? "我" : (sender?.role?.charAt(0).toUpperCase() ?? "?")}
      </div>

      <div style={{ maxWidth: "70%" }}>
        {!isHuman && (
          <Text
            type="secondary"
            style={{ fontSize: 12, marginBottom: 2, display: "block" }}
          >
            {sender?.role ?? `agent_${message.senderId}`}
          </Text>
        )}
        {toolData ? (
          <div
            style={{
              borderRadius: 12,
              border: "1px solid #e5e7eb",
              background: "#f8fafc",
              overflow: "hidden",
            }}
          >
            <button
              type="button"
              onClick={() => setToolExpanded((value) => !value)}
              style={{
                width: "100%",
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                gap: 12,
                padding: "10px 12px",
                border: "none",
                background: "transparent",
                cursor: "pointer",
                textAlign: "left",
              }}
            >
              <ToolCallBadge toolName={toolData.tool} />
              <Text
                type="secondary"
                style={{
                  fontSize: 12,
                  display: "inline-flex",
                  alignItems: "center",
                  gap: 6,
                  flexShrink: 0,
                }}
              >
                {toolExpanded ? <DownOutlined /> : <RightOutlined />}
                {toolExpanded ? "收起详情" : "展开详情"}
              </Text>
            </button>
            {toolExpanded ? (
              <div
                style={{
                  borderTop: "1px solid #e5e7eb",
                  padding: "12px",
                  background: "#fff",
                  display: "flex",
                  flexDirection: "column",
                  gap: 12,
                }}
              >
                <div>
                  <Text
                    strong
                    style={{ fontSize: 12, display: "block", marginBottom: 6 }}
                  >
                    调用参数
                  </Text>
                  <div
                    style={{
                      borderRadius: 8,
                      background: "#0f172a",
                      color: "#e2e8f0",
                      padding: "10px 12px",
                      fontSize: 12,
                      lineHeight: 1.6,
                      whiteSpace: "pre-wrap",
                      wordBreak: "break-word",
                    }}
                  >
                    {toolData.args || "无参数"}
                  </div>
                </div>
                <div>
                  <Text
                    strong
                    style={{ fontSize: 12, display: "block", marginBottom: 6 }}
                  >
                    执行结果
                  </Text>
                  <div
                    style={{
                      borderRadius: 8,
                      background: "#f8fafc",
                      color: "#0f172a",
                      padding: "10px 12px",
                      fontSize: 12,
                      lineHeight: 1.6,
                      whiteSpace: "pre-wrap",
                      wordBreak: "break-word",
                      border: "1px solid #e2e8f0",
                    }}
                  >
                    {toolData.result || "无返回结果"}
                  </div>
                </div>
              </div>
            ) : null}
          </div>
        ) : isThinking ? (
          <div
            style={{
              padding: "8px 12px",
              borderRadius: 8,
              background: "#f0f0f0",
              color: "#8c8c8c",
              display: "flex",
              alignItems: "center",
              gap: 8,
            }}
          >
            <LoadingOutlined style={{ fontSize: 14 }} />
            <span>{message.content}</span>
          </div>
        ) : (
          <div
            style={{
              padding: "8px 12px",
              borderRadius: 8,
              background: isHuman ? "#1677ff" : "#f0f0f0",
              color: isHuman ? "#fff" : "#000",
              wordBreak: "break-word",
              whiteSpace: "pre-wrap",
            }}
          >
            <Paragraph
              style={{ marginBottom: 0, color: "inherit" }}
              ellipsis={
                message.content.length > 1200
                  ? {
                      rows: 12,
                      expandable: "collapsible",
                    }
                  : false
              }
            >
              {message.content}
            </Paragraph>
          </div>
        )}
      </div>
    </div>
  );
}
