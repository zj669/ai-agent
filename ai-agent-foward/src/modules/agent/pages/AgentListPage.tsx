import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createAgent, fetchAgentList, type AgentListItem } from '../api/agentService'

function AgentListPage() {
  const navigate = useNavigate()
  const [agents, setAgents] = useState<AgentListItem[]>([])
  const [listError, setListError] = useState<string>('')
  const [createError, setCreateError] = useState<string>('')
  const [isCreating, setIsCreating] = useState(false)

  useEffect(() => {
    void fetchAgentList()
      .then((data) => {
        setAgents(data)
        setListError('')
      })
      .catch(() => {
        setAgents([])
        setListError('列表加载失败，请稍后重试')
      })
  }, [])

  const handleCreateAgent = async () => {
    if (isCreating) {
      return
    }

    setCreateError('')
    setIsCreating(true)
    try {
      const { id } = await createAgent()
      navigate(`/agents/${id}/workflow`)
    } catch {
      setCreateError('创建失败，请稍后重试')
    } finally {
      setIsCreating(false)
    }
  }

  return (
    <section>
      <h2 className="text-2xl font-semibold">Agent 列表</h2>
      <button
        type="button"
        className="mt-4 rounded bg-slate-900 px-3 py-2 text-sm text-white disabled:cursor-not-allowed disabled:bg-slate-400"
        onClick={handleCreateAgent}
        disabled={isCreating}
      >
        {isCreating ? '创建中...' : '新建 Agent'}
      </button>
      {createError ? <p className="mt-2 text-sm text-red-600">{createError}</p> : null}
      {listError ? <p className="mt-2 text-sm text-red-600">{listError}</p> : null}

      <ul className="mt-4 space-y-2 text-sm">
        {agents.map((agent) => (
          <li key={agent.id} className="rounded border border-slate-200 px-3 py-2">
            <div className="font-medium">{agent.name}</div>
            {agent.description ? <div className="text-slate-500">{agent.description}</div> : null}
          </li>
        ))}
      </ul>
    </section>
  )
}

export default AgentListPage
