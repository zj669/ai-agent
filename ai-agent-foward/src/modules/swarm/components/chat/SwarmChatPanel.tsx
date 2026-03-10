import { RobotOutlined } from '@ant-design/icons'
import { Tag, Typography } from 'antd'
import SwarmMessageList from './SwarmMessageList'
import SwarmComposer from './SwarmComposer'
import WaitingCard from './WaitingCard'
import type { SwarmMessage, SwarmAgent, AgentStatus } from '../../types/swarm'

const { Text } = Typography

const STATUS_TAG: Record<AgentStatus, { color: string; label: string }> = {
  IDLE: { color: 'green', label: '空闲' },
  BUSY: { color: 'red', label: '忙碌' },
  WAKING: { color: 'orange', label: '唤醒中' },
  STOPPED: { color: 'default', label: '已停止' },
}

interface Props {
  messages: SwarmMessage[]
  agents: SwarmAgent[]
  humanAgentId?: number
  onSend: (content: string) => Promise<void>
  onStop?: () => Promise<void>
  selectedGroupId: number | null
  selectedAgent?: SwarmAgent
  streamingContent?: string | null
  streamingAgentId?: number | null
  agentBusy?: boolean
  isStreaming?: boolean
  waitingForAgent?: number | null
}

export default function SwarmChatPanel({
  messages, agents, humanAgentId, onSend, onStop, selectedGroupId,
  selectedAgent, streamingContent, streamingAgentId, agentBusy, isStreaming, waitingForAgent,
}: Props) {
  const statusInfo = selectedAgent ? STATUS_TAG[selectedAgent.status] : null

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {selectedAgent && (
        <div style={{
          display: 'flex', alignItems: 'center', gap: 10,
          padding: '10px 16px',
          borderBottom: '1px solid #f0f0f0',
          background: '#fafafa',
        }}>
          <RobotOutlined style={{ fontSize: 18, color: '#722ed1' }} />
          <div style={{ flex: 1, minWidth: 0 }}>
            <Text strong style={{ fontSize: 14 }}>{selectedAgent.role}</Text>
            {selectedAgent.description && (
              <Text type="secondary" style={{ fontSize: 12, marginLeft: 8 }}>{selectedAgent.description}</Text>
            )}
          </div>
          {statusInfo && <Tag color={statusInfo.color}>{statusInfo.label}</Tag>}
        </div>
      )}

      <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        {messages.length === 0 && streamingContent === null ? (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: '#bfbfbf' }}>
            向 {selectedAgent?.role ?? 'Agent'} 发送消息开始对话
          </div>
        ) : (
          <SwarmMessageList
            messages={messages}
            agents={agents}
            humanAgentId={humanAgentId}
            streamingContent={streamingContent}
            streamingAgentId={streamingAgentId}
          />
        )}
      </div>

      {waitingForAgent && (
        <div style={{ padding: '0 16px' }}>
          <WaitingCard targetAgentId={waitingForAgent} agents={agents} />
        </div>
      )}

      <div style={{ padding: '8px 16px', borderTop: '1px solid #f0f0f0' }}>
        <SwarmComposer
          onSend={onSend}
          onStop={onStop}
          disabled={!selectedGroupId || agentBusy}
          isStreaming={isStreaming}
          agentName={selectedAgent?.role}
        />
      </div>
    </div>
  )
}
