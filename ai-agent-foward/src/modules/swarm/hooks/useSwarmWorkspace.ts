import { useState, useEffect, useCallback } from 'react'
import { listWorkspaces, createWorkspace, deleteWorkspace } from '../api/swarmService'
import type { SwarmWorkspace, WorkspaceDefaults } from '../types/swarm'

export function useSwarmWorkspace() {
  const [workspaces, setWorkspaces] = useState<SwarmWorkspace[]>([])
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listWorkspaces()
      setWorkspaces(data)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const create = useCallback(async (name: string): Promise<WorkspaceDefaults> => {
    const result = await createWorkspace(name)
    await load()
    return result
  }, [load])

  const remove = useCallback(async (id: number) => {
    await deleteWorkspace(id)
    await load()
  }, [load])

  return { workspaces, loading, reload: load, create, remove }
}
