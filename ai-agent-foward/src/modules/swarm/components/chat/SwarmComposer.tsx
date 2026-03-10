import { useState } from 'react'
import { Input, Button, Space } from 'antd'
import { SendOutlined, StopOutlined } from '@ant-design/icons'

const { TextArea } = Input

interface Props {
  onSend: (content: string) => Promise<void>
  onStop?: () => Promise<void>
  disabled?: boolean
  isStreaming?: boolean
  agentName?: string
}

export default function SwarmComposer({ onSend, onStop, disabled, isStreaming, agentName }: Props) {
  const [value, setValue] = useState('')
  const [sending, setSending] = useState(false)
  const [stopping, setStopping] = useState(false)

  const handleSend = async () => {
    const text = value.trim()
    if (!text) return
    setSending(true)
    try {
      await onSend(text)
      setValue('')
    } finally {
      setSending(false)
    }
  }

  const handleStop = async () => {
    if (!onStop) return
    setStopping(true)
    try {
      await onStop()
    } finally {
      setStopping(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && e.ctrlKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <Space.Compact style={{ width: '100%' }}>
      <TextArea
        value={value}
        onChange={e => setValue(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={agentName ? `向 ${agentName} 发送消息，Ctrl+Enter 发送` : '输入消息，Ctrl+Enter 发送'}
        autoSize={{ minRows: 1, maxRows: 4 }}
        disabled={disabled || isStreaming}
        style={{ flex: 1 }}
      />
      {isStreaming ? (
        <Button
          danger
          icon={<StopOutlined />}
          onClick={handleStop}
          loading={stopping}
        >
          终止
        </Button>
      ) : (
        <Button
          type="primary"
          icon={<SendOutlined />}
          onClick={handleSend}
          loading={sending}
          disabled={disabled || !value.trim()}
        >
          发送
        </Button>
      )}
    </Space.Compact>
  )
}
