import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Space, Spin, Empty, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useSwarmWorkspace } from '../hooks/useSwarmWorkspace'
import WorkspaceCard from '../components/workspace/WorkspaceCard'
import CreateWorkspaceModal from '../components/workspace/CreateWorkspaceModal'

export default function SwarmWorkspaceListPage() {
  const navigate = useNavigate()
  const { workspaces, loading, create, remove } = useSwarmWorkspace()
  const [modalOpen, setModalOpen] = useState(false)

  const handleCreate = async (name: string, llmConfigId: number) => {
    const result = await create(name, llmConfigId)
    setModalOpen(false)
    message.success('创建成功')
    navigate(`/swarm/${result.workspaceId}`)
  }

  const handleDelete = async (id: number) => {
    await remove(id)
    message.success('已删除')
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h2 style={{ margin: 0 }}>多 Agent 协作</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
          新建 Workspace
        </Button>
      </div>

      <Spin spinning={loading}>
        {workspaces.length === 0 ? (
          <Empty description="暂无 Workspace，点击上方按钮创建" />
        ) : (
          <Space wrap size={16}>
            {workspaces.map(ws => (
              <WorkspaceCard
                key={ws.id}
                workspace={ws}
                onEnter={(id) => navigate(`/swarm/${id}`)}
                onDelete={handleDelete}
              />
            ))}
          </Space>
        )}
      </Spin>

      <CreateWorkspaceModal
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleCreate}
      />
    </div>
  )
}
