import { useState } from 'react'
import { Tag, Typography } from 'antd'
import { ToolOutlined } from '@ant-design/icons'

const { Text } = Typography

const TOOL_ICONS: Record<string, string> = {
  create: '🔧',
  send: '📨',
  self: '🪪',
  list_agents: '📋',
  send_group_message: '📢',
  list_groups: '👥',
}

interface Props {
  toolName: string
  args?: string
  result?: string
}

export default function ToolCallBadge({ toolName, args, result }: Props) {
  const [expanded, setExpanded] = useState(false)
  const icon = TOOL_ICONS[toolName] ?? '🔧'

  return (
    <div style={{ marginBottom: 8 }}>
      <Tag
        icon={<ToolOutlined />}
        color="processing"
        style={{ cursor: 'pointer' }}
        onClick={() => setExpanded(!expanded)}
      >
        {icon} {toolName}
      </Tag>
      {expanded && (
        <div style={{ marginTop: 4, padding: 8, background: '#fafafa', borderRadius: 4, fontSize: 12 }}>
          {args && (
            <div>
              <Text type="secondary">参数：</Text>
              <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{args}</pre>
            </div>
          )}
          {result && (
            <div style={{ marginTop: 4 }}>
              <Text type="secondary">结果：</Text>
              <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{result}</pre>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
