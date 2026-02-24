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
  color: string
  bgColor: string
}

const statCards: StatCardConfig[] = [
  {
    title: 'Agent 总数',
    key: 'agentCount',
    icon: <RobotOutlined />,
    color: '#2970FF',
    bgColor: '#EFF4FF',
  },
  {
    title: '已发布',
    key: 'publishedAgentCount',
    icon: <CheckCircleOutlined />,
    color: '#12B76A',
    bgColor: '#ECFDF3',
  },
  {
    title: '对话总数',
    key: 'conversationCount',
    icon: <MessageOutlined />,
    color: '#7A5AF8',
    bgColor: '#F4F3FF',
  },
  {
    title: '待审核',
    key: 'pendingReviewCount',
    icon: <AuditOutlined />,
    color: '#F79009',
    bgColor: '#FFFAEB',
  },
]

const recentActivities = [
  {
    icon: <RobotOutlined style={{ color: '#2970FF' }} />,
    description: "Agent 'GPT助手' 已发布",
    time: '2 分钟前',
  },
  {
    icon: <FileTextOutlined style={{ color: '#12B76A' }} />,
    description: "知识库 '产品文档' 上传了 3 个文件",
    time: '15 分钟前',
  },
  {
    icon: <MessageOutlined style={{ color: '#7A5AF8' }} />,
    description: '对话 #1024 已完成',
    time: '1 小时前',
  },
  {
    icon: <SettingOutlined style={{ color: '#F79009' }} />,
    description: "Agent '客服机器人' 配置已更新",
    time: '3 小时前',
  },
  {
    icon: <SafetyCertificateOutlined style={{ color: '#2970FF' }} />,
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
    <div>
      {/* Welcome Banner */}
      <Card
        style={{
          background: '#fff',
          borderRadius: 12,
          marginBottom: 24,
          border: '1px solid #EAECF0',
        }}
      >
        <Title level={4} style={{ color: '#101828', margin: 0 }}>
          欢迎回来，管理员
        </Title>
        <Text style={{ color: '#667085', fontSize: 14 }}>
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
                background: '#fff',
                borderRadius: 12,
                border: '1px solid #EAECF0',
                minHeight: 120,
              }}
              styles={{ body: { padding: '20px 24px' } }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div>
                  <Text style={{ color: '#667085', fontSize: 14 }}>{card.title}</Text>
                  <div style={{ fontSize: 32, fontWeight: 600, color: '#101828', lineHeight: 1.2, marginTop: 8 }}>
                    {stats?.[card.key] ?? '-'}
                  </div>
                </div>
                <div style={{
                  width: 48, height: 48, borderRadius: 12,
                  background: card.bgColor,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 22, color: card.color,
                }}>
                  {card.icon}
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      {/* Quick Actions */}
      <Card style={{ borderRadius: 12, marginBottom: 24, border: '1px solid #EAECF0' }}>
        <Title level={5} style={{ marginTop: 0, color: '#101828' }}>快捷操作</Title>
        <Space size="middle">
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/agents')}
            style={{ background: '#2970FF', borderColor: '#2970FF' }}>
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
      <Card style={{ borderRadius: 12, border: '1px solid #EAECF0' }}>
        <Title level={5} style={{ marginTop: 0, color: '#101828' }}>最近活动</Title>
        <List
          itemLayout="horizontal"
          dataSource={recentActivities}
          renderItem={(item) => (
            <List.Item extra={<Text style={{ color: '#667085' }}><ClockCircleOutlined style={{ marginRight: 4 }} />{item.time}</Text>}>
              <List.Item.Meta
                avatar={<Avatar icon={item.icon} style={{ backgroundColor: '#F2F4F7' }} />}
                description={<Text style={{ color: '#344054' }}>{item.description}</Text>}
              />
            </List.Item>
          )}
        />
      </Card>
    </div>
  )
}

export default DashboardPage