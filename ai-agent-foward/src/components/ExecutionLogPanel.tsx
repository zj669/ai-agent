import { useEffect, useRef } from 'react';
import { X, Terminal } from 'lucide-react';

interface ExecutionLogPanelProps {
  logs: string[];
  isExecuting: boolean;
  onClose: () => void;
}

/**
 * 执行日志面板
 */
export function ExecutionLogPanel({ logs, isExecuting, onClose }: ExecutionLogPanelProps) {
  const logContainerRef = useRef<HTMLDivElement>(null);

  // 自动滚动到底部
  useEffect(() => {
    if (logContainerRef.current) {
      logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
    }
  }, [logs]);

  return (
    <div className="workflow-log-panel rounded-xl p-4 w-96 max-h-[60vh] flex flex-col">
      {/* 标题栏 */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Terminal className="w-5 h-5 text-cyan-600" />
          <h3 className="text-lg font-semibold text-slate-800">执行日志</h3>
          {isExecuting && (
            <span className="px-2 py-1 text-xs bg-cyan-500/20 text-cyan-300 rounded-full border border-cyan-500/40">
              执行中
            </span>
          )}
        </div>
        <button
          onClick={onClose}
          className="workflow-toolbar-btn p-1 rounded transition-colors"
        >
          <X className="w-5 h-5 text-slate-500" />
        </button>
      </div>

      {/* 日志内容 */}
      <div
        ref={logContainerRef}
        className="flex-1 bg-slate-950/85 border border-slate-700 rounded-lg p-3 overflow-y-auto font-mono text-sm"
      >
        {logs.length === 0 ? (
          <div className="text-slate-500 text-center py-8">暂无日志</div>
        ) : (
          logs.map((log, index) => (
            <div
              key={index}
              className={`
                mb-1 ${
                  log.includes('[错误]')
                    ? 'text-red-400'
                    : log.includes('[连接成功]')
                    ? 'text-green-400'
                    : 'text-gray-300'
                }
              `}
            >
              {log}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
