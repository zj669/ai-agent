import { useState, useEffect, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import { Layout, Spin } from 'antd'
import SwarmSidebar from '../components/sidebar/SwarmSidebar'
import SwarmChatPanel from '../components/chat/SwarmChatPanel'
import { useSwarmAgents } from '../hooks/useSwarmAgents'
import { useSwarmMessages } from '../hooks/useSwarmMessages'
import { useUIStream } from '../hooks/useUIStream'
import { getWorkspaceDefaults, listGroups, stopAgent } from '../api/swarmService'
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

  const { messages, load: loadMessages, send } = useSwarmMessages(selectedGroupId, humanAgentId ?? undefined)
  const [streamingContent, setStreamingContent] = useState<string | null>(null)
  const [streamingAgentId, setStreamingAgentId] = useState<number | null>(null)
  const [waitingForAgent, setWaitingForAgent] = useState<number | null>(null)

  useEffect(() => {
    if (!wid) return
    getWorkspaceDefaults(wid).then(defaults => {
      setHumanAgentId(defaults.humanAgentId)
      setSelectedAgentId(defaults.assistantAgentId)
      setSelectedGroupId(defaults.defaultGroupId)
    })
  }, [wid])

  const loadGroups = useCallback(() => {
    if (!wid) return
    listGroups(wid).then(setGroups)
  }, [wid])

  useEffect(() => { loadGroups() }, [loadGroups])

  useEffect(() => {
    if (!selectedAgentId || !humanAgentId || !wid) return
    const group = groups.find(g =>
      g.memberIds?.includes(humanAgentId) && g.memberIds?.includes(selectedAgentId)
    )
    if (group) {
      setSelectedGroupId(group.id)
    }
  }, [selectedAgentId, humanAgentId, groups, wid])

  useEffect(() => { loadMessages() }, [loadMessages])

  useUIStream(wid, {
    onAgentCreated: () => {
      reloadAgents()
      loadGroups()
    },
    onMessageCreated: (data) => {
      try {
        const parsed = JSON.parse(data)
        // #region agent log
        console.warn('[DEBUG d9647c] onMessageCreated', { parsedGroupId: parsed.groupId, typeGroupId: typeof parsed.groupId, selectedGroupId, typeSelected: typeof selectedGroupId, match: parsed.groupId === selectedGroupId })
        // #endregion
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
      } catch { /* ignore */ }
    },
    onStreamChunk: (data) => {
      try {
        const parsed = JSON.parse(data)
        if (parsed.groupId === selectedGroupId) {
          setStreamingContent(prev => (prev ?? '') + parsed.chunk)
        }
      } catch { /* ignore */ }
    },
    onStreamDone: (data) => {
      try {
        const parsed = JSON.parse(data)
        // #region agent log
        console.warn('[DEBUG d9647c] onStreamDone', { parsedGroupId: parsed.groupId, selectedGroupId, match: parsed.groupId === selectedGroupId })
        // #endregion
        if (parsed.groupId === selectedGroupId) {
          setStreamingContent(null)
          setStreamingAgentId(null)
          loadMessages()
          reloadAgents()
        }
      } catch { /* ignore */ }
    },
    onWaiting: (data) => {
      try {
        const parsed = JSON.parse(data)
        if (parsed.groupId === selectedGroupId) {
          setWaitingForAgent(parsed.targetAgentId)
        }
      } catch { /* ignore */ }
    },
    onWaitingDone: (data) => {
      try {
        const parsed = JSON.parse(data)
        if (parsed.groupId === selectedGroupId) {
          setWaitingForAgent(null)
        }
      } catch { /* ignore */ }
    },
  })

  const handleSend = useCallback(async (content: string) => {
    if (!humanAgentId || !selectedAgentId) return
    setStreamingContent('')
    setStreamingAgentId(selectedAgentId)
    await send(humanAgentId, content)
  }, [humanAgentId, selectedAgentId, send])

  const handleStop = useCallback(async () => {
    if (!selectedAgentId) return
    // #region agent log
    console.warn('[DEBUG d9647c] handleStop called', { selectedAgentId })
    // #endregion
    try {
      await stopAgent(selectedAgentId)
      // #region agent log
      console.warn('[DEBUG d9647c] stopAgent API succeeded', { selectedAgentId })
      // #endregion
    } catch (err) {
      // #region agent log
      console.warn('[DEBUG d9647c] stopAgent API FAILED', { selectedAgentId, error: String(err) })
      // #endregion
    }
    setStreamingContent(null)
    setStreamingAgentId(null)
    setWaitingForAgent(null)
    loadMessages()
    reloadAgents()
  }, [selectedAgentId, loadMessages, reloadAgents])

  const isStreaming = streamingContent !== null

  const handleSelectAgent = useCallback((agentId: number) => {
    const agent = agents.find(a => a.id === agentId)
    if (!agent || agent.role === 'human') return
    setSelectedAgentId(agentId)
  }, [agents])

  const selectedAgent = agents.find(a => a.id === selectedAgentId)

  if (!wid) return <Spin />

  return (
    <Layout style={{ height: 'calc(100vh - 112px)', background: '#fff' }}>
      <Sider width={260} style={{ background: '#fafafa', borderRight: '1px solid #f0f0f0' }}>
        <SwarmSidebar
          agents={agents}
          selectedAgentId={selectedAgentId}
          onSelectAgent={handleSelectAgent}
        />
      </Sider>
      <Content style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        <SwarmChatPanel
          messages={messages}
          agents={agents}
          humanAgentId={humanAgentId ?? undefined}
          onSend={handleSend}
          onStop={handleStop}
          selectedGroupId={selectedGroupId}
          selectedAgent={selectedAgent}
          streamingContent={streamingContent}
          streamingAgentId={streamingAgentId}
          agentBusy={selectedAgent?.status === 'BUSY'}
          isStreaming={isStreaming}
          waitingForAgent={waitingForAgent}
        />
      </Content>
    </Layout>
  )
}
