import { Switch, Tooltip } from 'antd';
import { Maximize2, Minimize2 } from 'lucide-react';

interface NodeSizeToggleProps {
  useLargeNodes: boolean;
  onChange: (value: boolean) => void;
}

export function NodeSizeToggle({ useLargeNodes, onChange }: NodeSizeToggleProps) {
  return (
    <Tooltip title={useLargeNodes ? '切换到紧凑节点' : '切换到大尺寸节点'}>
      <div className="flex items-center gap-2 px-3 py-1.5 bg-white border border-gray-200 rounded-lg">
        <Minimize2 className="w-4 h-4 text-gray-500" />
        <Switch
          size="small"
          checked={useLargeNodes}
          onChange={onChange}
        />
        <Maximize2 className="w-4 h-4 text-gray-500" />
      </div>
    </Tooltip>
  );
}
