import { Typography } from 'antd'
import { LoadingOutlined } from '@ant-design/icons'
import ToolCallBadge from './ToolCallBadge'
import type { SwarmMessage, SwarmAgent } from '../../types/swarm'

const { Text } = Typography

const AGENT_COLORS = ['#1677ff', '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16', '#52c41a', '#2f54eb']

interface Props {
  message: SwarmMessage
  agents: SwarmAgent[]
  humanAgentId?: number
}

export default function SwarmMessageBubble({ message, agents, humanAgentId }: Props) {
  const isHuman = message.senderId === humanAgentId
  const sender = agents.find(a => a.id === message.senderId)
  const colorIndex = agents.findIndex(a => a.id === message.senderId)
  const color = AGENT_COLORS[colorIndex % AGENT_COLORS.length]

  const isToolCall = message.contentType === 'tool_call'
  const isThinking = message.contentType === 'thinking'

  let toolData: { tool: string; args: string; result: string } | null = null
  if (isToolCall) {
    try {
      const parsed = JSON.parse(message.content)
      toolData = {
        tool: parsed.tool,
        args: typeof parsed.args === 'string' ? parsed.args : JSON.stringify(parsed.args, null, 2),
        result: typeof parsed.result === 'string' ? parsed.result : JSON.stringify(parsed.result, null, 2),
      }
    } catch { /* fallback to plain text */ }
  }

  return (
    <div style={{
      display: 'flex',
      flexDirection: isHuman ? 'row-reverse' : 'row',
      marginBottom: 12,
      gap: 8,
    }}>
      <div style={{
        width: 32, height: 32, borderRadius: '50%',
        background: isHuman ? '#1677ff' : color,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: '#fff', fontSize: 12, fontWeight: 600, flexShrink: 0,
      }}>
        {isHuman ? '我' : (sender?.role?.charAt(0).toUpperCase() ?? '?')}
      </div>

      <div style={{ maxWidth: '70%' }}>
        {!isHuman && (
          <Text type="secondary" style={{ fontSize: 12, marginBottom: 2, display: 'block' }}>
            {sender?.role ?? `agent_${message.senderId}`}
          </Text>
        )}
        {toolData ? (
          <ToolCallBadge toolName={toolData.tool} args={toolData.args} result={toolData.result} />
        ) : isThinking ? (
          <div style={{
            padding: '8px 12px',
            borderRadius: 8,
            background: '#f0f0f0',
            color: '#8c8c8c',
            display: 'flex',
            alignItems: 'center',
            gap: 8,
          }}>
            <LoadingOutlined style={{ fontSize: 14 }} />
            <span>{message.content}</span>
          </div>
        ) : (
          <div style={{
            padding: '8px 12px',
            borderRadius: 8,
            background: isHuman ? '#1677ff' : '#f0f0f0',
            color: isHuman ? '#fff' : '#000',
            wordBreak: 'break-word',
            whiteSpace: 'pre-wrap',
          }}>
            {message.content}
          </div>
        )}
      </div>
    </div>
  )
}
