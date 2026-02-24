import { useState, useCallback, useEffect } from 'react'
import { listAgents } from '../api/swarmService'
import type { SwarmAgent } from '../types/swarm'

export function useSwarmAgents(workspaceId: number | null) {
  const [agents, setAgents] = useState<SwarmAgent[]>([])
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!workspaceId) return
    setLoading(true)
    try {
      const data = await listAgents(workspaceId)
      setAgents(data)
    } finally {
      setLoading(false)
    }
  }, [workspaceId])

  useEffect(() => { load() }, [load])

  const addAgent = useCallback((agent: SwarmAgent) => {
    setAgents(prev => {
      if (prev.some(a => a.id === agent.id)) return prev
      return [...prev, agent]
    })
  }, [])

  return { agents, loading, reload: load, addAgent }
}
