import { Drawer, Tabs } from 'antd'
import LlmHistoryPanel from './LlmHistoryPanel'
import RealtimeContentPanel from './RealtimeContentPanel'
import RealtimeToolsPanel from './RealtimeToolsPanel'
import { useAgentStream } from '../../hooks/useAgentStream'

interface Props {
  open: boolean
  onClose: () => void
  agentId: number | null
  agentRole?: string
  llmHistory?: string
}

export default function AgentDetailDrawer({ open, onClose, agentId, agentRole, llmHistory }: Props) {
  const stream = useAgentStream(open ? agentId : null)

  const items = [
    {
      key: 'content',
      label: '实时输出',
      children: <RealtimeContentPanel content={stream.content} status={stream.status} />,
    },
    {
      key: 'tools',
      label: '工具调用',
      children: <RealtimeToolsPanel toolCalls={stream.toolCalls} />,
    },
    {
      key: 'history',
      label: 'LLM History',
      children: <LlmHistoryPanel llmHistory={llmHistory} />,
    },
  ]

  return (
    <Drawer
      title={`Agent 详情 — ${agentRole ?? 'unknown'}`}
      open={open}
      onClose={onClose}
      width={400}
      destroyOnClose
    >
      <Tabs items={items} defaultActiveKey="content" size="small" />
    </Drawer>
  )
}
