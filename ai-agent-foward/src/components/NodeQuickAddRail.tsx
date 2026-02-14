import {
  PlayCircle,
  StopCircle,
  MessageSquare,
  Globe,
  GitBranch,
  Wrench,
  Plus
} from 'lucide-react';
import { NodeType } from '../types/workflow';

interface NodeQuickAddRailProps {
  onAddNode: (type: NodeType) => void;
}

const NODE_ITEMS = [
  { type: NodeType.START, label: '开始', icon: PlayCircle, tone: 'text-emerald-700 bg-emerald-100' },
  { type: NodeType.LLM, label: '大模型', icon: MessageSquare, tone: 'text-violet-700 bg-violet-100' },
  { type: NodeType.CONDITION, label: '条件', icon: GitBranch, tone: 'text-amber-700 bg-amber-100' },
  { type: NodeType.HTTP, label: 'HTTP', icon: Globe, tone: 'text-blue-700 bg-blue-100' },
  { type: NodeType.TOOL, label: '工具', icon: Wrench, tone: 'text-cyan-700 bg-cyan-100' },
  { type: NodeType.END, label: '结束', icon: StopCircle, tone: 'text-rose-700 bg-rose-100' }
];

export function NodeQuickAddRail({ onAddNode }: NodeQuickAddRailProps) {
  return (
    <aside className="workflow-bottom-dock pointer-events-auto absolute bottom-5 left-1/2 z-30 -translate-x-1/2">
      <div className="flex items-center gap-2 rounded-2xl border border-white/40 bg-white/75 p-2.5 shadow-2xl backdrop-blur-xl">
        {NODE_ITEMS.map((item) => {
          const Icon = item.icon;
          return (
            <div
              key={item.type}
              className="group flex min-w-[88px] select-none flex-col items-center gap-1.5 rounded-xl border border-slate-200/80 bg-white/85 px-2 py-2 transition hover:-translate-y-0.5 hover:border-violet-300 hover:shadow-sm"
            >
              <div className={`flex h-8 w-8 items-center justify-center rounded-xl ${item.tone}`}>
                <Icon className="h-4 w-4" />
              </div>
              <span className="text-xs font-medium text-slate-700">{item.label}</span>
              <button
                type="button"
                onClick={() => onAddNode(item.type)}
                className="inline-flex h-6 w-6 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 transition hover:border-violet-300 hover:text-violet-700"
              >
                <Plus className="h-3.5 w-3.5" />
              </button>
            </div>
          );
        })}
      </div>
    </aside>
  );
}
