import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import SwarmSearchBar from './SwarmSearchBar'
import AgentTreeList from './AgentTreeList'
import type { SwarmAgent } from '../../types/swarm'

interface Props {
  agents: SwarmAgent[]
  selectedAgentId: number | null
  onSelectAgent: (agentId: number) => void
  unreadMap?: Record<number, number>
}

export default function SwarmSidebar({ agents, selectedAgentId, onSelectAgent, unreadMap }: Props) {
  const [search, setSearch] = useState('')
  const navigate = useNavigate()

  const filtered = search
    ? agents.filter(a => a.role.toLowerCase().includes(search.toLowerCase()))
    : agents

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', padding: 8 }}>
      <Button
        type="text"
        icon={<ArrowLeftOutlined />}
        onClick={() => navigate('/swarm')}
        style={{ textAlign: 'left', marginBottom: 4, color: '#667085' }}
      >
        返回工作区列表
      </Button>
      <SwarmSearchBar value={search} onChange={setSearch} />
      <div style={{ flex: 1, overflow: 'auto' }}>
        <AgentTreeList
          agents={filtered}
          selectedAgentId={selectedAgentId}
          onSelect={onSelectAgent}
          unreadMap={unreadMap}
        />
      </div>
    </div>
  )
}
