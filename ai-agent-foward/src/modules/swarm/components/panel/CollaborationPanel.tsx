import { useMemo, useState } from "react";
import {
  Card,
  Drawer,
  Empty,
  Progress,
  Space,
  Tag,
  Typography,
} from "antd";
import {
  CheckCircleFilled,
  ClockCircleOutlined,
  CloseCircleFilled,
  EditOutlined,
  RightOutlined,
} from "@ant-design/icons";
import type { WritingCollaborationCard } from "../../types/swarm";
import { AGENT_GRADIENTS } from "../../styles/swarm-colors";

const { Paragraph, Text } = Typography;

const STATUS_META: Record<
  string,
  { color: string; bg: string; label: string; icon: React.ReactNode }
> = {
  IDLE: {
    color: "#52c41a",
    bg: "#f6ffed",
    label: "空闲",
    icon: <ClockCircleOutlined />,
  },
  WAITING: {
    color: "#faad14",
    bg: "#fffbe6",
    label: "等待中",
    icon: <ClockCircleOutlined />,
  },
  WAKING: {
    color: "#faad14",
    bg: "#fffbe6",
    label: "唤醒中",
    icon: <ClockCircleOutlined />,
  },
  BUSY: {
    color: "#722ed1",
    bg: "#f9f0ff",
    label: "执行中",
    icon: <span className="animate-spin-slow">◌</span>,
  },
  PLANNED: {
    color: "#8c8c8c",
    bg: "#f5f5f5",
    label: "待派发",
    icon: <EditOutlined />,
  },
  ASSIGNED: {
    color: "#1677ff",
    bg: "#e6f4ff",
    label: "已分配",
    icon: <RightOutlined />,
  },
  RUNNING: {
    color: "#722ed1",
    bg: "#f9f0ff",
    label: "执行中",
    icon: <span className="animate-spin-slow">◌</span>,
  },
  DONE: {
    color: "#52c41a",
    bg: "#f6ffed",
    label: "已完成",
    icon: <CheckCircleFilled />,
  },
  FAILED: {
    color: "#ff4d4f",
    bg: "#fff2f0",
    label: "失败",
    icon: <CloseCircleFilled />,
  },
};

function getStatusMeta(status?: string) {
  return (
    STATUS_META[status ?? ""] ?? {
      color: "#8c8c8c",
      bg: "#f5f5f5",
      label: status ?? "未知",
      icon: null,
    }
  );
}

interface Props {
  cards: WritingCollaborationCard[];
}

