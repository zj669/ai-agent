import { Layout, Menu } from 'antd'
import {
  DashboardOutlined,
  RobotOutlined,
  BookOutlined,
  MessageOutlined,
  AuditOutlined
} from '@ant-design/icons'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'

const { Header, Sider, Content } = Layout

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '工作台' },
  { key: '/agents', icon: <RobotOutlined />, label: 'Agent' },
  { key: '/knowledge', icon: <BookOutlined />, label: '知识库' },
  { key: '/chat', icon: <MessageOutlined />, label: '聊天' },
  { key: '/reviews', icon: <AuditOutlined />, label: '审核' }
]

function AppShell() {
  const location = useLocation()
  const navigate = useNavigate()

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="light" width={200} style={{ borderRight: '1px solid #f0f0f0' }}>
        <div style={{ padding: '16px 24px', fontWeight: 600, fontSize: 16 }}>
          AI Agent 平台
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ borderRight: 'none' }}
        />
      </Sider>
      <Layout>
        <Content style={{ padding: 24, background: '#fff' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default AppShell
