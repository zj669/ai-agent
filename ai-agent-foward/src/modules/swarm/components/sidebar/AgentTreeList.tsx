import { Tree, Badge, Typography } from 'antd'
import { UserOutlined, RobotOutlined } from '@ant-design/icons'
import type { SwarmAgent } from '../../types/swarm'
import type { DataNode } from 'antd/es/tree'

const { Text } = Typography

interface Props {
  agents: SwarmAgent[]
  selectedAgentId: number | null
  onSelect: (agentId: number) => void
  unreadMap?: Record<number, number>
}

function buildTree(agents: SwarmAgent[], unreadMap: Record<number, number>): DataNode[] {
  const map = new Map<number, SwarmAgent>()
  agents.forEach(a => map.set(a.id, a))

  const childrenOf = (parentId: number | null): DataNode[] => {
    return agents
      .filter(a => (a.parentId ?? null) === parentId)
      .map(a => {
        const isHuman = a.role === 'human'
        const unread = unreadMap[a.id] ?? 0
        return {
          key: a.id,
          title: (
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              {isHuman ? <UserOutlined /> : <RobotOutlined />}
              <Text ellipsis style={{ maxWidth: 120 }}>{a.role}</Text>
              {unread > 0 && <Badge count={unread} size="small" />}
            </span>
          ),
          children: childrenOf(a.id),
        }
      })
  }

  // 找根节点（没有 parentId 的）
  const roots = agents.filter(a => !a.parentId)
  if (roots.length === 0) return []

  return roots.map(root => ({
    key: root.id,
    title: (
      <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        {root.role === 'human' ? <UserOutlined /> : <RobotOutlined />}
        <Text ellipsis style={{ maxWidth: 120 }}>{root.role}</Text>
      </span>
    ),
    children: childrenOf(root.id),
  }))
}

export default function AgentTreeList({ agents, selectedAgentId, onSelect, unreadMap = {} }: Props) {
  const treeData = buildTree(agents, unreadMap)

  return (
    <Tree
      treeData={treeData}
      selectedKeys={selectedAgentId ? [selectedAgentId] : []}
      onSelect={(keys) => {
        if (keys.length > 0) onSelect(Number(keys[0]))
      }}
      defaultExpandAll
      blockNode
      style={{ background: 'transparent' }}
    />
  )
}
