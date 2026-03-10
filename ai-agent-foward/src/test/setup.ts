import '@testing-library/jest-dom'
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

afterEach(() => {
  cleanup()
})

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }),
})

global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}

Element.prototype.scrollIntoView = () => {}

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
