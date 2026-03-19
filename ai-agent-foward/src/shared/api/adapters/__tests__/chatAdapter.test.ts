import { describe, expect, it, vi } from "vitest";
import {
  createConversation,
  getConversationList,
  getConversationMessages,
  getReviewDetail,
  rejectExecution,
  resumeExecution,
  stopWorkflowExecution,
  type ConversationListData,
  type MessageDTO,
  type ReviewDetailData,
} from "../chatAdapter";
import type { ApiClientLike } from "../../client";

describe("chatAdapter", () => {
  it("createConversation should call chat conversations endpoint", async () => {
    const client: ApiClientLike = {
      get: vi.fn(),
      post: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: "success",
          data: "conv-1",
        },
      }),
    };

    const result = await createConversation({ userId: 1, agentId: 2 }, client);

    expect(client.post).toHaveBeenCalledWith(
      "/api/chat/conversations",
      undefined,
      {
        params: {
          userId: 1,
          agentId: 2,
        },
      },
    );
    expect(result).toBe("conv-1");
  });

  it("getConversationList should unwrap list response", async () => {
    const payload: ConversationListData = {
      total: 1,
      pages: 1,
      list: [
        {
          id: "conv-1",
          userId: "1",
          agentId: "2",
          title: "New Chat",
          createdAt: "2026-02-21T00:00:00",
          updatedAt: "2026-02-21T00:01:00",
        },
      ],
    };

    const client: ApiClientLike = {
      get: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: "success",
          data: payload,
        },
      }),
      post: vi.fn(),
    };

    const result = await getConversationList({ userId: 1, agentId: 2 }, client);

    expect(client.get).toHaveBeenCalledWith("/api/chat/conversations", {
      params: {
        userId: 1,
        agentId: 2,
        page: 1,
        size: 20,
      },
    });
    expect(result).toEqual(payload);
  });

  it("getConversationMessages should unwrap message array", async () => {
    const messages: MessageDTO[] = [
      {
        id: "m1",
        conversationId: "conv-1",
        role: "USER",
        content: "hello",
        status: "COMPLETED",
        createdAt: "2026-02-21T00:00:00",
        thoughtProcess: null,
      },
    ];

    const client: ApiClientLike = {
      get: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: "success",
          data: messages,
        },
      }),
      post: vi.fn(),
    };

    const result = await getConversationMessages(
      { userId: 1, conversationId: "conv-1" },
      client,
    );

    expect(client.get).toHaveBeenCalledWith(
      "/api/chat/conversations/conv-1/messages",
      {
        params: {
          userId: 1,
          page: 1,
          size: 50,
          order: "asc",
        },
      },
    );
    expect(result).toEqual(messages);
  });

  it("stopWorkflowExecution should call stop endpoint", async () => {
    const client: ApiClientLike = {
      get: vi.fn(),
      post: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: "success",
          data: null,
        },
      }),
    };

    await stopWorkflowExecution({ executionId: "exec-1" }, client);

    expect(client.post).toHaveBeenCalledWith("/api/workflow/execution/stop", {
      executionId: "exec-1",
    });
  });

  it("getReviewDetail should read raw DTO response", async () => {
    const detail: ReviewDetailData = {
      executionId: "exec-1",
      nodeId: "llm-1",
      nodeName: "LLM 节点",
      executionVersion: 3,
      triggerPhase: "AFTER_EXECUTION",
      nodes: [
        {
          nodeId: "start",
          nodeName: "开始",
          nodeType: "START",
          status: "SUCCEEDED",
          inputs: { query: "hello" },
          outputs: { query: "hello" },
        },
        {
          nodeId: "llm-1",
          nodeName: "LLM 节点",
          nodeType: "LLM",
          status: "PAUSED_FOR_REVIEW",
          inputs: { input: "hello" },
          outputs: { text: "world" },
        },
      ],
    };

    const client: ApiClientLike = {
      get: vi.fn().mockResolvedValue({ data: detail }),
      post: vi.fn(),
    };

    const result = await getReviewDetail("exec-1", client);

    expect(client.get).toHaveBeenCalledWith("/api/workflow/reviews/exec-1");
    expect(result).toEqual(detail);
  });

  it("resumeExecution should accept empty 200 response body", async () => {
    const client: ApiClientLike = {
      get: vi.fn(),
      post: vi.fn().mockResolvedValue({ data: "" }),
    };

    await resumeExecution(
      {
        executionId: "exec-1",
        nodeId: "llm-1",
        expectedVersion: 3,
        edits: { text: "patched" },
        comment: "ok",
      },
      client,
    );

    expect(client.post).toHaveBeenCalledWith("/api/workflow/reviews/resume", {
      executionId: "exec-1",
      nodeId: "llm-1",
      expectedVersion: 3,
      edits: { text: "patched" },
      comment: "ok",
    });
  });

  it("rejectExecution should accept empty 200 response body", async () => {
    const client: ApiClientLike = {
      get: vi.fn(),
      post: vi.fn().mockResolvedValue({ data: "" }),
    };

    await rejectExecution(
      {
        executionId: "exec-1",
        nodeId: "llm-1",
        reason: "bad output",
      },
      client,
    );

    expect(client.post).toHaveBeenCalledWith("/api/workflow/reviews/reject", {
      executionId: "exec-1",
      nodeId: "llm-1",
      reason: "bad output",
    });
  });
});
