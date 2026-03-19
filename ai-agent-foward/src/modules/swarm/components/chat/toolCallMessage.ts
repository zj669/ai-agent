import type { SwarmMessage } from '../../types/swarm'

export interface ToolCallViewData {
  tool: string
  args: string
  result: string
}

function formatPayload(value: unknown): string {
  if (value === null || value === undefined) {
    return ''
  }

  return typeof value === 'string' ? value : JSON.stringify(value, null, 2)
}

export function parseToolCallMessage(message: Pick<SwarmMessage, 'contentType' | 'content'>): ToolCallViewData | null {
  const raw = message.content?.trim()
  if (!raw) {
    return null
  }

  if (message.contentType !== 'tool_call' && !raw.startsWith('{')) {
    return null
  }

  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>
    if (typeof parsed.tool !== 'string' || parsed.tool.trim() === '') {
      return null
    }

    const hasArgs = Object.prototype.hasOwnProperty.call(parsed, 'args')
    const hasResult = Object.prototype.hasOwnProperty.call(parsed, 'result')
    if (!hasArgs && !hasResult) {
      return null
    }

    return {
      tool: parsed.tool,
      args: formatPayload(parsed.args),
      result: formatPayload(parsed.result),
    }
  } catch {
    return null
  }
}

export function getToolCallSignature(message: Pick<SwarmMessage, 'contentType' | 'content'>): string | null {
  const parsed = parseToolCallMessage(message)
  if (!parsed) {
    return null
  }

  return JSON.stringify(parsed)
}
