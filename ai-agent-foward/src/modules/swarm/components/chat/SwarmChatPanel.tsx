import SwarmMessageList from './SwarmMessageList'
import SwarmComposer from './SwarmComposer'
import type { SwarmMessage, SwarmAgent } from '../../types/swarm'

interface Props {
  messages: SwarmMessage[]
  agents: SwarmAgent[]
  humanAgentId?: number
  onSend: (content: string) => Promise<void>
  selectedGroupId: number | null
}

export default function SwarmChatPanel({ messages, agents, humanAgentId, onSend, selectedGroupId }: Props) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <SwarmMessageList messages={messages} agents={agents} humanAgentId={humanAgentId} />
      <div style={{ padding: '8px 16px', borderTop: '1px solid #f0f0f0' }}>
        <SwarmComposer onSend={onSend} disabled={!selectedGroupId} />
      </div>
    </div>
  )
}
