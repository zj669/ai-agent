import { Layout, Menu, Avatar, Dropdown, Breadcrumb, Badge, Space, Typography } from 'antd'
import {
  DashboardOutlined,
  RobotOutlined,
  BookOutlined,
  MessageOutlined,
  AuditOutlined,
  UserOutlined,
  SettingOutlined,
  BellOutlined,
  LogoutOutlined,
} from '@ant-design/icons'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useState } from 'react'

const { Sider, Header, Content } = Layout
const { Text } = Typography

const SIDER_WIDTH = 220
const SIDER_COLLAPSED_WIDTH = 80

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '工作台' },
  { key: '/agents', icon: <RobotOutlined />, label: 'Agent 管理' },
  { key: '/knowledge', icon: <BookOutlined />, label: '知识库' },
  { key: '/chat', icon: <MessageOutlined />, label: '智能对话' },
  { key: '/reviews', icon: <AuditOutlined />, label: '审核中心' },
]

const breadcrumbMap: Record<string, string> = {
  '/dashboard': '工作台',
  '/agents': 'Agent 管理',
  '/knowledge': '知识库',
  '/chat': '智能对话',
  '/reviews': '审核中心',
}

function AppShell() {
  const location = useLocation()
  const navigate = useNavigate()
  const [collapsed, setCollapsed] = useState(false)

  const currentLabel = breadcrumbMap[location.pathname] ?? ''

  const userDropdownItems = {
    items: [
      { key: 'settings', icon: <SettingOutlined />, label: '个人设置' },
      { type: 'divider' as const },
      { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', danger: true },
    ],
    onClick: ({ key }: { key: string }) => {
      if (key === 'logout') {
        navigate('/login')
      }
    },
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {/* Sider */}
      <Sider
        theme="dark"
        width={SIDER_WIDTH}
        collapsedWidth={SIDER_COLLAPSED_WIDTH}
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        breakpoint="lg"
        style={{ background: '#001529' }}
      >
        {/* Logo */}
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start',
            padding: collapsed ? 0 : '0 20px',
            cursor: 'pointer',
            overflow: 'hidden',
            whiteSpace: 'nowrap',
          }}
          onClick={() => navigate('/dashboard')}
        >
          <RobotOutlined
            style={{
              fontSize: 24,
              background: 'linear-gradient(135deg, #1677ff, #722ed1)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              flexShrink: 0,
            }}
          />
          {!collapsed && (
            <span
              style={{
                marginLeft: 12,
                fontSize: 18,
                fontWeight: 700,
                background: 'linear-gradient(135deg, #1677ff, #722ed1)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
              }}
            >
              AI Agent
            </span>
          )}
        </div>

        {/* Navigation Menu */}
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ borderRight: 'none' }}
        />

        {/* Bottom User Area */}
        <div
          style={{
            position: 'absolute',
            bottom: 48,
            left: 0,
            right: 0,
            padding: collapsed ? '12px 0' : '12px 16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start',
            gap: 10,
            borderTop: '1px solid rgba(255,255,255,0.1)',
          }}
        >
          <Avatar size={32} icon={<UserOutlined />} style={{ backgroundColor: '#1677ff', flexShrink: 0 }} />
          {!collapsed && (
            <>
              <Text style={{ color: 'rgba(255,255,255,0.85)', flex: 1 }} ellipsis>
                管理员
              </Text>
              <SettingOutlined
                style={{ color: 'rgba(255,255,255,0.45)', cursor: 'pointer', fontSize: 14 }}
                onClick={() => navigate('/settings')}
              />
            </>
          )}
        </div>
      </Sider>

      {/* Right Layout */}
      <Layout>
        {/* Header */}
        <Header
          style={{
            height: 64,
            background: '#fff',
            padding: '0 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
            zIndex: 1,
          }}
        >
          {/* Left: Breadcrumb */}
          <Breadcrumb
            items={[
              { title: '首页' },
              ...(currentLabel ? [{ title: currentLabel }] : []),
            ]}
          />

          {/* Right: Actions */}
          <Space size={16}>
            <Badge count={0} showZero={false}>
              <BellOutlined style={{ fontSize: 18, cursor: 'pointer', color: '#595959' }} />
            </Badge>
            <Dropdown menu={userDropdownItems} placement="bottomRight" arrow>
              <Space style={{ cursor: 'pointer' }}>
                <Avatar size={28} icon={<UserOutlined />} style={{ backgroundColor: '#1677ff' }} />
                <Text style={{ color: '#595959' }}>管理员</Text>
              </Space>
            </Dropdown>
          </Space>
        </Header>

        {/* Content */}
        <Content style={{ padding: 24, background: '#f5f7fa', overflow: 'auto' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default AppShell
