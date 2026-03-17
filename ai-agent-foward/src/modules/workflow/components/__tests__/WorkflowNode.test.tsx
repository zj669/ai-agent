import { render, screen, fireEvent } from "@testing-library/react";
import { vi } from "vitest";

const mockStore = {
  expandedNodeId: "",
  toggleNodeExpand: vi.fn(),
  markDirty: vi.fn(),
  nodeTemplates: [
    { id: 1, typeCode: "LLM", name: "LLM", icon: "🧠", configFieldGroups: [] },
    {
      id: 2,
      typeCode: "START",
      name: "开始",
      icon: "▶",
      configFieldGroups: [],
    },
  ],
};

let nodeConfigTabsProps: Record<string, unknown> | null = null;
let reactFlowNodes: Array<{ id: string; data: Record<string, unknown> }> = [];
let reactFlowEdges: Array<{ source: string; target: string }> = [];

vi.mock("../../stores/useEditorStore", () => ({
  useEditorStore: (selector: (s: typeof mockStore) => unknown) =>
    selector(mockStore),
}));

vi.mock("../NodeConfigTabs", () => ({
  default: (props: Record<string, unknown>) => {
    nodeConfigTabsProps = props;
    return <div data-testid="node-config-tabs">Config Tabs</div>;
  },
}));

vi.mock("@xyflow/react", () => ({
  Handle: (props: Record<string, unknown>) => (
    <div
      data-testid={`handle-${props.id ?? props.type}`}
      data-type={props.type}
      data-position={props.position}
    />
  ),
  Position: { Left: "left", Right: "right", Top: "top", Bottom: "bottom" },
  useReactFlow: () => ({
    setNodes: vi.fn(),
    setEdges: vi.fn(),
    getEdges: vi.fn(() => reactFlowEdges),
    getNodes: vi.fn(() => reactFlowNodes),
  }),
}));

const { default: WorkflowNode } = await import("../WorkflowNode");

