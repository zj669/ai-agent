import { Handle, Position } from '@xyflow/react'
import type { AgentStatus } from '../../types/swarm'

const STATUS_COLORS: Record<AgentStatus, string> = {
  IDLE: '#52c41a',
  BUSY: '#ff4d4f',
  WAKING: '#faad14',
  STOPPED: '#d9d9d9',
}

interface NodeData {
  role: string
  status: AgentStatus
  agentId: number
  [key: string]: unknown
}

export default function GraphNode({ data }: { data: NodeData }) {
  const isHuman = data.role === 'human'
  const statusColor = STATUS_COLORS[data.status] ?? '#d9d9d9'

  return (
    <div style={{
      width: 64, height: 64, borderRadius: '50%',
      background: isHuman ? '#1677ff' : '#fff',
      border: `3px solid ${isHuman ? '#1677ff' : '#722ed1'}`,
      display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center',
      cursor: isHuman ? 'default' : 'pointer', position: 'relative',
      boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
    }}>
      {/* 状态灯 */}
      <div style={{
        position: 'absolute', top: 2, right: 2,
        width: 10, height: 10, borderRadius: '50%',
        background: statusColor,
        border: '2px solid #fff',
        animation: data.status === 'BUSY' ? 'pulse 1s infinite' : undefined,
      }} />

      {/* 图标 */}
      <span style={{ fontSize: 20 }}>{isHuman ? '👤' : '🤖'}</span>

      {/* 角色名 */}
      <div style={{
        position: 'absolute', bottom: -20,
        fontSize: 11, color: '#595959',
        whiteSpace: 'nowrap', textAlign: 'center',
      }}>
        {data.role}
      </div>

      <Handle type="target" position={Position.Top} style={{ opacity: 0 }} />
      <Handle type="source" position={Position.Bottom} style={{ opacity: 0 }} />

      <style>{`
        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.4; }
        }
      `}</style>
    </div>
  )
}
