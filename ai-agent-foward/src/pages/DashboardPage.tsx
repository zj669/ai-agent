import { Card, Row, Col, Statistic, Button, Space, Progress, List, Tag } from 'antd';
import {
  RobotOutlined,
  MessageOutlined,
  BookOutlined,
  ApartmentOutlined,
  ReloadOutlined,
  PlusOutlined,
  RocketOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useDashboard } from '../hooks/useDashboard';

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { stats, loading, refresh } = useDashboard();

  const successRate = stats.totalExecutions > 0
    ? Math.round((stats.successfulExecutions / stats.totalExecutions) * 100)
    : 0;

  const quickActions = [
    {
      title: '创建 Agent',
      icon: <RobotOutlined />,
      onClick: () => navigate('/agents/create'),
      type: 'primary' as const
    },
    {
      title: '开始对话',
      icon: <MessageOutlined />,
      onClick: () => navigate('/chat')
    },
    {
      title: '上传文档',
      icon: <BookOutlined />,
      onClick: () => navigate('/knowledge')
    },
    {
      title: '查看工作流',
      icon: <ApartmentOutlined />,
      onClick: () => navigate('/workflows')
    }
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 style={{ margin: 0 }}>欢迎使用 AI Agent Platform</h1>
        <Button
          icon={<ReloadOutlined />}
          onClick={refresh}
          loading={loading}
        >
          刷新
        </Button>
      </div>

      {/* Statistics Cards */}
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="Agent 总数"
              value={stats.agentCount}
              prefix={<RobotOutlined style={{ color: '#1890ff' }} />}
              suffix={
                <Button
                  type="link"
                  size="small"
                  onClick={() => navigate('/agents')}
                >
                  查看
                </Button>
              }
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="工作流执行"
              value={stats.totalExecutions}
              prefix={<ApartmentOutlined style={{ color: '#52c41a' }} />}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="对话总数"
              value={stats.conversationCount}
              prefix={<MessageOutlined style={{ color: '#faad14' }} />}
              suffix={
                <Button
                  type="link"
                  size="small"
                  onClick={() => navigate('/chat')}
                >
                  查看
                </Button>
              }
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="知识库总数"
              value={stats.knowledgeDatasetCount}
              prefix={<BookOutlined style={{ color: '#722ed1' }} />}
              suffix={
                <Button
                  type="link"
                  size="small"
                  onClick={() => navigate('/knowledge')}
                >
                  查看
                </Button>
              }
            />
          </Card>
        </Col>
      </Row>

      {/* Performance Metrics */}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <Card title="执行统计" loading={loading}>
            <Space direction="vertical" style={{ width: '100%' }} size="large">
              <div>
                <div style={{ marginBottom: 8 }}>
                  <span>成功率</span>
                  <span style={{ float: 'right', fontWeight: 'bold' }}>
                    {successRate}%
                  </span>
                </div>
                <Progress
                  percent={successRate}
                  status={successRate >= 80 ? 'success' : successRate >= 50 ? 'normal' : 'exception'}
                  strokeColor={{
                    '0%': '#108ee9',
                    '100%': '#87d068',
                  }}
                />
              </div>

              <Row gutter={16}>
                <Col span={8}>
                  <Statistic
                    title="成功"
                    value={stats.successfulExecutions}
                    prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
                    valueStyle={{ fontSize: 20 }}
                  />
                </Col>
                <Col span={8}>
                  <Statistic
                    title="失败"
                    value={stats.failedExecutions}
                    prefix={<CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
                    valueStyle={{ fontSize: 20 }}
                  />
                </Col>
                <Col span={8}>
                  <Statistic
                    title="平均响应"
                    value={stats.avgResponseTime}
                    suffix="ms"
                    prefix={<ClockCircleOutlined style={{ color: '#1890ff' }} />}
                    valueStyle={{ fontSize: 20 }}
                  />
                </Col>
              </Row>
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card title="快速操作">
            <List
              grid={{ gutter: 16, xs: 1, sm: 2, md: 2, lg: 2, xl: 2, xxl: 2 }}
              dataSource={quickActions}
              renderItem={(item) => (
                <List.Item>
                  <Button
                    type={item.type || 'default'}
                    icon={item.icon}
                    onClick={item.onClick}
                    block
                    size="large"
                  >
                    {item.title}
                  </Button>
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>

      {/* Getting Started */}
      <Card title="快速开始" style={{ marginTop: 16 }}>
        <List
          dataSource={[
            {
              title: '1. 创建 Agent',
              description: '创建你的第一个 AI Agent，配置模型和参数',
              action: () => navigate('/agents/create')
            },
            {
              title: '2. 上传知识库',
              description: '上传文档到知识库，让 Agent 具备专业知识',
              action: () => navigate('/knowledge')
            },
            {
              title: '3. 开始对话',
              description: '与 Agent 对话，体验 AI 助手的强大功能',
              action: () => navigate('/chat')
            }
          ]}
          renderItem={(item) => (
            <List.Item
              actions={[
                <Button
                  type="link"
                  icon={<RocketOutlined />}
                  onClick={item.action}
                >
                  开始
                </Button>
              ]}
            >
              <List.Item.Meta
                title={item.title}
                description={item.description}
              />
            </List.Item>
          )}
        />
      </Card>
    </div>
  );
};
