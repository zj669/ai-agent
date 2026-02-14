import { Input } from 'antd';
import { Binary, Network, Layers3, Sparkles } from 'lucide-react';

interface WorkflowConfigPanelProps {
  workflowName: string;
  workflowDescription: string;
  onWorkflowNameChange: (value: string) => void;
  onWorkflowDescriptionChange: (value: string) => void;
  nodeCount: number;
  edgeCount: number;
}

export function WorkflowConfigPanel({
  workflowName,
  workflowDescription,
  onWorkflowNameChange,
  onWorkflowDescriptionChange,
  nodeCount,
  edgeCount
}: WorkflowConfigPanelProps) {
  return (
    <aside className="workflow-floating-panel pointer-events-auto absolute left-5 top-5 z-20 w-[340px] rounded-2xl border border-white/40 bg-white/70 shadow-xl backdrop-blur-xl">
      <div className="border-b border-white/50 px-4 py-3">
        <div className="text-base font-semibold text-slate-800">Workflow Config</div>
        <div className="mt-1 text-xs text-slate-500">左侧悬浮配置面板（Dify 风格）</div>
      </div>

      <div className="max-h-[calc(100vh-180px)] overflow-y-auto space-y-4 px-4 py-4">
        <div>
          <label className="mb-2 block text-xs font-medium text-slate-700">名称</label>
          <Input
            value={workflowName}
            onChange={(event) => onWorkflowNameChange(event.target.value)}
            maxLength={20}
            showCount
            placeholder="输入工作流名称"
            className="rounded-xl"
          />
        </div>

        <div>
          <label className="mb-2 block text-xs font-medium text-slate-700">简介</label>
          <Input.TextArea
            value={workflowDescription}
            onChange={(event) => onWorkflowDescriptionChange(event.target.value)}
            maxLength={120}
            showCount
            rows={4}
            placeholder="描述目标、输入输出和主要能力"
            className="rounded-xl"
          />
        </div>

        <div className="space-y-2 rounded-2xl border border-violet-100 bg-violet-50/60 p-3">
          <div className="text-xs font-medium text-violet-700">编排概览</div>
          <div className="flex items-center justify-between rounded-lg bg-white/80 px-2.5 py-2">
            <span className="flex items-center gap-2 text-xs text-slate-600"><Binary className="h-4 w-4 text-violet-600" />节点数量</span>
            <span className="text-sm font-semibold text-slate-800">{nodeCount}</span>
          </div>
          <div className="flex items-center justify-between rounded-lg bg-white/80 px-2.5 py-2">
            <span className="flex items-center gap-2 text-xs text-slate-600"><Network className="h-4 w-4 text-violet-600" />连接数量</span>
            <span className="text-sm font-semibold text-slate-800">{edgeCount}</span>
          </div>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white/80 p-3">
          <div className="mb-1 flex items-center gap-2 text-sm font-medium text-slate-700">
            <Layers3 className="h-4 w-4 text-violet-600" />布局建议
          </div>
          <p className="text-xs leading-5 text-slate-500">
            左侧配置，画布中拖拽连接，底部 Dock 快速添加节点。
          </p>
        </div>

        <div className="rounded-2xl border border-amber-200/70 bg-amber-50/70 p-3">
          <div className="mb-1 flex items-center gap-2 text-sm font-medium text-amber-800">
            <Sparkles className="h-4 w-4" />运行提示
          </div>
          <p className="text-xs leading-5 text-amber-700/90">运行前建议先保存草稿，避免版本冲突。</p>
        </div>
      </div>
    </aside>
  );
}
