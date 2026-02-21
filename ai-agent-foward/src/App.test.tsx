import { render, screen } from '@testing-library/react'
import { TestRouter } from './app/router'
import App from './app/App'

describe('App', () => {
  it('默认使用 light 主题', () => {
    render(<App />)
    expect(document.documentElement).toHaveClass('light')
  })

  it('已登录访问 /dashboard 时显示工作台', async () => {
    localStorage.setItem('accessToken', 'token')

    render(<TestRouter initialEntries={['/dashboard']} />)

    expect(await screen.findByRole('heading', { name: 'AI Agent 平台' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '工作台' })).toBeInTheDocument()
  })
})
