export const DEFAULT_THEME = 'light'

export function applyDefaultTheme() {
  const root = document.documentElement
  root.classList.remove('dark')
  root.classList.add(DEFAULT_THEME)
}
