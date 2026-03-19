import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { useState } from "react";
import { vi } from "vitest";

const {
  fetchWorkflowDetailMock,
  publishWorkflowMock,
  saveWorkflowMock,
  fetchNodeTemplatesMock,
} = vi.hoisted(() => ({
  fetchWorkflowDetailMock: vi.fn(),
  publishWorkflowMock: vi.fn(),
  saveWorkflowMock: vi.fn(),
  fetchNodeTemplatesMock: vi.fn(),
}));

vi.mock("../api/workflowService", () => ({
  fetchWorkflowDetail: (...args: unknown[]) => fetchWorkflowDetailMock(...args),
  publishWorkflow: (...args: unknown[]) => publishWorkflowMock(...args),
  saveWorkflow: (...args: unknown[]) => saveWorkflowMock(...args),
  fetchNodeTemplates: (...args: unknown[]) => fetchNodeTemplatesMock(...args),
}));

const mockStoreState = {
  agentName: "测试 Agent",
  agentDescription: "",
  agentIcon: "",
  version: 3,
  isDirty: false,
  expandedNodeId: "",
  operationState: "idle" as "idle" | "saving" | "publishing",
  nodeTemplates: [] as unknown[],
  panelCollapsed: false,
  setAgentInfo: vi.fn((info: Record<string, unknown>) => {
    Object.assign(mockStoreState, info);
  }),
  markDirty: vi.fn(() => {
    mockStoreState.isDirty = true;
  }),
  markClean: vi.fn(() => {
    mockStoreState.isDirty = false;
  }),
  toggleNodeExpand: vi.fn(),
  setOperationState: vi.fn((s: string) => {
    mockStoreState.operationState = s as "idle" | "saving" | "publishing";
  }),
  setNodeTemplates: vi.fn(),
  togglePanel: vi.fn(),
  reset: vi.fn(),
};

vi.mock("../stores/useEditorStore", () => ({
  useEditorStore: (selector?: (s: typeof mockStoreState) => unknown) =>
    selector ? selector(mockStoreState) : mockStoreState,
}));

vi.mock("react-router-dom", async () => {
  const actual =
    await vi.importActual<typeof import("react-router-dom")>(
      "react-router-dom",
    );
  return {
    ...actual,
    useParams: () => ({ agentId: "1001" }),
  };
});

// Capture ReactFlow callbacks so tests can simulate canvas interactions
let capturedOnConnect:
  | ((conn: { source: string; target: string }) => void)
  | null = null;

vi.mock("@xyflow/react", () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  function ReactFlow(props: any) {
    capturedOnConnect = props.onConnect;
    const nodes = props.nodes as Array<{
      id: string;
      data: { label: string; nodeType: string };
    }>;
    return (
      <div data-testid="reactflow-canvas">
        {nodes?.map(
          (n: { id: string; data: { label: string; nodeType: string } }) => (
            <div key={n.id} data-testid={`rf-node-${n.id}`}>
              {n.data.label}（{n.data.nodeType}）
            </div>
          ),
        )}
      </div>
    );
  }
  return {
    ReactFlow,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    addEdge: (conn: any, edges: any[]) => [
      ...edges,
      {
        id: `${conn.source}-${conn.target}`,
        source: conn.source,
        target: conn.target,
      },
    ],
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useNodesState: (initial: any[]) => {
      const [nodes, setNodes] = useState(initial);
      return [nodes, setNodes, () => {}];
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useEdgesState: (initial: any[]) => {
      const [edges, setEdges] = useState(initial);
      return [edges, setEdges, () => {}];
    },
    Controls: () => null,
    MiniMap: () => null,
    Background: () => null,
    BackgroundVariant: { Dots: "dots" },
    Handle: () => null,
    Position: { Top: "top", Bottom: "bottom", Left: "left", Right: "right" },
  };
});

// eslint-disable-next-line @typescript-eslint/no-explicit-any
vi.mock("../components/EditorHeader", () => ({
  default: (props: any) => (
    <div data-testid="editor-header">
      <span>{props.agentName}</span>
      <span>{props.isDirty ? "未保存" : "已保存"}</span>
      <span>{props.operationState}</span>
      <button onClick={props.onSave}>保存</button>
      <button onClick={props.onPublish}>发布</button>
    </div>
  ),
}));

vi.mock("../components/AgentConfigPanel", () => ({
  default: () => <div data-testid="agent-config-panel" />,
}));

