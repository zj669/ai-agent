import { applyDefaultTheme, DEFAULT_THEME } from '../defaultTheme'

describe('default theme', () => {
  it('默认主题为 light', () => {
    expect(DEFAULT_THEME).toBe('light')
  })

  it('应用默认主题到 html 根节点', () => {
    document.documentElement.className = 'dark custom'

    applyDefaultTheme()

    expect(document.documentElement).toHaveClass('light')
    expect(document.documentElement).not.toHaveClass('dark')
  })
})
