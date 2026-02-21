import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { TestRouter } from '../../../app/router'
import { login } from '../../../shared/api/adapters/authAdapter'

vi.mock('../../../shared/api/adapters/authAdapter', () => ({
  login: vi.fn()
}))

describe('remember me', () => {
  beforeEach(() => {
    vi.mocked(login).mockResolvedValue({
      token: 'api-token',
      refreshToken: 'refresh-token',
      expireIn: 604800,
      deviceId: 'device-1',
      user: {
        id: 1,
        username: '测试用户',
        email: 'user@example.com',
        avatarUrl: null,
        phone: null,
        status: 1,
        createdAt: '2026-02-20T00:00:00'
      }
    })
  })
  it('勾选记住我后登录会持久化登录并保存邮箱', async () => {
    localStorage.clear()
    sessionStorage.clear()

    render(<TestRouter initialEntries={['/login']} />)

    fireEvent.change(screen.getByLabelText('邮箱'), { target: { value: 'user@example.com' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: '12345678' } })
    fireEvent.click(screen.getByLabelText('记住我'))
    fireEvent.click(screen.getByRole('button', { name: '登录' }))

    await waitFor(() => {
      expect(login).toHaveBeenCalledWith({
        email: 'user@example.com',
        password: '12345678'
      })
      expect(localStorage.getItem('accessToken')).toBe('api-token')
      expect(sessionStorage.getItem('accessToken')).toBeNull()
      expect(localStorage.getItem('rememberedEmail')).toBe('user@example.com')
    })

    expect(await screen.findByRole('heading', { name: '工作台' })).toBeInTheDocument()
  })

  it('未勾选记住我时使用会话级登录并清除已保存邮箱', async () => {
    localStorage.setItem('rememberedEmail', 'old@example.com')
    localStorage.setItem('accessToken', 'old-token')
    sessionStorage.clear()

    render(<TestRouter initialEntries={['/login']} />)

    fireEvent.change(screen.getByLabelText('邮箱'), { target: { value: 'new@example.com' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: '12345678' } })
    fireEvent.click(screen.getByRole('button', { name: '登录' }))

    await waitFor(() => {
      expect(login).toHaveBeenCalledWith({
        email: 'new@example.com',
        password: '12345678'
      })
      expect(sessionStorage.getItem('accessToken')).toBe('api-token')
      expect(localStorage.getItem('accessToken')).toBeNull()
      expect(localStorage.getItem('rememberedEmail')).toBeNull()
    })
  })

  it('存在已记住邮箱时自动回填', () => {
    localStorage.setItem('rememberedEmail', 'preset@example.com')

    render(<TestRouter initialEntries={['/login']} />)

    const emailInput = screen.getByLabelText('邮箱') as HTMLInputElement
    expect(emailInput.value).toBe('preset@example.com')
  })

  it('登录成功时优先回跳 redirect', async () => {
    localStorage.clear()
    sessionStorage.clear()

    render(<TestRouter initialEntries={['/login?redirect=%2Fagents']} />)

    fireEvent.change(screen.getByLabelText('邮箱'), { target: { value: 'user@example.com' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: '12345678' } })
    fireEvent.click(screen.getByRole('button', { name: '登录' }))

    expect(await screen.findByRole('heading', { name: 'Agent 列表' })).toBeInTheDocument()
  })

  it('redirect 非法时登录后回退到 /dashboard', async () => {
    localStorage.clear()
    sessionStorage.clear()

    render(<TestRouter initialEntries={['/login?redirect=https%3A%2F%2Fevil.com']} />)

    fireEvent.change(screen.getByLabelText('邮箱'), { target: { value: 'user@example.com' } })
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: '12345678' } })
    fireEvent.click(screen.getByRole('button', { name: '登录' }))

    expect(await screen.findByRole('heading', { name: '工作台' })).toBeInTheDocument()
  })
})
