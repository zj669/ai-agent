import { useState, useCallback } from 'react'
import { getMessages, sendMessage } from '../api/swarmService'
import type { SwarmMessage } from '../types/swarm'

export function useSwarmMessages(groupId: number | null, readerId?: number) {
  const [messages, setMessages] = useState<SwarmMessage[]>([])
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!groupId) return
    setLoading(true)
    try {
      const data = await getMessages(groupId, !!readerId, readerId)
      setMessages(data)
    } finally {
      setLoading(false)
    }
  }, [groupId, readerId])

  const send = useCallback(async (senderId: number, content: string) => {
    if (!groupId) return
    const msg = await sendMessage(groupId, senderId, content)
    setMessages(prev => [...prev, msg])
    return msg
  }, [groupId])

  const appendMessage = useCallback((msg: SwarmMessage) => {
    setMessages(prev => {
      if (prev.some(m => m.id === msg.id)) return prev
      return [...prev, msg]
    })
  }, [])

  return { messages, loading, load, send, appendMessage }
}
