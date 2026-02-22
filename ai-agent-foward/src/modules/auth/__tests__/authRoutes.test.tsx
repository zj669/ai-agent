import { render, screen, waitFor } from '@testing-library/react'
import { TestRouter } from '../../../app/router'

describe('auth routes', () => {
  it('未登录访问受保护路由会跳转到登录页', async () => {
    localStorage.removeItem('accessToken')
    sessionStorage.removeItem('accessToken')
    let currentPath = '/dashboard?tab=recent'

    render(
      <TestRouter
        initialEntries={['/dashboard?tab=recent']}
        onPathChange={(path) => {
          currentPath = path
        }}
      />
    )

    expect(await screen.findByRole('heading', { name: '登录' })).toBeInTheDocument()

    await waitFor(() => {
      expect(currentPath).toBe('/login')
    })
  })

  it('可访问注册页', async () => {
    render(<TestRouter initialEntries={['/register']} />)

    expect(await screen.findByRole('heading', { name: '注册' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '创建账号' })).toBeInTheDocument()
  })

  it('可访问忘记密码页', async () => {
    render(<TestRouter initialEntries={['/forgot-password']} />)

    expect(await screen.findByRole('heading', { name: '忘记密码' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '重置密码' })).toBeInTheDocument()
  })

  it('已登录访问受保护路由保持在工作台', async () => {
    localStorage.setItem('accessToken', 'token')
    sessionStorage.removeItem('accessToken')
    let currentPath = '/dashboard'

    render(
      <TestRouter
        initialEntries={['/dashboard']}
        onPathChange={(path) => {
          currentPath = path
        }}
      />
    )

    expect(await screen.findByRole('heading', { name: '工作台' })).toBeInTheDocument()

    await waitFor(() => {
      expect(currentPath).toBe('/dashboard')
    })
  })

  it('已登录访问 /agents/:agentId/workflow 可达并渲染页面', async () => {
    localStorage.setItem('accessToken', 'token')
    sessionStorage.removeItem('accessToken')

    render(<TestRouter initialEntries={['/agents/agent-123/workflow']} />)

    expect(await screen.findByTestId('rf__wrapper')).toBeInTheDocument()
    expect(screen.getByLabelText('返回')).toBeInTheDocument()
  })
})
