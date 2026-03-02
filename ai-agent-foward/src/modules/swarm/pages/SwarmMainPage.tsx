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
  const [streamingContent, setStreamingContent] = useState<string | null>(null)
  const [streamingAgentId, setStreamingAgentId] = useState<number | null>(null)
  const [waitingForAgent, setWaitingForAgent] = useState<number | null>(null)

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
  const loadGroups = useCallback(() => {
    if (!wid) return
    listGroups(wid).then(setGroups)
  }, [wid])

  useEffect(() => { loadGroups() }, [loadGroups])

  // 选中 agent 时加载对应群消息
  // 现在群是三方群（human + 主agent + 子agent），选中子agent时找到包含该agent的群即可
  useEffect(() => {
    if (!selectedAgentId || !humanAgentId || !wid) return
    // 找到包含 human 和 selected agent 的群（三方群或P2P群都行）
    const group = groups.find(g =>
      g.memberIds?.includes(humanAgentId) && g.memberIds?.includes(selectedAgentId)
    )
    if (group) {
      setSelectedGroupId(group.id)
    }
    // 如果没找到群，不再自动创建P2P群（三方群在后端创建agent时已建好）
  }, [selectedAgentId, humanAgentId, groups, wid])

  // 加载消息
  useEffect(() => { loadMessages() }, [loadMessages])

  // SSE 实时更新
  useUIStream(wid, {
    onAgentCreated: (data) => {
      console.log('[SSE] onAgentCreated fired, reloading agents and groups', data)
      reloadAgents()
      loadGroups()
    },
    onMessageCreated: (data) => {
      try {
        const parsed = JSON.parse(data)
        if (parsed.groupId === selectedGroupId) {
          loadMessages()
        }
      } catch { /* ignore */ }
    },
    onStreamStart: (data) => {
      try {
        const parsed = JSON.parse(data)
        if (parsed.groupId === selectedGroupId) {
          setStreamingContent('')
          setStreamingAgentId(parsed.agentId)
        }
      } catch {}
    },
    onStreamChunk: (data) => {
      try {
        const parsed = JSON.parse(data)
        if (parsed.groupId === selectedGroupId) {
          setStreamingContent(prev => (prev ?? '') + parsed.chunk)
        }
      } catch {}
    },
    onStreamDone: (data) => {
      try {
        const parsed = JSON.parse(data)
        if (parsed.groupId === selectedGroupId) {
          setStreamingContent(null)
          setStreamingAgentId(null)
          loadMessages()
        }
      } catch {}
    },
    onWaiting: (data) => {
      try {
        const parsed = JSON.parse(data)
        if (parsed.groupId === selectedGroupId) {
          setWaitingForAgent(parsed.targetAgentId)
        }
      } catch {}
    },
    onWaitingDone: (data) => {
      try {
        const parsed = JSON.parse(data)
        if (parsed.groupId === selectedGroupId) {
          setWaitingForAgent(null)
        }
      } catch {}
    },
  })

  const handleSend = useCallback(async (content: string) => {
    if (!humanAgentId) return
    await send(humanAgentId, content)
  }, [humanAgentId, send])

  const handleSelectAgent = useCallback((agentId: number) => {
    const agent = agents.find(a => a.id === agentId)
    if (!agent || agent.role === 'human') return
    setSelectedAgentId(agentId)
  }, [agents])

  const selectedAgentBusy = agents.find(a => a.id === selectedAgentId)?.status === 'BUSY'

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
            streamingContent={streamingContent}
            streamingAgentId={streamingAgentId}
            agentBusy={selectedAgentBusy}
            waitingForAgent={waitingForAgent}
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
