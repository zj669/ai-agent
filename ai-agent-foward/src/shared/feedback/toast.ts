export type ToastType = 'success' | 'error' | 'warning' | 'info'

const TOAST_DURATION = 3000
const CONTAINER_ID = 'app-toast-container'

const TYPE_STYLES: Record<ToastType, string> = {
  success: 'background:#10b981;color:#fff',
  error: 'background:#ef4444;color:#fff',
  warning: 'background:#f59e0b;color:#fff',
  info: 'background:#3b82f6;color:#fff',
}

function getContainer(): HTMLElement {
  let container = document.getElementById(CONTAINER_ID)
  if (!container) {
    container = document.createElement('div')
    container.id = CONTAINER_ID
    container.style.cssText =
      'position:fixed;top:24px;right:24px;z-index:99999;display:flex;flex-direction:column;gap:8px;pointer-events:none'
    document.body.appendChild(container)
  }
  return container
}

export function showToast(message: string, type: ToastType = 'info'): void {
  const container = getContainer()

  const el = document.createElement('div')
  el.style.cssText = `${TYPE_STYLES[type]};padding:10px 20px;border-radius:8px;font-size:14px;box-shadow:0 4px 12px rgba(0,0,0,.15);opacity:0;transition:opacity .2s;max-width:360px;word-break:break-word`
  el.textContent = message
  container.appendChild(el)

  // fade in
  requestAnimationFrame(() => {
    el.style.opacity = '1'
  })

  // fade out & remove
  setTimeout(() => {
    el.style.opacity = '0'
    el.addEventListener('transitionend', () => el.remove(), { once: true })
  }, TOAST_DURATION)
}
