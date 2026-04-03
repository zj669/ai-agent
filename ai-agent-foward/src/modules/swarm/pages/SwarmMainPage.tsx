import { useState, useEffect, useCallback, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeftOutlined } from "@ant-design/icons";
import { Button, Grid, Layout, Spin, Typography, Divider } from "antd";
import SwarmChatPanel from "../components/chat/SwarmChatPanel";
import CollaborationPanel from "../components/panel/CollaborationPanel";
import { useSwarmAgents } from "../hooks/useSwarmAgents";
import { useSwarmMessages } from "../hooks/useSwarmMessages";
import { useUIStream, type TaskNotificationPayload } from "../hooks/useUIStream";
import {
  getWorkspaceDefaults,
  getWritingSessionOverview,
  listWritingSessions,
  stopAgent,
} from "../api/swarmService";
import type {
  LiveToolCallStep,
  SwarmMessage,
  WorkerCardData,
  WritingSessionOverview,
  WritingSessionSummary,
} from "../types/swarm";
import SwarmNotification, {
  type SwarmNotificationData,
  type NotificationType,
} from "../components/SwarmNotification";
import WorkerCard from "../components/WorkerCard";

const { Content } = Layout;
const { Title, Text } = Typography;

export default function SwarmMainPage() {
  const navigate = useNavigate();
  const screens = Grid.useBreakpoint();
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const wid = workspaceId ? Number(workspaceId) : null;

  const { agents, reload: reloadAgents } = useSwarmAgents(wid);
  const [userId, setUserId] = useState<number | null>(null);
  const [assistantAgentId, setAssistantAgentId] = useState<number | null>(null);
  const [defaultGroupId, setDefaultGroupId] = useState<number | null>(null);

  const {
    messages,
    load: loadMessages,
    send,
  } = useSwarmMessages(defaultGroupId, userId ?? undefined);
  const [streamingContent, setStreamingContent] = useState<string | null>(null);
  const [streamingAgentId, setStreamingAgentId] = useState<number | null>(null);
  const [waitingForAgents, setWaitingForAgents] = useState<number[]>([]);
  const [liveToolCalls, setLiveToolCalls] = useState<LiveToolCallStep[]>([]);
  const [writingSessions, setWritingSessions] = useState<
    WritingSessionSummary[]
  >([]);
  const [activeSessionId, setActiveSessionId] = useState<number | null>(null);
  const [sessionOverview, setSessionOverview] =
    useState<WritingSessionOverview | null>(null);
  const [overviewLoading, setOverviewLoading] = useState(false);
  const [overviewInitialized, setOverviewInitialized] = useState(false);
  const [overviewError, setOverviewError] = useState<string | null>(null);
  const overviewRequestIdRef = useRef(0);
  const liveToolCleanupTimerRef = useRef<number | null>(null);
  const overviewRefreshTimerRef = useRef<number | null>(null);
  const streamingContentRef = useRef<string | null>(null);
  const waitingForAgentsRef = useRef<number[]>([]);
  const liveToolCallsRef = useRef<LiveToolCallStep[]>([]);
  const [notifications, setNotifications] = useState<SwarmNotificationData[]>([]);
  const [workerCards, setWorkerCards] = useState<WorkerCardData[]>([]);
  const workerCardsRef = useRef<WorkerCardData[]>([]);
  const notificationIdRef = useRef(0);
  const coordinatorAgentIdRef = useRef<number | null>(null);
  const sendInProgressRef = useRef(false);

  useEffect(() => {
    streamingContentRef.current = streamingContent;
  }, [streamingContent]);

  useEffect(() => {
    waitingForAgentsRef.current = waitingForAgents;
  }, [waitingForAgents]);

  useEffect(() => {
    liveToolCallsRef.current = liveToolCalls;
  }, [liveToolCalls]);

  const clearLiveToolCleanupTimer = useCallback(() => {
    if (liveToolCleanupTimerRef.current !== null) {
      window.clearTimeout(liveToolCleanupTimerRef.current);
      liveToolCleanupTimerRef.current = null;
    }
  }, []);

  const settleStreamingIfIdle = useCallback(() => {
    const hasRunningToolCalls = liveToolCallsRef.current.some(
      (step) => step.status === "running",
    );
    const hasWaitingAgents = waitingForAgentsRef.current.length > 0;
    if (hasRunningToolCalls || hasWaitingAgents) {
      return;
    }
    if (streamingContentRef.current === "") {
      streamingContentRef.current = null;
      setStreamingContent(null);
      setStreamingAgentId(null);
    }
  }, []);

  const scheduleLiveToolCleanup = useCallback(() => {
    clearLiveToolCleanupTimer();
    liveToolCleanupTimerRef.current = window.setTimeout(() => {
      setLiveToolCalls((current) => {
        const next = current.filter((step) => step.status !== "done");
        liveToolCallsRef.current = next;
        return next;
      });
      settleStreamingIfIdle();
      liveToolCleanupTimerRef.current = null;
    }, 4000);
  }, [clearLiveToolCleanupTimer, settleStreamingIfIdle]);

  useEffect(
    () => () => {
      clearLiveToolCleanupTimer();
      if (overviewRefreshTimerRef.current !== null) {
        window.clearTimeout(overviewRefreshTimerRef.current);
        overviewRefreshTimerRef.current = null;
      }
    },
    [clearLiveToolCleanupTimer],
  );

  useEffect(() => {
    if (!wid) return;
    setOverviewInitialized(false);
    setOverviewLoading(false);
    setSessionOverview(null);
    setWritingSessions([]);
    setActiveSessionId(null);
    setOverviewError(null);
    streamingContentRef.current = null;
    waitingForAgentsRef.current = [];
    liveToolCallsRef.current = [];
    setNotifications([]);
    setWorkerCards([]);
    workerCardsRef.current = [];
    notificationIdRef.current = 0;
    getWorkspaceDefaults(wid).then((defaults) => {
      setUserId(defaults.userId);
      setAssistantAgentId(defaults.assistantAgentId);
      setDefaultGroupId(defaults.defaultGroupId);
      coordinatorAgentIdRef.current = defaults.assistantAgentId;
    });
  }, [wid]);

  const reloadWritingOverview = useCallback(async () => {
    if (!wid) return;
    const requestId = ++overviewRequestIdRef.current;
    const shouldShowBlockingLoading =
      !overviewInitialized ||
      (sessionOverview == null && activeSessionId != null);
    if (shouldShowBlockingLoading) {
      setOverviewLoading(true);
    }
    if (!overviewInitialized) {
      setOverviewError(null);
    }
    try {
      const sessions = await listWritingSessions(wid);
      if (requestId !== overviewRequestIdRef.current) return;
      setWritingSessions(sessions);
      const nextSessionId =
        sessions.find((session) => session.id === activeSessionId)?.id ??
        sessions[0]?.id ??
        null;
      setActiveSessionId(nextSessionId);
      if (nextSessionId) {
        const overview = await getWritingSessionOverview(nextSessionId);
        if (requestId !== overviewRequestIdRef.current) return;
        setSessionOverview(overview);
      } else if (requestId === overviewRequestIdRef.current) {
        setSessionOverview(null);
      }
    } catch (error) {
      if (requestId !== overviewRequestIdRef.current) return;
      console.error("[SwarmMainPage] Failed to reload writing overview", error);
      setOverviewError("协作面板加载失败，正在等待下一次刷新");
    } finally {
      if (requestId === overviewRequestIdRef.current) {
        setOverviewLoading(false);
        setOverviewInitialized(true);
      }
    }
  }, [wid, activeSessionId, overviewInitialized, sessionOverview]);

  const scheduleOverviewRefresh = useCallback(
    (delay = 180) => {
      if (overviewRefreshTimerRef.current !== null) {
        window.clearTimeout(overviewRefreshTimerRef.current);
      }
      overviewRefreshTimerRef.current = window.setTimeout(() => {
        overviewRefreshTimerRef.current = null;
        reloadWritingOverview();
      }, delay);
    },
    [reloadWritingOverview],
  );

  useEffect(() => {
    loadMessages();
  }, [loadMessages]);

  useEffect(() => {
    reloadWritingOverview();
  }, [reloadWritingOverview]);

  useEffect(() => {
    if (!wid) return;
    const shouldPoll =
      streamingContent !== null ||
      waitingForAgents.length > 0 ||
      (sessionOverview?.session.status != null &&
        !["COMPLETED", "FAILED"].includes(sessionOverview.session.status));

    if (!shouldPoll) return;

    const timer = window.setInterval(() => {
      reloadAgents();
      reloadWritingOverview();
    }, 3000);

    return () => window.clearInterval(timer);
  }, [
    wid,
    streamingContent,
    waitingForAgents.length,
    sessionOverview?.session.status,
    reloadAgents,
    reloadWritingOverview,
  ]);

  useUIStream(wid, {
    onAgentCreated: () => {
      reloadAgents();
      scheduleOverviewRefresh();
    },
    onMessageCreated: (data) => {
      try {
        const parsed = JSON.parse(data) as { senderId?: number };
        if (parsed.senderId != null) {
          setWaitingForAgents((current) =>
            current.filter((agentId) => agentId !== parsed.senderId),
          );
        }
      } catch {
        // ignore malformed payloads
      }
      // Skip SSE-triggered reload while a user send is in flight — the HTTP
      // response will replace the optimistic message and this is handled in
      // useSwarmMessages. Without this guard, loadMessages() races with the
      // send() success handler and wipes the optimistic entry before it can
      // be confirmed, causing the sent message to flash and disappear.
      if (!sendInProgressRef.current) {
        loadMessages();
      }
      reloadAgents();
      scheduleOverviewRefresh();
    },
    onStreamStart: (data) => {
      try {
        const parsed = JSON.parse(data);
        if (parsed.groupId === defaultGroupId) {
          clearLiveToolCleanupTimer();
          streamingContentRef.current = "";
          setStreamingContent("");
          setStreamingAgentId(parsed.agentId);
        }
      } catch {
        /* ignore */
      }
    },
    onStreamChunk: (data) => {
      try {
        const parsed = JSON.parse(data);
        if (parsed.groupId === defaultGroupId) {
          setStreamingContent((prev) => {
            const next = (prev ?? "") + parsed.chunk;
            streamingContentRef.current = next;
            return next;
          });
        }
      } catch {
        /* ignore */
      }
    },
    onStreamDone: (data) => {
      try {
        const parsed = JSON.parse(data);
        if (parsed.groupId === defaultGroupId) {
          streamingContentRef.current = null;
          setStreamingContent(null);
          setStreamingAgentId(null);
          scheduleLiveToolCleanup();
          loadMessages();
          reloadAgents();
          scheduleOverviewRefresh();
        }
      } catch {
        /* ignore */
      }
    },
    onWaiting: (data) => {
      try {
        const parsed = JSON.parse(data) as {
          groupId?: number;
          targetAgentId?: number;
        };
        if (parsed.groupId === defaultGroupId) {
          clearLiveToolCleanupTimer();
          if (parsed.targetAgentId != null) {
            setWaitingForAgents((current) => {
              const next = current.includes(parsed.targetAgentId as number)
                ? current
                : [...current, parsed.targetAgentId as number];
              waitingForAgentsRef.current = next;
              return next;
            });
          }
          scheduleOverviewRefresh();
        }
      } catch {
        /* ignore */
      }
    },
    onWaitingDone: (data) => {
      try {
        const parsed = JSON.parse(data) as {
          groupId?: number;
          targetAgentId?: number;
        };
        if (parsed.groupId === defaultGroupId) {
          setWaitingForAgents((current) => {
            const next =
              parsed.targetAgentId == null
                ? []
                : current.filter((agentId) => agentId !== parsed.targetAgentId);
            waitingForAgentsRef.current = next;
            return next;
          });
          scheduleLiveToolCleanup();
          reloadAgents();
          scheduleOverviewRefresh();
        }
      } catch {
        /* ignore */
      }
    },
    onToolCallStart: (data) => {
      try {
        const parsed = JSON.parse(data) as Partial<LiveToolCallStep>;
        if (
          parsed.groupId !== defaultGroupId ||
          !parsed.toolCallId ||
          !parsed.tool ||
          parsed.agentId == null ||
          parsed.groupId == null
        ) {
          return;
        }
        const toolCallId = parsed.toolCallId;
        const agentId = parsed.agentId;
        const groupId = parsed.groupId;
        const tool = parsed.tool;
        clearLiveToolCleanupTimer();
        setLiveToolCalls((current) => {
          const nextStep: LiveToolCallStep = {
            toolCallId,
            agentId,
            groupId,
            tool,
            argsPreview: parsed.argsPreview,
            resultPreview: parsed.resultPreview,
            status: "running",
            startedAt: Date.now(),
          };
          const existingIndex = current.findIndex(
            (step) => step.toolCallId === toolCallId,
          );
          if (existingIndex < 0) {
            const next = [...current, nextStep];
            liveToolCallsRef.current = next;
            return next;
          }
          const next = [...current];
          next[existingIndex] = {
            ...next[existingIndex],
            ...nextStep,
            startedAt: next[existingIndex].startedAt ?? nextStep.startedAt,
          };
          liveToolCallsRef.current = next;
          return next;
        });
        setStreamingContent((prev) => {
          const next = prev ?? "";
          streamingContentRef.current = next;
          return next;
        });
        setStreamingAgentId((prev) => prev ?? agentId);
      } catch {
        /* ignore */
      }
    },
    onToolCallDone: (data) => {
      try {
        const parsed = JSON.parse(data) as Partial<LiveToolCallStep>;
        if (
          parsed.groupId !== defaultGroupId ||
          !parsed.toolCallId ||
          !parsed.tool ||
          parsed.agentId == null ||
          parsed.groupId == null
        ) {
          return;
        }
        const toolCallId = parsed.toolCallId;
        const agentId = parsed.agentId;
        const groupId = parsed.groupId;
        const tool = parsed.tool;
        setLiveToolCalls((current) => {
          const existingIndex = current.findIndex(
            (step) => step.toolCallId === toolCallId,
          );
          const completedAt = Date.now();
          if (existingIndex < 0) {
            const completedStep: LiveToolCallStep = {
              toolCallId,
              agentId,
              groupId,
              tool,
              argsPreview: parsed.argsPreview,
              resultPreview: parsed.resultPreview,
              status: "done",
              startedAt: completedAt,
              finishedAt: completedAt,
            };
            const next = [...current, completedStep];
            liveToolCallsRef.current = next;
            return next;
          }
          const next = [...current];
          next[existingIndex] = {
            ...next[existingIndex],
            argsPreview: parsed.argsPreview ?? next[existingIndex].argsPreview,
            resultPreview:
              parsed.resultPreview ?? next[existingIndex].resultPreview,
            status: "done",
            finishedAt: completedAt,
          };
          liveToolCallsRef.current = next;
          return next;
        });
        scheduleLiveToolCleanup();
        scheduleOverviewRefresh(120);
      } catch {
        /* ignore */
      }
    },
    onTaskNotification: (payload: TaskNotificationPayload) => {
      console.log("[SwarmMainPage] Task notification:", payload);
      const agentRole =
        agents.find((a) => a.id === payload.agentId)?.role ||
        `agent_${payload.agentId}`;
      const notificationType: NotificationType =
        payload.status === "completed"
          ? "completed"
          : payload.status === "failed"
            ? "failed"
            : payload.status === "killed"
              ? "killed"
              : payload.status === "coordinator_turn_complete"
                ? "completed"
                : "completed";

      // Add notification toast
      const notificationId = String(notificationIdRef.current++);
      setNotifications((prev) => [
        ...prev,
        {
          id: notificationId,
          agentId: payload.agentId,
          agentRole,
          type: notificationType,
          timestamp: payload.timestamp ?? Date.now(),
        },
      ]);

      // Update WorkerCard
      setWorkerCards((current) => {
        const existingIndex = current.findIndex(
          (c) => c.agentId === payload.agentId,
        );
        const update: WorkerCardData = {
          agentId: payload.agentId,
          agentRole,
          status:
            payload.status === "completed" || payload.status === "coordinator_turn_complete"
              ? "completed"
              : payload.status === "failed"
                ? "failed"
                : payload.status === "killed"
                  ? "failed"
                  : "idle",
          startTime: undefined,
          tokenCount: payload.totalTokens,
          latestMessage: payload.summary,
          phase: payload.phase,
        };

        if (existingIndex >= 0) {
          const next = [...current];
          next[existingIndex] = update;
          workerCardsRef.current = next;
          return next;
        } else {
          const next = [...current, update];
          workerCardsRef.current = next;
          return next;
        }
      });

      reloadAgents();
      scheduleOverviewRefresh();
    },
  });

  const handleSend = useCallback(
    async (content: string) => {
      if (!userId || !assistantAgentId) return;
      clearLiveToolCleanupTimer();
      liveToolCallsRef.current = [];
      setLiveToolCalls([]);
      waitingForAgentsRef.current = [];
      setWaitingForAgents([]);
      streamingContentRef.current = "";
      setStreamingContent("");
      setStreamingAgentId(assistantAgentId);
      sendInProgressRef.current = true;
      await send(userId, content);
      sendInProgressRef.current = false;
    },
    [userId, assistantAgentId, clearLiveToolCleanupTimer, send],
  );

  const handleStop = useCallback(async () => {
    if (!assistantAgentId) return;
    try {
      await stopAgent(assistantAgentId);
    } catch {
      // ignore stop failures in UI
    }
    clearLiveToolCleanupTimer();
    liveToolCallsRef.current = [];
    setLiveToolCalls([]);
    streamingContentRef.current = null;
    setStreamingContent(null);
    setStreamingAgentId(null);
    waitingForAgentsRef.current = [];
    setWaitingForAgents([]);
    loadMessages();
    reloadAgents();
    reloadWritingOverview();
  }, [
    assistantAgentId,
    clearLiveToolCleanupTimer,
    loadMessages,
    reloadAgents,
    reloadWritingOverview,
  ]);

  const isStreaming = streamingContent !== null;
  const selectedAgent =
    agents.find((a) => a.id === assistantAgentId) ??
    agents.find((a) => a.role === "assistant");
  const displayedMessages = ((): SwarmMessage[] => {
    if (sessionOverview) {
      // Show confirmed root messages, but ALWAYS append any still-pending optimistic
      // messages so they remain visible even if sessionOverview.rootConversation
      // refreshes before the backend includes the confirmed entry.
      const confirmedIds = new Set(
        sessionOverview.rootConversation.map((m) => m.id),
      );
      const pendingOptimistic = messages.filter(
        (m) => m.id < 0 && !confirmedIds.has(m.id),
      );
      return [
        ...(sessionOverview.rootConversation as SwarmMessage[]),
        ...pendingOptimistic,
      ];
    }
    return messages;
  })();
  const latestLiveToolCall = liveToolCalls.length
    ? liveToolCalls[liveToolCalls.length - 1]
    : null;
  const processingState =
    streamingContent !== null
      ? null
      : liveToolCalls.some((step) => step.status === "running")
        ? null
        : waitingForAgents.length > 0 && assistantAgentId
          ? {
              agentId: assistantAgentId,
              title: "正在等待协作结果",
              detail: `已派发 ${waitingForAgents.length} 个协作 Agent，主 Agent 正在等待并继续调度。`,
            }
          : selectedAgent?.status === "BUSY"
            ? {
                agentId: selectedAgent.id,
                title: "正在继续思考",
                detail: latestLiveToolCall
                  ? `已完成「${latestLiveToolCall.tool}」，正在继续整理上下文与生成回复。`
                  : "主 Agent 仍在运行，正在生成下一段内容。",
              }
            : selectedAgent?.status === "WAITING"
              ? {
                  agentId: selectedAgent.id,
                  title: "正在处理中",
                  detail: latestLiveToolCall
                    ? `刚完成「${latestLiveToolCall.tool}」，正在等待下一步结果并继续推进。`
                    : "主 Agent 仍在处理中，界面会在收到下一段输出后继续刷新。",
                }
              : null;
  const isMobile = !screens.lg;

  if (!wid) return <Spin />;

  return (
    <Layout style={{ height: "calc(100vh - 112px)", background: "#f6f8fb" }}>
      <Content
        style={{
          display: "flex",
          flexDirection: "column",
          height: "100%",
          padding: 16,
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 12,
            padding: "0 0 12px",
          }}
        >
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate("/swarm")}
            style={{ color: "#667085" }}
          >
            返回工作区列表
          </Button>
        </div>
        <div
          style={{
            flex: 1,
            minHeight: 0,
            display: "grid",
            gridTemplateColumns: isMobile
              ? "1fr"
              : "minmax(0, 1fr) minmax(280px, 0.6fr)",
            gap: 12,
          }}
        >
          <div
            style={{
              minHeight: 0,
              background: "#fff",
              borderRadius: 16,
              border: "1px solid #eaecf0",
              overflow: "hidden",
              boxShadow: "0 4px 16px rgba(15, 23, 42, 0.04)",
            }}
          >
            <SwarmChatPanel
              messages={displayedMessages}
              agents={agents}
              userId={userId ?? undefined}
              onSend={handleSend}
              onStop={handleStop}
              selectedGroupId={defaultGroupId}
              selectedAgent={selectedAgent}
              streamingContent={streamingContent}
              streamingAgentId={streamingAgentId}
              liveToolCalls={liveToolCalls}
              agentBusy={selectedAgent?.status === "BUSY"}
              isStreaming={isStreaming}
              waitingForAgents={waitingForAgents}
              processingState={processingState}
              onStopWaitingAgent={(agentId) => {
                setWaitingForAgents((current) =>
                  current.filter((item) => item !== agentId),
                );
              }}
            />
          </div>

          <div
            style={{
              minHeight: 0,
              background: "#fff",
              borderRadius: 16,
              border: "1px solid #eaecf0",
              overflow: "hidden",
              boxShadow: "0 4px 16px rgba(15, 23, 42, 0.04)",
              display: "flex",
              flexDirection: "column",
            }}
          >
            <div style={{ display: "flex", flexDirection: "column", gap: 4, padding: "14px 14px 0" }}>
              <Title level={4} style={{ margin: 0 }}>
                协作面板
              </Title>
              <Text type="secondary">
                {sessionOverview?.session.title ||
                  writingSessions[0]?.title ||
                  "主 Agent 会在这里展示子 Agent 的任务和结果摘要"}
              </Text>
              {overviewError ? (
                <Text type="danger" style={{ fontSize: 12 }}>
                  {overviewError}
                </Text>
              ) : null}
            </div>
            <div style={{ flex: 1, minHeight: 0, padding: "8px 14px 14px" }}>
              {overviewLoading && !overviewInitialized ? (
                <div
                  style={{
                    display: "flex",
                    height: "100%",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <Spin />
                </div>
              ) : writingSessions.length > 0 &&
                sessionOverview == null &&
                !overviewError ? (
                <div
                  style={{
                    display: "flex",
                    height: "100%",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <Spin tip="正在加载协作卡片..." />
                </div>
              ) : (
                <CollaborationPanel
                  cards={sessionOverview?.collaborationCards ?? []}
                />
              )}
            </div>

            {/* WorkerCards — 显示 Coordinator 派发的 Worker 状态 */}
            {workerCards.length > 0 && (
              <>
                <Divider style={{ margin: "0 14px" }} />
                <div style={{ display: "flex", flexDirection: "column", gap: 8, padding: "0 14px 14px" }}>
                  <Text strong style={{ fontSize: 13, color: "#595959" }}>
                    Worker 状态
                  </Text>
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      gap: 8,
                      maxHeight: 200,
                      overflow: "auto",
                    }}
                  >
                    {workerCards.map((worker) => (
                      <WorkerCard
                        key={worker.agentId}
                        worker={worker}
                        onStop={(agentId) => {
                          stopAgent(agentId).catch(() => {});
                          setWorkerCards((current) =>
                            current.map((c) =>
                              c.agentId === agentId
                                ? { ...c, status: "failed" as const }
                                : c,
                            ),
                          );
                        }}
                      />
                    ))}
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
      </Content>

      {/* Swarm 通知 Toast — 右下角 */}
      <SwarmNotification
        notifications={notifications}
        onDismiss={(id) =>
          setNotifications((current) => current.filter((n) => n.id !== id))
        }
      />
    </Layout>
  );
}
