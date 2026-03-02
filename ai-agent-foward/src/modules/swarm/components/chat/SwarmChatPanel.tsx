import SwarmMessageList from './SwarmMessageList'
import SwarmComposer from './SwarmComposer'
import WaitingCard from './WaitingCard'
import type { SwarmMessage, SwarmAgent } from '../../types/swarm'

interface Props {
  messages: SwarmMessage[]
  agents: SwarmAgent[]
  humanAgentId?: number
  onSend: (content: string) => Promise<void>
  selectedGroupId: number | null
  streamingContent?: string | null
  streamingAgentId?: number | null
  agentBusy?: boolean
  waitingForAgent?: number | null
}

export default function SwarmChatPanel({ messages, agents, humanAgentId, onSend, selectedGroupId, streamingContent, streamingAgentId, agentBusy, waitingForAgent }: Props) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <SwarmMessageList messages={messages} agents={agents} humanAgentId={humanAgentId} streamingContent={streamingContent} streamingAgentId={streamingAgentId} />
      {waitingForAgent && (
        <div style={{ padding: '0 16px' }}>
          <WaitingCard targetAgentId={waitingForAgent} agents={agents} />
        </div>
      )}
      <div style={{ padding: '8px 16px', borderTop: '1px solid #f0f0f0' }}>
        <SwarmComposer onSend={onSend} disabled={!selectedGroupId || agentBusy} />
      </div>
    </div>
  )
}
