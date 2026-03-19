import { Tag, Tooltip } from "antd";
import {
  CheckCircleFilled,
  LoadingOutlined,
  ToolOutlined,
} from "@ant-design/icons";

const TOOL_ICONS: Record<string, string> = {
  create: "🔧",
  createAgent: "🤖",
  executeWorkflow: "⚡",
  send: "📨",
  self: "🪪",
  listAgents: "📋",
  list_agents: "📋",
  send_group_message: "📢",
  list_groups: "👥",
  writing_session: "🧭",
  writing_agent: "🧠",
  writing_task: "📝",
  writing_result: "📚",
  writing_result_by_task: "📚",
  writing_result_by_task_uuid: "🧷",
  writing_draft: "📄",
};

const TOOL_LABELS: Record<string, string> = {
  createAgent: "创建 Agent",
  executeWorkflow: "执行工作流",
  send: "派发消息",
  self: "读取自身",
  listAgents: "查看 Agent",
  writing_session: "记录写作任务",
  writing_agent: "创建协作 Agent",
  writing_task: "拆解写作任务",
  writing_result: "收集任务结果",
  writing_result_by_task: "按任务ID回填结果",
  writing_result_by_task_uuid: "按任务UUID回填结果",
  writing_draft: "汇总阶段草稿",
};

interface Props {
  toolName: string;
  status?: "running" | "done";
  showLabel?: boolean;
}

export default function ToolCallBadge({
  toolName,
  status = "done",
  showLabel = true,
}: Props) {
  const icon = TOOL_ICONS[toolName] ?? "🔧";
  const label = TOOL_LABELS[toolName] ?? toolName;

  return (
    <Tooltip title={`工具调用: ${label}`}>
      <Tag
        icon={
          status === "running" ? (
            <LoadingOutlined spin />
          ) : status === "done" ? (
            <CheckCircleFilled />
          ) : (
            <ToolOutlined />
          )
        }
        color={status === "running" ? "processing" : "success"}
        style={{
          marginInlineEnd: 0,
          borderRadius: 999,
          paddingInline: 10,
          paddingBlock: 4,
          display: "inline-flex",
          alignItems: "center",
          gap: 6,
          boxShadow:
            status === "running"
              ? "0 0 0 4px rgba(22, 119, 255, 0.08)"
              : "none",
        }}
      >
        <span>{icon}</span>
        {showLabel ? <span>{label}</span> : null}
      </Tag>
    </Tooltip>
  );
}
