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

    expect(await screen.findByText('AI Agent')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: '欢迎回来，管理员' })).toBeInTheDocument()
  })
})
