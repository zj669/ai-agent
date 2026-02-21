import '@testing-library/jest-dom'

const originalGetContext = HTMLCanvasElement.prototype.getContext

Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', {
  value: function getContextMock(this: HTMLCanvasElement, contextId: string, options?: unknown) {
    if (contextId === 'webgl' || contextId === 'experimental-webgl') {
      return null
    }

    if (typeof originalGetContext === 'function') {
      return originalGetContext.call(this, contextId as never, options as never)
    }

    return null
  },
  writable: true,
  configurable: true
})
