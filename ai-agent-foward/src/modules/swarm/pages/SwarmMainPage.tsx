import { useState, useEffect, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import { Layout, Spin } from 'antd'
import SwarmSidebar from '../components/sidebar/SwarmSidebar'
import SwarmChatPanel from '../components/chat/SwarmChatPanel'
import SwarmGraph from '../components/graph/SwarmGraph'
import AgentDetailDrawer from '../components/drawer/AgentDetailDrawer'
import { useSwarmAgents } from '../hooks/useSwarmAgents'
import { useSwarmMessages } from '../hooks/useSwarmMessages'
import { useUIStream } from '../hooks/useUIStream'
import { getWorkspaceDefaults, listGroups } from '../api/swarmService'
import type { SwarmGroup } from '../types/swarm'

const { Sider, Content } = Layout

export default function SwarmMainPage() {
  const { workspaceId } = useParams<{ workspaceId: string }>()
  const wid = workspaceId ? Number(workspaceId) : null

  const { agents, reload: reloadAgents } = useSwarmAgents(wid)
  const [humanAgentId, setHumanAgentId] = useState<number | null>(null)
  const [selectedAgentId, setSelectedAgentId] = useState<number | null>(null)
  const [selectedGroupId, setSelectedGroupId] = useState<number | null>(null)
  const [groups, setGroups] = useState<SwarmGroup[]>([])
  const [drawerOpen, setDrawerOpen] = useState(false)

  const { messages, load: loadMessages, send } = useSwarmMessages(selectedGroupId, humanAgentId ?? undefined)

  // 加载默认资源
  useEffect(() => {
    if (!wid) return
    getWorkspaceDefaults(wid).then(defaults => {
      setHumanAgentId(defaults.humanAgentId)
      setSelectedAgentId(defaults.assistantAgentId)
      setSelectedGroupId(defaults.defaultGroupId)
    })
  }, [wid])

  // 加载群组
  useEffect(() => {
    if (!wid) return
    listGroups(wid).then(setGroups)
  }, [wid])

  // 选中 agent 时加载对应群消息
  useEffect(() => {
    if (!selectedAgentId || !humanAgentId) return
    // 找到 human 和 selected agent 的 P2P 群
    const group = groups.find(g =>
      g.memberIds?.includes(humanAgentId) && g.memberIds?.includes(selectedAgentId) && g.memberIds.length === 2
    )
    if (group) {
      setSelectedGroupId(group.id)
    }
  }, [selectedAgentId, humanAgentId, groups])

  // 加载消息
  useEffect(() => { loadMessages() }, [loadMessages])

  // SSE 实时更新
  useUIStream(wid, {
    onAgentCreated: () => reloadAgents(),
    onMessageCreated: (data) => {
      try {
        const parsed = JSON.parse(data)
        if (parsed.groupId === selectedGroupId) {
          loadMessages()
        }
      } catch { /* ignore */ }
    },
  })

  const handleSend = useCallback(async (content: string) => {
    if (!humanAgentId) return
    await send(humanAgentId, content)
  }, [humanAgentId, send])

  const handleSelectAgent = useCallback((agentId: number) => {
    setSelectedAgentId(agentId)
    setDrawerOpen(true)
  }, [])

  if (!wid) return <Spin />

  return (
    <Layout style={{ height: 'calc(100vh - 112px)', background: '#fff' }}>
      <Sider width={240} style={{ background: '#fafafa', borderRight: '1px solid #f0f0f0' }}>
        <SwarmSidebar
          agents={agents}
          selectedAgentId={selectedAgentId}
          onSelectAgent={handleSelectAgent}
        />
      </Sider>
      <Layout>
        {/* Graph 区域 */}
        <div style={{ height: '40%', borderBottom: '1px solid #f0f0f0' }}>
          <SwarmGraph agents={agents} onNodeClick={handleSelectAgent} />
        </div>
        {/* Chat 区域 */}
        <Content style={{ height: '60%' }}>
          <SwarmChatPanel
            messages={messages}
            agents={agents}
            humanAgentId={humanAgentId ?? undefined}
            onSend={handleSend}
            selectedGroupId={selectedGroupId}
          />
        </Content>
      </Layout>

      <AgentDetailDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        agentId={selectedAgentId}
        agentRole={agents.find(a => a.id === selectedAgentId)?.role}
      />
    </Layout>
  )
}
