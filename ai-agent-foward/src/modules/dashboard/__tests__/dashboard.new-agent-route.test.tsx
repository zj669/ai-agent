import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { vi } from 'vitest'
import DashboardPage from '../pages/DashboardPage'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate
  }
})

vi.mock('../../../shared/api/adapters/dashboardAdapter', () => ({
  getDashboardStats: vi.fn().mockResolvedValue({
    agentCount: 3,
    workflowExecutionCount: 10,
    conversationCount: 5,
    knowledgeBaseCount: 2,
    recentExecutions: [],
  })
}))

describe('dashboard new agent route', () => {
  it('点击新建 Agent 仅跳转到 /agents', async () => {
    render(<DashboardPage />)

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /新建 Agent/ })).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: /新建 Agent/ }))

    expect(mockNavigate).toHaveBeenCalledWith('/agents')
  })
})
