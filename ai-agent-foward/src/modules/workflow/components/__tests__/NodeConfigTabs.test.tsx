import { render, screen, fireEvent } from "@testing-library/react";
import { vi } from "vitest";

vi.mock("../FieldRenderer", () => ({
  default: ({ field, value }: any) => (
    <div data-testid={`field-${field.fieldKey}`}>
      {field.fieldLabel}: {String(value ?? "")}
    </div>
  ),
}));

const { default: NodeConfigTabs } = await import("../NodeConfigTabs");

const mockTemplate = {
  id: 1,
  typeCode: "LLM",
  name: "LLM",
  configFieldGroups: [
    {
      groupName: "基础配置",
      fields: [
        {
          fieldId: 1,
          fieldKey: "model",
          fieldLabel: "模型",
          fieldType: "text",
        },
        {
          fieldId: 2,
          fieldKey: "systemPrompt",
          fieldLabel: "系统提示词",
          fieldType: "textarea",
        },
      ],
    },
  ],
} as any;

const genericTemplate = {
  id: 99,
  typeCode: "TOOL",
  name: "工具",
  configFieldGroups: [],
} as any;

const knowledgeTemplate = {
  id: 2,
  typeCode: "KNOWLEDGE",
  name: "知识库",
  configFieldGroups: [],
} as any;

