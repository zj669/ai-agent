import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Button, Card, Tag, Space, Row, Col, Empty, Spin, Input,
  Typography, message, Avatar, Popconfirm
} from 'antd'
import {
  PlusOutlined, EditOutlined, DeleteOutlined,
  CloudUploadOutlined, StopOutlined, RobotOutlined, SearchOutlined
} from '@ant-design/icons'
import {
  getAgentList,
  publishAgent,
  offlineAgent,
  deleteAgent,
  createAgent,
  type AgentSummary
} from '../../../shared/api/adapters/agentAdapter'

const { Text } = Typography

const AVATAR_COLORS = [
  '#1677ff', '#52c41a', '#faad14', '#eb2f96',
  '#722ed1', '#13c2c2', '#fa541c', '#2f54eb',
]

const statusMap: Record<string, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: 'default' },
  '0': { label: '草稿', color: 'default' },
  PUBLISHED: { label: '已发布', color: 'green' },
  '1': { label: '已发布', color: 'green' },
  OFFLINE: { label: '已下线', color: 'red' },
  '2': { label: '已下线', color: 'red' },
}

const getStatus = (s: string) => statusMap[s] || { label: s, color: 'default' }

export default function AgentListPage() {
  const navigate = useNavigate()
  const [agents, setAgents] = useState<AgentSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [keyword, setKeyword] = useState('')

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

  const filteredAgents = useMemo(() => {
    if (!keyword.trim()) return agents
    const kw = keyword.trim().toLowerCase()
    return agents.filter(
      a => a.name.toLowerCase().includes(kw) ||
           (a.description && a.description.toLowerCase().includes(kw))
    )
  }, [agents, keyword])

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

  const handleDelete = async (id: number) => {
    try {
      await deleteAgent(id)
      message.success('已删除')
      void loadAgents()
    } catch { message.error('删除失败') }
  }

  return (
    <div style={{ padding: 24 }}>
      {/* Page Header */}
      <div style={{
        display: 'flex', justifyContent: 'space-between',
        alignItems: 'center', marginBottom: 24, flexWrap: 'wrap', gap: 12,
      }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          Agent 管理
        </Typography.Title>
        <Space size="middle" style={{ flex: 1, justifyContent: 'center', maxWidth: 400 }}>
          <Input
            placeholder="搜索 Agent 名称或描述"
            prefix={<SearchOutlined />}
            allowClear
            value={keyword}
            onChange={e => setKeyword(e.target.value)}
            style={{ width: '100%' }}
          />
        </Space>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          loading={creating}
          onClick={() => void handleCreate()}
        >
          新建 Agent
        </Button>
      </div>

      {/* Content */}
      {loading ? (
        <div style={{ textAlign: 'center', paddingTop: 80 }}>
          <Spin size="large" />
        </div>
      ) : filteredAgents.length === 0 ? (
        <Empty
          description="还没有 Agent，点击上方按钮创建"
          style={{ paddingTop: 80 }}
        >
          <Button
            type="primary"
            icon={<PlusOutlined />}
            loading={creating}
            onClick={() => void handleCreate()}
          >
            新建 Agent
          </Button>
        </Empty>
      ) : (
        <Row gutter={[16, 16]}>
          {filteredAgents.map((agent, index) => {
            const st = getStatus(agent.status)
            const isPublished = agent.status === 'PUBLISHED' || agent.status === '1'
            const avatarColor = AVATAR_COLORS[index % AVATAR_COLORS.length]

            return (
              <Col key={agent.id} xs={24} sm={12} md={8} lg={6}>
                <Card
                  hoverable
                  style={{
                    borderRadius: 8,
                    transition: 'box-shadow 0.3s ease',
                  }}
                  styles={{
                    body: { padding: '20px 16px 12px' },
                    actions: { borderTop: '1px solid #f0f0f0' },
                  }}
                  onMouseEnter={e => {
                    (e.currentTarget as HTMLElement).style.boxShadow =
                      '0 6px 20px rgba(0,0,0,0.12)'
                  }}
                  onMouseLeave={e => {
                    (e.currentTarget as HTMLElement).style.boxShadow = ''
                  }}
                  actions={[
                    <Button
                      key="edit"
                      type="text"
                      size="small"
                      icon={<EditOutlined />}
                      onClick={() => navigate(`/agents/${agent.id}/workflow`)}
                    >
                      编辑
                    </Button>,
                    isPublished ? (
                      <Button
                        key="offline"
                        type="text"
                        size="small"
                        icon={<StopOutlined />}
                        onClick={() => void handleOffline(agent.id)}
                      >
                        下线
                      </Button>
                    ) : (
                      <Button
                        key="publish"
                        type="text"
                        size="small"
                        icon={<CloudUploadOutlined />}
                        onClick={() => void handlePublish(agent.id)}
                      >
                        发布
                      </Button>
                    ),
                    <Popconfirm
                      key="delete"
                      title="确认删除"
                      description="删除后不可恢复，确定要删除该 Agent 吗？"
                      onConfirm={(e) => { e?.stopPropagation(); void handleDelete(agent.id) }}
                      okText="删除"
                      okButtonProps={{ danger: true }}
                      cancelText="取消"
                    >
                      <Button
                        type="text"
                        size="small"
                        danger
                        icon={<DeleteOutlined />}
                      >
                        删除
                      </Button>
                    </Popconfirm>,
                  ]}
                >
                  {/* Avatar */}
                  <div style={{ textAlign: 'center', marginBottom: 16 }}>
                    <Avatar
                      size={56}
                      icon={<RobotOutlined />}
                      style={{ backgroundColor: avatarColor }}
                    />
                  </div>

                  {/* Name + Status */}
                  <div style={{
                    textAlign: 'center', marginBottom: 8,
                    display: 'flex', justifyContent: 'center',
                    alignItems: 'center', gap: 8,
                  }}>
                    <Text strong style={{ fontSize: 15 }}>
                      {agent.name}
                    </Text>
                    <Tag color={st.color}>{st.label}</Tag>
                  </div>

                  {/* Description */}
                  <Text
                    type="secondary"
                    style={{
                      display: '-webkit-box',
                      WebkitLineClamp: 2,
                      WebkitBoxOrient: 'vertical',
                      overflow: 'hidden',
                      fontSize: 13,
                      minHeight: 40,
                      textAlign: 'center',
                    }}
                  >
                    {agent.description || '暂无描述'}
                  </Text>

                  {/* Time */}
                  <div style={{
                    color: '#999', fontSize: 12,
                    marginTop: 8, textAlign: 'center',
                  }}>
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
