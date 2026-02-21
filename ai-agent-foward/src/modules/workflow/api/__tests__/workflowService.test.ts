import { describe, expect, it, vi } from 'vitest'
import { fetchWorkflowDetail, publishWorkflow, saveWorkflow } from '../workflowService'

const {
  getAgentDetailMock,
  publishAgentMock,
  updateAgentMock
} = vi.hoisted(() => ({
  getAgentDetailMock: vi.fn(),
  publishAgentMock: vi.fn(),
  updateAgentMock: vi.fn()
}))

vi.mock('../../../../shared/api/adapters/agentAdapter', () => ({
  getAgentDetail: (...args: unknown[]) => getAgentDetailMock(...args),
  publishAgent: (...args: unknown[]) => publishAgentMock(...args),
  updateAgent: (...args: unknown[]) => updateAgentMock(...args)
}))

describe('workflowService', () => {
  it('fetchWorkflowDetail 应该解析 graphJson', async () => {
    getAgentDetailMock.mockResolvedValueOnce({
      id: 1001,
      name: '客服助手',
      description: '智能客服对话机器人',
      icon: 'robot',
      graphJson: '{"nodes":[{"id":"n1"}],"edges":[]}',
      version: 3,
      publishedVersionId: 5,
      status: 1
    })

    const result = await fetchWorkflowDetail(1001)

    expect(getAgentDetailMock).toHaveBeenCalledWith(1001)
    expect(result.agentId).toBe(1001)
    expect(result.graph).toEqual({ nodes: [{ id: 'n1' }], edges: [] })
  })

  it('fetchWorkflowDetail 在 graphJson 非法时返回 null graph', async () => {
    getAgentDetailMock.mockResolvedValueOnce({
      id: 1001,
      name: '客服助手',
      graphJson: '{not-json}',
      version: 3,
      status: 0
    })

    const result = await fetchWorkflowDetail(1001)

    expect(result.graph).toBeNull()
  })

  it('saveWorkflow 应该序列化 graphJson 并刷新详情', async () => {
    updateAgentMock.mockResolvedValueOnce(undefined)
    getAgentDetailMock.mockResolvedValueOnce({
      id: 1001,
      name: '客服助手',
      description: '更新后',
      icon: 'robot',
      graphJson: '{"nodes":[{"id":"start"}],"edges":[]}',
      version: 4,
      publishedVersionId: 6,
      status: 1
    })

    const result = await saveWorkflow({
      agentId: 1001,
      version: 3,
      name: '客服助手',
      description: '更新前',
      icon: 'robot',
      graph: {
        nodes: [{ id: 'start' }],
        edges: []
      }
    })

    expect(updateAgentMock).toHaveBeenCalledWith({
      id: 1001,
      name: '客服助手',
      description: '更新前',
      icon: 'robot',
      version: 3,
      graphJson: '{"nodes":[{"id":"start"}],"edges":[]}'
    })
    expect(getAgentDetailMock).toHaveBeenCalledWith(1001)
    expect(result.version).toBe(4)
  })

  it('publishWorkflow 应该调用发布并刷新详情', async () => {
    publishAgentMock.mockResolvedValueOnce(undefined)
    getAgentDetailMock.mockResolvedValueOnce({
      id: 1001,
      name: '客服助手',
      graphJson: '{"nodes":[],"edges":[]}',
      version: 5,
      publishedVersionId: 7,
      status: 1
    })

    const result = await publishWorkflow(1001)

    expect(publishAgentMock).toHaveBeenCalledWith({ id: 1001 })
    expect(getAgentDetailMock).toHaveBeenCalledWith(1001)
    expect(result.version).toBe(5)
  })
})
