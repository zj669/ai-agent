import { useNavigate } from 'react-router-dom'

function DashboardPage() {
  const navigate = useNavigate()

  return (
    <section>
      <h2 className="text-2xl font-semibold">工作台</h2>
      <p className="mt-3 text-sm text-muted-foreground">这是受保护页面，需登录后访问。</p>
      <button
        type="button"
        className="mt-4 rounded bg-slate-900 px-3 py-2 text-sm text-white"
        onClick={() => navigate('/agents')}
      >
        新建 Agent
      </button>
    </section>
  )
}

export default DashboardPage
