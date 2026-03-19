import { useEffect, useMemo, useRef, useState, useCallback } from "react";
import {
  Button,
  Input,
  Avatar,
  Typography,
  Spin,
  Empty,
  Divider,
  Badge,
  Alert,
  Modal,
  Collapse,
  Tag,
  Tabs,
} from "antd";
import {
  RobotOutlined,
  UserOutlined,
  SendOutlined,
  PlusOutlined,
  StopOutlined,
  SearchOutlined,
  MessageOutlined,
  PauseCircleOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  NodeIndexOutlined,
} from "@ant-design/icons";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeHighlight from "rehype-highlight";
import {
  createChatConversation,
  fetchConversationList,
  fetchConversationMessages,
  startChatStream,
  resumeChatStream,
  stopChatExecution,
  fetchReviewDetail,
  submitResumeExecution,
  submitRejectExecution,
  type ChatConversation,
  type ChatMessage as BaseChatMessage,
  type ReviewDetailData,
} from "../api/chatService";
import {
  fetchAgentList,
  type AgentListItem,
} from "../../agent/api/agentService";
import {
  getPendingReviews,
  type PendingReview,
} from "../../../shared/api/adapters/reviewAdapter";

/** 思考步骤 */
interface ThinkingStep {
  nodeId: string;
  nodeName: string;
  nodeType: string;
  content: string;
  status: "running" | "done" | "failed";
}

/** 扩展的聊天消息，包含思考步骤 */
interface ChatMessage extends BaseChatMessage {
  thinkingSteps?: ThinkingStep[];
}

const { Text } = Typography;
const { TextArea } = Input;
const USER_ID = 1;

