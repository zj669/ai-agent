import { Card, Button, Space, Typography, Popconfirm } from 'antd'
import { TeamOutlined, DeleteOutlined, ArrowRightOutlined } from '@ant-design/icons'
import type { SwarmWorkspace } from '../../types/swarm'

const { Text } = Typography

interface Props {
  workspace: SwarmWorkspace
  onEnter: (id: number) => void
  onDelete: (id: number) => void
}

export default function WorkspaceCard({ workspace, onEnter, onDelete }: Props) {
  return (
    <Card
      hoverable
      style={{ width: 320 }}
      actions={[
        <Button type="link" icon={<ArrowRightOutlined />} onClick={() => onEnter(workspace.id)}>
          进入
        </Button>,
        <Popconfirm title="确认删除？" onConfirm={() => onDelete(workspace.id)}>
          <Button type="link" danger icon={<DeleteOutlined />}>删除</Button>
        </Popconfirm>,
      ]}
    >
      <Card.Meta
        avatar={<TeamOutlined style={{ fontSize: 24, color: '#1677ff' }} />}
        title={workspace.name}
        description={
          <Space direction="vertical" size={2}>
            <Text type="secondary">{workspace.agentCount} 个 Agent</Text>
            <Text type="secondary">创建于 {workspace.createdAt?.split('T')[0]}</Text>
          </Space>
        }
      />
    </Card>
  )
}
