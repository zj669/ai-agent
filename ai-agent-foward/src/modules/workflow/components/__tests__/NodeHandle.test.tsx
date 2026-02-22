import { render } from '@testing-library/react'
import { vi } from 'vitest'

vi.mock('@xyflow/react', () => ({
  Handle: (props: Record<string, unknown>) => (
    <div data-testid={`handle-${props.id}`} data-type={props.type} data-position={props.position} />
  ),
  Position: { Left: 'left', Right: 'right' },
}))

const { NodeTargetHandle, NodeSourceHandle } = await import('../NodeHandle')

describe('NodeHandle', () => {
  it('NodeTargetHandle renders left-side target handle', () => {
    const { getByTestId } = render(<NodeTargetHandle handleId="target" />)
    const el = getByTestId('handle-target')
    expect(el.dataset.type).toBe('target')
    expect(el.dataset.position).toBe('left')
  })

  it('NodeSourceHandle renders right-side source handle', () => {
    const { getByTestId } = render(<NodeSourceHandle handleId="source" />)
    const el = getByTestId('handle-source')
    expect(el.dataset.type).toBe('source')
    expect(el.dataset.position).toBe('right')
  })
})
