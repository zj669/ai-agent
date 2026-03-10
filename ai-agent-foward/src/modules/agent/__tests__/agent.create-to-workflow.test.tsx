import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, vi } from 'vitest'
import AgentListPage from '../pages/AgentListPage'

const { mockNavigate, mockCreateAgent, mockGetAgentList } = vi.hoisted(() => ({
  mockNavigate: vi.fn(),
  mockCreateAgent: vi.fn().mockResolvedValue(123),
  mockGetAgentList: vi.fn()
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate
  }
})

vi.mock('../../../shared/api/adapters/agentAdapter', () => ({
  createAgent: mockCreateAgent,
  getAgentList: () => mockGetAgentList(),
  publishAgent: vi.fn(),
  offlineAgent: vi.fn(),
  deleteAgent: vi.fn(),
}))

describe('agent create to workflow', () => {
  beforeEach(() => {
    mockNavigate.mockReset()
    mockCreateAgent.mockReset()
    mockGetAgentList.mockReset()

    mockCreateAgent.mockResolvedValue(123)
    mockGetAgentList.mockResolvedValue([
      {
        id: 1001,
        userId: 1,
        name: '测试 Agent',
        description: '用于列表渲染断言',
        status: 'PUBLISHED',
        updateTime: '2026-02-20T10:00:00'
      }
    ])
  })

  it('列表页首屏加载会调用列表接口并渲染返回数据', async () => {
    render(<AgentListPage />)

    await waitFor(() => {
      expect(mockGetAgentList).toHaveBeenCalledTimes(1)
    })

    expect(await screen.findByText('测试 Agent')).toBeInTheDocument()
  })

  it('列表页点击新建后创建并跳转 workflow 页面', async () => {
    render(<AgentListPage />)

    await screen.findByText('测试 Agent')

    fireEvent.click(screen.getByRole('button', { name: /新建 Agent/ }))

    await waitFor(() => {
      expect(mockCreateAgent).toHaveBeenCalledTimes(1)
      expect(mockNavigate).toHaveBeenCalledWith('/agents/123/workflow')
    })
  })

  it('创建失败时不跳转并显示错误提示', async () => {
    mockCreateAgent.mockRejectedValueOnce(new Error('create failed'))
    render(<AgentListPage />)

    await screen.findByText('测试 Agent')

    fireEvent.click(screen.getByRole('button', { name: /新建 Agent/ }))

    expect(await screen.findByText('创建失败')).toBeInTheDocument()
    expect(mockNavigate).not.toHaveBeenCalled()
  })

  it('创建进行中时按钮禁用，避免重复点击', async () => {
    let resolveCreate: ((value: number) => void) | undefined
    mockCreateAgent.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve
        })
    )

    render(<AgentListPage />)

    await screen.findByText('测试 Agent')

    const button = screen.getByRole('button', { name: /新建 Agent/ })
    fireEvent.click(button)
    fireEvent.click(button)

    expect(mockCreateAgent).toHaveBeenCalledTimes(1)

    resolveCreate?.(456)

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/agents/456/workflow')
    })
  })

  it('列表加载失败时显示最小错误提示', async () => {
    mockGetAgentList.mockRejectedValueOnce(new Error('list failed'))
    render(<AgentListPage />)

    expect(await screen.findByText('列表加载失败')).toBeInTheDocument()
  })
})
