import { Tag, Typography } from 'antd'

const { Text } = Typography

interface Props {
  content: string
  status: 'idle' | 'streaming' | 'done' | 'error'
}

export default function RealtimeContentPanel({ content, status }: Props) {
  return (
    <div>
      <div style={{ marginBottom: 8 }}>
        <Tag color={status === 'streaming' ? 'processing' : status === 'done' ? 'success' : status === 'error' ? 'error' : 'default'}>
          {status}
        </Tag>
      </div>
      <div style={{
        padding: 8, background: '#fafafa', borderRadius: 4,
        minHeight: 60, maxHeight: 300, overflow: 'auto',
        fontSize: 13, whiteSpace: 'pre-wrap',
      }}>
        {content || <Text type="secondary">等待 Agent 输出...</Text>}
        {status === 'streaming' && <span className="cursor-blink">▊</span>}
      </div>
      <style>{`
        .cursor-blink { animation: blink 1s step-end infinite; }
        @keyframes blink { 50% { opacity: 0; } }
      `}</style>
    </div>
  )
}
