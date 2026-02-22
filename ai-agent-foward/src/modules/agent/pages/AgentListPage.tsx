import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Card, Tag, Space, Modal, message, Empty, Spin, Row, Col } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, CloudUploadOutlined, StopOutlined } from '@ant-design/icons'
import {
  getAgentList,
  publishAgent,
  offlineAgent,
  deleteAgent,
  createAgent,
  type AgentSummary
} from '../../../shared/api/adapters/agentAdapter'

const statusMap: Record<string, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: 'default' },
  '0': { label: '草稿', color: 'default' },
  PUBLISHED: { label: '已发布', color: 'green' },
  '1': { label: '已发布', color: 'green' },
  OFFLINE: { label: '已下线', color: 'red' },
  '2': { label: '已下线', color: 'red' }
}

function AgentListPage() {
  const navigate = useNavigate()
  const [agents, setAgents] = useState<AgentSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)

  const loadAgents = async () => {
    setLoading(true)
    try {
      const data = await getAgentList()
      setAgents(data)
    } catch {
      message.error('列表加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void loadAgents() }, [])

  const handleCreate = async () => {
    if (creating) return
    setCreating(true)
    try {
      const id = await createAgent({ name: '未命名 Agent' })
      navigate(`/agents/${id}/workflow`)
    } catch {
      message.error('创建失败')
    } finally {
      setCreating(false)
    }
  }

  const handlePublish = async (id: number) => {
    try {
      await publishAgent({ id })
      message.success('发布成功')
      void loadAgents()
    } catch { message.error('发布失败') }
  }

  const handleOffline = async (id: number) => {
    try {
      await offlineAgent(id)
      message.success('已下线')
      void loadAgents()
    } catch { message.error('下线失败') }
  }

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '删除后不可恢复，确定要删除该 Agent 吗？',
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await deleteAgent(id)
          message.success('已删除')
          void loadAgents()
        } catch { message.error('删除失败') }
      }
    })
  }

  const getStatus = (s: string) => statusMap[s] || { label: s, color: 'default' }

  if (loading) return <Spin style={{ display: 'block', marginTop: 48 }} />

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h2 style={{ fontSize: 20, fontWeight: 600, margin: 0 }}>Agent 列表</h2>
        <Button type="primary" icon={<PlusOutlined />} loading={creating} onClick={() => void handleCreate()}>
          新建 Agent
        </Button>
      </div>

      {agents.length === 0 ? <Empty description="暂无 Agent" /> : (
        <Row gutter={[16, 16]}>
          {agents.map((agent) => {
            const st = getStatus(agent.status)
            const isPublished = agent.status === 'PUBLISHED' || agent.status === '1'
            return (
              <Col key={agent.id} xs={24} sm={12} lg={8}>
                <Card
                  hoverable
                  title={<Space>{agent.name}<Tag color={st.color}>{st.label}</Tag></Space>}
                  extra={
                    <Button size="small" icon={<EditOutlined />} onClick={() => navigate(`/agents/${agent.id}/workflow`)}>
                      编辑
                    </Button>
                  }
                  actions={[
                    isPublished
                      ? <Button type="text" size="small" icon={<StopOutlined />} onClick={() => void handleOffline(agent.id)}>下线</Button>
                      : <Button type="text" size="small" icon={<CloudUploadOutlined />} onClick={() => void handlePublish(agent.id)}>发布</Button>,
                    <Button type="text" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(agent.id)}>删除</Button>
                  ]}
                >
                  <div style={{ color: '#666', fontSize: 13, minHeight: 40 }}>
                    {agent.description || '暂无描述'}
                  </div>
                  <div style={{ color: '#999', fontSize: 12, marginTop: 8 }}>
                    更新于 {agent.updateTime}
                  </div>
                </Card>
              </Col>
            )
          })}
        </Row>
      )}
    </div>
  )
}

export default AgentListPage
