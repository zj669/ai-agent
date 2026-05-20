import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../../../shared/api/adapters/authAdapter', () => ({
  sendEmailCode: vi.fn(),
  resetPassword: vi.fn(),
}))

import ForgotPasswordPage from '../ForgotPasswordPage'
import { resetPassword, sendEmailCode } from '../../../shared/api/adapters/authAdapter'

describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(sendEmailCode).mockResolvedValue(undefined)
    vi.mocked(resetPassword).mockResolvedValue(undefined)
  })

  it('发送验证码后可提交验证码和新密码完成重置', async () => {
    render(
      <MemoryRouter>
        <ForgotPasswordPage />
      </MemoryRouter>
    )

    fireEvent.change(screen.getByPlaceholderText('请输入邮箱'), {
      target: { value: 'user@example.com' },
    })
    fireEvent.click(screen.getByRole('button', { name: '发送验证码' }))

    await waitFor(() => {
      expect(sendEmailCode).toHaveBeenCalledWith('user@example.com')
      expect(screen.getByPlaceholderText('请输入6位验证码')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByPlaceholderText('请输入6位验证码'), {
      target: { value: '123456' },
    })
    fireEvent.change(screen.getByPlaceholderText('请输入新密码（至少8位）'), {
      target: { value: 'new-pass-123' },
    })
    fireEvent.change(screen.getByPlaceholderText('请再次输入新密码'), {
      target: { value: 'new-pass-123' },
    })
    fireEvent.click(screen.getByRole('button', { name: '重置密码' }))

    await waitFor(() => {
      expect(resetPassword).toHaveBeenCalledWith({
        email: 'user@example.com',
        code: '123456',
        newPassword: 'new-pass-123',
        confirmPassword: 'new-pass-123',
      })
      expect(screen.getByText('密码已重置，请使用新密码登录')).toBeInTheDocument()
    })
  })
})