function makeLocalId(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function isHumanReviewPauseSummary(content?: string): boolean {
  if (!content) return false;
  const normalized = content.trim();
  return (
    normalized.startsWith("⏸️ 工作流已在节点「") &&
    normalized.includes("暂停，等待人工审核")
  );
}

function formatTime(iso: string): string {
  try {
    return new Date(iso).toLocaleString("zh-CN", { hour12: false });
  } catch {
    return iso;
  }
}

/* ---- typing dots keyframes (injected once) ---- */
const TYPING_STYLE_ID = "chat-typing-dots-style";
if (
  typeof document !== "undefined" &&
  !document.getElementById(TYPING_STYLE_ID)
) {
  const style = document.createElement("style");
  style.id = TYPING_STYLE_ID;
  style.textContent = `
@keyframes typingBounce {
  0%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-4px); }
}
.typing-dots { display: inline-flex; gap: 3px; align-items: center; padding: 4px 0; }
.typing-dots span {
  width: 6px; height: 6px; border-radius: 50%; background: #999;
  animation: typingBounce 1.2s infinite ease-in-out;
}
.typing-dots span:nth-child(2) { animation-delay: 0.15s; }
.typing-dots span:nth-child(3) { animation-delay: 0.3s; }
`;
  document.head.appendChild(style);
}

function TypingDots() {
  return (
    <div className="typing-dots">
      <span />
      <span />
      <span />
    </div>
  );
}

function SystemEventBubble({ message }: { message: ChatMessage }) {
  return (
    <div style={{ textAlign: "center", marginBottom: 16 }}>
      <Text
        type="secondary"
        style={{
          fontSize: 12,
          background: "#f5f5f5",
          padding: "4px 12px",
          borderRadius: 12,
        }}
      >
        {message.content}
      </Text>
    </div>
  );
}

/* ---- Node type icons ---- */
const NODE_ICONS: Record<string, string> = {
  LLM: "🧠",
  KNOWLEDGE: "📚",
  TOOL: "🔧",
  HTTP: "🌐",
  CONDITION: "🔀",
  START: "▶",
  END: "■",
};

/* ---- Thinking Steps Block ---- */
function ThinkingStepItem({ step }: { step: ThinkingStep }) {
  const [expanded, setExpanded] = useState(false);
  const icon = NODE_ICONS[step.nodeType] ?? "⚙️";
  const statusIcon =
    step.status === "running" ? "⏳" : step.status === "done" ? "✅" : "❌";

  return (
    <div
      style={{
        background: "#f8f9fa",
        border: "1px solid #e8e8e8",
        borderRadius: 8,
        marginBottom: 6,
        overflow: "hidden",
        transition: "all 0.2s",
      }}
    >
      <div
        onClick={() => step.content && setExpanded(!expanded)}
        style={{
          display: "flex",
          alignItems: "center",
          gap: 8,
          padding: "8px 12px",
          cursor: step.content ? "pointer" : "default",
          userSelect: "none",
        }}
      >
        <span style={{ fontSize: 14 }}>{statusIcon}</span>
        <span style={{ fontSize: 13 }}>{icon}</span>
        <span style={{ fontSize: 13, fontWeight: 500, color: "#333", flex: 1 }}>
          {step.nodeName}
        </span>
        {step.content && (
          <span
            style={{
              fontSize: 11,
              color: "#999",
              transform: expanded ? "rotate(180deg)" : "rotate(0)",
              transition: "transform 0.2s",
            }}
          >
            ▼
          </span>
        )}
      </div>
      {expanded && step.content && (
        <div
          style={{
            padding: "0 12px 10px",
            fontSize: 12,
            color: "#555",
            lineHeight: 1.6,
            borderTop: "1px solid #f0f0f0",
            maxHeight: 200,
            overflowY: "auto",
            whiteSpace: "pre-wrap",
            wordBreak: "break-word",
          }}
        >
          {step.content}
        </div>
      )}
    </div>
  );
}

function ThinkingStepsBlock({
  steps,
  isStreaming,
}: {
  steps: ThinkingStep[];
  isStreaming: boolean;
}) {
  const [collapsed, setCollapsed] = useState(false);
  const doneCount = steps.filter((s) => s.status === "done").length;
  const totalCount = steps.length;

  return (
    <div style={{ marginBottom: 8 }}>
      <div
        onClick={() => setCollapsed(!collapsed)}
        style={{
          display: "flex",
          alignItems: "center",
          gap: 6,
          padding: "6px 0",
          cursor: "pointer",
          userSelect: "none",
        }}
      >
        {isStreaming && !collapsed ? (
          <Spin size="small" />
        ) : (
          <span style={{ fontSize: 13 }}>💭</span>
        )}
        <span style={{ fontSize: 13, color: "#666", fontWeight: 500 }}>
          {isStreaming ? "思考中..." : `已完成 ${doneCount}/${totalCount} 步`}
        </span>
        <span
          style={{
            fontSize: 11,
            color: "#999",
            transform: collapsed ? "rotate(0)" : "rotate(180deg)",
            transition: "transform 0.2s",
          }}
        >
          ▼
        </span>
      </div>
      {!collapsed && (
        <div style={{ paddingLeft: 4 }}>
          {steps.map((step) => (
            <ThinkingStepItem key={step.nodeId} step={step} />
          ))}
        </div>
      )}
    </div>
  );
}

/* ---- Human Review Modal ---- */
interface HumanReviewModalProps {
  open: boolean;
  loading: boolean;
  detail: ReviewDetailData | null;
  onClose: () => void;
  onResume: (
    edits: Record<string, unknown>,
    comment: string,
    nodeEdits?: Record<string, Record<string, unknown>>,
  ) => void;
  onReject?: (reason: string) => void;
  rejectLoading?: boolean;
}

const STATUS_TAG: Record<
  string,
  { color: string; icon: React.ReactNode; label: string }
> = {
  SUCCEEDED: {
    color: "success",
    icon: <CheckCircleOutlined />,
    label: "已完成",
  },
  PENDING: { color: "default", icon: <ClockCircleOutlined />, label: "待执行" },
  PAUSED_FOR_REVIEW: {
    color: "warning",
    icon: <PauseCircleOutlined />,
    label: "待审核",
  },
};

const LLM_OUTPUT_KEYS = ["llm_output", "response", "text"] as const;

function getErrorMessage(error: unknown, fallback: string) {
  if (
    typeof error === "object" &&
    error !== null &&
    "message" in error &&
    typeof error.message === "string" &&
    error.message.trim()
  ) {
    return error.message;
  }

  return fallback;
}

function formatValue(v: unknown): string {
  if (v === null || v === undefined) return "";
  if (typeof v === "string") return v;
  return JSON.stringify(v, null, 2);
}

function normalizeNodeOutputs(
  nodeType: string,
  outputs?: Record<string, unknown> | null,
) {
  if (!outputs || nodeType !== "LLM") return outputs ?? undefined;

  const next: Record<string, unknown> = { ...outputs };
  const primaryKey =
    next.llm_output !== undefined
      ? "llm_output"
      : next.response !== undefined
        ? "response"
        : next.text !== undefined
          ? "text"
          : null;

  if (!primaryKey) return next;

  const primaryValue = next[primaryKey];
  for (const key of LLM_OUTPUT_KEYS) {
    if (key !== primaryKey && next[key] === primaryValue) {
      delete next[key];
    }
  }

  return next;
}

function hydrateLlmOutputAliases(
  nodeType: string,
  outputs: Record<string, unknown>,
) {
  if (nodeType !== "LLM") return outputs;

  const primaryValue = outputs.llm_output ?? outputs.response ?? outputs.text;
  if (primaryValue === undefined) return outputs;

  return {
    ...outputs,
    llm_output: primaryValue,
    response: primaryValue,
    text: primaryValue,
  };
}

function HumanReviewModal({
  open,
  loading,
  detail,
  onClose,
  onResume,
  onReject,
  rejectLoading,
}: HumanReviewModalProps) {
  const [editValues, setEditValues] = useState<Record<string, string>>({});
  const [upstreamEdits, setUpstreamEdits] = useState<
    Record<string, Record<string, string>>
  >({});
  const [comment, setComment] = useState("");
  const [rejectModalVisible, setRejectModalVisible] = useState(false);
  const [rejectReason, setRejectReason] = useState("");

  const isBefore = detail?.triggerPhase === "BEFORE_EXECUTION";
  const nodes = detail?.nodes ?? [];
  const currentNode = nodes.find((n) => n.nodeId === detail?.nodeId);
  const completedNodes = nodes.filter((n) => n.nodeId !== detail?.nodeId);

  const currentNodeData = isBefore
    ? (currentNode?.inputs ?? {})
    : (normalizeNodeOutputs(
        currentNode?.nodeType ?? "",
        currentNode?.outputs,
      ) ?? {});

  useEffect(() => {
    if (currentNodeData && Object.keys(currentNodeData).length > 0) {
      const initial: Record<string, string> = {};
      Object.entries(currentNodeData).forEach(([k, v]) => {
        initial[k] = formatValue(v);
      });
      setEditValues(initial);
    } else {
      setEditValues({});
    }
    const upInit: Record<string, Record<string, string>> = {};
    completedNodes.forEach((n) => {
      upInit[n.nodeId] = {};
      if (n.inputs && Object.keys(n.inputs).length > 0) {
        Object.entries(n.inputs).forEach(([k, v]) => {
          upInit[n.nodeId][k] = formatValue(v);
        });
      }
      const normalizedOutputs = normalizeNodeOutputs(n.nodeType, n.outputs);
      if (normalizedOutputs && Object.keys(normalizedOutputs).length > 0) {
        Object.entries(normalizedOutputs).forEach(([k, v]) => {
          upInit[n.nodeId][k] = formatValue(v);
        });
      }
    });
    setUpstreamEdits(upInit);
  }, [detail, isBefore, currentNode]);

  const updateUpstreamField = (
    nodeId: string,
    field: string,
    value: string,
  ) => {
    setUpstreamEdits((prev) => ({
      ...prev,
      [nodeId]: { ...(prev[nodeId] ?? {}), [field]: value },
    }));
  };

  const handleOk = () => {
    let edits: Record<string, unknown> = {};
    Object.entries(editValues).forEach(([k, raw]) => {
      try {
        edits[k] = JSON.parse(raw);
      } catch {
        edits[k] = raw;
      }
    });
    edits = hydrateLlmOutputAliases(currentNode?.nodeType ?? "", edits);
    const nodeEdits: Record<string, Record<string, unknown>> = {};
    Object.entries(upstreamEdits).forEach(([nodeId, fields]) => {
      let parsed: Record<string, unknown> = {};
      Object.entries(fields).forEach(([k, raw]) => {
        try {
          parsed[k] = JSON.parse(raw);
        } catch {
          parsed[k] = raw;
        }
      });
      const nodeType =
        completedNodes.find((n) => n.nodeId === nodeId)?.nodeType ?? "";
      parsed = hydrateLlmOutputAliases(nodeType, parsed);
      nodeEdits[nodeId] = parsed;
    });
    onResume(
      edits,
      comment,
      Object.keys(nodeEdits).length > 0 ? nodeEdits : undefined,
    );
  };

  return (
    <>
      <Modal
        title={
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <PauseCircleOutlined style={{ color: "#faad14", fontSize: 20 }} />
            <span>人工审核检查点</span>
            <Tag color="orange">{detail?.nodeName}</Tag>
          </div>
        }
        open={open}
        onCancel={onClose}
        width={720}
        destroyOnHidden
        styles={{ body: { maxHeight: "70vh", overflowY: "auto" } }}
        footer={
          <div style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}>
            <Button onClick={onClose}>取消</Button>
            {onReject && (
              <Button
                danger
                loading={rejectLoading}
                onClick={() => setRejectModalVisible(true)}
              >
                拒绝
              </Button>
            )}
            <Button type="primary" loading={loading} onClick={handleOk}>
              修改并恢复执行
            </Button>
          </div>
        }
      >
        <div style={{ marginBottom: 12 }}>
          <Tag color={isBefore ? "blue" : "green"}>
            {isBefore ? "执行前暂停（审核输入）" : "执行后暂停（审核输出）"}
          </Tag>
        </div>

        <Tabs
          defaultActiveKey="current"
          items={[
            {
              key: "current",
              label: (
                <span>
                  <PauseCircleOutlined style={{ color: "#faad14" }} /> 当前节点
                  — {detail?.nodeName}
                </span>
              ),
              children: (
                <div>
                  {currentNode && (
                    <div style={{ marginBottom: 12 }}>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        节点类型: <Tag>{currentNode.nodeType}</Tag>
                        状态: <Tag color="warning">待审核</Tag>
                      </Text>
                    </div>
                  )}
                  <Text
                    strong
                    style={{ fontSize: 13, display: "block", marginBottom: 8 }}
                  >
                    {isBefore
                      ? "📥 输入数据（可编辑）"
                      : "📤 输出数据（可编辑）"}
                  </Text>
                  {Object.keys(currentNodeData).length === 0 ? (
                    <Empty
                      description="暂无数据"
                      image={Empty.PRESENTED_IMAGE_SIMPLE}
                    />
                  ) : (
                    Object.entries(currentNodeData).map(([key, val]) => {
                      return (
                        <div key={key} style={{ marginBottom: 12 }}>
                          <div
                            style={{
                              display: "flex",
                              alignItems: "center",
                              gap: 6,
                              marginBottom: 4,
                            }}
                          >
                            <Text strong style={{ fontSize: 12 }}>
                              {key}
                            </Text>
                          </div>
                          <Input.TextArea
                            value={editValues[key] ?? formatValue(val)}
                            onChange={(e) =>
                              setEditValues((prev) => ({
                                ...prev,
                                [key]: e.target.value,
                              }))
                            }
                            autoSize={{ minRows: 2, maxRows: 10 }}
                            style={{ fontFamily: "monospace", fontSize: 12 }}
                          />
                        </div>
                      );
                    })
                  )}
                </div>
              ),
            },
            {
              key: "upstream",
              label: (
                <span>
                  <NodeIndexOutlined /> 上游节点 ({completedNodes.length})
                </span>
              ),
              children:
                completedNodes.length === 0 ? (
                  <Empty
                    description="暂无上游节点数据"
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                  />
                ) : (
                  <Collapse
                    size="small"
                    items={completedNodes.map((n) => {
                      const statusInfo =
                        STATUS_TAG[n.status] ?? STATUS_TAG.PENDING;
                      return {
                        key: n.nodeId,
                        label: (
                          <div
                            style={{
                              display: "flex",
                              alignItems: "center",
                              gap: 8,
                            }}
                          >
                            <Tag>{n.nodeType}</Tag>
                            <Text strong style={{ fontSize: 13 }}>
                              {n.nodeName}
                            </Text>
                            <Tag
                              color={statusInfo.color}
                              icon={statusInfo.icon}
                            >
                              {statusInfo.label}
                            </Tag>
                          </div>
                        ),
                        children: (
                          <div>
                            {n.inputs && Object.keys(n.inputs).length > 0 && (
                              <>
                                <Text
                                  type="secondary"
                                  style={{
                                    fontSize: 11,
                                    display: "block",
                                    marginBottom: 4,
                                  }}
                                >
                                  📥 输入（可编辑）
                                </Text>
                                {Object.entries(n.inputs).map(([k, v]) => (
                                  <div key={k} style={{ marginBottom: 8 }}>
                                    <Text
                                      style={{ fontSize: 11, color: "#8c8c8c" }}
                                    >
                                      {k}:
                                    </Text>
                                    <Input.TextArea
                                      value={
                                        upstreamEdits[n.nodeId]?.[k] ??
                                        formatValue(v)
                                      }
                                      onChange={(e) =>
                                        updateUpstreamField(
                                          n.nodeId,
                                          k,
                                          e.target.value,
                                        )
                                      }
                                      autoSize={{ minRows: 2, maxRows: 8 }}
                                      style={{
                                        fontFamily: "monospace",
                                        fontSize: 11,
                                        marginTop: 2,
                                        background: "#f5f5f5",
                                      }}
                                    />
                                  </div>
                                ))}
                              </>
                            )}
                            {n.outputs && Object.keys(n.outputs).length > 0 && (
                              <>
                                <Divider style={{ margin: "8px 0" }} />
                                <Text
                                  type="secondary"
                                  style={{
                                    fontSize: 11,
                                    display: "block",
                                    marginBottom: 4,
                                  }}
                                >
                                  📤 输出（可编辑）
                                </Text>
                                {Object.entries(n.outputs).map(([k, v]) => (
                                  <div key={k} style={{ marginBottom: 8 }}>
                                    <Text
                                      style={{ fontSize: 11, color: "#8c8c8c" }}
                                    >
                                      {k}:
                                    </Text>
                                    <Input.TextArea
                                      value={
                                        upstreamEdits[n.nodeId]?.[k] ??
                                        formatValue(v)
                                      }
                                      onChange={(e) =>
                                        updateUpstreamField(
                                          n.nodeId,
                                          k,
                                          e.target.value,
                                        )
                                      }
                                      autoSize={{ minRows: 2, maxRows: 8 }}
                                      style={{
                                        fontFamily: "monospace",
                                        fontSize: 11,
                                        marginTop: 2,
                                      }}
                                    />
                                  </div>
                                ))}
                              </>
                            )}
                            {(!n.inputs ||
                              Object.keys(n.inputs).length === 0) &&
                              (!n.outputs ||
                                Object.keys(n.outputs).length === 0) && (
                                <Text type="secondary" style={{ fontSize: 12 }}>
                                  暂无数据
                                </Text>
                              )}
                          </div>
                        ),
                      };
                    })}
                  />
                ),
            },
          ]}
        />

        <Divider style={{ margin: "12px 0" }} />
        <div>
          <Text style={{ fontSize: 12 }}>💬 审核意见（可选）</Text>
          <Input.TextArea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="输入审核意见..."
            autoSize={{ minRows: 1, maxRows: 3 }}
            style={{ marginTop: 4 }}
          />
        </div>
      </Modal>

      {/* 拒绝确认弹窗 */}
      <Modal
        title="拒绝审核"
        open={rejectModalVisible}
        onOk={() => {
          onReject?.(rejectReason.trim() || "无原因");
          setRejectModalVisible(false);
          setRejectReason("");
        }}
        onCancel={() => setRejectModalVisible(false)}
        okText="确认拒绝"
        cancelText="取消"
        okButtonProps={{ danger: true, loading: rejectLoading }}
      >
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          <Input.TextArea
            rows={4}
            placeholder="请输入拒绝原因（可选）"
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
          />
        </div>
      </Modal>
    </>
  );
}

