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

    expect(await screen.findByText('AI Agent')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: '欢迎回来，管理员' })).toBeInTheDocument()
    expect(screen.getAllByText('工作台').length).toBeGreaterThan(0)
    expect(screen.getByText('Agent 管理')).toBeInTheDocument()
    expect(screen.getByText('知识库')).toBeInTheDocument()
    expect(screen.getByText('智能对话')).toBeInTheDocument()
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

    await screen.findByText('AI Agent')

    fireEvent.click(screen.getByText('知识库'))
    await waitFor(() => {
      expect(currentPath).toBe('/knowledge')
    })

    fireEvent.click(screen.getByText('智能对话'))
    await waitFor(() => {
      expect(currentPath).toBe('/chat')
    })
  })
})
