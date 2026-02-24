import { useEffect, useRef, useState } from 'react'
import { subscribeAgentStream } from '../api/swarmService'

interface AgentStreamState {
  content: string
  reasoning: string
  toolCalls: Array<{ name: string; args: string; result?: string }>
  status: 'idle' | 'streaming' | 'done' | 'error'
  error?: string
}

export function useAgentStream(agentId: number | null) {
  const [state, setState] = useState<AgentStreamState>({
    content: '', reasoning: '', toolCalls: [], status: 'idle',
  })
  const esRef = useRef<EventSource | null>(null)

  useEffect(() => {
    if (!agentId) {
      setState({ content: '', reasoning: '', toolCalls: [], status: 'idle' })
      return
    }

    const handler = (event: MessageEvent) => {
      const data = event.data
      switch (event.type) {
        case 'agent.stream':
          setState(prev => ({
            ...prev,
            status: 'streaming',
            content: prev.content + (data ?? ''),
          }))
          break
        case 'agent.done':
          setState(prev => ({ ...prev, status: 'done' }))
          break
        case 'agent.error':
          setState(prev => ({ ...prev, status: 'error', error: data }))
          break
      }
    }

    const es = subscribeAgentStream(agentId, handler)
    esRef.current = es

    return () => {
      es.close()
      esRef.current = null
    }
  }, [agentId])

  const reset = () => {
    setState({ content: '', reasoning: '', toolCalls: [], status: 'idle' })
  }

  return { ...state, reset }
}
