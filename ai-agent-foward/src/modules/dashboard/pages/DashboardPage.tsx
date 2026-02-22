import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Col, Row, Statistic, Button, Space, Typography } from 'antd'
import {
  RobotOutlined,
  CloudUploadOutlined,
  MessageOutlined,
  ThunderboltOutlined,
  AuditOutlined,
  PlusOutlined
} from '@ant-design/icons'
import { getDashboardStats, type DashboardStats } from '../../../shared/api/adapters/dashboardAdapter'

const { Title } = Typography

function DashboardPage() {
  const navigate = useNavigate()
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    void getDashboardStats()
      .then((data) => setStats(data))
      .catch(() => setStats(null))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <Title level={4} style={{ margin: 0 }}>工作台</Title>
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/agents')}>
            新建 Agent
          </Button>
        </Space>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={8}>
          <Card hoverable onClick={() => navigate('/agents')}>
            <Statistic
              title="Agent 总数"
              value={stats?.agentCount ?? '-'}
              prefix={<RobotOutlined />}
              loading={loading}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card hoverable onClick={() => navigate('/agents')}>
            <Statistic
              title="已发布"
              value={stats?.publishedAgentCount ?? '-'}
              prefix={<CloudUploadOutlined />}
              loading={loading}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card hoverable onClick={() => navigate('/chat')}>
            <Statistic
              title="会话数"
              value={stats?.conversationCount ?? '-'}
              prefix={<MessageOutlined />}
              loading={loading}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="执行次数"
              value={stats?.executionCount ?? '-'}
              prefix={<ThunderboltOutlined />}
              loading={loading}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card hoverable onClick={() => navigate('/reviews')}>
            <Statistic
              title="待审核"
              value={stats?.pendingReviewCount ?? '-'}
              prefix={<AuditOutlined />}
              loading={loading}
              valueStyle={stats?.pendingReviewCount ? { color: '#faad14' } : undefined}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default DashboardPage
