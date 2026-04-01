import { Tag } from 'antd'
import type { ServerStatus } from '../types/mcp'

interface Props {
  status: ServerStatus
}

const statusConfig: Record<ServerStatus, { label: string; color: string }> = {
  DISCONNECTED: { label: '未连接', color: 'default' },
  CONNECTING: { label: '连接中', color: 'processing' },
  CONNECTED: { label: '已连接', color: 'success' },
  ERROR: { label: '连接异常', color: 'error' },
}

export default function ServerStatusTag({ status }: Props) {
  const config = statusConfig[status] || { label: status, color: 'default' }
  return <Tag color={config.color}>{config.label}</Tag>
}
