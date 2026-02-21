import { render, screen } from '@testing-library/react'
import App from '../App'
import { bootApp } from '../boot'

describe('app boot', () => {
  it('启动时应用默认 light 主题', () => {
    document.documentElement.className = ''

    bootApp()

    expect(document.documentElement).toHaveClass('light')
  })

  it('渲染登录页作为应用入口', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: '登录' })).toBeInTheDocument()
    expect(screen.getByText('请先登录后访问工作台。')).toBeInTheDocument()
  })
})
