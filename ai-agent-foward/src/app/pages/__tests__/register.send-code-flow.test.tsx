import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { message } from 'antd'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../auth', () => ({
  isAuthenticated: () => false,
  saveAccessToken: vi.fn(),
}))

vi.mock('../../../shared/api/adapters/authAdapter', () => ({
  sendEmailCode: vi.fn(),
  register: vi.fn(),
}))

vi.mock('../components/WorkflowAnimation', () => ({
  default: () => <div data-testid="workflow-animation" />,
}))

import RegisterPage from '../RegisterPage'
import { sendEmailCode } from '../../../shared/api/adapters/authAdapter'

describe('RegisterPage send code flow', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(sendEmailCode).mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('发送验证码成功后进入验证注册步骤并显示60s倒计时', async () => {
    render(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>
    )

    fireEvent.change(screen.getByPlaceholderText('请输入邮箱'), {
      target: { value: 'user@example.com' },
    })

    fireEvent.click(screen.getByRole('button', { name: '发送验证码' }))

    await waitFor(() => {
      expect(sendEmailCode).toHaveBeenCalledWith('user@example.com')
      expect(screen.getByText('验证码已发送至')).toBeInTheDocument()
      expect(screen.getByText('user@example.com')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /60s/ })).toBeInTheDocument()
      expect(screen.getByPlaceholderText('请输入6位验证码')).toBeInTheDocument()
    })
  })

  it('发送验证码失败时停留在邮箱步骤并展示具体错误', async () => {
    vi.mocked(sendEmailCode).mockRejectedValueOnce({ code: 'RATE_LIMITED', message: '操作过于频繁，请稍后再试' })
    const errorSpy = vi.spyOn(message, 'error').mockImplementation(() => undefined as never)

    render(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>
    )

    fireEvent.change(screen.getByPlaceholderText('请输入邮箱'), {
      target: { value: 'user@example.com' },
    })

    fireEvent.click(screen.getByRole('button', { name: '发送验证码' }))

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('操作过于频繁，请稍后再试')
      expect(screen.getByPlaceholderText('请输入邮箱')).toBeInTheDocument()
      expect(screen.queryByText('验证码已发送至')).not.toBeInTheDocument()
    })
  })
})
