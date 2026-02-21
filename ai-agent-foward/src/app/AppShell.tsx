import { NavLink, Outlet } from 'react-router-dom'

function AppShell() {
  return (
    <main className="min-h-screen bg-background text-foreground">
      <header className="border-b border-slate-200 px-6 py-4">
        <h1 className="text-lg font-semibold">AI Agent 平台</h1>
      </header>
      <section className="mx-auto flex max-w-6xl gap-6 px-6 py-8">
        <aside className="w-56 shrink-0 border-r border-slate-200 pr-4" aria-label="主导航">
          <nav className="space-y-2">
            <NavLink
              to="/dashboard"
              className={({ isActive }) =>
                `block rounded px-3 py-2 text-sm ${isActive ? 'bg-slate-100 font-medium' : 'text-muted-foreground'}`
              }
            >
              工作台
            </NavLink>
            <NavLink
              to="/agents"
              className={({ isActive }) =>
                `block rounded px-3 py-2 text-sm ${isActive ? 'bg-slate-100 font-medium' : 'text-muted-foreground'}`
              }
            >
              Agent
            </NavLink>
            <NavLink
              to="/knowledge"
              className={({ isActive }) =>
                `block rounded px-3 py-2 text-sm ${isActive ? 'bg-slate-100 font-medium' : 'text-muted-foreground'}`
              }
            >
              知识库
            </NavLink>
            <NavLink
              to="/chat"
              className={({ isActive }) =>
                `block rounded px-3 py-2 text-sm ${isActive ? 'bg-slate-100 font-medium' : 'text-muted-foreground'}`
              }
            >
              聊天
            </NavLink>
          </nav>
        </aside>
        <div className="flex-1">
          <Outlet />
        </div>
      </section>
    </main>
  )
}

export default AppShell
