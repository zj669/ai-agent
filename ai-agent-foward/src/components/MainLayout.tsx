import { Layout, Menu, Avatar, Dropdown, Space } from 'antd';
import {
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
  DashboardOutlined,
  RobotOutlined,
  MessageOutlined,
  BookOutlined,
  ApartmentOutlined,
  AuditOutlined
} from '@ant-design/icons';
import { useNavigate, useLocation, Outlet } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { TokenExpirationWarning } from './TokenExpirationWarning';
import type { MenuProps } from 'antd';

const { Header, Sider, Content } = Layout;

export const MainLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuthStore();
  const isWorkflowEditor =
    location.pathname.startsWith('/workflows/') && location.pathname !== '/workflows';

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人信息',
      onClick: () => navigate('/profile')
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: '设置',
      onClick: () => navigate('/settings')
    },
    {
      type: 'divider'
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout
    }
  ];

  const sidebarMenuItems: MenuProps['items'] = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: '看板',
      onClick: () => navigate('/dashboard')
    },
    {
      key: '/agents',
      icon: <RobotOutlined />,
      label: 'Agent 管理',
      onClick: () => navigate('/agents')
    },
    {
      key: '/workflows',
      icon: <ApartmentOutlined />,
      label: '工作流',
      onClick: () => navigate('/workflows')
    },
    {
      key: '/chat',
      icon: <MessageOutlined />,
      label: '对话',
      onClick: () => navigate('/chat')
    },
    {
      key: '/knowledge',
      icon: <BookOutlined />,
      label: '知识库',
      onClick: () => navigate('/knowledge')
    },
    {
      key: '/human-review',
      icon: <AuditOutlined />,
      label: '人工审核',
      onClick: () => navigate('/human-review')
    }
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <TokenExpirationWarning />

      <Header style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        background: '#fff',
        padding: '0 24px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <h1 style={{ margin: 0, fontSize: '20px', fontWeight: 'bold' }}>
            AI Agent Platform
          </h1>
        </div>

        <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
          <Space style={{ cursor: 'pointer' }}>
            <Avatar
              src={user?.avatarUrl}
              icon={!user?.avatarUrl && <UserOutlined />}
            />
            <span>{user?.username || user?.email}</span>
          </Space>
        </Dropdown>
      </Header>

      <Layout>
        <Sider
          width={200}
          style={{ background: '#fff' }}
          breakpoint="lg"
          collapsedWidth="0"
        >
          <Menu
            mode="inline"
            selectedKeys={[location.pathname]}
            items={sidebarMenuItems}
            style={{ height: '100%', borderRight: 0 }}
          />
        </Sider>

        <Layout style={{ padding: isWorkflowEditor ? 0 : '24px' }}>
          <Content
            style={{
              background: isWorkflowEditor ? 'transparent' : '#fff',
              padding: isWorkflowEditor ? 0 : 24,
              margin: 0,
              minHeight: isWorkflowEditor ? 'calc(100vh - 64px)' : 280,
              borderRadius: isWorkflowEditor ? 0 : '8px'
            }}
          >
            <Outlet />
          </Content>
        </Layout>
      </Layout>
    </Layout>
  );
};
