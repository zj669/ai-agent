import { Layout, Menu, Avatar, Dropdown, Breadcrumb, Badge, Space, Typography, ConfigProvider } from 'antd'
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
  ClusterOutlined,
} from '@ant-design/icons'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useState } from 'react'

const { Sider, Header, Content } = Layout
const { Text } = Typography

const SIDER_WIDTH = 220
const SIDER_COLLAPSED_WIDTH = 64

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '工作台' },
  { key: '/agents', icon: <RobotOutlined />, label: 'Agent 管理' },
  { key: '/knowledge', icon: <BookOutlined />, label: '知识库' },
  { key: '/chat', icon: <MessageOutlined />, label: '智能对话' },
  { key: '/swarm', icon: <ClusterOutlined />, label: '多Agent协作' },
  { key: '/reviews', icon: <AuditOutlined />, label: '审核中心' },
  { key: '/llm-config', icon: <SettingOutlined />, label: '模型配置' },
]

const breadcrumbMap: Record<string, string> = {
  '/dashboard': '工作台',
  '/agents': 'Agent 管理',
  '/knowledge': '知识库',
  '/chat': '智能对话',
  '/swarm': '多Agent协作',
  '/reviews': '审核中心',
  '/llm-config': '模型配置',
}

/* Light-theme sider menu overrides */
const siderMenuTheme = {
  components: {
    Menu: {
      itemBg: 'transparent',
      itemColor: '#344054',
      itemHoverColor: '#2970FF',
      itemHoverBg: '#EFF4FF',
      itemSelectedColor: '#2970FF',
      itemSelectedBg: '#EFF4FF',
      iconSize: 16,
      itemHeight: 40,
      itemBorderRadius: 8,
      itemMarginInline: 8,
    },
  },
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
      {/* Sider — light theme */}
      <Sider
        theme="light"
        width={SIDER_WIDTH}
        collapsedWidth={SIDER_COLLAPSED_WIDTH}
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        breakpoint="lg"
        style={{
          background: '#fff',
          borderRight: '1px solid #EAECF0',
        }}
      >
        {/* Logo */}
        <div
          style={{
            height: 56,
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start',
            padding: collapsed ? 0 : '0 20px',
            cursor: 'pointer',
            overflow: 'hidden',
            whiteSpace: 'nowrap',
            borderBottom: '1px solid #EAECF0',
          }}
          onClick={() => navigate('/dashboard')}
        >
          <RobotOutlined
            style={{
              fontSize: 22,
              color: '#2970FF',
              flexShrink: 0,
            }}
          />
          {!collapsed && (
            <span
              style={{
                marginLeft: 10,
                fontSize: 16,
                fontWeight: 700,
                color: '#101828',
              }}
            >
              AI Agent
            </span>
          )}
        </div>

        {/* Navigation Menu */}
        <ConfigProvider theme={siderMenuTheme}>
          <Menu
            mode="inline"
            selectedKeys={[location.pathname]}
            items={menuItems}
            onClick={({ key }) => navigate(key)}
            style={{ borderRight: 'none', marginTop: 8 }}
          />
        </ConfigProvider>

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
            borderTop: '1px solid #EAECF0',
          }}
        >
          <Avatar size={32} icon={<UserOutlined />} style={{ backgroundColor: '#2970FF', flexShrink: 0 }} />
          {!collapsed && (
            <>
              <Text style={{ color: '#344054', flex: 1 }} ellipsis>
                管理员
              </Text>
              <SettingOutlined
                style={{ color: '#667085', cursor: 'pointer', fontSize: 14 }}
                onClick={() => navigate('/settings')}
              />
            </>
          )}
        </div>
      </Sider>

      {/* Right Layout */}
      <Layout style={{ background: '#F9FAFB' }}>
        {/* Header */}
        <Header
          style={{
            height: 56,
            background: '#fff',
            padding: '0 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '1px solid #EAECF0',
            lineHeight: '56px',
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
              <BellOutlined style={{ fontSize: 18, cursor: 'pointer', color: '#667085' }} />
            </Badge>
            <Dropdown menu={userDropdownItems} placement="bottomRight" arrow>
              <Space style={{ cursor: 'pointer' }}>
                <Avatar size={28} icon={<UserOutlined />} style={{ backgroundColor: '#2970FF' }} />
                <Text style={{ color: '#344054' }}>管理员</Text>
              </Space>
            </Dropdown>
          </Space>
        </Header>

        {/* Content */}
        <Content style={{ padding: 24, background: '#F9FAFB', overflow: 'auto' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default AppShell