describe("NodeConfigTabs", () => {
  it("renders 3 tabs: 输入, 输出, 配置", () => {
    render(
      <NodeConfigTabs
        template={mockTemplate}
        inputSchema={[]}
        outputSchema={[]}
        userConfig={{}}
        onConfigChange={vi.fn()}
      />,
    );
    expect(screen.getByRole("tab", { name: "输入" })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "输出" })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "配置" })).toBeInTheDocument();
  });

  it("shows config tab content by default", () => {
    render(
      <NodeConfigTabs
        template={mockTemplate}
        inputSchema={[]}
        outputSchema={[]}
        userConfig={{ model: "gpt-4" }}
        onConfigChange={vi.fn()}
      />,
    );
    expect(screen.getByTestId("field-model")).toBeInTheDocument();
    expect(screen.getByTestId("field-systemPrompt")).toBeInTheDocument();
  });

  it("switches to input tab", () => {
    render(
      <NodeConfigTabs
        template={genericTemplate}
        inputSchema={[{ key: "user_input", label: "用户输入", type: "string" }]}
        outputSchema={[]}
        userConfig={{}}
        onConfigChange={vi.fn()}
      />,
    );
    fireEvent.click(screen.getByRole("tab", { name: "输入" }));
    expect(screen.getByText("用户输入")).toBeInTheDocument();
  });

  it("switches to output tab", () => {
    render(
      <NodeConfigTabs
        template={genericTemplate}
        inputSchema={[]}
        outputSchema={[{ key: "result", label: "输出结果", type: "string" }]}
        userConfig={{}}
        onConfigChange={vi.fn()}
      />,
    );
    fireEvent.click(screen.getByRole("tab", { name: "输出" }));
    expect(screen.getByText("输出结果")).toBeInTheDocument();
  });

  it("shows group name for config fields", () => {
    render(
      <NodeConfigTabs
        template={mockTemplate}
        inputSchema={[]}
        outputSchema={[]}
        userConfig={{}}
        onConfigChange={vi.fn()}
      />,
    );
    expect(screen.getByText("基础配置")).toBeInTheDocument();
  });

  it("LLM 输入页默认展示 Prompt 模板文本框", () => {
    render(
      <NodeConfigTabs
        nodeType="LLM"
        template={mockTemplate}
        inputSchema={[{ key: "input", label: "用户问题", type: "string" }]}
        outputSchema={[]}
        userConfig={{}}
        onConfigChange={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole("tab", { name: "输入" }));

    expect(screen.getByText("Prompt 模板")).toBeInTheDocument();
    expect(screen.getByDisplayValue("{{inputs.query}}")).toBeInTheDocument();
    expect(screen.queryByText("用户问题")).not.toBeInTheDocument();
  });

  it("LLM Prompt 模板支持插入多跳节点变量", () => {
    const onConfigChange = vi.fn();

    render(
      <NodeConfigTabs
        nodeType="LLM"
        template={mockTemplate}
        inputSchema={[]}
        outputSchema={[]}
        userConfig={{ userPromptTemplate: "用户问题：\n" }}
        promptTemplateVariables={[
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
        ]}
        onConfigChange={onConfigChange}
      />,
    );

    fireEvent.click(screen.getByRole("tab", { name: "输入" }));
    fireEvent.click(
      screen.getByRole("button", {
        name: "知识库节点 · 知识列表 knowledge-1.output.knowledge_list",
      }),
    );

    expect(onConfigChange).toHaveBeenCalledWith(
      "userPromptTemplate",
      "用户问题：\n{{knowledge-1.output.knowledge_list}}",
    );
  });

  it("支持为 LLM 节点选择多个参考节点", () => {
    const onConfigChange = vi.fn();

    render(
      <NodeConfigTabs
        nodeType="LLM"
        template={{
          ...mockTemplate,
          configFieldGroups: [
            ...mockTemplate.configFieldGroups,
            {
              groupName: "上下文配置",
              fields: [
                {
                  fieldId: 3,
                  fieldKey: "contextRefNodes",
                  fieldLabel: "参考节点",
                  fieldType: "NODE_MULTI_SELECT",
                },
              ],
            },
          ],
        }}
        inputSchema={[]}
        outputSchema={[]}
        userConfig={{ contextRefNodes: ["knowledge-1"] }}
        contextReferenceNodes={[
          {
            nodeId: "knowledge-1",
            nodeName: "知识库节点",
            nodeType: "KNOWLEDGE",
          },
          { nodeId: "tool-1", nodeName: "工具节点", nodeType: "TOOL" },
        ]}
        onConfigChange={onConfigChange}
      />,
    );

    expect(screen.getByText("参考节点")).toBeInTheDocument();
    expect(
      screen.queryByTestId("field-contextRefNodes"),
    ).not.toBeInTheDocument();
    fireEvent.click(screen.getByLabelText("引用节点-tool-1"));

    expect(onConfigChange).toHaveBeenCalledWith("contextRefNodes", [
      "knowledge-1",
      "tool-1",
    ]);
  });

  it("为知识库节点补回标准 query 输入字段，而不是生成随机字段", () => {
    const onInputSchemaChange = vi.fn();

    render(
      <NodeConfigTabs
        template={knowledgeTemplate}
        inputSchema={[]}
        outputSchema={[]}
        userConfig={{}}
        onConfigChange={vi.fn()}
        onInputSchemaChange={onInputSchemaChange}
      />,
    );

    fireEvent.click(screen.getByRole("tab", { name: "输入" }));
    fireEvent.click(screen.getByRole("button", { name: "+ 恢复查询词字段" }));

    expect(onInputSchemaChange).toHaveBeenCalledWith([
      expect.objectContaining({
        key: "query",
        label: "查询词",
        type: "string",
        sourceRef: "start.output.query",
        required: true,
        system: true,
      }),
    ]);
  });

  it("知识库节点已有 query 时，不再显示添加输入字段按钮", () => {
    render(
      <NodeConfigTabs
        template={knowledgeTemplate}
        inputSchema={[
          {
            key: "query",
            label: "查询词",
            type: "string",
            system: true,
            required: true,
          },
        ]}
        outputSchema={[]}
        userConfig={{}}
        onConfigChange={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole("tab", { name: "输入" }));

    expect(
      screen.queryByRole("button", { name: "+ 恢复查询词字段" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "+ 添加输入字段" }),
    ).not.toBeInTheDocument();
  });
});
