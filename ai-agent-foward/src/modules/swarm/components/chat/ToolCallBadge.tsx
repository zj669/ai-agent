import { Tag, Tooltip } from "antd";
import {
  CheckCircleFilled,
  LoadingOutlined,
} from "@ant-design/icons";

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
  const label = TOOL_LABELS[toolName] ?? toolName;

  return (
    <Tooltip title={`工具调用: ${label}`}>
      <Tag
        icon={
          status === "running" ? (
            <LoadingOutlined style={{ fontSize: 11 }} />
          ) : (
            <CheckCircleFilled style={{ fontSize: 11, color: "#52c41a" }} />
          )
        }
        color={status === "running" ? "processing" : "default"}
        style={{
          marginInlineEnd: 0,
          borderRadius: 999,
          paddingInline: 8,
          paddingBlock: 2,
          display: "inline-flex",
          alignItems: "center",
          gap: 4,
          fontSize: 12,
          border: "1px solid",
          borderColor: status === "running" ? "#91caff" : "#d9d9d9",
          background: status === "running" ? "#e6f4ff" : "#fafafa",
          color: status === "running" ? "#1677ff" : "#595959",
        }}
      >
        {showLabel ? <span>{label}</span> : null}
      </Tag>
    </Tooltip>
  );
}