vi.mock("../components/CanvasToolbar", () => ({
  default: () => <div data-testid="canvas-toolbar" />,
}));

vi.mock("../components/WorkflowNode", () => ({
  default: () => <div />,
}));

vi.mock("../components/CustomEdge", () => ({
  default: () => <g />,
}));

vi.mock("../components/CustomConnectionLine", () => ({
  default: () => <g />,
}));

// Lazy import so mocks are applied first
const { default: WorkflowEditorPage } =
  await import("../pages/WorkflowEditorPage");

/** Helper: simulate a ReactFlow connection via the captured onConnect callback */
function simulateConnect(source: string, target: string) {
  act(() => {
    capturedOnConnect?.({ source, target });
  });
}

function resetMockStoreState() {
  mockStoreState.agentName = "测试 Agent";
  mockStoreState.agentDescription = "";
  mockStoreState.agentIcon = "";
  mockStoreState.version = 3;
  mockStoreState.isDirty = false;
  mockStoreState.expandedNodeId = "";
  mockStoreState.operationState = "idle";
  mockStoreState.nodeTemplates = [];
  mockStoreState.panelCollapsed = false;
  mockStoreState.setAgentInfo.mockClear();
  mockStoreState.markDirty.mockClear();
  mockStoreState.markClean.mockClear();
  mockStoreState.toggleNodeExpand.mockClear();
  mockStoreState.setOperationState.mockClear();
  mockStoreState.setNodeTemplates.mockClear();
  mockStoreState.togglePanel.mockClear();
  mockStoreState.reset.mockClear();
}

