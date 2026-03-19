import { useMemo, useState } from "react";
import { Card, Drawer, Empty, Space, Tag, Typography } from "antd";
import type {
  WritingCollaborationCard,
  WritingDraftSummary,
} from "../../types/swarm";

const { Paragraph, Text, Title } = Typography;

const STATUS_META: Record<string, { color: string; label: string }> = {
  IDLE: { color: "default", label: "空闲" },
  ASSIGNED: { color: "processing", label: "已分配" },
  RUNNING: { color: "blue", label: "执行中" },
  DONE: { color: "success", label: "已完成" },
  FAILED: { color: "error", label: "失败" },
};

interface Props {
  cards: WritingCollaborationCard[];
  latestDraft?: WritingDraftSummary | null;
}

function getStatusMeta(status?: string) {
  return (
    STATUS_META[status ?? ""] ?? { color: "default", label: status ?? "未知" }
  );
}

export default function CollaborationPanel({ cards, latestDraft }: Props) {
  const [activeCard, setActiveCard] = useState<WritingCollaborationCard | null>(
    null,
  );
  const [draftVisible, setDraftVisible] = useState(false);

  const sortedCards = useMemo(
    () =>
      [...cards].sort((a, b) => {
        const aRunning = a.status === "RUNNING" ? 0 : 1;
        const bRunning = b.status === "RUNNING" ? 0 : 1;
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
        <Card
          hoverable={!!latestDraft}
          onClick={() => {
            if (latestDraft) {
              setDraftVisible(true);
            }
          }}
          size="small"
          style={{
            borderRadius: 16,
            borderColor: "#e7eaf3",
            boxShadow: "0 8px 22px rgba(15, 23, 42, 0.05)",
          }}
        >
          <Space direction="vertical" size={4} style={{ width: "100%" }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              当前草稿
            </Text>
            {latestDraft ? (
              <>
                <Space align="center" size={8}>
                  <Title level={5} style={{ margin: 0 }}>
                    {latestDraft.title || `草稿 V${latestDraft.versionNo}`}
                  </Title>
                  <Tag
                    color={latestDraft.status === "FINAL" ? "success" : "gold"}
                  >
                    {latestDraft.status === "FINAL"
                      ? "最终稿"
                      : `V${latestDraft.versionNo}`}
                  </Tag>
                </Space>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  点击查看完整草稿
                </Text>
                <Paragraph
                  style={{ marginBottom: 0, whiteSpace: "pre-wrap" }}
                  ellipsis={{ rows: 4, expandable: false }}
                >
                  {latestDraft.content || "主 Agent 还没有生成草稿。"}
                </Paragraph>
              </>
            ) : (
              <Text type="secondary">还没有生成草稿</Text>
            )}
          </Space>
        </Card>

        <div
          style={{ flex: 1, minHeight: 0, overflow: "auto", paddingRight: 4 }}
        >
          <Space direction="vertical" size={12} style={{ width: "100%" }}>
            {sortedCards.length === 0 ? (
              <Card
                size="small"
                style={{ borderRadius: 16, borderColor: "#e7eaf3" }}
              >
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description="主 Agent 还没有创建协作子 Agent"
                />
              </Card>
            ) : (
              sortedCards.map((card) => {
                const meta = getStatusMeta(card.status);
                return (
                  <Card
                    key={card.writingAgentId}
                    hoverable
                    size="small"
                    onClick={() => setActiveCard(card)}
                    style={{
                      borderRadius: 16,
                      borderColor: "#e7eaf3",
                      boxShadow: "0 8px 20px rgba(15, 23, 42, 0.05)",
                    }}
                  >
                    <Space
                      direction="vertical"
                      size={8}
                      style={{ width: "100%" }}
                    >
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          gap: 12,
                        }}
                      >
                        <div style={{ minWidth: 0 }}>
                          <Title level={5} style={{ margin: 0, fontSize: 16 }}>
                            {card.role}
                          </Title>
                          {card.description && (
                            <Text type="secondary" style={{ fontSize: 12 }}>
                              {card.description}
                            </Text>
                          )}
                        </div>
                        <Tag color={meta.color}>{meta.label}</Tag>
                      </div>
                      <div>
                        <Text strong style={{ fontSize: 12 }}>
                          当前任务
                        </Text>
                        <Paragraph
                          style={{ margin: "4px 0 0", whiteSpace: "pre-wrap" }}
                          ellipsis={{ rows: 2 }}
                        >
                          {card.currentTask?.title || "暂无任务"}
                        </Paragraph>
                      </div>
                      <div>
                        <Text strong style={{ fontSize: 12 }}>
                          最新结果
                        </Text>
                        <Paragraph
                          style={{ margin: "4px 0 0", whiteSpace: "pre-wrap" }}
                          ellipsis={{ rows: 2 }}
                        >
                          {card.latestResult?.summary || "暂无结果"}
                        </Paragraph>
                      </div>
                    </Space>
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
                <Tag color={getStatusMeta(activeCard.status).color}>
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

      <Drawer
        title={latestDraft?.title || "当前草稿"}
        open={draftVisible}
        width={720}
        onClose={() => setDraftVisible(false)}
      >
        {latestDraft ? (
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <Space align="center" size={8}>
              <Tag color={latestDraft.status === "FINAL" ? "success" : "gold"}>
                {latestDraft.status === "FINAL"
                  ? "最终稿"
                  : `V${latestDraft.versionNo}`}
              </Tag>
              <Text type="secondary">版本号：{latestDraft.versionNo}</Text>
            </Space>
            <Paragraph style={{ marginBottom: 0, whiteSpace: "pre-wrap" }}>
              {latestDraft.content || "当前草稿暂无内容"}
            </Paragraph>
          </Space>
        ) : (
          <Text type="secondary">还没有生成草稿</Text>
        )}
      </Drawer>
    </>
  );
}
