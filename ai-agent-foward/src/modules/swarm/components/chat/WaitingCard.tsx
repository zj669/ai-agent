import { useState } from "react";
import { Button, Space } from "antd";
import {
  LoadingOutlined,
  CheckCircleOutlined,
  StopOutlined,
} from "@ant-design/icons";
import { stopAgent } from "../../api/swarmService";
import type { SwarmAgent } from "../../types/swarm";

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
      style={{
        padding: "8px 12px",
        margin: "8px 0",
        background: done ? "#f6ffed" : "#fffbe6",
        border: `1px solid ${done ? "#b7eb8f" : "#ffe58f"}`,
        borderRadius: 8,
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
      }}
    >
      <Space>
        {done ? (
          <CheckCircleOutlined style={{ color: "#52c41a" }} />
        ) : (
          <LoadingOutlined style={{ color: "#faad14" }} />
        )}
        <span style={{ fontSize: 13 }}>
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