function ChatPage() {
  const [agents, setAgents] = useState<AgentListItem[]>([]);
  const [selectedAgentId, setSelectedAgentId] = useState<number | null>(null);
  const [conversations, setConversations] = useState<ChatConversation[]>([]);
  const [activeConversationId, setActiveConversationId] = useState<string>("");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [listError, setListError] = useState("");
  const [streamError, setStreamError] = useState("");
  const [isSending, setIsSending] = useState(false);
  const [loadingAgents, setLoadingAgents] = useState(true);
  const [agentSearch, setAgentSearch] = useState("");

  const abortControllerRef = useRef<AbortController | null>(null);
  const executionIdRef = useRef("");
  const isStoppedRef = useRef(false);
  const hasStreamErrorRef = useRef(false);
  const pendingStopRef = useRef(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const skipMessageLoadRef = useRef(false);
  const selectedAgentIdRef = useRef<number | null>(selectedAgentId);
  selectedAgentIdRef.current = selectedAgentId;

  // Human review state
  const [reviewModalOpen, setReviewModalOpen] = useState(false);
  const [reviewDetail, setReviewDetail] = useState<ReviewDetailData | null>(
    null,
  );
  const [reviewLoading, setReviewLoading] = useState(false);
  const [rejectLoading, setRejectLoading] = useState(false);
  const pausedExecutionRef = useRef<{
    executionId: string;
    nodeId: string;
    assistantMessageId?: string;
  } | null>(null);
  const [pendingReviewsForAgent, setPendingReviewsForAgent] = useState<
    PendingReview[]
  >([]);

  // Load agent list
  useEffect(() => {
    setLoadingAgents(true);
    void fetchAgentList()
      .then((data) => {
        const published = data.filter(
          (a) => a.status === "PUBLISHED" || a.status === "1",
        );
        setAgents(published.length > 0 ? published : data);
      })
      .catch(() => setAgents([]))
      .finally(() => setLoadingAgents(false));
  }, []);

  // Load conversations when agent selected
  useEffect(() => {
    if (!selectedAgentId) {
      setConversations([]);
      setActiveConversationId("");
      setMessages([]);
      setPendingReviewsForAgent([]);
      return;
    }
    void fetchConversationList(USER_ID, selectedAgentId)
      .then((data) => {
        setConversations(data);
        setListError("");
      })
      .catch(() => {
        setConversations([]);
        setListError("会话列表加载失败");
      });
    // Also load pending reviews for this agent
    void getPendingReviews()
      .then((reviews) => {
        const agentReviews = reviews.filter(
          (r) => r.agentName === `Agent-${selectedAgentId}`,
        );
        setPendingReviewsForAgent(agentReviews);
      })
      .catch(() => setPendingReviewsForAgent([]));
  }, [selectedAgentId]);

  // Load messages when conversation selected
  useEffect(() => {
    if (!activeConversationId) {
      setMessages([]);
      return;
    }
    if (skipMessageLoadRef.current) {
      skipMessageLoadRef.current = false;
      return;
    }
    void fetchConversationMessages(USER_ID, activeConversationId)
      .then((data) => {
        setMessages(data);
        setStreamError("");
      })
      .catch(() => {
        setMessages([]);
        setStreamError("消息加载失败");
      });
  }, [activeConversationId]);

  // Auto-scroll
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // Cleanup
  useEffect(() => {
    return () => {
      abortControllerRef.current?.abort();
    };
  }, []);

  const canSend = useMemo(
    () => input.trim().length > 0 && !isSending && !!selectedAgentId,
    [input, isSending, selectedAgentId],
  );

  const selectedAgent = useMemo(
    () => agents.find((a) => a.id === selectedAgentId),
    [agents, selectedAgentId],
  );

  const filteredAgents = useMemo(() => {
    if (!agentSearch.trim()) return agents;
    const kw = agentSearch.trim().toLowerCase();
    return agents.filter(
      (a) =>
        a.name.toLowerCase().includes(kw) ||
        a.description?.toLowerCase().includes(kw),
    );
  }, [agents, agentSearch]);

  const visibleMessages = useMemo(
    () =>
      messages.filter(
        (message) =>
          !(
            message.role === "ASSISTANT" &&
            isHumanReviewPauseSummary(message.content)
          ),
      ),
    [messages],
  );

  const appendDelta = (id: string, delta: string) => {
    setMessages((cur) =>
      cur.map((m) =>
        m.id === id
          ? { ...m, content: m.content + delta, status: "STREAMING" }
          : m,
      ),
    );
  };

  const appendThought = (
    id: string,
    delta: string,
    nodeId: string,
    nodeName: string,
    nodeType: string,
  ) => {
    setMessages((cur) =>
      cur.map((m) => {
        if (m.id !== id) return m;
        const steps = [...(m.thinkingSteps ?? [])];
        const existing = steps.find((s) => s.nodeId === nodeId);
        if (existing) {
          existing.content += delta;
        } else {
          steps.push({
            nodeId,
            nodeName,
            nodeType,
            content: delta,
            status: "running",
          });
        }
        return { ...m, thinkingSteps: steps };
      }),
    );
  };

  const updateNodeStatus = (
    id: string,
    nodeId: string,
    nodeName: string,
    nodeType: string,
    status: "running" | "done" | "failed",
  ) => {
    setMessages((cur) =>
      cur.map((m) => {
        if (m.id !== id) return m;
        const steps = [...(m.thinkingSteps ?? [])];
        const existing = steps.find((s) => s.nodeId === nodeId);
        if (existing) {
          existing.status = status;
        } else if (status === "running") {
          steps.push({
            nodeId,
            nodeName,
            nodeType,
            content: "",
            status: "running",
          });
        }
        return { ...m, thinkingSteps: steps };
      }),
    );
  };

  const activeConversationIdRef = useRef(activeConversationId);
  activeConversationIdRef.current = activeConversationId;

  const refreshPendingReviews = useCallback(async () => {
    const agentId = selectedAgentIdRef.current;
    if (!agentId) {
      setPendingReviewsForAgent([]);
      return;
    }

    try {
      const reviews = await getPendingReviews();
      setPendingReviewsForAgent(
        reviews.filter((r) => r.agentName === `Agent-${agentId}`),
      );
    } catch {
      setPendingReviewsForAgent([]);
    }
  }, []);

  const finishMsg = (id: string, status: "COMPLETED" | "FAILED") => {
    setMessages((cur) => {
      const target = cur.find((m) => m.id === id);
      // 如果 assistant 消息没有流式内容，延迟从后端拉取最终消息
      if (
        target &&
        target.role === "ASSISTANT" &&
        (!target.content || target.content === "...") &&
        status === "COMPLETED" &&
        activeConversationIdRef.current
      ) {
        setTimeout(() => {
          const cid = activeConversationIdRef.current;
          if (!cid) return;
          void fetchConversationMessages(USER_ID, cid)
            .then((data) => {
              setMessages(data);
              setStreamError("");
            })
            .catch(() => {});
        }, 500);
      }
      return cur.map((m) => (m.id === id ? { ...m, status } : m));
    });
  };

  const resetRuntime = (ctrl?: AbortController) => {
    if (ctrl && abortControllerRef.current !== ctrl) return;
    abortControllerRef.current = null;
    executionIdRef.current = "";
    pendingStopRef.current = false;
    isStoppedRef.current = false;
    hasStreamErrorRef.current = false;
  };

  const submitStop = async () => {
    const eid = executionIdRef.current;
    if (!eid) return;
    try {
      await stopChatExecution(eid);
    } catch {
      setStreamError("中断请求提交失败");
    }
  };

  const handleCreateConversation = async () => {
    if (!selectedAgentId) return;
    setListError("");
    try {
      const id = await createChatConversation(USER_ID, selectedAgentId);
      const list = await fetchConversationList(USER_ID, selectedAgentId);
      setConversations(list);
      setActiveConversationId(id);
    } catch {
      setListError("创建会话失败");
    }
  };

  const handleSend = async () => {
    const content = input.trim();
    if (!content || isSending || !selectedAgentId) return;
    setStreamError("");
    setIsSending(true);
    setInput("");

    let convId = activeConversationId;
    if (!convId) {
      try {
        convId = await createChatConversation(USER_ID, selectedAgentId);
        const list = await fetchConversationList(USER_ID, selectedAgentId);
        setConversations(list);
        skipMessageLoadRef.current = true;
        setActiveConversationId(convId);
      } catch {
        setStreamError("创建会话失败");
        setIsSending(false);
        return;
      }
    }

    const userMsg: ChatMessage = {
      id: makeLocalId("u"),
      role: "USER",
      content,
      status: "COMPLETED",
      createdAt: new Date().toISOString(),
    };
    const aId = makeLocalId("a");
    const assistantMsg: ChatMessage = {
      id: aId,
      role: "ASSISTANT",
      content: "",
      status: "STREAMING",
      createdAt: new Date().toISOString(),
    };
    setMessages((cur) => [...cur, userMsg, assistantMsg]);

    const ctrl = new AbortController();
    abortControllerRef.current = ctrl;
    executionIdRef.current = "";
    pendingStopRef.current = false;
    isStoppedRef.current = false;
    hasStreamErrorRef.current = false;

    try {
      await startChatStream(
        {
          userId: USER_ID,
          agentId: selectedAgentId,
          conversationId: convId,
          content,
        },
        {
          onConnected: (id) => {
            executionIdRef.current = id;
            if (pendingStopRef.current) void submitStop();
          },
          onDelta: (d) => {
            if (!isStoppedRef.current && !hasStreamErrorRef.current)
              appendDelta(aId, d);
          },
          onThought: (d, nid, name, ntype) => {
            if (!isStoppedRef.current && !hasStreamErrorRef.current)
              appendThought(aId, d, nid, name, ntype);
          },
          onNodeStart: (nid, name, ntype) => {
            if (!isStoppedRef.current && !hasStreamErrorRef.current)
              updateNodeStatus(aId, nid, name, ntype, "running");
          },
          onNodeFinish: (nid, name, ntype, st) => {
            if (!isStoppedRef.current && !hasStreamErrorRef.current)
              updateNodeStatus(
                aId,
                nid,
                name,
                ntype,
                st === "FAILED" ? "failed" : "done",
              );
          },
          onFinish: () => {
            if (!isStoppedRef.current && !hasStreamErrorRef.current)
              finishMsg(aId, "COMPLETED");
          },
          onError: (msg) => {
            hasStreamErrorRef.current = true;
            finishMsg(aId, "FAILED");
            setStreamError(msg);
          },
          onPaused: (eid, nid) => {
            void handlePaused(eid, nid, aId);
          },
        },
        ctrl.signal,
      );
      if (!isStoppedRef.current && !hasStreamErrorRef.current)
        finishMsg(aId, "COMPLETED");
    } catch {
      if (!isStoppedRef.current) {
        finishMsg(aId, "FAILED");
        setStreamError("流式消息发送失败");
      }
    } finally {
      setIsSending(false);
      resetRuntime(ctrl);
    }
  };

  const handleStop = async () => {
    if (!isSending) return;
    isStoppedRef.current = true;
    pendingStopRef.current = true;
    abortControllerRef.current?.abort();
    await submitStop();
    setMessages((cur) =>
      cur.map((m) =>
        m.status === "STREAMING" ? { ...m, status: "FAILED" } : m,
      ),
    );
    setStreamError((c) => c || "已手动中断");
    setIsSending(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      void handleSend();
    }
  };

  const handlePaused = useCallback(
    async (
      executionId: string,
      nodeId: string,
      assistantMessageId?: string,
    ) => {
      pausedExecutionRef.current = {
        executionId,
        nodeId,
        assistantMessageId,
      };
      setMessages((cur) =>
        cur.map(
          (m): ChatMessage =>
            m.status === "STREAMING" ? { ...m, status: "COMPLETED" } : m,
        ),
      );
      setIsSending(false);

      try {
        const detail = await fetchReviewDetail(executionId);
        setReviewDetail(detail);
        setReviewModalOpen(true);
        await refreshPendingReviews();
      } catch {
        setStreamError("获取审核详情失败");
      }
    },
    [refreshPendingReviews],
  );

  const startResumedStream = useCallback(
    (executionId: string, assistantMessageId?: string) => {
      const aId = assistantMessageId?.trim() || makeLocalId("a");
      let reusedExistingMessage = false;
      setMessages((cur) => {
        if (assistantMessageId) {
          const next = cur.map((m) => {
            if (m.id === assistantMessageId && m.role === "ASSISTANT") {
              reusedExistingMessage = true;
              return { ...m, status: "STREAMING" as const };
            }
            return m;
          });
          if (reusedExistingMessage) {
            return next;
          }
        }

        const assistantMsg: ChatMessage = {
          id: aId,
          role: "ASSISTANT",
          content: "",
          status: "STREAMING",
          createdAt: new Date().toISOString(),
        };
        return [...cur, assistantMsg];
      });

      const ctrl = new AbortController();
      abortControllerRef.current = ctrl;
      executionIdRef.current = executionId;
      pendingStopRef.current = false;
      isStoppedRef.current = false;
      hasStreamErrorRef.current = false;
      setIsSending(true);

      void resumeChatStream(
        executionId,
        {
          onConnected: (id) => {
            executionIdRef.current = id;
            if (pendingStopRef.current) void submitStop();
          },
          onDelta: (d) => {
            if (!isStoppedRef.current && !hasStreamErrorRef.current)
              appendDelta(aId, d);
          },
          onThought: (d, nid, name, ntype) => {
            if (!isStoppedRef.current && !hasStreamErrorRef.current)
              appendThought(aId, d, nid, name, ntype);
          },
          onNodeStart: (nid, name, ntype) => {
            if (!isStoppedRef.current && !hasStreamErrorRef.current)
              updateNodeStatus(aId, nid, name, ntype, "running");
          },
          onNodeFinish: (nid, name, ntype, st) => {
            if (!isStoppedRef.current && !hasStreamErrorRef.current)
              updateNodeStatus(
                aId,
                nid,
                name,
                ntype,
                st === "FAILED" ? "failed" : "done",
              );
          },
          onFinish: () => {
            if (!isStoppedRef.current && !hasStreamErrorRef.current)
              finishMsg(aId, "COMPLETED");
          },
          onError: (msg) => {
            hasStreamErrorRef.current = true;
            finishMsg(aId, "FAILED");
            setStreamError(msg);
          },
          onPaused: (eid, nid) => {
            void handlePaused(eid, nid, aId);
          },
        },
        ctrl.signal,
      )
        .then(() => {
          if (!isStoppedRef.current && !hasStreamErrorRef.current)
            finishMsg(aId, "COMPLETED");
        })
        .catch(() => {
          if (!isStoppedRef.current) {
            finishMsg(aId, "FAILED");
            setStreamError("恢复后的流式消息接收失败");
          }
        })
        .finally(() => {
          setIsSending(false);
          resetRuntime(ctrl);
          void refreshPendingReviews();
        });
    },
    [handlePaused, refreshPendingReviews],
  );

  const handleResumeExecution = useCallback(
    async (
      edits: Record<string, unknown>,
      comment: string,
      nodeEdits?: Record<string, Record<string, unknown>>,
    ) => {
      const paused = pausedExecutionRef.current;
      if (!paused) return;

      setReviewLoading(true);
      try {
        await submitResumeExecution({
          executionId: paused.executionId,
          nodeId: paused.nodeId,
          expectedVersion: reviewDetail?.executionVersion,
          edits,
          comment,
          nodeEdits,
        });
        setReviewModalOpen(false);
        setReviewDetail(null);
        pausedExecutionRef.current = null;

        // Remove from pending list
        setPendingReviewsForAgent((prev) =>
          prev.filter((r) => r.executionId !== paused.executionId),
        );
        startResumedStream(paused.executionId, paused.assistantMessageId);
      } catch (error) {
        setStreamError(getErrorMessage(error, "恢复执行失败"));
      } finally {
        setReviewLoading(false);
      }
    },
    [reviewDetail, startResumedStream],
  );

  const handleRejectExecution = useCallback(async (reason: string) => {
    const paused = pausedExecutionRef.current;
    if (!paused) return;

    setRejectLoading(true);
    try {
      await submitRejectExecution({
        executionId: paused.executionId,
        nodeId: paused.nodeId,
        reason,
      });
      setReviewModalOpen(false);
      setReviewDetail(null);
      pausedExecutionRef.current = null;

      setPendingReviewsForAgent((prev) =>
        prev.filter((r) => r.executionId !== paused.executionId),
      );
      setStreamError("当前审核节点已被拒绝，后续执行不会继续。");
    } catch (error) {
      setStreamError(getErrorMessage(error, "拒绝执行失败"));
    } finally {
      setRejectLoading(false);
    }
  }, []);

  /* ---- RENDER ---- */
  return (
    <div
      style={{
        display: "flex",
        height: "calc(100vh - 64px - 48px)",
        overflow: "hidden",
      }}
    >
      {/* ===== Left Panel ===== */}
      <div
        style={{
          width: 280,
          background: "#fff",
          borderRight: "1px solid #f0f0f0",
          display: "flex",
          flexDirection: "column",
          flexShrink: 0,
        }}
      >
        {/* Agent search */}
        <div style={{ padding: "16px 16px 8px" }}>
          <Input.Search
            placeholder="搜索 Agent..."
            prefix={<SearchOutlined style={{ color: "#bfbfbf" }} />}
            allowClear
            value={agentSearch}
            onChange={(e) => setAgentSearch(e.target.value)}
            style={{ marginBottom: 8 }}
          />
        </div>

        {/* Agent list */}
        <div
          style={{
            flex: "0 0 auto",
            maxHeight: 220,
            overflowY: "auto",
            padding: "0 8px",
          }}
        >
          {loadingAgents ? (
            <div style={{ textAlign: "center", padding: 24 }}>
              <Spin />
            </div>
          ) : filteredAgents.length === 0 ? (
            <Empty
              description="暂无可用 Agent"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              style={{ margin: "16px 0" }}
            />
          ) : (
            filteredAgents.map((agent) => {
              const isSelected = agent.id === selectedAgentId;
              return (
                <div
                  key={agent.id}
                  onClick={() => setSelectedAgentId(agent.id)}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 10,
                    padding: "10px 12px",
                    marginBottom: 4,
                    borderRadius: 8,
                    cursor: "pointer",
                    borderLeft: isSelected
                      ? "3px solid #1677ff"
                      : "3px solid transparent",
                    background: isSelected ? "#e6f4ff" : "transparent",
                    transition: "all 0.2s",
                  }}
                >
                  <Avatar
                    size={36}
                    icon={<RobotOutlined />}
                    style={{
                      background: isSelected ? "#1677ff" : "#d9d9d9",
                      flexShrink: 0,
                    }}
                  />
                  <div style={{ overflow: "hidden", flex: 1 }}>
                    <Text
                      strong
                      ellipsis
                      style={{ fontSize: 14, display: "block" }}
                    >
                      {agent.name}
                    </Text>
                    {agent.description && (
                      <Text type="secondary" ellipsis style={{ fontSize: 12 }}>
                        {agent.description}
                      </Text>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>

        <Divider style={{ margin: "8px 0" }} />

        {/* Conversation list */}
        <div style={{ flex: 1, overflowY: "auto", padding: "0 8px" }}>
          {listError && (
            <Alert
              type="error"
              title={listError}
              showIcon
              style={{ margin: "0 8px 8px" }}
            />
          )}
          {selectedAgentId && conversations.length === 0 && !listError && (
            <Empty
              description="暂无对话"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              style={{ marginTop: 24 }}
            />
          )}
          {conversations.map((c) => {
            const isActive = activeConversationId === c.id;
            return (
              <div
                key={c.id}
                onClick={() => setActiveConversationId(c.id)}
                style={{
                  padding: "10px 12px",
                  marginBottom: 4,
                  borderRadius: 8,
                  cursor: "pointer",
                  background: isActive ? "#e6f4ff" : "transparent",
                  transition: "background 0.2s",
                }}
              >
                <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                  <MessageOutlined
                    style={{
                      color: isActive ? "#1677ff" : "#bfbfbf",
                      fontSize: 14,
                    }}
                  />
                  <Text
                    ellipsis
                    style={{
                      fontSize: 13,
                      flex: 1,
                      color: isActive ? "#1677ff" : undefined,
                    }}
                  >
                    {c.title || c.id.slice(0, 8)}
                  </Text>
                </div>
                <Text type="secondary" style={{ fontSize: 11, marginLeft: 20 }}>
                  {formatTime(c.updatedAt)}
                </Text>
              </div>
            );
          })}
        </div>

        {/* New conversation button */}
        <div style={{ padding: 12 }}>
          <Button
            type="dashed"
            icon={<PlusOutlined />}
            block
            disabled={!selectedAgentId}
            onClick={() => void handleCreateConversation()}
          >
            新建对话
          </Button>
        </div>
      </div>

      {/* ===== Right Panel ===== */}
      <div
        style={{
          flex: 1,
          display: "flex",
          flexDirection: "column",
          background: "#fafafa",
          minWidth: 0,
        }}
      >
        {!selectedAgentId ? (
          /* Empty state */
          <div
            style={{
              flex: 1,
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            <RobotOutlined style={{ fontSize: 72, color: "#d9d9d9" }} />
            <Text type="secondary" style={{ fontSize: 16, marginTop: 16 }}>
              选择一个 Agent 开始对话
            </Text>
          </div>
        ) : (
          <>
            {/* Top bar */}
            <div
              style={{
                height: 56,
                padding: "0 20px",
                display: "flex",
                alignItems: "center",
                gap: 10,
                background: "#fff",
                borderBottom: "1px solid #f0f0f0",
                flexShrink: 0,
              }}
            >
              <Avatar
                size={32}
                icon={<RobotOutlined />}
                style={{ background: "#1677ff" }}
              />
              <Text strong style={{ fontSize: 15 }}>
                {selectedAgent?.name ?? "Agent"}
              </Text>
              <Badge
                status="processing"
                text={
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    在线
                  </Text>
                }
              />
            </div>

            {/* Message area */}
            <div style={{ flex: 1, overflowY: "auto", padding: "16px 20px" }}>
              {/* Pending review banner */}
              {pendingReviewsForAgent.length > 0 && !reviewModalOpen && (
                <Alert
                  type="warning"
                  showIcon
                  icon={<PauseCircleOutlined />}
                  title={
                    <span>
                      有 {pendingReviewsForAgent.length} 个待审核项
                      {pendingReviewsForAgent.map((r) => (
                        <Button
                          key={`${r.executionId}-${r.nodeId}`}
                          type="link"
                          size="small"
                          onClick={async () => {
                            pausedExecutionRef.current = {
                              executionId: r.executionId,
                              nodeId: r.nodeId,
                            };
                            try {
                              const detail = await fetchReviewDetail(
                                r.executionId,
                              );
                              setReviewDetail(detail);
                              setReviewModalOpen(true);
                            } catch {
                              setStreamError("获取审核详情失败");
                            }
                          }}
                        >
                          查看 {r.nodeName}
                        </Button>
                      ))}
                    </span>
                  }
                  style={{ marginBottom: 12 }}
                />
              )}
              {visibleMessages.length === 0 && (
                <div style={{ textAlign: "center", paddingTop: 80 }}>
                  <Empty description="发送消息开始对话" />
                </div>
              )}
              {visibleMessages.map((m) => {
                const isUser = m.role === "USER";
                const isSystem = m.role === "SYSTEM";

                if (isSystem) {
                  return <SystemEventBubble key={m.id} message={m} />;
                }
                return (
                  <div
                    key={m.id}
                    style={{
                      display: "flex",
                      justifyContent: isUser ? "flex-end" : "flex-start",
                      marginBottom: 16,
                      gap: 8,
                    }}
                  >
                    {!isUser && (
                      <Avatar
                        size={32}
                        icon={<RobotOutlined />}
                        style={{
                          background: "#1677ff",
                          flexShrink: 0,
                          marginTop: 2,
                        }}
                      />
                    )}
                    <div style={{ maxWidth: "70%" }}>
                      {/* 思考步骤折叠区域 */}
                      {!isUser &&
                        m.thinkingSteps &&
                        m.thinkingSteps.length > 0 && (
                          <ThinkingStepsBlock
                            steps={m.thinkingSteps}
                            isStreaming={m.status === "STREAMING"}
                          />
                        )}
                      <div
                        style={{
                          padding: "10px 14px",
                          borderRadius: isUser
                            ? "12px 12px 2px 12px"
                            : "12px 12px 12px 2px",
                          background: isUser ? "#1677ff" : "#f5f5f5",
                          color: isUser ? "#fff" : "#333",
                          wordBreak: "break-word",
                        }}
                      >
                        {isUser ? (
                          <div
                            style={{
                              whiteSpace: "pre-wrap",
                              fontSize: 14,
                              lineHeight: 1.6,
                            }}
                          >
                            {m.content || "..."}
                          </div>
                        ) : (
                          <div
                            className="markdown-body"
                            style={{ fontSize: 14, lineHeight: 1.6 }}
                          >
                            {m.content ? (
                              <ReactMarkdown
                                remarkPlugins={[remarkGfm]}
                                rehypePlugins={[rehypeHighlight]}
                              >
                                {m.content}
                              </ReactMarkdown>
                            ) : m.status === "STREAMING" ? (
                              <TypingDots />
                            ) : (
                              "..."
                            )}
                            {m.status === "STREAMING" && m.content && (
                              <TypingDots />
                            )}
                          </div>
                        )}
                      </div>
                      <div
                        style={{
                          fontSize: 11,
                          marginTop: 4,
                          color: "#999",
                          textAlign: isUser ? "right" : "left",
                        }}
                      >
                        {formatTime(m.createdAt)}
                      </div>
                    </div>
                    {isUser && (
                      <Avatar
                        size={32}
                        icon={<UserOutlined />}
                        style={{
                          background: "#87d068",
                          flexShrink: 0,
                          marginTop: 2,
                        }}
                      />
                    )}
                  </div>
                );
              })}
              <div ref={messagesEndRef} />
            </div>

            {/* Input area */}
            <div
              style={{
                padding: "12px 20px 16px",
                background: "#fff",
                borderTop: "1px solid #f0f0f0",
                flexShrink: 0,
              }}
            >
              {streamError && (
                <Alert
                  type="error"
                  title={streamError}
                  showIcon
                  closable
                  onClose={() => setStreamError("")}
                  style={{ marginBottom: 8 }}
                />
              )}
              <div style={{ display: "flex", gap: 8, alignItems: "flex-end" }}>
                <TextArea
                  value={input}
                  placeholder="输入消息..."
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  disabled={isSending}
                  autoSize={{ minRows: 1, maxRows: 4 }}
                  style={{ flex: 1, borderRadius: 8 }}
                />
                {isSending ? (
                  <Button
                    danger
                    icon={<StopOutlined />}
                    onClick={() => void handleStop()}
                  >
                    停止
                  </Button>
                ) : (
                  <Button
                    type="primary"
                    icon={<SendOutlined />}
                    disabled={!canSend}
                    onClick={() => void handleSend()}
                  >
                    发送
                  </Button>
                )}
              </div>
              <Text
                type="secondary"
                style={{ fontSize: 11, marginTop: 4, display: "block" }}
              >
                Enter 发送, Shift+Enter 换行
              </Text>
            </div>
          </>
        )}
      </div>

      {/* ===== Human Review Modal ===== */}
      <HumanReviewModal
        open={reviewModalOpen}
        loading={reviewLoading}
        detail={reviewDetail}
        onClose={() => setReviewModalOpen(false)}
        onResume={handleResumeExecution}
        onReject={handleRejectExecution}
        rejectLoading={rejectLoading}
      />
    </div>
  );
}

export default ChatPage;
