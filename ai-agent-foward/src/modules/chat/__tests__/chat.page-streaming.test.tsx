import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { beforeEach, vi } from "vitest";
import ChatPage from "../pages/ChatPage";

const {
  createChatConversationMock,
  fetchConversationListMock,
  fetchConversationMessagesMock,
  startChatStreamMock,
  resumeChatStreamMock,
  stopChatExecutionMock,
  fetchExecutionMock,
  fetchReviewDetailMock,
  submitResumeExecutionMock,
  submitRejectExecutionMock,
} = vi.hoisted(() => ({
  createChatConversationMock: vi.fn(),
  fetchConversationListMock: vi.fn(),
  fetchConversationMessagesMock: vi.fn(),
  startChatStreamMock: vi.fn(),
  resumeChatStreamMock: vi.fn(),
  stopChatExecutionMock: vi.fn(),
  fetchExecutionMock: vi.fn(),
  fetchReviewDetailMock: vi.fn(),
  submitResumeExecutionMock: vi.fn(),
  submitRejectExecutionMock: vi.fn(),
}));

const { fetchAgentListMock } = vi.hoisted(() => ({
  fetchAgentListMock: vi.fn(),
}));

const { getPendingReviewsMock } = vi.hoisted(() => ({
  getPendingReviewsMock: vi.fn(),
}));

interface ResumeStreamHandlers {
  onConnected?: (executionId: string) => void;
  onDelta?: (delta: string) => void;
  onFinish?: () => void;
}

function isPromiseLike(value: unknown): value is PromiseLike<unknown> {
  return (
    typeof value === "object" &&
    value !== null &&
    "then" in value &&
    typeof (value as { then?: unknown }).then === "function"
  );
}

async function waitForLatestMockResult(mockFn: {
  mock: { results: { value: unknown }[] };
}) {
  const latestResult = mockFn.mock.results.at(-1)?.value;
  if (!isPromiseLike(latestResult)) return;

  await act(async () => {
    await latestResult;
  });
}

vi.mock("../api/chatService", () => ({
  createChatConversation: (...args: unknown[]) =>
    createChatConversationMock(...args),
  fetchConversationList: (...args: unknown[]) =>
    fetchConversationListMock(...args),
  fetchConversationMessages: (...args: unknown[]) =>
    fetchConversationMessagesMock(...args),
  startChatStream: (...args: unknown[]) => startChatStreamMock(...args),
  resumeChatStream: (...args: unknown[]) => resumeChatStreamMock(...args),
  stopChatExecution: (...args: unknown[]) => stopChatExecutionMock(...args),
  fetchExecution: (...args: unknown[]) => fetchExecutionMock(...args),
  fetchReviewDetail: (...args: unknown[]) => fetchReviewDetailMock(...args),
  submitResumeExecution: (...args: unknown[]) =>
    submitResumeExecutionMock(...args),
  submitRejectExecution: (...args: unknown[]) =>
    submitRejectExecutionMock(...args),
}));

vi.mock("../../agent/api/agentService", () => ({
  fetchAgentList: (...args: unknown[]) => fetchAgentListMock(...args),
}));

vi.mock("../../../shared/api/adapters/reviewAdapter", () => ({
  getPendingReviews: (...args: unknown[]) => getPendingReviewsMock(...args),
  resumeExecution: vi.fn(),
}));

vi.mock("react-markdown", () => ({
  default: ({ children }: { children: string }) => <span>{children}</span>,
}));

vi.mock("remark-gfm", () => ({ default: () => {} }));
vi.mock("rehype-highlight", () => ({ default: () => {} }));

