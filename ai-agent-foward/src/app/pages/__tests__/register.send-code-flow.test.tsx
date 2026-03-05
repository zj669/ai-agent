import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeAll, describe, expect, it, vi } from 'vitest'

vi.mock('../auth', () => ({
  isAuthenticated: () => false,
  saveAccessToken: vi.fn(),
}))

vi.mock('../../../shared/api/adapters/authAdapter', () => ({
  sendEmailCode: vi.fn().mockImplementation(
    () => new Promise<void>((resolve) => setTimeout(resolve, 100))
  ),
  register: vi.fn(),
}))

vi.mock('../components/WorkflowAnimation', () => ({
  default: () => <div data-testid="workflow-animation" />,
}))

import RegisterPage from '../RegisterPage'

beforeAll(() => {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation(() => ({
      matches: false,
      media: '',
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  })
})

describe('RegisterPage send code flow', () => {
  it('点击发送验证码后立即进入验证注册步骤并显示60s倒计时', async () => {
    render(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>
    )

    fireEvent.change(screen.getByPlaceholderText('请输入邮箱'), {
      target: { value: 'user@example.com' },
    })

    fireEvent.click(screen.getByRole('button', { name: '发送验证码' }))

    // 立即进入第二步
    expect(screen.getByText('验证码已发送至')).toBeInTheDocument()
    expect(screen.getByText('user@example.com')).toBeInTheDocument()

    // 倒计时立即开始
    expect(screen.getByRole('button', { name: /60s/ })).toBeInTheDocument()

    // 发送逻辑完成后仍保持在第二步
    await waitFor(() => {
      expect(screen.getByPlaceholderText('请输入6位验证码')).toBeInTheDocument()
    })
  })
})
