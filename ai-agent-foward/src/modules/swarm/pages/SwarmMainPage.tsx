import { useState, useEffect, useCallback, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeftOutlined } from "@ant-design/icons";
import { Button, Grid, Layout, Spin, Typography } from "antd";
import SwarmChatPanel from "../components/chat/SwarmChatPanel";
import CollaborationPanel from "../components/panel/CollaborationPanel";
import { useSwarmAgents } from "../hooks/useSwarmAgents";
import { useSwarmMessages } from "../hooks/useSwarmMessages";
import { useUIStream } from "../hooks/useUIStream";
import {
  getWorkspaceDefaults,
  getWritingSessionOverview,
  listWritingSessions,
  stopAgent,
} from "../api/swarmService";
import type {
  LiveToolCallStep,
  SwarmMessage,
  WritingSessionOverview,
  WritingSessionSummary,
} from "../types/swarm";

const { Content } = Layout;
const { Title, Text } = Typography;

export default function SwarmMainPage() {
  const navigate = useNavigate();
  const screens = Grid.useBreakpoint();
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const wid = workspaceId ? Number(workspaceId) : null;

  const { agents, reload: reloadAgents } = useSwarmAgents(wid);
  const [humanAgentId, setHumanAgentId] = useState<number | null>(null);
  const [assistantAgentId, setAssistantAgentId] = useState<number | null>(null);
  const [defaultGroupId, setDefaultGroupId] = useState<number | null>(null);

  const {
    messages,
    load: loadMessages,
    send,
  } = useSwarmMessages(defaultGroupId, humanAgentId ?? undefined);
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
    getWorkspaceDefaults(wid).then((defaults) => {
      setHumanAgentId(defaults.humanAgentId);
      setAssistantAgentId(defaults.assistantAgentId);
      setDefaultGroupId(defaults.defaultGroupId);
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
      loadMessages();
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
  });

  const handleSend = useCallback(
    async (content: string) => {
      if (!humanAgentId || !assistantAgentId) return;
      clearLiveToolCleanupTimer();
      liveToolCallsRef.current = [];
      setLiveToolCalls([]);
      waitingForAgentsRef.current = [];
      setWaitingForAgents([]);
      streamingContentRef.current = "";
      setStreamingContent("");
      setStreamingAgentId(assistantAgentId);
      await send(humanAgentId, content);
    },
    [humanAgentId, assistantAgentId, clearLiveToolCleanupTimer, send],
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
  const rootConversationMessages: SwarmMessage[] = sessionOverview
    ? sessionOverview.rootConversation.map((message) => ({
        id: message.id,
        groupId: defaultGroupId ?? 0,
        senderId: message.senderId,
        content: message.content,
        contentType: message.contentType,
        sendTime: message.sendTime,
      }))
    : messages;
  const optimisticMessages = messages.filter((message) => message.id < 0);
  const displayedMessages = sessionOverview
    ? [...rootConversationMessages, ...optimisticMessages]
    : messages;
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
              : "minmax(0, 1.6fr) minmax(320px, 0.9fr)",
            gap: 16,
          }}
        >
          <div
            style={{
              minHeight: 0,
              background: "#fff",
              borderRadius: 16,
              border: "1px solid #eaecf0",
              overflow: "hidden",
              boxShadow: "0 8px 24px rgba(15, 23, 42, 0.04)",
            }}
          >
            <SwarmChatPanel
              messages={displayedMessages}
              agents={agents}
              humanAgentId={humanAgentId ?? undefined}
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
              background: "linear-gradient(180deg, #fffdf8 0%, #f8fafc 100%)",
              borderRadius: 16,
              border: "1px solid #eaecf0",
              overflow: "hidden",
              boxShadow: "0 8px 24px rgba(15, 23, 42, 0.04)",
              padding: 16,
              display: "flex",
              flexDirection: "column",
              gap: 12,
            }}
          >
            <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
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
            {overviewLoading && !overviewInitialized ? (
              <div
                style={{
                  display: "flex",
                  flex: 1,
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
                  flex: 1,
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                <Spin tip="正在加载协作卡片..." />
              </div>
            ) : (
              <CollaborationPanel
                cards={sessionOverview?.collaborationCards ?? []}
                latestDraft={sessionOverview?.latestDraft}
              />
            )}
          </div>
        </div>
      </Content>
    </Layout>
  );
}
