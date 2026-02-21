import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { vi } from 'vitest'
import WorkflowEditorPage from '../pages/WorkflowEditorPage'

const {
  fetchWorkflowDetailMock,
  publishWorkflowMock,
  saveWorkflowMock
} = vi.hoisted(() => ({
  fetchWorkflowDetailMock: vi.fn(),
  publishWorkflowMock: vi.fn(),
  saveWorkflowMock: vi.fn()
}))

vi.mock('../api/workflowService', () => ({
  fetchWorkflowDetail: (...args: unknown[]) => fetchWorkflowDetailMock(...args),
  publishWorkflow: (...args: unknown[]) => publishWorkflowMock(...args),
  saveWorkflow: (...args: unknown[]) => saveWorkflowMock(...args)
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useParams: () => ({ agentId: '1001' })
  }
})

describe('workflow editor interaction', () => {
  beforeEach(() => {
    fetchWorkflowDetailMock.mockReset()
    publishWorkflowMock.mockReset()
    saveWorkflowMock.mockReset()

    fetchWorkflowDetailMock.mockResolvedValue({
      agentId: 1001,
      version: 3,
      name: '测试 Agent',
      graphJson: undefined,
      graph: null
    })
    saveWorkflowMock.mockResolvedValue({
      agentId: 1001,
      version: 4,
      name: '测试 Agent',
      graphJson: undefined,
      graph: null
    })
    publishWorkflowMock.mockResolvedValue({
      agentId: 1001,
      version: 5,
      name: '测试 Agent',
      graphJson: undefined,
      graph: null
    })
  })

  it('展示节点列表与 agentId', async () => {
    render(<WorkflowEditorPage />)

    expect(screen.getByText('当前 Agent: 1001')).toBeInTheDocument()
    expect(await screen.findByText('开始节点（START）')).toBeInTheDocument()
    expect(screen.getByText('结束节点（END）')).toBeInTheDocument()
  })

  it('添加合法连线后展示在列表中', async () => {
    render(<WorkflowEditorPage />)

    await screen.findByText('开始节点（START）')
    fireEvent.click(screen.getByRole('button', { name: '添加连线' }))

    expect(screen.getByText('start → end')).toBeInTheDocument()
  })

  it('自连时显示显式错误反馈', async () => {
    render(<WorkflowEditorPage />)

    await screen.findByText('开始节点（START）')
    fireEvent.change(screen.getByLabelText('target-node'), { target: { value: 'start' } })
    fireEvent.click(screen.getByRole('button', { name: '添加连线' }))

    expect(screen.getByText('不允许节点自连')).toBeInTheDocument()
  })

  it('重复连线时显示显式错误反馈', async () => {
    render(<WorkflowEditorPage />)

    await screen.findByText('开始节点（START）')
    const button = screen.getByRole('button', { name: '添加连线' })
    fireEvent.click(button)
    fireEvent.click(button)

    expect(screen.getByText('该连线已存在，请勿重复添加')).toBeInTheDocument()
  })

  it('点击节点配置可显示配置入口内容', async () => {
    render(<WorkflowEditorPage />)

    await screen.findByText('开始节点（START）')
    fireEvent.click(screen.getAllByRole('button', { name: '节点配置' })[0])

    expect(screen.getByText('节点 ID: start')).toBeInTheDocument()
    expect(screen.getByText('节点名称: 开始节点')).toBeInTheDocument()
    expect(screen.getByText('节点类型: START')).toBeInTheDocument()
  })

  it('保存前校验失败时不发保存请求并显示反馈', async () => {
    render(<WorkflowEditorPage />)

    await screen.findByText('开始节点（START）')
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect(await screen.findByText('至少添加一条连线后再保存')).toBeInTheDocument()
    expect(saveWorkflowMock).not.toHaveBeenCalled()
  })

  it('发布前校验失败时不发发布请求并显示反馈', async () => {
    render(<WorkflowEditorPage />)

    await screen.findByText('开始节点（START）')
    fireEvent.click(screen.getByRole('button', { name: '发布' }))

    expect(await screen.findByText('至少添加一条连线后再保存')).toBeInTheDocument()
    expect(publishWorkflowMock).not.toHaveBeenCalled()
  })

  it('保存成功后进入已保存态，发布成功后刷新反馈', async () => {
    render(<WorkflowEditorPage />)

    await screen.findByText('开始节点（START）')
    fireEvent.click(screen.getByRole('button', { name: '添加连线' }))
    expect(screen.getByText('状态: 未保存')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(saveWorkflowMock).toHaveBeenCalledTimes(1)
    })
    expect(await screen.findByText('保存成功')).toBeInTheDocument()
    expect(screen.getByText('状态: 已保存')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '发布' }))

    await waitFor(() => {
      expect(publishWorkflowMock).toHaveBeenCalledWith(1001)
    })
    expect(await screen.findByText('发布成功')).toBeInTheDocument()
  })

  it('未保存状态下发布会被拦截并提示先保存', async () => {
    render(<WorkflowEditorPage />)

    await screen.findByText('开始节点（START）')
    fireEvent.click(screen.getByRole('button', { name: '添加连线' }))
    fireEvent.click(screen.getByRole('button', { name: '发布' }))

    expect(await screen.findByText('请先保存后再发布')).toBeInTheDocument()
    expect(publishWorkflowMock).not.toHaveBeenCalled()
  })

  it('保存失败后可恢复操作且不污染已保存态', async () => {
    saveWorkflowMock.mockRejectedValueOnce(new Error('save failed'))

    render(<WorkflowEditorPage />)

    await screen.findByText('开始节点（START）')
    fireEvent.click(screen.getByRole('button', { name: '添加连线' }))
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    expect(await screen.findByText('保存失败，请稍后重试')).toBeInTheDocument()
    expect(screen.getByText('状态: 未保存')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '保存' })).toBeEnabled()
    expect(screen.getByRole('button', { name: '发布' })).toBeEnabled()
  })

  it('发布失败后可恢复操作且保持已保存态', async () => {
    publishWorkflowMock.mockRejectedValueOnce(new Error('publish failed'))

    render(<WorkflowEditorPage />)

    await screen.findByText('开始节点（START）')
    fireEvent.click(screen.getByRole('button', { name: '添加连线' }))
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await screen.findByText('保存成功')
    fireEvent.click(screen.getByRole('button', { name: '发布' }))

    expect(await screen.findByText('发布失败，请稍后重试')).toBeInTheDocument()
    expect(screen.getByText('状态: 已保存')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '保存' })).toBeEnabled()
    expect(screen.getByRole('button', { name: '发布' })).toBeEnabled()
  })
})