describe("workflow editor interaction", () => {
  beforeEach(() => {
    fetchWorkflowDetailMock.mockReset();
    publishWorkflowMock.mockReset();
    saveWorkflowMock.mockReset();
    fetchNodeTemplatesMock.mockReset();
    capturedOnConnect = null;
    resetMockStoreState();

    fetchWorkflowDetailMock.mockResolvedValue({
      agentId: 1001,
      version: 3,
      name: "测试 Agent",
      graphJson: undefined,
      graph: null,
    });
    saveWorkflowMock.mockResolvedValue({
      agentId: 1001,
      version: 4,
      name: "测试 Agent",
      graphJson: undefined,
      graph: null,
    });
    publishWorkflowMock.mockResolvedValue({
      agentId: 1001,
      version: 5,
      name: "测试 Agent",
      graphJson: undefined,
      graph: null,
    });
    fetchNodeTemplatesMock.mockResolvedValue([]);
  });

  it("展示 EditorHeader 和节点列表", async () => {
    render(<WorkflowEditorPage />);

    expect(screen.getByTestId("editor-header")).toBeInTheDocument();
    expect(await screen.findByText("开始节点（START）")).toBeInTheDocument();
    expect(screen.getByText("结束节点（END）")).toBeInTheDocument();
  });

  it("添加合法连线后调用 store.markDirty", async () => {
    render(<WorkflowEditorPage />);
    await screen.findByText("开始节点（START）");

    simulateConnect("start", "end");

    expect(mockStoreState.markDirty).toHaveBeenCalled();
  });

  it("自连时显示显式错误反馈", async () => {
    render(<WorkflowEditorPage />);
    await screen.findByText("开始节点（START）");

    simulateConnect("start", "start");

    expect(screen.getByText("不允许节点自连")).toBeInTheDocument();
  });

  it("重复连线时显示显式错误反馈", async () => {
    render(<WorkflowEditorPage />);
    await screen.findByText("开始节点（START）");

    simulateConnect("start", "end");
    simulateConnect("start", "end");

    expect(screen.getByText("该连线已存在，请勿重复添加")).toBeInTheDocument();
  });

  it("保存前校验失败时不发保存请求并显示反馈", async () => {
    render(<WorkflowEditorPage />);
    await screen.findByText("开始节点（START）");

    fireEvent.click(screen.getByRole("button", { name: "保存" }));

    expect(
      await screen.findByText("至少添加一条连线后再保存"),
    ).toBeInTheDocument();
    expect(saveWorkflowMock).not.toHaveBeenCalled();
  });

  it("发布前校验失败时不发发布请求并显示反馈", async () => {
    render(<WorkflowEditorPage />);
    await screen.findByText("开始节点（START）");

    fireEvent.click(screen.getByRole("button", { name: "发布" }));

    expect(
      await screen.findByText("至少添加一条连线后再保存"),
    ).toBeInTheDocument();
    expect(publishWorkflowMock).not.toHaveBeenCalled();
  });

  it("保存成功后调用 store.markClean 和 store.setAgentInfo", async () => {
    render(<WorkflowEditorPage />);
    await screen.findByText("开始节点（START）");

    simulateConnect("start", "end");

    fireEvent.click(screen.getByRole("button", { name: "保存" }));

    await waitFor(() => {
      expect(saveWorkflowMock).toHaveBeenCalledTimes(1);
    });
    expect(await screen.findByText("保存成功")).toBeInTheDocument();
    expect(mockStoreState.markClean).toHaveBeenCalled();
    expect(mockStoreState.setAgentInfo).toHaveBeenCalledWith({ version: 4 });

    fireEvent.click(screen.getByRole("button", { name: "发布" }));

    await waitFor(() => {
      expect(publishWorkflowMock).toHaveBeenCalledWith(1001);
    });
    expect(await screen.findByText("发布成功")).toBeInTheDocument();
  });

  it("未保存状态下发布会被拦截并提示先保存", async () => {
    render(<WorkflowEditorPage />);
    await screen.findByText("开始节点（START）");

    simulateConnect("start", "end");
    // markDirty side-effect already sets isDirty = true
    fireEvent.click(screen.getByRole("button", { name: "发布" }));

    expect(await screen.findByText("请先保存后再发布")).toBeInTheDocument();
    expect(publishWorkflowMock).not.toHaveBeenCalled();
  });

  it("保存失败后可恢复操作", async () => {
    saveWorkflowMock.mockRejectedValueOnce(new Error("save failed"));

    render(<WorkflowEditorPage />);
    await screen.findByText("开始节点（START）");

    simulateConnect("start", "end");
    fireEvent.click(screen.getByRole("button", { name: "保存" }));

    expect(await screen.findByText("保存失败，请稍后重试")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "保存" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "发布" })).toBeEnabled();
  });

  it("发布失败后可恢复操作", async () => {
    publishWorkflowMock.mockRejectedValueOnce(new Error("publish failed"));

    render(<WorkflowEditorPage />);
    await screen.findByText("开始节点（START）");

    simulateConnect("start", "end");
    fireEvent.click(screen.getByRole("button", { name: "保存" }));

    await screen.findByText("保存成功");
    fireEvent.click(screen.getByRole("button", { name: "发布" }));

    expect(await screen.findByText("发布失败，请稍后重试")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "保存" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "发布" })).toBeEnabled();
  });

  it("INITIAL_NODES 使用水平布局位置", async () => {
    render(<WorkflowEditorPage />);
    const startNode = await screen.findByText("开始节点（START）");
    const endNode = screen.getByText("结束节点（END）");
    expect(startNode).toBeInTheDocument();
    expect(endNode).toBeInTheDocument();
  });

  it("保存时保留知识库 query 字段上用户显式选择的 sourceRef，并为下游 LLM 自动补充 contextRefNodes", async () => {
    fetchWorkflowDetailMock.mockResolvedValueOnce({
      agentId: 1001,
      version: 3,
      name: "测试 Agent",
      graphJson: undefined,
      graph: {
        version: "1.0",
        startNodeId: "start",
        nodes: [
          {
            nodeId: "start",
            nodeName: "开始节点",
            nodeType: "START",
            position: { x: 50, y: 250 },
            inputSchema: [
              {
                key: "inputMessage",
                label: "用户输入",
                type: "string",
                system: true,
                required: true,
              },
            ],
            outputSchema: [
              {
                key: "inputMessage",
                label: "用户输入",
                type: "string",
                system: true,
              },
            ],
            userConfig: {},
          },
          {
            nodeId: "knowledge-1",
            nodeName: "知识库节点",
            nodeType: "KNOWLEDGE",
            position: { x: 250, y: 250 },
            inputSchema: [
              {
                key: "field_1773740324458",
                label: "新字段",
                type: "string",
                sourceRef: "start.output.inputMessage",
              },
            ],
            outputSchema: [
              {
                key: "knowledge_list",
                label: "知识列表",
                type: "array",
                system: true,
              },
            ],
            userConfig: {},
          },
          {
            nodeId: "llm-1",
            nodeName: "LLM 节点",
            nodeType: "LLM",
            position: { x: 450, y: 250 },
            inputSchema: [],
            outputSchema: [
              {
                key: "llm_output",
                label: "大模型输出",
                type: "string",
                system: true,
              },
            ],
            userConfig: {},
          },
          {
            nodeId: "end",
            nodeName: "结束节点",
            nodeType: "END",
            position: { x: 650, y: 250 },
            inputSchema: [
              {
                key: "output",
                label: "输出内容",
                type: "string",
                system: true,
                required: true,
              },
            ],
            outputSchema: [
              {
                key: "output",
                label: "输出内容",
                type: "string",
                system: true,
              },
            ],
            userConfig: {},
          },
        ],
        edges: [
          { edgeId: "edge-1", source: "start", target: "knowledge-1" },
          { edgeId: "edge-2", source: "knowledge-1", target: "llm-1" },
          { edgeId: "edge-3", source: "llm-1", target: "end" },
        ],
      },
    });
    fetchNodeTemplatesMock.mockResolvedValueOnce([
      {
        id: 1,
        typeCode: "START",
        name: "开始节点",
        initialSchema: {
          inputSchema: [
            {
              key: "inputMessage",
              label: "用户输入",
              type: "string",
              system: true,
              required: true,
            },
          ],
          outputSchema: [
            {
              key: "inputMessage",
              label: "用户输入",
              type: "string",
              system: true,
            },
            { key: "query", label: "查询词", type: "string", system: true },
          ],
        },
        defaultSchemaPolicy: {},
        configFieldGroups: [],
      },
      {
        id: 2,
        typeCode: "KNOWLEDGE",
        name: "知识库节点",
        initialSchema: {
          inputSchema: [
            {
              key: "query",
              label: "查询词",
              type: "string",
              system: true,
              required: true,
              sourceRef: "start.output.query",
            },
          ],
          outputSchema: [
            {
              key: "knowledge_list",
              label: "知识列表",
              type: "array",
              system: true,
            },
          ],
        },
        defaultSchemaPolicy: {},
        configFieldGroups: [],
      },
      {
        id: 3,
        typeCode: "LLM",
        name: "LLM 节点",
        initialSchema: {
          inputSchema: [],
          outputSchema: [
            {
              key: "llm_output",
              label: "大模型输出",
              type: "string",
              system: true,
            },
          ],
        },
        defaultSchemaPolicy: {},
        configFieldGroups: [],
      },
      {
        id: 4,
        typeCode: "END",
        name: "结束节点",
        initialSchema: {
          inputSchema: [
            {
              key: "output",
              label: "输出内容",
              type: "string",
              system: true,
              required: true,
            },
          ],
          outputSchema: [
            { key: "output", label: "输出内容", type: "string", system: true },
          ],
        },
        defaultSchemaPolicy: {},
        configFieldGroups: [],
      },
    ]);

    render(<WorkflowEditorPage />);
    await screen.findByText("知识库节点（KNOWLEDGE）");

    fireEvent.click(screen.getByRole("button", { name: "保存" }));

    await waitFor(() => {
      expect(saveWorkflowMock).toHaveBeenCalledTimes(1);
    });

    const savePayload = saveWorkflowMock.mock.calls[0][0];
    const savedNodes = savePayload.graph.nodes;
    const startNode = savedNodes.find((node: any) => node.nodeId === "start");
    const knowledgeNode = savedNodes.find(
      (node: any) => node.nodeId === "knowledge-1",
    );
    const llmNode = savedNodes.find((node: any) => node.nodeId === "llm-1");

    expect(startNode.outputSchema).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ key: "query", type: "string", system: true }),
      ]),
    );
    expect(knowledgeNode.inputSchema).toEqual([
      expect.objectContaining({
        key: "query",
        label: "查询词",
        type: "string",
        sourceRef: "start.output.inputMessage",
        required: true,
        system: true,
      }),
    ]);
    expect(llmNode.userConfig.userPromptTemplate).toBe("{{inputs.query}}");
    expect(llmNode.userConfig.contextRefNodes).toEqual(["knowledge-1"]);
  });

  it("用户显式清空 contextRefNodes 后，保存时不再自动回填默认引用", async () => {
    fetchWorkflowDetailMock.mockResolvedValueOnce({
      agentId: 1001,
      version: 3,
      name: "测试 Agent",
      graphJson: undefined,
      graph: {
        version: "1.0",
        startNodeId: "start",
        nodes: [
          {
            nodeId: "start",
            nodeName: "开始节点",
            nodeType: "START",
            position: { x: 50, y: 250 },
            inputSchema: [
              {
                key: "inputMessage",
                label: "用户输入",
                type: "string",
                system: true,
                required: true,
              },
            ],
            outputSchema: [
              {
                key: "inputMessage",
                label: "用户输入",
                type: "string",
                system: true,
              },
              {
                key: "query",
                label: "查询词",
                type: "string",
                system: true,
              },
            ],
            userConfig: {},
          },
          {
            nodeId: "knowledge-1",
            nodeName: "知识库节点",
            nodeType: "KNOWLEDGE",
            position: { x: 250, y: 250 },
            inputSchema: [
              {
                key: "query",
                label: "查询词",
                type: "string",
                system: true,
                required: true,
                sourceRef: "start.output.query",
              },
            ],
            outputSchema: [
              {
                key: "knowledge_list",
                label: "知识列表",
                type: "array",
                system: true,
              },
            ],
            userConfig: {},
          },
          {
            nodeId: "llm-1",
            nodeName: "LLM 节点",
            nodeType: "LLM",
            position: { x: 450, y: 250 },
            inputSchema: [],
            outputSchema: [
              {
                key: "llm_output",
                label: "大模型输出",
                type: "string",
                system: true,
              },
            ],
            userConfig: { contextRefNodes: [] },
          },
          {
            nodeId: "end",
            nodeName: "结束节点",
            nodeType: "END",
            position: { x: 650, y: 250 },
            inputSchema: [
              {
                key: "output",
                label: "输出内容",
                type: "string",
                system: true,
                required: true,
              },
            ],
            outputSchema: [
              {
                key: "output",
                label: "输出内容",
                type: "string",
                system: true,
              },
            ],
            userConfig: {},
          },
        ],
        edges: [
          { edgeId: "edge-1", source: "start", target: "knowledge-1" },
          { edgeId: "edge-2", source: "knowledge-1", target: "llm-1" },
          { edgeId: "edge-3", source: "llm-1", target: "end" },
        ],
      },
    });
    fetchNodeTemplatesMock.mockResolvedValueOnce([
      {
        id: 1,
        typeCode: "START",
        name: "开始节点",
        initialSchema: {
          inputSchema: [
            {
              key: "inputMessage",
              label: "用户输入",
              type: "string",
              system: true,
              required: true,
            },
          ],
          outputSchema: [
            {
              key: "inputMessage",
              label: "用户输入",
              type: "string",
              system: true,
            },
            {
              key: "query",
              label: "查询词",
              type: "string",
              system: true,
            },
          ],
        },
        defaultSchemaPolicy: {},
        configFieldGroups: [],
      },
      {
        id: 2,
        typeCode: "KNOWLEDGE",
        name: "知识库节点",
        initialSchema: {
          inputSchema: [
            {
              key: "query",
              label: "查询词",
              type: "string",
              system: true,
              required: true,
              sourceRef: "start.output.query",
            },
          ],
          outputSchema: [
            {
              key: "knowledge_list",
              label: "知识列表",
              type: "array",
              system: true,
            },
          ],
        },
        defaultSchemaPolicy: {},
        configFieldGroups: [],
      },
      {
        id: 3,
        typeCode: "LLM",
        name: "LLM 节点",
        initialSchema: {
          inputSchema: [],
          outputSchema: [
            {
              key: "llm_output",
              label: "大模型输出",
              type: "string",
              system: true,
            },
          ],
        },
        defaultSchemaPolicy: {},
        configFieldGroups: [],
      },
      {
        id: 4,
        typeCode: "END",
        name: "结束节点",
        initialSchema: {
          inputSchema: [
            {
              key: "output",
              label: "输出内容",
              type: "string",
              system: true,
              required: true,
            },
          ],
          outputSchema: [
            {
              key: "output",
              label: "输出内容",
              type: "string",
              system: true,
            },
          ],
        },
        defaultSchemaPolicy: {},
        configFieldGroups: [],
      },
    ]);

    render(<WorkflowEditorPage />);
    await screen.findByText("LLM 节点（LLM）");

    fireEvent.click(screen.getByRole("button", { name: "保存" }));

    await waitFor(() => {
      expect(saveWorkflowMock).toHaveBeenCalledTimes(1);
    });

    const savePayload = saveWorkflowMock.mock.calls[0][0];
    const llmNode = savePayload.graph.nodes.find(
      (node: any) => node.nodeId === "llm-1",
    );

    expect(llmNode.userConfig.contextRefNodes).toEqual([]);
  });
});
