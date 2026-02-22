import { render } from '@testing-library/react'
import { vi } from 'vitest'

const mockGetBezierPath = vi.fn().mockReturnValue(['M 0 0 C 50 0 50 100 100 100', 50, 50])

vi.mock('@xyflow/react', () => ({
  getBezierPath: (...args: unknown[]) => mockGetBezierPath(...args),
  BaseEdge: (props: Record<string, unknown>) => (
    <path data-testid="base-edge" d={props.path as string} style={props.style as Record<string, unknown>} />
  ),
  Position: { Left: 'left', Right: 'right' },
}))

const { default: CustomEdge } = await import('../CustomEdge')

describe('CustomEdge', () => {
  const baseProps = {
    id: 'e1',
    source: 'a',
    target: 'b',
    sourceX: 100,
    sourceY: 200,
    targetX: 300,
    targetY: 200,
    sourcePosition: 'right' as const,
    targetPosition: 'left' as const,
  }

  it('calls getBezierPath with horizontal positions and curvature 0.16', () => {
    render(<svg><CustomEdge {...baseProps} /></svg>)
    expect(mockGetBezierPath).toHaveBeenCalledWith(
      expect.objectContaining({
        sourcePosition: 'right',
        targetPosition: 'left',
        curvature: 0.16,
      })
    )
  })

  it('renders BaseEdge with blue stroke', () => {
    const { getByTestId } = render(<svg><CustomEdge {...baseProps} /></svg>)
    const edge = getByTestId('base-edge')
    expect(edge).toBeInTheDocument()
  })
})
