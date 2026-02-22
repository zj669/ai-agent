import { memo } from 'react'
import { Handle, Position } from '@xyflow/react'
import { cn } from '../../../lib/utils'

const handleBase = '!h-4 !w-4 !rounded-none !border-none !bg-transparent !outline-none'

export const NodeTargetHandle = memo(({ handleId, className }: { handleId: string; className?: string }) => (
  <Handle
    id={handleId}
    type="target"
    position={Position.Left}
    className={cn(
      handleBase,
      'after:absolute after:left-1.5 after:top-1 after:h-2 after:w-0.5 after:rounded-full after:bg-blue-500',
      className,
    )}
  />
))
NodeTargetHandle.displayName = 'NodeTargetHandle'

export const NodeSourceHandle = memo(({ handleId, className }: { handleId: string; className?: string }) => (
  <Handle
    id={handleId}
    type="source"
    position={Position.Right}
    className={cn(
      handleBase,
      'after:absolute after:right-1.5 after:top-1 after:h-2 after:w-0.5 after:rounded-full after:bg-blue-500',
      className,
    )}
  />
))
NodeSourceHandle.displayName = 'NodeSourceHandle'
