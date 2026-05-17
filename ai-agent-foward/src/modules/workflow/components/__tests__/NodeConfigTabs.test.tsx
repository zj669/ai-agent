import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { vi } from "vitest";

const { getAllToolsMock } = vi.hoisted(() => ({
  getAllToolsMock: vi.fn(),
}));

vi.mock("../FieldRenderer", () => ({
  default: ({
    field,
    value,
  }: {
    field: { fieldKey: string; fieldLabel: string };
    value?: unknown;
  }) => (
    <div data-testid={`field-${field.fieldKey}`}>
      {field.fieldLabel}: {String(value ?? "")}
    </div>
  ),
}));

vi.mock("../../../mcp/api/mcpAdapter", () => ({
  mcpAdapter: {
    getAllTools: getAllToolsMock,
  },
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
};

const genericTemplate = {
  id: 99,
  typeCode: "TOOL",
  name: "工具",
  configFieldGroups: [],
};

const knowledgeTemplate = {
  id: 2,
  typeCode: "KNOWLEDGE",
  name: "知识库",
  configFieldGroups: [],
};

describe("NodeConfigTabs", () => {
  beforeEach(() => {
    getAllToolsMock.mockReset();
    getAllToolsMock.mockResolvedValue([]);
  });

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
    expect(screen.getByDisplayValue("{{start.output.inputMessage}}")).toBeInTheDocument();
    expect(screen.queryByText("高级映射")).not.toBeInTheDocument();
    expect(screen.queryByText("用户问题")).not.toBeInTheDocument();
  });

  it("LLM Prompt 模板支持点击插入多跳节点变量", () => {
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
            label: "知识库节点 · 知识列表",
            detail: "knowledge-1.output.knowledge_list",
            template: "{{knowledge-1.output.knowledge_list}}",
          },
        ]}
        onConfigChange={onConfigChange}
      />,
    );

    fireEvent.click(screen.getByRole("tab", { name: "输入" }));
    const promptTextarea = screen.getByRole("textbox");
    fireEvent.change(promptTextarea, {
      target: {
        value: "用户问题：\n{{knowledge",
        selectionStart: "用户问题：\n{{knowledge".length,
      },
    });
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

  it("LLM Prompt 模板支持按 Tab 应用候选变量", () => {
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
            label: "知识库节点 · 知识列表",
            detail: "knowledge-1.output.knowledge_list",
            template: "{{knowledge-1.output.knowledge_list}}",
          },
        ]}
        onConfigChange={onConfigChange}
      />,
    );

    fireEvent.click(screen.getByRole("tab", { name: "输入" }));
    const promptTextarea = screen.getByRole("textbox");
    fireEvent.change(promptTextarea, {
      target: {
        value: "用户问题：\n{{knowledge",
        selectionStart: "用户问题：\n{{knowledge".length,
      },
    });
    fireEvent.keyDown(promptTextarea, { key: "Tab" });

    expect(onConfigChange).toHaveBeenCalledWith(
      "userPromptTemplate",
      "用户问题：\n{{knowledge-1.output.knowledge_list}}",
    );
  });

  it("LLM 配置页不再展示参考节点配置", () => {
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
        onConfigChange={onConfigChange}
      />,
    );

    expect(screen.queryByText("参考节点")).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("field-contextRefNodes"),
    ).not.toBeInTheDocument();
    expect(onConfigChange).not.toHaveBeenCalled();
  });

  it("LLM 文本输出页只展示主输出字段", () => {
    render(
      <NodeConfigTabs
        nodeType="LLM"
        template={mockTemplate}
        inputSchema={[]}
        outputSchema={[]}
        userConfig={{ llmOutputMode: "text" }}
        onConfigChange={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole("tab", { name: "输出" }));

    expect(screen.getByText("response · string")).toBeInTheDocument();
    expect(screen.queryByText("LLM 原始输出")).not.toBeInTheDocument();
    expect(screen.queryByText("文本别名")).not.toBeInTheDocument();
  });

  it("LLM JSON 输出页只展示 JSON 主输出字段", () => {
    render(
      <NodeConfigTabs
        nodeType="LLM"
        template={mockTemplate}
        inputSchema={[]}
        outputSchema={[]}
        userConfig={{ llmOutputMode: "json" }}
        onConfigChange={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole("tab", { name: "输出" }));

    expect(screen.getByText("json_output · object")).toBeInTheDocument();
    expect(screen.queryByText("response · string")).not.toBeInTheDocument();
    expect(screen.queryByText("LLM 原始输出")).not.toBeInTheDocument();
    expect(screen.queryByText("文本别名")).not.toBeInTheDocument();
  });

  it("LLM JSON 输出字段需要填写字段名和字段描述", () => {
    const onOutputSchemaChange = vi.fn();

    render(
      <NodeConfigTabs
        nodeType="LLM"
        template={mockTemplate}
        inputSchema={[]}
        outputSchema={[
          {
            key: "json_output",
            label: "JSON 输出",
            type: "object",
            system: true,
          },
          {
            key: "title",
            label: "title",
            type: "string",
            description: "旧描述",
            required: true,
          },
        ]}
        userConfig={{ llmOutputMode: "json" }}
        onConfigChange={vi.fn()}
        onOutputSchemaChange={onOutputSchemaChange}
      />,
    );

    fireEvent.click(screen.getByRole("tab", { name: "输出" }));

    fireEvent.change(screen.getByPlaceholderText("字段名"), {
      target: { value: "summary" },
    });
    expect(onOutputSchemaChange).toHaveBeenLastCalledWith([
      expect.objectContaining({ key: "json_output" }),
      expect.objectContaining({
        key: "summary",
        label: "summary",
        description: "旧描述",
        required: true,
      }),
    ]);

    fireEvent.change(screen.getByPlaceholderText("字段描述"), {
      target: { value: "文章摘要" },
    });
    expect(onOutputSchemaChange).toHaveBeenLastCalledWith([
      expect.objectContaining({ key: "json_output" }),
      expect.objectContaining({
        key: "title",
        description: "文章摘要",
        required: true,
      }),
    ]);
  });

  it("LLM 从 JSON 切回文本输出时移除 JSON 自定义字段", () => {
    const onConfigChange = vi.fn();
    const onOutputSchemaChange = vi.fn();

    render(
      <NodeConfigTabs
        nodeType="LLM"
        template={mockTemplate}
        inputSchema={[]}
        outputSchema={[
          {
            key: "json_output",
            label: "JSON 输出",
            type: "object",
            system: true,
          },
          {
            key: "title",
            label: "title",
            type: "string",
            description: "文章标题",
            required: true,
          },
        ]}
        userConfig={{ llmOutputMode: "json" }}
        onConfigChange={onConfigChange}
        onOutputSchemaChange={onOutputSchemaChange}
      />,
    );

    fireEvent.click(screen.getByRole("tab", { name: "输出" }));
    fireEvent.click(screen.getByRole("button", { name: "文本输出" }));

    expect(onConfigChange).toHaveBeenCalledWith("llmOutputMode", "text");
    expect(onOutputSchemaChange).toHaveBeenCalledWith([
      expect.objectContaining({
        key: "response",
        type: "string",
        system: true,
      }),
    ]);
    const lastCall =
      onOutputSchemaChange.mock.calls[onOutputSchemaChange.mock.calls.length - 1];
    expect(lastCall[0]).toHaveLength(1);
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
        sourceRef: "",
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

  it("工具节点选择工具时去重重复的同名 MCP 工具", async () => {
    getAllToolsMock.mockResolvedValue([
      {
        serverId: 7,
        serverName: "CSDN",
        toolName: "send_article",
        fullName: "mcp__7__send_article",
        description: "发送 CSDN 文章",
        inputSchema: "{}",
      },
      {
        serverId: 8,
        serverName: "CSDN",
        toolName: "send_article",
        fullName: "mcp__8__send_article",
        description: "发送 CSDN 文章",
        inputSchema: "{}",
      },
    ]);

    render(
      <NodeConfigTabs
        nodeType="TOOL"
        template={genericTemplate}
        inputSchema={[]}
        outputSchema={[]}
        userConfig={{}}
        onConfigChange={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(getAllToolsMock).toHaveBeenCalledTimes(1);
    });

    const toolOptions = within(screen.getByRole("combobox"))
      .getAllByRole("option")
      .filter((option) => option.textContent === "CSDN / send_article");

    expect(toolOptions).toHaveLength(1);
  });
});