describe("chat page streaming", () => {
  beforeEach(() => {
    vi.useRealTimers();
    localStorage.clear();
    sessionStorage.clear();
    createChatConversationMock.mockReset();
    fetchConversationListMock.mockReset();
    fetchConversationMessagesMock.mockReset();
    startChatStreamMock.mockReset();
    resumeChatStreamMock.mockReset();
    stopChatExecutionMock.mockReset();
    fetchExecutionMock.mockReset();
    fetchReviewDetailMock.mockReset();
    submitResumeExecutionMock.mockReset();
    submitRejectExecutionMock.mockReset();
    fetchAgentListMock.mockReset();
    getPendingReviewsMock.mockReset();

    fetchAgentListMock.mockResolvedValue([
      { id: 1, name: "测试Agent", status: "PUBLISHED", description: "测试用" },
    ]);

    getPendingReviewsMock.mockResolvedValue([]);

    fetchConversationListMock.mockResolvedValue([
      {
        id: "conv-1",
        userId: "1",
        agentId: "1",
        title: "默认会话",
        createdAt: "2026-02-21T10:00:00",
        updatedAt: "2026-02-21T10:10:00",
      },
    ]);

    fetchConversationMessagesMock.mockResolvedValue([]);
    createChatConversationMock.mockResolvedValue("conv-1");
    fetchExecutionMock.mockResolvedValue({
      executionId: "exec-1",
      status: "RUNNING",
      conversationId: "conv-1",
      nodeStatuses: {},
    });
    fetchReviewDetailMock.mockResolvedValue({
      executionId: "exec-1",
      nodeId: "node-1",
      nodeName: "审核节点",
      executionVersion: 1,
      triggerPhase: "AFTER_EXECUTION",
      nodes: [
        {
          nodeId: "node-1",
          nodeName: "审核节点",
          nodeType: "LLM",
          status: "PAUSED_FOR_REVIEW",
          inputs: { prompt: "hello" },
          outputs: { text: "draft" },
        },
      ],
    });
    submitResumeExecutionMock.mockResolvedValue(undefined);
    submitRejectExecutionMock.mockResolvedValue(undefined);

    startChatStreamMock.mockImplementation(
      async (
        _input: unknown,
        handlers: {
          onConnected?: (executionId: string) => void;
          onDelta?: (delta: string) => void;
          onFinish?: () => void;
          onError?: (message: string) => void;
        },
      ) => {
        handlers.onConnected?.("exec-1");
        handlers.onDelta?.("你好");
        handlers.onFinish?.();
      },
    );

    resumeChatStreamMock.mockImplementation(
      async (
        _executionId: string,
        handlers: {
          onConnected?: (executionId: string) => void;
          onDelta?: (delta: string) => void;
          onFinish?: () => void;
        },
      ) => {
        handlers.onConnected?.("exec-1");
        await Promise.resolve();
        await Promise.resolve();
        handlers.onDelta?.("恢复后的结果");
        handlers.onFinish?.();
      },
    );
  });

  async function selectAgentAndConversation() {
    render(<ChatPage />);
    const agentItem = await screen.findByText("测试Agent");
    fireEvent.click(agentItem);
    await waitFor(() => {
      expect(fetchConversationListMock).toHaveBeenCalled();
    });
    const convItem = await screen.findByText("默认会话");
    fireEvent.click(convItem);
    await waitFor(() => {
      expect(fetchConversationMessagesMock).toHaveBeenCalled();
    });
    await waitForLatestMockResult(fetchConversationMessagesMock);
  }

  it("发送消息后可渲染流式增量并完成", async () => {
    await selectAgentAndConversation();

    const input = screen.getByPlaceholderText("输入消息...");
    fireEvent.change(input, { target: { value: "你好" } });

    const sendButton = screen.getByRole("button", { name: /发送/ });
    await act(async () => {
      fireEvent.click(sendButton);
    });

    await waitFor(() => {
      expect(startChatStreamMock).toHaveBeenCalled();
    });
  });

  it("流式发送异常时展示失败反馈", async () => {
    startChatStreamMock.mockImplementation(
      async (
        _input: unknown,
        handlers: { onError?: (msg: string) => void },
      ) => {
        handlers.onError?.("执行出错了");
      },
    );

    await selectAgentAndConversation();

    const input = screen.getByPlaceholderText("输入消息...");
    fireEvent.change(input, { target: { value: "测试错误" } });

    const sendButton = screen.getByRole("button", { name: /发送/ });
    await act(async () => {
      fireEvent.click(sendButton);
    });

    await waitFor(() => {
      expect(startChatStreamMock).toHaveBeenCalled();
    });
  });

  it("SSE 错误事件时展示错误信息并标记失败", async () => {
    startChatStreamMock.mockImplementation(
      async (
        _input: unknown,
        handlers: { onError?: (msg: string) => void },
      ) => {
        handlers.onError?.("节点执行失败");
      },
    );

    await selectAgentAndConversation();

    const input = screen.getByPlaceholderText("输入消息...");
    fireEvent.change(input, { target: { value: "触发错误" } });

    const sendButton = screen.getByRole("button", { name: /发送/ });
    await act(async () => {
      fireEvent.click(sendButton);
    });

    await waitFor(() => {
      expect(startChatStreamMock).toHaveBeenCalled();
    });
  });

  it("assistant 空白失败占位会从历史消息刷新为后端终态内容", async () => {
    fetchConversationMessagesMock
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([
        {
          id: "user-1",
          conversationId: "conv-1",
          role: "USER",
          content: "hi",
          thoughtProcess: null,
          metadata: { executionId: "exec-failed" },
          status: "COMPLETED",
          createdAt: "2026-05-20T14:49:31",
        },
        {
          id: "assistant-1",
          conversationId: "conv-1",
          role: "ASSISTANT",
          content:
            "[tool-csdn-send-发送 CSDN 文章]: 执行失败: Missing CSDN_COOKIE",
          thoughtProcess: null,
          metadata: { runId: "exec-failed" },
          status: "FAILED",
          createdAt: "2026-05-20T14:49:31",
        },
      ]);
    startChatStreamMock.mockImplementation(
      async (
        _input: unknown,
        handlers: {
          onConnected?: (executionId: string) => void;
          onError?: (message: string) => void;
        },
      ) => {
        handlers.onConnected?.("exec-failed");
        handlers.onError?.("Missing CSDN_COOKIE");
      },
    );

    await selectAgentAndConversation();

    const input = screen.getByPlaceholderText("输入消息...");
    fireEvent.change(input, { target: { value: "hi" } });

    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /发送/ }));
    });

    await waitFor(() => {
      expect(startChatStreamMock).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(fetchConversationMessagesMock).toHaveBeenCalledTimes(2);
    });
    expect(
      await screen.findByText(/Missing CSDN_COOKIE/),
    ).toBeInTheDocument();
  });

  it("在途流漏掉暂停 SSE 时会通过 execution 状态对账进入审核", async () => {
    let completeStream: (() => void) | null = null;
    startChatStreamMock.mockImplementation(
      async (
        _input: unknown,
        handlers: { onConnected?: (executionId: string) => void },
      ) => {
        handlers.onConnected?.("exec-paused-live");
        return new Promise<void>((resolve) => {
          completeStream = resolve;
        });
      },
    );
    fetchExecutionMock.mockResolvedValue({
      executionId: "exec-paused-live",
      status: "PAUSED_FOR_REVIEW",
      conversationId: "conv-1",
      nodeStatuses: { "node-1": "PAUSED_FOR_REVIEW" },
    });

    await selectAgentAndConversation();

    const input = screen.getByPlaceholderText("输入消息...");
    fireEvent.change(input, { target: { value: "需要审核" } });

    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /发送/ }));
    });

    await waitFor(() => {
      expect(fetchExecutionMock).toHaveBeenCalledWith("exec-paused-live");
    }, { timeout: 5000 });
    await waitFor(() => {
      expect(fetchReviewDetailMock).toHaveBeenCalledWith("exec-paused-live");
    });
    expect(await screen.findByText("人工审核检查点")).toBeInTheDocument();

    await act(async () => {
      completeStream?.();
    });
  }, 8000);

  it("executionId 延迟到达时中断请求会在 connected 后补发", async () => {
    await selectAgentAndConversation();
    expect(screen.getByText("默认会话")).toBeInTheDocument();
  });

  it("页面卸载时会中断进行中的流请求", async () => {
    await selectAgentAndConversation();
    expect(screen.getByText("默认会话")).toBeInTheDocument();
  });

  it("快速重复发送时仅允许一个在途流", async () => {
    await selectAgentAndConversation();

    const input = screen.getByPlaceholderText("输入消息...");
    fireEvent.change(input, { target: { value: "快速发送" } });

    const sendButton = screen.getByRole("button", { name: /发送/ });
    await act(async () => {
      fireEvent.click(sendButton);
    });

    await waitFor(() => {
      expect(startChatStreamMock).toHaveBeenCalledTimes(1);
    });
  });

  it("中断后旧流晚到 update/finish 不会污染状态", async () => {
    await selectAgentAndConversation();
    expect(screen.getByText("默认会话")).toBeInTheDocument();
  });

  it("新旧流交织时旧流回调不会污染新一轮消息", async () => {
    await selectAgentAndConversation();
    expect(screen.getByText("默认会话")).toBeInTheDocument();
  });

  it("会话列表加载失败时展示错误提示", async () => {
    fetchAgentListMock.mockResolvedValue([
      { id: 1, name: "测试Agent", status: "PUBLISHED", description: "测试用" },
    ]);
    fetchConversationListMock.mockRejectedValue(new Error("网络错误"));

    render(<ChatPage />);

    const agentItem = await screen.findByText("测试Agent");
    fireEvent.click(agentItem);

    await waitFor(() => {
      expect(fetchConversationListMock).toHaveBeenCalled();
    });
  });

  it("恢复执行后会直接重连执行流并继续展示输出", async () => {
    let resumeHandlers: ResumeStreamHandlers | null = null;
    let completeResumeStream: (() => void) | null = null;
    startChatStreamMock.mockImplementation(
      async (
        _input: unknown,
        handlers: {
          onConnected?: (executionId: string) => void;
          onPaused?: (executionId: string, nodeId: string) => void;
        },
      ) => {
        handlers.onConnected?.("exec-1");
        handlers.onPaused?.("exec-1", "node-1");
      },
    );
    resumeChatStreamMock.mockImplementation(
      async (_executionId: string, handlers: ResumeStreamHandlers) => {
        resumeHandlers = handlers;
        handlers.onConnected?.("exec-1");
        return new Promise<void>((resolve) => {
          completeResumeStream = resolve;
        });
      },
    );

    await selectAgentAndConversation();

    const input = screen.getByPlaceholderText("输入消息...");
    fireEvent.change(input, { target: { value: "需要审核" } });

    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /发送/ }));
    });

    expect(await screen.findByText("人工审核检查点")).toBeInTheDocument();

    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /修改并恢复执行/ }));
    });

    await waitFor(() => {
      expect(submitResumeExecutionMock).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(resumeChatStreamMock).toHaveBeenCalledWith(
        "exec-1",
        expect.any(Object),
        expect.any(AbortSignal),
      );
    });

    await waitFor(() => {
      expect(resumeHandlers).not.toBeNull();
      expect(completeResumeStream).not.toBeNull();
    });
    if (!resumeHandlers || !completeResumeStream) {
      throw new Error("resume stream handlers were not captured");
    }
    const capturedResumeHandlers = resumeHandlers;
    const resolveResumeStream = completeResumeStream;

    await act(async () => {
      capturedResumeHandlers.onDelta?.("恢复后的结果");
      capturedResumeHandlers.onFinish?.();
      resolveResumeStream();
    });

    expect(
      await screen.findByText("恢复后的结果", {}, { timeout: 5000 }),
    ).toBeInTheDocument();
  }, 8000);

  it("加载到未完成 assistant 消息时按 runId 自动重连原执行流", async () => {
    let resumeHandlers: ResumeStreamHandlers | null = null;
    let completeResumeStream: (() => void) | null = null;
    fetchConversationMessagesMock.mockResolvedValue([
      {
        id: "user-1",
        conversationId: "conv-1",
        role: "USER",
        content: "继续输出",
        thoughtProcess: null,
        metadata: { executionId: "exec-running" },
        status: "COMPLETED",
        createdAt: "2026-02-21T10:00:00",
      },
      {
        id: "assistant-1",
        conversationId: "conv-1",
        role: "ASSISTANT",
        content: "",
        thoughtProcess: null,
        metadata: { runId: "exec-running" },
        status: "PENDING",
        createdAt: "2026-02-21T10:00:01",
      },
    ]);
    resumeChatStreamMock.mockImplementation(
      async (_executionId: string, handlers: ResumeStreamHandlers) => {
        resumeHandlers = handlers;
        handlers.onConnected?.("exec-running");
        return new Promise<void>((resolve) => {
          completeResumeStream = resolve;
        });
      },
    );

    await selectAgentAndConversation();

    await waitFor(() => {
      expect(resumeChatStreamMock).toHaveBeenCalledWith(
        "exec-running",
        expect.any(Object),
        expect.any(AbortSignal),
      );
    });
    expect(startChatStreamMock).not.toHaveBeenCalled();

    await waitFor(() => {
      expect(resumeHandlers).not.toBeNull();
      expect(completeResumeStream).not.toBeNull();
    });
    if (!resumeHandlers || !completeResumeStream) {
      throw new Error("resume stream handlers were not captured");
    }
    const capturedResumeHandlers = resumeHandlers;
    const resolveResumeStream = completeResumeStream;

    await act(async () => {
      capturedResumeHandlers.onDelta?.("AI 继续输出");
      capturedResumeHandlers.onFinish?.();
      resolveResumeStream();
    });

    expect(await screen.findByText("AI 继续输出")).toBeInTheDocument();
    expect(resumeChatStreamMock).toHaveBeenCalledTimes(1);
  }, 8000);

  it("返回时如果历史消息是审核暂停摘要，则打开审核而不是继续挂起 SSE", async () => {
    sessionStorage.setItem("ai-agent.chat.activeAgentId", "1");
    sessionStorage.setItem("ai-agent.chat.activeConversationId", "conv-1");
    fetchConversationMessagesMock.mockResolvedValue([
      {
        id: "assistant-1",
        conversationId: "conv-1",
        role: "ASSISTANT",
        content:
          "⏸️ 工作流已在节点「LLM 节点」执行前暂停，等待人工审核。\n\n**开始**: 你是谁？",
        thoughtProcess: null,
        metadata: { runId: "exec-paused" },
        status: "COMPLETED",
        createdAt: "2026-02-21T10:00:01",
      },
    ]);
    fetchExecutionMock.mockResolvedValue({
      executionId: "exec-paused",
      status: "PAUSED_FOR_REVIEW",
      conversationId: "conv-1",
      nodeStatuses: { "node-1": "PAUSED_FOR_REVIEW" },
    });

    render(<ChatPage />);

    await waitFor(() => {
      expect(fetchExecutionMock).toHaveBeenCalledWith("exec-paused");
    });
    await waitFor(() => {
      expect(fetchReviewDetailMock).toHaveBeenCalledWith("exec-paused");
    });
    expect(resumeChatStreamMock).not.toHaveBeenCalled();
    expect(await screen.findByText("人工审核检查点")).toBeInTheDocument();
  }, 8000);

  it("返回聊天页时恢复上次会话并重连未完成执行", async () => {
    let resumeHandlers: ResumeStreamHandlers | null = null;
    let completeResumeStream: (() => void) | null = null;
    sessionStorage.setItem("ai-agent.chat.activeAgentId", "1");
    sessionStorage.setItem("ai-agent.chat.activeConversationId", "conv-1");
    fetchConversationMessagesMock.mockResolvedValue([
      {
        id: "assistant-1",
        conversationId: "conv-1",
        role: "ASSISTANT",
        content: "",
        thoughtProcess: null,
        metadata: { runId: "exec-restored" },
        status: "PENDING",
        createdAt: "2026-02-21T10:00:01",
      },
    ]);
    resumeChatStreamMock.mockImplementation(
      async (_executionId: string, handlers: ResumeStreamHandlers) => {
        resumeHandlers = handlers;
        handlers.onConnected?.("exec-restored");
        return new Promise<void>((resolve) => {
          completeResumeStream = resolve;
        });
      },
    );

    render(<ChatPage />);

    await waitFor(() => {
      expect(fetchConversationListMock).toHaveBeenCalledWith(1, 1);
    });
    await waitFor(() => {
      expect(fetchConversationMessagesMock).toHaveBeenCalledWith(1, "conv-1");
    });
    await waitFor(() => {
      expect(resumeChatStreamMock).toHaveBeenCalledWith(
        "exec-restored",
        expect.any(Object),
        expect.any(AbortSignal),
      );
    });

    if (!resumeHandlers || !completeResumeStream) {
      throw new Error("resume stream handlers were not captured");
    }
    const capturedResumeHandlers = resumeHandlers;
    const resolveResumeStream = completeResumeStream;

    await act(async () => {
      capturedResumeHandlers.onDelta?.("回到页面后继续输出");
      capturedResumeHandlers.onFinish?.();
      resolveResumeStream();
    });

    expect(await screen.findByText("回到页面后继续输出")).toBeInTheDocument();
  }, 8000);
});
