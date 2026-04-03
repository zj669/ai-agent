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
      setMessages((prev: SwarmMessage[]) => {
        const optimistic = prev.filter(m => m.id < 0)
        const prevServerIds = new Set(
          prev.filter(m => m.id > 0).map(m => m.id)
        )
        // Replace optimistic entries if server returned a confirmed version (same content + sender + timestamp)
        const confirmed = optimistic.map(opt => {
          const match = data.find(
            d =>
              d.id > 0 &&
              d.senderId === opt.senderId &&
              d.content === opt.content
          )
          return match ?? opt
        })
        // Add server messages not yet present
        const newFromServer = data.filter(
          d => d.id > 0 && !prevServerIds.has(d.id)
        )
        const merged = [...confirmed, ...newFromServer]
        // Safety: if server shrunk the list (race), keep optimistic
        if (optimistic.length > 0 && merged.length < prev.length) {
          return prev
        }
        return merged
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

  const appendMessage = useCallback((msg: SwarmMessage) => {
    setMessages(prev => {
      if (prev.some(m => m.id === msg.id)) return prev
      return [...prev, msg]
    })
  }, [])

  return { messages, loading, load, send, appendMessage }
}
