import { describe, expect, it } from 'vitest'
import { getToolCallSignature, parseToolCallMessage } from '../toolCallMessage'

describe('toolCallMessage', () => {
  it('parses tool_call messages into badge data', () => {
    const parsed = parseToolCallMessage({
      contentType: 'tool_call',
      content: '{"tool":"createAgent","args":{"role":"editor"},"result":"ok"}',
    })

    expect(parsed).toEqual({
      tool: 'createAgent',
      args: '{\n  "role": "editor"\n}',
      result: 'ok',
    })
  })

  it('recognizes raw tool json even when contentType is text', () => {
    const parsed = parseToolCallMessage({
      contentType: 'text',
      content: '{"tool":"createAgent","args":"","result":"{\\"assistantAgentId\\":30}"}',
    })

    expect(parsed?.tool).toBe('createAgent')
    expect(parsed?.result).toContain('assistantAgentId')
  })

  it('returns null for normal assistant text', () => {
    expect(parseToolCallMessage({
      contentType: 'text',
      content: '这是普通回复，不是工具调用',
    })).toBeNull()
  })

  it('builds the same signature for duplicated tool payloads', () => {
    const textMessage = {
      contentType: 'text' as const,
      content: '{"tool":"createAgent","args":{"role":"editor"},"result":"ok"}',
    }
    const toolMessage = {
      contentType: 'tool_call' as const,
      content: '{"tool":"createAgent","args":{"role":"editor"},"result":"ok"}',
    }

    expect(getToolCallSignature(textMessage)).toBe(getToolCallSignature(toolMessage))
  })
})
