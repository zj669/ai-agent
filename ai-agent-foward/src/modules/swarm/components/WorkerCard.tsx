import { useState } from "react";
import { Badge, Button, Space, Typography } from "antd";
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExpandOutlined,
  LoadingOutlined,
  PauseCircleOutlined,
} from "@ant-design/icons";
import type { WorkerCardData, WorkerStatus } from "../../types/swarm";

const { Text } = Typography;

interface Props {
  worker: WorkerCardData;
  onStop?: (agentId: number) => void;
}

const STATUS_META: Record<
  WorkerStatus,
  { color: string; icon: React.ReactNode; label: string }
> = {
  running: {
    color: "#1677ff",
    icon: <LoadingOutlined spin />,
    label: "运行中",
  },
  idle: {
    color: "#52c41a",
    icon: <PauseCircleOutlined />,
    label: "空闲",
  },
  completed: {
    color: "#52c41a",
    icon: <CheckCircleOutlined />,
    label: "已完成",
  },
  failed: {
    color: "#ff4d4f",
    icon: <CloseCircleOutlined />,
    label: "失败",
  },
};

function formatDuration(ms?: number): string {
  if (ms == null) return "-";
  const seconds = Math.floor(ms / 1000);
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  if (minutes < 60) return `${minutes}m ${remainingSeconds}s`;
  const hours = Math.floor(minutes / 60);
  return `${hours}h ${minutes % 60}m`;
}

export default function WorkerCard({ worker, onStop }: Props) {
  const [expanded, setExpanded] = useState(false);
  const meta = STATUS_META[worker.status];
  const elapsed =
    worker.startTime != null ? Date.now() - worker.startTime : undefined;

  return (
    <>
      <style>{`
        .swarm-worker-card {
          border: 1px solid #e7eaf3;
          border-radius: 10px;
          background: #fff;
          overflow: hidden;
          transition: box-shadow 0.2s;
        }
        .swarm-worker-card:hover {
          box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
        }
        .swarm-worker-card.expanded {
          box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
        }
        .swarm-worker-card-header {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 8px 12px;
          cursor: pointer;
          user-select: none;
        }
        .swarm-worker-card-body {
          padding: 8px 12px;
          border-top: 1px solid #f0f0f0;
          background: #fafafa;
          max-height: 120px;
          overflow: hidden;
          transition: max-height 0.2s ease;
        }
        .swarm-worker-card-body.expanded {
          max-height: 300px;
          overflow: auto;
        }
        .swarm-worker-card-body pre {
          margin: 0;
          white-space: pre-wrap;
          word-break: break-all;
          font-size: 12px;
          font-family: monospace;
          color: #595959;
        }
      `}</style>
      <div className={`swarm-worker-card ${expanded ? "expanded" : ""}`}>
        <div
          className="swarm-worker-card-header"
          onClick={() => setExpanded((e) => !e)}
        >
          <Badge color={meta.color} />
          <Text strong style={{ fontSize: 13, flex: 1 }}>
            {worker.agentRole}
          </Text>
          <Space size={4}>
            {worker.phase && (
              <Text type="secondary" style={{ fontSize: 11 }}>
                {worker.phase}
              </Text>
            )}
            <span style={{ color: meta.color, fontSize: 12 }}>{meta.icon}</span>
            <Text type="secondary" style={{ fontSize: 11 }}>
              {meta.label}
            </Text>
          </Space>
          <Button
            type="text"
            size="small"
            icon={<ExpandOutlined rotate={expanded ? 180 : 0} />}
            onClick={(e) => {
              e.stopPropagation();
              setExpanded((e) => !e);
            }}
            style={{ padding: "0 4px", height: "auto" }}
          />
        </div>

        <div style={{ padding: "4px 12px 8px", display: "flex", gap: 12 }}>
          <Text type="secondary" style={{ fontSize: 11 }}>
            耗时: {formatDuration(elapsed)}
          </Text>
          {worker.tokenCount != null && worker.tokenCount > 0 && (
            <Text type="secondary" style={{ fontSize: 11 }}>
              Token: {worker.tokenCount}
            </Text>
          )}
          {worker.status === "running" && onStop && (
            <Button
              type="text"
              size="small"
              danger
              onClick={(e) => {
                e.stopPropagation();
                onStop(worker.agentId);
              }}
              style={{ fontSize: 11, padding: "0 4px", height: "auto" }}
            >
              停止
            </Button>
          )}
        </div>

        {worker.latestMessage && (
          <div
            className={`swarm-worker-card-body ${expanded ? "expanded" : ""}`}
          >
            <pre>{worker.latestMessage}</pre>
          </div>
        )}
      </div>
    </>
  );
}
