import { useEffect, useRef } from 'react'
import { Empty } from 'antd'
import SwarmMessageBubble from './SwarmMessageBubble'
import type { SwarmMessage, SwarmAgent } from '../../types/swarm'

interface Props {
  messages: SwarmMessage[]
  agents: SwarmAgent[]
  humanAgentId?: number
  streamingContent?: string | null
  streamingAgentId?: number | null
}

export default function SwarmMessageList({ messages, agents, humanAgentId, streamingContent, streamingAgentId }: Props) {
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages.length, streamingContent])

  if (messages.length === 0) {
    return <Empty description="暂无消息" style={{ marginTop: 40 }} />
  }

  return (
    <div style={{ flex: 1, overflow: 'auto', padding: '12px 16px' }}>
      {messages.map(msg => (
        <SwarmMessageBubble
          key={msg.id}
          message={msg}
          agents={agents}
          humanAgentId={humanAgentId}
        />
      ))}
      {streamingContent !== null && streamingContent !== undefined && streamingAgentId && (
        <SwarmMessageBubble
          message={{
            id: -1,
            groupId: 0,
            senderId: streamingAgentId,
            content: streamingContent === '' ? '正在思考...' : streamingContent + '▌',
            contentType: streamingContent === '' ? 'thinking' : 'text',
            sendTime: new Date().toISOString(),
          }}
          agents={agents}
          humanAgentId={humanAgentId}
        />
      )}
      <div ref={bottomRef} />
    </div>
  )
}
