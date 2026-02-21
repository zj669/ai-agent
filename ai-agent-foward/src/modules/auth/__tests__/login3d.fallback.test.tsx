import { render, screen } from '@testing-library/react'
import { TestRouter } from '../../../app/router'

describe('login 3d fallback', () => {
  it('WebGL 不可用时展示降级背景文案', async () => {
    const originalGetContext = HTMLCanvasElement.prototype.getContext

    HTMLCanvasElement.prototype.getContext = (function getContextMock(
      this: HTMLCanvasElement,
      ...args: Parameters<HTMLCanvasElement['getContext']>
    ) {
      const [contextId] = args
      if (contextId === 'webgl' || contextId === 'experimental-webgl') {
        return null
      }
      return originalGetContext.call(this, ...args)
    }) as HTMLCanvasElement['getContext']

    render(<TestRouter initialEntries={['/login']} />)

    expect(await screen.findByText('当前设备不支持 WebGL，已切换简化背景。')).toBeInTheDocument()

    HTMLCanvasElement.prototype.getContext = originalGetContext
  })
})
