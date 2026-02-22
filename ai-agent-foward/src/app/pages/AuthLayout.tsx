import { Link, useLocation } from 'react-router-dom'
import Auth3DScene from './Auth3DScene'

const tabs = [
  { path: '/login', label: '登录' },
  { path: '/register', label: '注册' },
  { path: '/forgot-password', label: '忘记密码' },
] as const

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  const { pathname } = useLocation()

  return (
    <main className="relative flex min-h-screen text-foreground">
      {/* 左侧：3D 场景 */}
      <aside aria-label="3d-background" className="hidden md:block md:w-[45%]">
        <Auth3DScene />
      </aside>

      {/* 右侧：认证表单 */}
      <section className="relative z-10 flex w-full flex-col items-center justify-center bg-gradient-to-bl from-slate-50 via-white to-blue-50/60 px-6 py-10 md:w-[55%] md:px-12 lg:px-20">
        <div className="w-full max-w-[420px]">
          {/* 品牌标识 */}
          <div className="mb-10 flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary text-sm font-bold text-primary-foreground">
              AI
            </div>
            <span className="text-lg font-semibold tracking-tight text-foreground">AI Agent</span>
          </div>

          {/* Tab 导航 */}
          <nav className="mb-8 flex gap-1 rounded-xl bg-slate-100/80 p-1">
            {tabs.map(({ path, label }) => (
              <Link
                key={path}
                to={path}
                className={`flex-1 rounded-lg px-3 py-2 text-center text-sm font-medium transition-all ${
                  pathname === path
                    ? 'bg-white text-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                {label}
              </Link>
            ))}
          </nav>

          {/* 表单内容 */}
          {children}
        </div>
      </section>
    </main>
  )
}
