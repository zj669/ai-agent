import { useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/es/locale/zh_CN';
import { useAuthStore } from './stores/authStore';
import { ProtectedRoute } from './components/ProtectedRoute';
import { MainLayout } from './components/MainLayout';
import { LoginPage } from './pages/LoginPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';
import { DashboardPage } from './pages/DashboardPage';
import { AgentListPage } from './pages/AgentListPage';
import { AgentFormPage } from './pages/AgentFormPage';
import { ChatPage } from './pages/ChatPage';
import { KnowledgePage } from './pages/KnowledgePage';
import { HumanReviewPage } from './pages/HumanReviewPage';

function App() {
  const { initializeAuth } = useAuthStore();

  useEffect(() => {
    // Initialize auth state from localStorage
    initializeAuth();
  }, [initializeAuth]);

  return (
    <ConfigProvider locale={zhCN}>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />

        {/* Protected routes */}
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <MainLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />

          {/* Agent Management */}
          <Route path="agents" element={<AgentListPage />} />
          <Route path="agents/create" element={<AgentFormPage />} />
          <Route path="agents/:id/edit" element={<AgentFormPage />} />

          {/* Other routes */}
          <Route path="chat" element={<ChatPage />} />
          <Route path="knowledge" element={<KnowledgePage />} />
          <Route path="human-review" element={<HumanReviewPage />} />
          <Route path="profile" element={<div>个人信息页面（待实现）</div>} />
          <Route path="settings" element={<div>设置页面（待实现）</div>} />
        </Route>

        {/* 404 */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </ConfigProvider>
  );
}

export default App;
