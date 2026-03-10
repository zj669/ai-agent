import { useEffect } from 'react'
import { BrowserRouter, MemoryRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import AppShell from './AppShell'
import RequireAuth from './AuthGuard'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import RegisterPage from './pages/RegisterPage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import DashboardPage from '../modules/dashboard/pages/DashboardPage'
import AgentListPage from '../modules/agent/pages/AgentListPage'
import WorkflowEditorPage from '../modules/workflow/pages/WorkflowEditorPage'
import KnowledgePage from '../modules/knowledge/pages/KnowledgePage'
import ChatPage from '../modules/chat/pages/ChatPage'
import ReviewPage from '../modules/review/pages/ReviewPage'
import LlmConfigPage from '../modules/llm-config/pages/LlmConfigPage'
import SwarmWorkspaceListPage from '../modules/swarm/pages/SwarmWorkspaceListPage'
import SwarmMainPage from '../modules/swarm/pages/SwarmMainPage'
import SettingsPage from '../modules/settings/pages/SettingsPage'

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route element={<RequireAuth />}>
        <Route path="/" element={<AppShell />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="agents" element={<AgentListPage />} />
          <Route path="knowledge" element={<KnowledgePage />} />
          <Route path="chat" element={<ChatPage />} />
          <Route path="reviews" element={<ReviewPage />} />
          <Route path="llm-config" element={<LlmConfigPage />} />
          <Route path="swarm" element={<SwarmWorkspaceListPage />} />
          <Route path="swarm/:workspaceId" element={<SwarmMainPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>
        {/* 全屏独立路由，不经过 AppShell */}
        <Route path="agents/:agentId/workflow" element={<WorkflowEditorPage />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export function AppRouter() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  )
}

function PathObserver({ onPathChange }: { onPathChange?: (path: string) => void }) {
  const location = useLocation()

  useEffect(() => {
    onPathChange?.(location.pathname)
  }, [location.pathname, onPathChange])

  return null
}

export function TestRouter({
  initialEntries = ['/'],
  onPathChange
}: {
  initialEntries?: string[]
  onPathChange?: (path: string) => void
}) {
  return (
    <MemoryRouter initialEntries={initialEntries}>
      <PathObserver onPathChange={onPathChange} />
      <AppRoutes />
    </MemoryRouter>
  )
}
