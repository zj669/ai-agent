import { validateConnection } from '../validation/validateConnection'

describe('workflow validate connection', () => {
  it('禁止自环 SELF_LOOP', () => {
    const result = validateConnection({ source: 'n1', target: 'n1' })

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.code).toBe('SELF_LOOP')
    }
  })
})
