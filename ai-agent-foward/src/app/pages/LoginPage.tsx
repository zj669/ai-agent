import { FormEvent, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { saveAccessToken } from '../auth'
import { login } from '../../shared/api/adapters/authAdapter'

function isWebGLAvailable() {
  try {
    const canvas = document.createElement('canvas')
    return Boolean(canvas.getContext('webgl') || canvas.getContext('experimental-webgl'))
  } catch {
    return false
  }
}

function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const rememberedEmail = useMemo(() => localStorage.getItem('rememberedEmail') ?? '', [])
  const [email, setEmail] = useState(rememberedEmail)
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
  const [webglSupported] = useState(isWebGLAvailable)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const session = await login({ email, password })
    saveAccessToken(session.token, rememberMe)

    if (rememberMe) {
      localStorage.setItem('rememberedEmail', email)
    } else {
      localStorage.removeItem('rememberedEmail')
    }

    const searchParams = new URLSearchParams(location.search)
    const redirect = searchParams.get('redirect')
    const target = redirect && redirect.startsWith('/') ? redirect : '/dashboard'

    navigate(target, { replace: true })
  }

  return (
    <main className="min-h-screen bg-background text-foreground">
      <section className="mx-auto grid max-w-5xl gap-8 px-6 py-20 md:grid-cols-2">
        <div>
          <h1 className="text-2xl font-semibold">登录</h1>
          <p className="mt-3 text-sm text-muted-foreground">请先登录后访问工作台。</p>

          <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
            <div>
              <label htmlFor="email" className="block text-sm">
                邮箱
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                className="mt-1 w-full rounded border border-border bg-background px-3 py-2"
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-sm">
                密码
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                className="mt-1 w-full rounded border border-border bg-background px-3 py-2"
              />
            </div>

            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(event) => setRememberMe(event.target.checked)}
              />
              记住我
            </label>

            <button type="submit" className="rounded bg-primary px-4 py-2 text-sm text-primary-foreground">
              登录
            </button>
          </form>
        </div>

        <aside aria-label="3d-background" className="rounded border border-border p-4">
          {webglSupported ? (
            <div className="h-full min-h-48 rounded bg-muted" />
          ) : (
            <p className="text-sm text-muted-foreground">当前设备不支持 WebGL，已切换简化背景。</p>
          )}
        </aside>
      </section>
    </main>
  )
}

export default LoginPage
