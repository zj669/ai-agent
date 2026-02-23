import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Col, Row, Button, List, Avatar, Space, Typography } from 'antd'
import {
  RobotOutlined,
  CheckCircleOutlined,
  MessageOutlined,
  AuditOutlined,
  PlusOutlined,
  UploadOutlined,
  EyeOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  SettingOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons'
import { getDashboardStats, type DashboardStats } from '../../../shared/api/adapters/dashboardAdapter'

const { Title, Text } = Typography

interface StatCardConfig {
  title: string
  key: keyof DashboardStats
  icon: React.ReactNode
  gradient: string
  trend?: string
}

const statCards: StatCardConfig[] = [
  {
    title: 'Agent 总数',
    key: 'agentCount',
    icon: <RobotOutlined />,
    gradient: 'linear-gradient(135deg, #1677ff 0%, #4096ff 100%)',
    trend: '+12% ↑',
  },
  {
    title: '已发布',
    key: 'publishedAgentCount',
    icon: <CheckCircleOutlined />,
    gradient: 'linear-gradient(135deg, #52c41a 0%, #73d13d 100%)',
  },
  {
    title: '对话总数',
    key: 'conversationCount',
    icon: <MessageOutlined />,
    gradient: 'linear-gradient(135deg, #722ed1 0%, #9254de 100%)',
  },
  {
    title: '待审核',
    key: 'pendingReviewCount',
    icon: <AuditOutlined />,
    gradient: 'linear-gradient(135deg, #fa8c16 0%, #ffa940 100%)',
  },
]

const recentActivities = [
  {
    icon: <RobotOutlined style={{ color: '#1677ff' }} />,
    description: "Agent 'GPT助手' 已发布",
    time: '2 分钟前',
  },
  {
    icon: <FileTextOutlined style={{ color: '#52c41a' }} />,
    description: "知识库 '产品文档' 上传了 3 个文件",
    time: '15 分钟前',
  },
  {
    icon: <MessageOutlined style={{ color: '#722ed1' }} />,
    description: '对话 #1024 已完成',
    time: '1 小时前',
  },
  {
    icon: <SettingOutlined style={{ color: '#fa8c16' }} />,
    description: "Agent '客服机器人' 配置已更新",
    time: '3 小时前',
  },
  {
    icon: <SafetyCertificateOutlined style={{ color: '#13c2c2' }} />,
    description: '审核项 #89 已通过',
    time: '5 小时前',
  },
]


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
    <div style={{ padding: 24 }}>
      {/* Welcome Banner */}
      <Card
        style={{
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          borderRadius: 12,
          marginBottom: 24,
          border: 'none',
        }}
      >
        <Title level={3} style={{ color: '#fff', margin: 0 }}>
          欢迎回来，管理员
        </Title>
        <Text style={{ color: 'rgba(255, 255, 255, 0.85)', fontSize: 14 }}>
          这是您的 AI Agent 工作台概览
        </Text>
      </Card>

      {/* Stats Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {statCards.map((card) => (
          <Col span={6} key={card.key}>
            <Card
              loading={loading}
              style={{
                background: card.gradient,
                borderRadius: 12,
                border: 'none',
                boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)',
                overflow: 'hidden',
                position: 'relative',
                minHeight: 120,
              }}
              styles={{ body: { padding: '20px 24px', position: 'relative', zIndex: 1 } }}
            >
              <div style={{ position: 'absolute', right: 20, top: '50%', transform: 'translateY(-50%)', fontSize: 64, color: 'rgba(255, 255, 255, 0.2)', zIndex: 0 }}>
                {card.icon}
              </div>
              <Text style={{ color: 'rgba(255, 255, 255, 0.85)', fontSize: 14 }}>{card.title}</Text>
              <div style={{ fontSize: 36, fontWeight: 700, color: '#fff', lineHeight: 1.2, margin: '8px 0 4px' }}>
                {stats?.[card.key] ?? '-'}
              </div>
              {card.trend && (
                <Text style={{ color: 'rgba(255, 255, 255, 0.85)', fontSize: 12 }}>{card.trend}</Text>
              )}
            </Card>
          </Col>
        ))}
      </Row>

      {/* Quick Actions */}
      <Card style={{ borderRadius: 12, marginBottom: 24 }}>
        <Title level={5} style={{ marginTop: 0 }}>快捷操作</Title>
        <Space size="middle">
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/agents')}>
            新建 Agent
          </Button>
          <Button icon={<UploadOutlined />} onClick={() => navigate('/knowledge')}>
            上传文档
          </Button>
          <Button icon={<EyeOutlined />} onClick={() => navigate('/reviews')}>
            查看审核
          </Button>
        </Space>
      </Card>

      {/* Recent Activity */}
      <Card style={{ borderRadius: 12 }}>
        <Title level={5} style={{ marginTop: 0 }}>最近活动</Title>
        <List
          itemLayout="horizontal"
          dataSource={recentActivities}
          renderItem={(item) => (
            <List.Item extra={<Text type="secondary"><ClockCircleOutlined style={{ marginRight: 4 }} />{item.time}</Text>}>
              <List.Item.Meta
                avatar={<Avatar icon={item.icon} style={{ backgroundColor: '#f0f5ff' }} />}
                description={item.description}
              />
            </List.Item>
          )}
        />
      </Card>
    </div>
  )
}

export default DashboardPage