import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import RequireAuth from '../AuthGuard'
import { TestRouter } from '../router'

describe('router auth guard', () => {
  it('未登录访问 /dashboard?tab=recent 时会把 redirect 带到 /login', async () => {
    localStorage.removeItem('accessToken')
    sessionStorage.removeItem('accessToken')

    function LoginProbe() {
      const location = useLocation()
      return <p data-testid="login-search">{location.search}</p>
    }

    render(
      <MemoryRouter initialEntries={['/dashboard?tab=recent']}>
        <Routes>
          <Route element={<RequireAuth />}>
            <Route path="/dashboard" element={<div>dashboard</div>} />
          </Route>
          <Route path="/login" element={<LoginProbe />} />
        </Routes>
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByTestId('login-search')).toHaveTextContent('?redirect=%2Fdashboard%3Ftab%3Drecent')
    })
  })

  it('已登录访问 /dashboard 时保留在受保护页面', async () => {
    localStorage.setItem('accessToken', 'token')
    let currentPath = '/dashboard'

    render(
      <TestRouter
        initialEntries={['/dashboard']}
        onPathChange={(path) => {
          currentPath = path
        }}
      />
    )

    expect(await screen.findByRole('heading', { name: 'AI Agent 平台' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '工作台' })).toBeInTheDocument()
    expect(screen.getByRole('complementary', { name: '主导航' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '工作台' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Agent' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '知识库' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '聊天' })).toBeInTheDocument()
    await waitFor(() => {
      expect(currentPath).toBe('/dashboard')
    })
  })

  it('已登录可通过侧栏跳转到 /knowledge 与 /chat', async () => {
    localStorage.setItem('accessToken', 'token')
    let currentPath = '/dashboard'

    render(
      <TestRouter
        initialEntries={['/dashboard']}
        onPathChange={(path) => {
          currentPath = path
        }}
      />
    )

    await screen.findByRole('heading', { name: 'AI Agent 平台' })

    fireEvent.click(screen.getByRole('link', { name: '知识库' }))
    await waitFor(() => {
      expect(currentPath).toBe('/knowledge')
    })
    expect(screen.getByRole('heading', { name: '知识库' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('link', { name: '聊天' }))
    await waitFor(() => {
      expect(currentPath).toBe('/chat')
    })
    expect(screen.getByRole('heading', { name: '聊天' })).toBeInTheDocument()
  })
})
