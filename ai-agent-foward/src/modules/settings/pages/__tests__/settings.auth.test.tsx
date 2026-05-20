import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../../../../shared/api/client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

vi.mock('../../../../shared/api/adapters/authAdapter', () => ({
  resetPassword: vi.fn(),
}))

import SettingsPage from '../SettingsPage'
import { apiClient } from '../../../../shared/api/client'
import { resetPassword } from '../../../../shared/api/adapters/authAdapter'

const userInfo = {
  id: 1,
  username: '管理员',
  email: 'user@example.com',
  avatarUrl: null,
  phone: null,
  status: 1,
  createdAt: '2026-05-20T00:00:00',
}

describe('SettingsPage auth actions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(apiClient.get).mockResolvedValue({
      data: {
        code: 200,
        message: 'success',
        data: userInfo,
      },
    })
    vi.mocked(resetPassword).mockResolvedValue(undefined)
  })

  it('重置密码时提交后端要求的 confirmPassword 字段', async () => {
    render(<SettingsPage />)

    expect(await screen.findByText('个人设置')).toBeInTheDocument()
    fireEvent.click(screen.getByText('安全设置'))

    fireEvent.change(screen.getByPlaceholderText('请输入邮箱验证码'), {
      target: { value: '123456' },
    })
    fireEvent.change(screen.getByPlaceholderText('请输入新密码'), {
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
    })
  })
})
