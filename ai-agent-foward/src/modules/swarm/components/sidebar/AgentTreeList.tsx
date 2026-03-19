import { Tree, Badge, Typography } from "antd";
import { UserOutlined, RobotOutlined } from "@ant-design/icons";
import type { SwarmAgent, AgentStatus } from "../../types/swarm";
import type { DataNode } from "antd/es/tree";

const { Text } = Typography;

interface Props {
  agents: SwarmAgent[];
  selectedAgentId: number | null;
  onSelect: (agentId: number) => void;
  unreadMap?: Record<number, number>;
}

const STATUS_COLORS: Record<AgentStatus, string> = {
  IDLE: "#52c41a",
  BUSY: "#ff4d4f",
  WAITING: "#faad14",
  WAKING: "#faad14",
  STOPPED: "#bfbfbf",
};

function StatusDot({ status }: { status: AgentStatus }) {
  const color = STATUS_COLORS[status] ?? "#bfbfbf";
  return (
    <span
      style={{
        display: "inline-block",
        width: 8,
        height: 8,
        borderRadius: "50%",
        backgroundColor: color,
        flexShrink: 0,
        animation: status === "BUSY" ? "swarm-pulse 1.2s infinite" : undefined,
      }}
    />
  );
}

function buildTree(
  agents: SwarmAgent[],
  unreadMap: Record<number, number>,
  selectedId: number | null,
): DataNode[] {
  const childrenOf = (parentId: number | null): DataNode[] => {
    return agents
      .filter((a) => (a.parentId ?? null) === parentId)
      .map((a) => {
        const isHuman = a.role === "human";
        const unread = unreadMap[a.id] ?? 0;
        const isSelected = a.id === selectedId;
        return {
          key: a.id,
          title: (
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8,
                padding: "4px 8px",
                borderRadius: 6,
                background: isSelected ? "#e6f4ff" : "transparent",
                cursor: isHuman ? "default" : "pointer",
                opacity: isHuman ? 0.7 : 1,
              }}
            >
              {isHuman ? (
                <UserOutlined style={{ color: "#1677ff" }} />
              ) : (
                <RobotOutlined style={{ color: "#722ed1" }} />
              )}
              <div style={{ flex: 1, minWidth: 0 }}>
                <Text
                  strong
                  ellipsis
                  style={{ maxWidth: 140, display: "block", fontSize: 13 }}
                >
                  {a.role}
                </Text>
                {a.description && (
                  <Text
                    type="secondary"
                    ellipsis
                    style={{
                      maxWidth: 140,
                      display: "block",
                      fontSize: 11,
                      lineHeight: 1.3,
                    }}
                  >
                    {a.description}
                  </Text>
                )}
              </div>
              {!isHuman && <StatusDot status={a.status} />}
              {unread > 0 && <Badge count={unread} size="small" />}
            </div>
          ),
          children: childrenOf(a.id),
          selectable: !isHuman,
        };
      });
  };

  const roots = agents.filter((a) => !a.parentId);
  if (roots.length === 0) return [];

  return roots.map((root) => {
    const isHuman = root.role === "human";
    return {
      key: root.id,
      title: (
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 8,
            padding: "4px 8px",
            opacity: isHuman ? 0.7 : 1,
          }}
        >
          {isHuman ? (
            <UserOutlined style={{ color: "#1677ff" }} />
          ) : (
            <RobotOutlined style={{ color: "#722ed1" }} />
          )}
          <Text strong ellipsis style={{ maxWidth: 140, fontSize: 13 }}>
            {root.role}
          </Text>
        </div>
      ),
      children: childrenOf(root.id),
      selectable: !isHuman,
    };
  });
}

export default function AgentTreeList({
  agents,
  selectedAgentId,
  onSelect,
  unreadMap = {},
}: Props) {
  const treeData = buildTree(agents, unreadMap, selectedAgentId);

  return (
    <>
      <style>{`
        @keyframes swarm-pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.3; }
        }
        .swarm-tree .ant-tree-node-content-wrapper { padding: 0 !important; }
        .swarm-tree .ant-tree-node-selected { background: transparent !important; }
      `}</style>
      <Tree
        className="swarm-tree"
        treeData={treeData}
        selectedKeys={selectedAgentId ? [selectedAgentId] : []}
        onSelect={(keys) => {
          if (keys.length > 0) onSelect(Number(keys[0]));
        }}
        defaultExpandAll
        blockNode
        style={{ background: "transparent" }}
      />
    </>
  );
}
