import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { showToast } from '../toast'

describe('showToast', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('creates a toast container and appends a message element', () => {
    showToast('hello', 'info')

    const container = document.getElementById('app-toast-container')
    expect(container).not.toBeNull()
    expect(container!.children.length).toBe(1)
    expect(container!.children[0].textContent).toBe('hello')
  })

  it('reuses the same container for multiple toasts', () => {
    showToast('first', 'success')
    showToast('second', 'error')

    const containers = document.querySelectorAll('#app-toast-container')
    expect(containers.length).toBe(1)
    expect(containers[0].children.length).toBe(2)
  })

  it('applies error style for error type', () => {
    showToast('oops', 'error')

    const el = document.querySelector('#app-toast-container > div') as HTMLElement
    expect(el.style.background).toBe('rgb(239, 68, 68)')
  })

  it('fades out after duration', () => {
    showToast('bye', 'info')

    const el = document.querySelector('#app-toast-container > div') as HTMLElement
    vi.advanceTimersByTime(3000)
    expect(el.style.opacity).toBe('0')
  })
})
