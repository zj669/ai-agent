import { useState } from "react";
import { Button, Space } from "antd";
import {
  LoadingOutlined,
  CheckCircleOutlined,
  StopOutlined,
} from "@ant-design/icons";
import { stopAgent } from "../../api/swarmService";
import type { SwarmAgent } from "../../types/swarm";
import { AGENT_GRADIENTS, SWARM_COLORS } from "../../styles/swarm-colors";

interface Props {
  targetAgentId: number;
  agents: SwarmAgent[];
  done?: boolean;
  onStopped?: (agentId: number) => void;
}

export default function WaitingCard({
  targetAgentId,
  agents,
  done,
  onStopped,
}: Props) {
  const [stopping, setStopping] = useState(false);
  const targetAgent = agents.find((a) => a.id === targetAgentId);
  const colorIndex = agents.findIndex((a) => a.id === targetAgentId);
  const gradient = AGENT_GRADIENTS[colorIndex % AGENT_GRADIENTS.length];
  const roleName = targetAgent?.role ?? `agent_${targetAgentId}`;

  const handleStop = async () => {
    setStopping(true);
    try {
      await stopAgent(targetAgentId);
      onStopped?.(targetAgentId);
    } finally {
      setStopping(false);
    }
  };

  return (
    <div
      className="swarm-msg-enter"
      style={{
        display: "flex",
        alignItems: "center",
        gap: 10,
        padding: "10px 14px",
        margin: "4px 0 12px",
        background: done ? SWARM_COLORS.waitingDoneBg : SWARM_COLORS.waitingBg,
        border: `1px solid ${done ? SWARM_COLORS.waitingDoneBorder : SWARM_COLORS.waitingBorder}`,
        borderRadius: 12,
        borderLeft: done
          ? "3px solid #52c41a"
          : `3px solid ${SWARM_COLORS.waitingAccent}`,
      }}
    >
      <div
        style={{
          width: 28,
          height: 28,
          borderRadius: "50%",
          background: done ? "#52c41a" : gradient,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          color: "#fff",
          fontSize: 11,
          fontWeight: 600,
          flexShrink: 0,
        }}
      >
        {roleName.charAt(0).toUpperCase()}
      </div>
      <Space style={{ flex: 1 }}>
        {done ? (
          <CheckCircleOutlined style={{ color: "#52c41a" }} />
        ) : (
          <LoadingOutlined style={{ color: "#1677ff" }} />
        )}
        <span
          style={{
            fontSize: 13,
            color: done ? "#52c41a" : "#1a1a2e",
            fontWeight: done ? 500 : 400,
          }}
        >
          {done ? `✅ ${roleName} 已回复` : `⏳ 正在等待 ${roleName} 回复...`}
        </span>
      </Space>
      {!done && (
        <Button
          size="small"
          danger
          icon={<StopOutlined />}
          loading={stopping}
          onClick={handleStop}
        >
          终止
        </Button>
      )}
    </div>
  );
}