export default function CollaborationPanel({ cards }: Props) {
  const [activeCard, setActiveCard] = useState<WritingCollaborationCard | null>(
    null,
  );

  const sortedCards = useMemo(
    () =>
      [...cards].sort((a, b) => {
        const aRunning = a.status === "RUNNING" || a.status === "BUSY" ? 0 : 1;
        const bRunning = b.status === "RUNNING" || b.status === "BUSY" ? 0 : 1;
        if (aRunning !== bRunning) return aRunning - bRunning;
        return (a.sortOrder ?? 0) - (b.sortOrder ?? 0);
      }),
    [cards],
  );

  return (
    <>
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          gap: 12,
          height: "100%",
          overflow: "hidden",
        }}
      >
        <div
          style={{ flex: 1, minHeight: 0, overflow: "auto", paddingRight: 2 }}
        >
          <Space direction="vertical" size={10} style={{ width: "100%" }}>
            {sortedCards.length === 0 ? (
              <Card
                size="small"
                style={{
                  borderRadius: 16,
                  borderColor: "#e7eaf3",
                  background: "#fafbfc",
                }}
              >
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description="主 Agent 还没有创建协作子 Agent"
                />
              </Card>
            ) : (
              sortedCards.map((card, index) => {
                const meta = getStatusMeta(card.status);
                const gradient = AGENT_GRADIENTS[index % AGENT_GRADIENTS.length];
                const isRunning = card.status === "RUNNING" || card.status === "BUSY";
                const isDone = card.status === "DONE" || card.status === "IDLE";
                const isFailed = card.status === "FAILED";
                return (
                  <Card
                    key={`${card.swarmAgentId}-${card.status}-${card.currentTask?.title ?? ""}`}
                    hoverable
                    size="small"
                    onClick={() => setActiveCard(card)}
                    style={{
                      borderRadius: 12,
                      border: "1px solid #eaecf0",
                      boxShadow: isRunning
                        ? "0 4px 16px rgba(114,46,209,0.12)"
                        : "0 2px 8px rgba(15,23,42,0.04)",
                      transition: "all 0.2s ease",
                      overflow: "hidden",
                      padding: 0,
                    }}
                    styles={{ body: { padding: 0 } }}
                  >
                    {/* Card Header — gradient accent bar */}
                    <div
                      style={{
                        height: 3,
                        background: isRunning
                          ? "linear-gradient(90deg, #722ed1, #9254de)"
                          : isDone
                            ? "linear-gradient(90deg, #52c41a, #73d13d)"
                            : isFailed
                              ? "linear-gradient(90deg, #ff4d4f, #ff7875)"
                              : "linear-gradient(90deg, #d9d9d9, #e8e8e8)",
                        borderRadius: "12px 12px 0 0",
                      }}
                    />
                    <div style={{ padding: "12px 14px" }}>
                      <Space
                        direction="vertical"
                        size={6}
                        style={{ width: "100%" }}
                      >
                        {/* Role + Status row */}
                        <div
                          style={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                            gap: 8,
                          }}
                        >
                          <Space size={8} align="center">
                            {/* Agent avatar dot */}
                            <div
                              style={{
                                width: 28,
                                height: 28,
                                borderRadius: 8,
                                background: gradient,
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                color: "#fff",
                                fontSize: 12,
                                fontWeight: 700,
                                flexShrink: 0,
                              }}
                            >
                              {card.role.charAt(0).toUpperCase()}
                            </div>
                            <div style={{ minWidth: 0 }}>
                              <Text
                                strong
                                style={{ fontSize: 14, display: "block", lineHeight: 1.3 }}
                              >
                                {card.role}
                              </Text>
                              {card.description && (
                                <Text
                                  type="secondary"
                                  style={{ fontSize: 11, display: "block" }}
                                  ellipsis
                                >
                                  {card.description}
                                </Text>
                              )}
                            </div>
                          </Space>
                          <Space size={4} align="center">
                            <Tag
                              color={meta.bg}
                              style={{
                                color: meta.color,
                                border: `1px solid ${meta.color}30`,
                                borderRadius: 20,
                                fontSize: 11,
                                padding: "0 6px",
                                margin: 0,
                              }}
                            >
                              <Space size={3} align="center">
                                <span style={{ color: meta.color, fontSize: 10 }}>
                                  {meta.icon}
                                </span>
                                {meta.label}
                              </Space>
                            </Tag>
                          </Space>
                        </div>

                        {/* Divider */}
                        <div
                          style={{
                            height: 1,
                            background: "#f0f0f0",
                            margin: "2px 0",
                          }}
                        />

                        {/* Current Task */}
                        <div>
                          <Text
                            type="secondary"
                            style={{ fontSize: 11, fontWeight: 500 }}
                          >
                            当前任务
                          </Text>
                          <Paragraph
                            style={{
                              margin: "2px 0 0",
                              fontSize: 13,
                              lineHeight: 1.5,
                              whiteSpace: "pre-wrap",
                              color:
                                card.currentTask?.title
                                  ? "#262626"
                                  : "#bfbfbf",
                            }}
                            ellipsis={{ rows: 2 }}
                          >
                            {card.currentTask?.title || "暂无任务"}
                          </Paragraph>
                        </div>

                        {/* Latest Result */}
                        <div>
                          <Text
                            type="secondary"
                            style={{ fontSize: 11, fontWeight: 500 }}
                          >
                            最新结果
                          </Text>
                          <Paragraph
                            style={{
                              margin: "2px 0 0",
                              fontSize: 12,
                              lineHeight: 1.5,
                              whiteSpace: "pre-wrap",
                              color: card.latestResult?.summary
                                ? "#595959"
                                : "#bfbfbf",
                            }}
                            ellipsis={{ rows: 2 }}
                          >
                            {card.latestResult?.summary || "暂无结果"}
                          </Paragraph>
                        </div>

                        {/* Progress indicator for RUNNING */}
                        {isRunning && (
                          <Progress
                            percent={75}
                            size="small"
                            strokeColor="#722ed1"
                            trailColor="#f0e6ff"
                            showInfo={false}
                            style={{ margin: "2px 0 0" }}
                          />
                        )}
                      </Space>
                    </div>
                  </Card>
                );
              })
            )}
          </Space>
        </div>
      </div>

      <Drawer
        title={activeCard?.role ?? "协作详情"}
        open={!!activeCard}
        width={420}
        onClose={() => setActiveCard(null)}
      >
        {activeCard && (
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <div>
              <Text type="secondary">角色状态</Text>
              <div style={{ marginTop: 6 }}>
                <Tag color={getStatusMeta(activeCard.status).bg} style={{ color: getStatusMeta(activeCard.status).color }}>
                  {getStatusMeta(activeCard.status).label}
                </Tag>
              </div>
            </div>

            {activeCard.description && (
              <div>
                <Text type="secondary">职责说明</Text>
                <Paragraph style={{ marginTop: 6, whiteSpace: "pre-wrap" }}>
                  {activeCard.description}
                </Paragraph>
              </div>
            )}

            <div>
              <Text type="secondary">当前任务</Text>
              <Paragraph style={{ marginTop: 6, whiteSpace: "pre-wrap" }}>
                {activeCard.currentTask?.title || "暂无任务"}
              </Paragraph>
              {activeCard.currentTask?.taskUuid && (
                <Paragraph
                  type="secondary"
                  style={{ marginTop: -6, whiteSpace: "pre-wrap" }}
                >
                  任务标识：{activeCard.currentTask.taskUuid}
                </Paragraph>
              )}
              {activeCard.currentTask?.instruction && (
                <Paragraph type="secondary" style={{ whiteSpace: "pre-wrap" }}>
                  {activeCard.currentTask.instruction}
                </Paragraph>
              )}
            </div>

            <div>
              <Text type="secondary">最新结果摘要</Text>
              <Paragraph style={{ marginTop: 6, whiteSpace: "pre-wrap" }}>
                {activeCard.latestResult?.summary || "暂无结果"}
              </Paragraph>
            </div>

            <div>
              <Text type="secondary">结果正文</Text>
              <Paragraph style={{ marginTop: 6, whiteSpace: "pre-wrap" }}>
                {activeCard.latestResult?.content || "暂无正文"}
              </Paragraph>
            </div>
          </Space>
        )}
      </Drawer>
    </>
  );
}
