import { useState, useCallback } from 'react'
import { getMessages, sendMessage } from '../api/swarmService'
import type { SwarmMessage } from '../types/swarm'

export function useSwarmMessages(groupId: number | null, userId?: number) {
  const [messages, setMessages] = useState<SwarmMessage[]>([])
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!groupId) return
    setLoading(true)
    try {
      const data = await getMessages(groupId, !!userId, userId)
      setMessages(prev => {
        // Keep unconfirmed optimistic messages the server hasn't echoed back yet
        const stillPending = prev.filter(
          opt => opt.id < 0 &&
            !data.some(d => d.senderId === opt.senderId && d.content === opt.content)
        )
        return [...data, ...stillPending]
      })
    } finally {
      setLoading(false)
    }
  }, [groupId, userId])

  const send = useCallback(async (senderId: number, content: string) => {
    if (!groupId) return
    const optimistic: SwarmMessage = {
      id: -Date.now(),
      groupId,
      senderId,
      content,
      contentType: 'text',
      sendTime: new Date().toISOString(),
    }
    setMessages(prev => [...prev, optimistic])
    try {
      const msg = await sendMessage(groupId, senderId, content)
      setMessages(prev => {
        if (!msg) return prev // API returned null — keep optimistic, SSE will confirm
        return prev.map(m => m.id === optimistic.id ? msg : m)
      })
      return msg
    } catch {
      setMessages(prev => prev.filter(m => m.id !== optimistic.id))
    }
  }, [groupId])

  return { messages, loading, load, send }
}
