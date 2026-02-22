import { render } from '@testing-library/react'
import { vi } from 'vitest'

vi.mock('@xyflow/react', () => ({
  getBezierPath: () => ['M 0 0 C 50 0 50 100 100 100'],
  Position: { Left: 'left', Right: 'right' },
}))

const { default: CustomConnectionLine } = await import('../CustomConnectionLine')

describe('CustomConnectionLine', () => {
  it('renders a path and a target indicator rect', () => {
    const { container } = render(
      <svg>
        <CustomConnectionLine fromX={0} fromY={0} toX={100} toY={100} fromPosition="right" toPosition="left" />
      </svg>
    )
    const path = container.querySelector('path')
    expect(path).toBeInTheDocument()
    expect(path?.getAttribute('stroke')).toBe('#D0D5DD')

    const rect = container.querySelector('rect')
    expect(rect).toBeInTheDocument()
    expect(rect?.getAttribute('fill')).toBe('#2970FF')
  })
})
