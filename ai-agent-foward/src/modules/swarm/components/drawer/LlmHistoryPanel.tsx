import { Typography, Empty } from 'antd'

const { Text } = Typography

interface Props {
  llmHistory?: string
}

export default function LlmHistoryPanel({ llmHistory }: Props) {
  if (!llmHistory) {
    return <Empty description="暂无 LLM 历史" image={Empty.PRESENTED_IMAGE_SIMPLE} />
  }

  let parsed: Array<{ role: string; content: string }> = []
  try {
    parsed = JSON.parse(llmHistory)
  } catch {
    return <pre style={{ fontSize: 12, whiteSpace: 'pre-wrap' }}>{llmHistory}</pre>
  }

  return (
    <div style={{ maxHeight: 400, overflow: 'auto' }}>
      {parsed.map((msg, i) => (
        <div key={i} style={{ marginBottom: 8, padding: 8, background: '#fafafa', borderRadius: 4 }}>
          <Text strong style={{ fontSize: 12, color: msg.role === 'assistant' ? '#722ed1' : '#1677ff' }}>
            {msg.role}
          </Text>
          <div style={{ fontSize: 12, whiteSpace: 'pre-wrap', marginTop: 4 }}>
            {msg.content || '(empty)'}
          </div>
        </div>
      ))}
    </div>
  )
}