describe("WorkflowNode", () => {
  beforeEach(() => {
    nodeConfigTabsProps = null;
    reactFlowNodes = [];
    reactFlowEdges = [];
    mockStore.markDirty.mockClear();
  });

  it("renders collapsed node with icon and label", () => {
    render(
      <WorkflowNode
        id="llm-1"
        data={{ label: "LLM 节点", nodeType: "LLM" }}
        selected={false}
      />,
    );
    expect(screen.getByText("LLM 节点")).toBeInTheDocument();
    expect(screen.getByText("LLM")).toBeInTheDocument();
  });

  it("shows expand arrow for non-START/END nodes", () => {
    render(
      <WorkflowNode
        id="llm-1"
        data={{ label: "LLM 节点", nodeType: "LLM" }}
        selected={false}
      />,
    );
    expect(screen.getByLabelText("展开配置")).toBeInTheDocument();
  });

  it("shows expand arrow for START node (canExpand is always true)", () => {
    render(
      <WorkflowNode
        id="start"
        data={{ label: "开始", nodeType: "START" }}
        selected={false}
      />,
    );
    expect(screen.getByLabelText("展开配置")).toBeInTheDocument();
  });

  it("calls toggleNodeExpand when arrow clicked", () => {
    render(
      <WorkflowNode
        id="llm-1"
        data={{ label: "LLM 节点", nodeType: "LLM" }}
        selected={false}
      />,
    );
    fireEvent.click(screen.getByLabelText("展开配置"));
    expect(mockStore.toggleNodeExpand).toHaveBeenCalledWith("llm-1");
  });

  it("shows NodeConfigTabs when expanded", () => {
    mockStore.expandedNodeId = "llm-1";
    render(
      <WorkflowNode
        id="llm-1"
        data={{ label: "LLM 节点", nodeType: "LLM" }}
        selected={false}
      />,
    );
    expect(screen.getByTestId("node-config-tabs")).toBeInTheDocument();
    expect(nodeConfigTabsProps?.nodeType).toBe("LLM");
    mockStore.expandedNodeId = "";
  });

  it("为 LLM 节点传入可达的多跳祖先节点作为参考节点候选", () => {
    mockStore.expandedNodeId = "llm-1";
    reactFlowNodes = [
      { id: "start", data: { label: "开始节点", nodeType: "START" } },
      {
        id: "knowledge-1",
        data: { label: "知识库节点", nodeType: "KNOWLEDGE" },
      },
      { id: "tool-1", data: { label: "工具节点", nodeType: "TOOL" } },
      { id: "llm-1", data: { label: "LLM 节点", nodeType: "LLM" } },
    ];
    reactFlowEdges = [
      { source: "start", target: "knowledge-1" },
      { source: "knowledge-1", target: "tool-1" },
      { source: "tool-1", target: "llm-1" },
    ];

    render(
      <WorkflowNode
        id="llm-1"
        data={{ label: "LLM 节点", nodeType: "LLM" }}
        selected={false}
      />,
    );

    expect(nodeConfigTabsProps?.contextReferenceNodes).toEqual([
      { nodeId: "start", nodeName: "开始节点", nodeType: "START" },
      { nodeId: "knowledge-1", nodeName: "知识库节点", nodeType: "KNOWLEDGE" },
      { nodeId: "tool-1", nodeName: "工具节点", nodeType: "TOOL" },
    ]);
    mockStore.expandedNodeId = "";
  });

  it("为 LLM 节点透传 Prompt 模板变量，包含全局输入和多跳祖先输出", () => {
    mockStore.expandedNodeId = "llm-1";
    reactFlowNodes = [
      {
        id: "start",
        data: {
          label: "开始节点",
          nodeType: "START",
          outputSchema: [
            { key: "query", label: "查询词", type: "string" },
            { key: "inputMessage", label: "用户输入", type: "string" },
          ],
        },
      },
      {
        id: "knowledge-1",
        data: {
          label: "知识库节点",
          nodeType: "KNOWLEDGE",
          outputSchema: [
            { key: "knowledge_list", label: "知识列表", type: "array" },
          ],
        },
      },
      {
        id: "llm-1",
        data: { label: "LLM 节点", nodeType: "LLM", outputSchema: [] },
      },
    ];
    reactFlowEdges = [
      { source: "start", target: "knowledge-1" },
      { source: "knowledge-1", target: "llm-1" },
    ];

    render(
      <WorkflowNode
        id="llm-1"
        data={{ label: "LLM 节点", nodeType: "LLM" }}
        selected={false}
      />,
    );

    expect(nodeConfigTabsProps?.promptTemplateVariables).toEqual(
      expect.arrayContaining([
        {
          label: "查询词",
          detail: "inputs.query",
          template: "{{inputs.query}}",
          category: "inputs",
        },
        {
          label: "知识库节点 · 知识列表",
          detail: "knowledge-1.output.knowledge_list",
          template: "{{knowledge-1.output.knowledge_list}}",
          category: "node",
        },
      ]),
    );
    mockStore.expandedNodeId = "";
  });

  it("renders left target handle and right source handle for LLM node", () => {
    const { getByTestId } = render(
      <WorkflowNode
        id="llm-1"
        data={{ label: "LLM 节点", nodeType: "LLM" }}
        selected={false}
      />,
    );
    expect(getByTestId("handle-target").dataset.position).toBe("left");
    expect(getByTestId("handle-source").dataset.position).toBe("right");
  });

  it("CONDITION node renders multiple source handles from branches", () => {
    const data = {
      label: "条件节点",
      nodeType: "CONDITION",
      branches: [
        { id: "branch-0", name: "如果" },
        { id: "else", name: "否则" },
      ],
    };
    const { getByTestId } = render(
      <WorkflowNode id="cond-1" data={data} selected={false} />,
    );
    expect(getByTestId("handle-target")).toBeInTheDocument();
    expect(getByTestId("handle-branch-0")).toBeInTheDocument();
    expect(getByTestId("handle-else")).toBeInTheDocument();
  });
});
