import { vi } from 'vitest'

const getMock = vi.fn()
vi.mock('../../client', () => ({
  apiClient: { get: (...args: unknown[]) => getMock(...args) },
}))

const { fetchNodeTemplates } = await import('../metadataAdapter')

describe('metadataAdapter', () => {
  beforeEach(() => getMock.mockReset())

  it('fetchNodeTemplates returns template list', async () => {
    const templates = [
      { id: 1, typeCode: 'LLM', name: 'LLM 节点', configFieldGroups: [] },
    ]
    getMock.mockResolvedValue({ data: { code: 200, message: 'ok', data: templates } })

    const result = await fetchNodeTemplates()

    expect(getMock).toHaveBeenCalledWith('/api/meta/node-templates')
    expect(result).toEqual(templates)
  })
})
