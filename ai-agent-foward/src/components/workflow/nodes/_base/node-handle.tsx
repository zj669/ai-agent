import { Handle, Position, type HandleProps } from '@xyflow/react';

interface NodeHandleProps extends Omit<HandleProps, 'type' | 'position'> {
  direction: 'in' | 'out';
}

export function WorkflowNodeHandle({ direction, ...rest }: NodeHandleProps) {
  return (
    <Handle
      type={direction === 'in' ? 'target' : 'source'}
      position={direction === 'in' ? Position.Left : Position.Right}
      {...rest}
    />
  );
}
