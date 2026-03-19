import { Empty } from "antd";
import ToolCallBadge from "../chat/ToolCallBadge";

interface ToolCall {
  name: string;
  args: string;
  result?: string;
}

interface Props {
  toolCalls: ToolCall[];
}

export default function RealtimeToolsPanel({ toolCalls }: Props) {
  if (toolCalls.length === 0) {
    return (
      <Empty description="暂无工具调用" image={Empty.PRESENTED_IMAGE_SIMPLE} />
    );
  }

  return (
    <div style={{ maxHeight: 300, overflow: "auto" }}>
      {toolCalls.map((tc, i) => (
        <ToolCallBadge key={i} toolName={tc.name} />
      ))}
    </div>
  );
}
