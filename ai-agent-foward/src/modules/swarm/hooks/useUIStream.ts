import { useEffect, useRef } from 'react'
import { subscribeUIStream } from '../api/swarmService'

interface UIStreamCallbacks {
  onAgentCreated?: (data: string) => void
  onMessageCreated?: (data: string) => void
  onLlmStart?: (data: string) => void
  onLlmDone?: (data: string) => void
}

export function useUIStream(workspaceId: number | null, callbacks: UIStreamCallbacks) {
  const callbacksRef = useRef(callbacks)
  callbacksRef.current = callbacks

  useEffect(() => {
    if (!workspaceId) return

    const handler = (event: MessageEvent) => {
      const cb = callbacksRef.current
      switch (event.type) {
        case 'ui.agent.created':
          cb.onAgentCreated?.(event.data)
          break
        case 'ui.message.created':
          cb.onMessageCreated?.(event.data)
          break
        case 'ui.agent.llm.start':
          cb.onLlmStart?.(event.data)
          break
        case 'ui.agent.llm.done':
          cb.onLlmDone?.(event.data)
          break
      }
    }

    const es = subscribeUIStream(workspaceId, handler)
    return () => es.close()
  }, [workspaceId])
}
