import { useState, useEffect, useMemo } from 'react';
import {
  Form,
  Input,
  Button,
  Collapse,
  Select,
  Slider,
  InputNumber,
  Switch,
  Space,
  Divider,
  Tag,
  Popconfirm,
  Tooltip,
  message
} from 'antd';
import {
  X,
  Play,
  PlayCircle,
  StopCircle,
  MessageSquare,
  Globe,
  GitBranch,
  Wrench,
  Copy,
  Trash2,
  Save,
  Plus,
  Minus,
  Variable,
  Settings,
  Zap,
  FileText,
  Bug,
  Database
} from 'lucide-react';
import { ExecutionContextDTO, ReactFlowNode, NodeType } from '../types/workflow';

interface NodePropertiesPanelProps {
  node: ReactFlowNode | null;
  onClose: () => void;
  onUpdate: (nodeId: string, data: Partial<ReactFlowNode['data']>) => void;
  onDelete: (nodeId: string) => void;
  onDuplicate?: (nodeId: string) => void;
  onStepRun?: (nodeId: string) => void | Promise<void>;
  nodeExecutionLogs?: string[];
  executionContext?: ExecutionContextDTO | null;
  isExecuting?: boolean;
}

const NODE_TYPE_CONFIG = {
  [NodeType.START]: { label: '开始节点', icon: PlayCircle, color: 'text-emerald-300', bgColor: 'bg-emerald-500/15', borderColor: 'border-emerald-400/30' },
  [NodeType.END]: { label: '结束节点', icon: StopCircle, color: 'text-rose-300', bgColor: 'bg-rose-500/15', borderColor: 'border-rose-400/30' },
  [NodeType.LLM]: { label: 'LLM 节点', icon: MessageSquare, color: 'text-violet-300', bgColor: 'bg-violet-500/15', borderColor: 'border-violet-400/30' },
  [NodeType.HTTP]: { label: 'HTTP 节点', icon: Globe, color: 'text-blue-300', bgColor: 'bg-blue-500/15', borderColor: 'border-blue-400/30' },
  [NodeType.CONDITION]: { label: '条件节点', icon: GitBranch, color: 'text-amber-300', bgColor: 'bg-amber-500/15', borderColor: 'border-amber-400/30' },
  [NodeType.TOOL]: { label: '工具节点', icon: Wrench, color: 'text-cyan-300', bgColor: 'bg-cyan-500/15', borderColor: 'border-cyan-400/30' }
};

const AVAILABLE_MODELS = [
  { value: 'gpt-4o', label: 'GPT-4o' },
  { value: 'gpt-4o-mini', label: 'GPT-4o Mini' },
  { value: 'gpt-4-turbo', label: 'GPT-4 Turbo' },
  { value: 'gpt-3.5-turbo', label: 'GPT-3.5 Turbo' },
  { value: 'claude-3-opus', label: 'Claude 3 Opus' },
  { value: 'claude-3-sonnet', label: 'Claude 3 Sonnet' },
  { value: 'claude-3-haiku', label: 'Claude 3 Haiku' },
  { value: 'deepseek-chat', label: 'DeepSeek Chat' },
  { value: 'qwen-turbo', label: 'Qwen Turbo' }
];

const HTTP_METHODS = [
  { value: 'GET', label: 'GET' },
  { value: 'POST', label: 'POST' },
  { value: 'PUT', label: 'PUT' },
  { value: 'DELETE', label: 'DELETE' },
  { value: 'PATCH', label: 'PATCH' }
];

const CONDITION_MODES = [
  { value: 'EXPRESSION', label: '表达式模式 (SpEL)' },
  { value: 'LLM', label: 'LLM 语义理解' }
];

function prettyJson(value: unknown): string {
  if (!value || (typeof value === 'object' && Object.keys(value as object).length === 0)) return '{}';
  if (typeof value === 'string') return value;
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return '{}';
  }
}

export function NodePropertiesPanel({
  node,
  onClose,
  onUpdate,
  onDelete,
  onDuplicate,
  onStepRun,
  nodeExecutionLogs = [],
  executionContext,
  isExecuting = false
}: NodePropertiesPanelProps) {
  const [form] = Form.useForm();
  const [activeKeys, setActiveKeys] = useState<string[]>(['basic', 'config']);
  const [tab, setTab] = useState<'config' | 'debug' | 'variables'>('config');

  const typeConfig = useMemo(() => {
    if (!node) return null;
    return NODE_TYPE_CONFIG[node.data.nodeType] || NODE_TYPE_CONFIG[NodeType.START];
  }, [node]);

  useEffect(() => {
    if (!node) return;
    form.setFieldsValue({
      label: node.data.label,
      description: node.data.config?.properties?.description || '',
      model: node.data.config?.properties?.model || 'gpt-4o',
      systemPrompt: node.data.config?.properties?.systemPrompt || '',
      temperature: node.data.config?.properties?.temperature ?? 0.7,
      maxTokens: node.data.config?.properties?.maxTokens || 2048,
      method: node.data.config?.properties?.method || 'GET',
      url: node.data.config?.properties?.url || '',
      headers: node.data.config?.properties?.headers || '',
      body: node.data.config?.properties?.body || '',
      conditionMode: node.data.config?.properties?.mode || 'EXPRESSION',
      expression: node.data.config?.properties?.expression || '',
      branches: node.data.config?.properties?.branches || [{ name: '分支1', condition: '' }],
      toolName: node.data.config?.properties?.toolName || '',
      toolParams: node.data.config?.properties?.toolParams || '',
      timeoutMs: node.data.config?.timeoutMs || 30000,
      requiresHumanReview: node.data.config?.humanReviewConfig?.enabled || false,
      reviewDescription: node.data.config?.humanReviewConfig?.description || '',
      maxRetries: node.data.config?.retryPolicy?.maxRetries || 0,
      retryDelayMs: node.data.config?.retryPolicy?.retryDelayMs || 1000,
      inputs: prettyJson(node.data.inputs),
      outputs: prettyJson(node.data.outputs)
    });
  }, [node, form]);

  const parseJsonField = (raw: string, fieldLabel: string) => {
    const input = raw?.trim();
    if (!input) return {};
    try {
      return JSON.parse(input);
    } catch {
      message.error(`${fieldLabel} 不是合法 JSON`);
      throw new Error(`${fieldLabel} invalid json`);
    }
  };

  const handleSave = () => {
    form.validateFields().then(values => {
      if (!node) return;
      const { label, description, ...configProps } = values;
      const inputs = parseJsonField(configProps.inputs, '输入变量');
      const outputs = parseJsonField(configProps.outputs, '输出变量');

      const config = {
        ...node.data.config,
        properties: {
          description,
          ...(node.data.nodeType === NodeType.LLM && {
            model: configProps.model,
            systemPrompt: configProps.systemPrompt,
            temperature: configProps.temperature,
            maxTokens: configProps.maxTokens
          }),
          ...(node.data.nodeType === NodeType.HTTP && {
            method: configProps.method,
            url: configProps.url,
            headers: configProps.headers,
            body: configProps.body
          }),
          ...(node.data.nodeType === NodeType.CONDITION && {
            mode: configProps.conditionMode,
            expression: configProps.expression,
            branches: configProps.branches
          }),
          ...(node.data.nodeType === NodeType.TOOL && {
            toolName: configProps.toolName,
            toolParams: configProps.toolParams
          })
        },
        timeoutMs: configProps.timeoutMs,
        humanReviewConfig: {
          enabled: configProps.requiresHumanReview,
          description: configProps.reviewDescription
        },
        retryPolicy: {
          maxRetries: configProps.maxRetries,
          retryDelayMs: configProps.retryDelayMs
        }
      };

      onUpdate(node.id, { label, config, inputs, outputs });
      message.success('配置已保存');
    });
  };

  const insertVariable = (variable: string) => {
    const currentValue = form.getFieldValue('systemPrompt') || '';
    form.setFieldValue('systemPrompt', `${currentValue}{{${variable}}}`);
  };

  const renderLLMConfig = () => (
    <>
      <Form.Item label="模型选择" name="model" rules={[{ required: true, message: '请选择模型' }]}>
        <Select options={AVAILABLE_MODELS} placeholder="选择 AI 模型" showSearch optionFilterProp="label" />
      </Form.Item>
      <Form.Item label="系统提示词" required>
        <div className="space-y-2">
          <div className="flex gap-2 flex-wrap">
            {['input', 'context', 'history'].map((it) => (
              <Tooltip key={it} title={`插入 ${it} 变量`}>
                <Tag className="cursor-pointer hover:bg-blue-100" onClick={() => insertVariable(it)}>
                  <Variable className="w-3 h-3 inline mr-1" />
                  {it}
                </Tag>
              </Tooltip>
            ))}
          </div>
          <Form.Item name="systemPrompt" noStyle rules={[{ required: true, message: '请输入系统提示词' }]}>
            <Input.TextArea rows={6} placeholder="输入系统提示词，可使用 {{变量名}} 插入变量..." className="font-mono text-sm" />
          </Form.Item>
        </div>
      </Form.Item>
      <div className="grid grid-cols-2 gap-4">
        <Form.Item label="温度" name="temperature">
          <Slider min={0} max={2} step={0.1} marks={{ 0: '精确', 1: '平衡', 2: '创意' }} />
        </Form.Item>
        <Form.Item label="最大 Token" name="maxTokens">
          <InputNumber min={1} max={128000} step={256} className="w-full" placeholder="2048" />
        </Form.Item>
      </div>
    </>
  );

  const renderHTTPConfig = () => (
    <>
      <div className="grid grid-cols-3 gap-4">
        <Form.Item label="请求方法" name="method" rules={[{ required: true }]}>
          <Select options={HTTP_METHODS} />
        </Form.Item>
        <Form.Item label="URL" name="url" className="col-span-2" rules={[{ required: true, message: '请输入 URL' }]}>
          <Input placeholder="https://api.example.com/endpoint" />
        </Form.Item>
      </div>
      <Form.Item label="请求头 (JSON)" name="headers">
        <Input.TextArea rows={3} placeholder='{"Content-Type": "application/json"}' className="font-mono text-sm" />
      </Form.Item>
      <Form.Item label="请求体" name="body">
        <Input.TextArea rows={4} placeholder='{"key": "value"}' className="font-mono text-sm" />
      </Form.Item>
    </>
  );

  const renderConditionConfig = () => (
    <>
      <Form.Item label="条件模式" name="conditionMode" rules={[{ required: true }]}>
        <Select options={CONDITION_MODES} />
      </Form.Item>
      <Form.Item noStyle shouldUpdate={(prev, curr) => prev.conditionMode !== curr.conditionMode}>
        {({ getFieldValue }) =>
          getFieldValue('conditionMode') === 'EXPRESSION' ? (
            <Form.Item label="条件表达式 (SpEL)" name="expression">
              <Input.TextArea rows={3} placeholder="#input.score > 80" className="font-mono text-sm" />
            </Form.Item>
          ) : (
            <Form.Item label="LLM 判断提示词" name="expression">
              <Input.TextArea rows={3} placeholder="根据用户输入判断是否需要进一步处理..." />
            </Form.Item>
          )
        }
      </Form.Item>
      <Divider orientation="left" plain>
        <span className="text-sm text-gray-500">分支配置</span>
      </Divider>
      <Form.List name="branches">
        {(fields, { add, remove }) => (
          <div className="space-y-3">
            {fields.map(({ key, name, ...restField }) => (
              <div key={key} className="flex gap-2 items-start p-3 bg-gray-50 rounded-lg">
                <div className="flex-1 space-y-2">
                  <Form.Item {...restField} name={[name, 'name']} rules={[{ required: true, message: '请输入分支名称' }]} className="mb-2">
                    <Input placeholder="分支名称" addonBefore={`#${name + 1}`} />
                  </Form.Item>
                  <Form.Item {...restField} name={[name, 'condition']} className="mb-0">
                    <Input placeholder="条件表达式 (可选)" />
                  </Form.Item>
                </div>
                {fields.length > 1 && (
                  <Button type="text" danger icon={<Minus className="w-4 h-4" />} onClick={() => remove(name)} className="mt-1" />
                )}
              </div>
            ))}
            <Button type="dashed" onClick={() => add({ name: `分支${fields.length + 1}`, condition: '' })} block icon={<Plus className="w-4 h-4" />}>
              添加分支
            </Button>
          </div>
        )}
      </Form.List>
    </>
  );

  const renderToolConfig = () => (
    <>
      <Form.Item label="工具名称" name="toolName" rules={[{ required: true, message: '请输入工具名称' }]}>
        <Input placeholder="例如: web_search, calculator" />
      </Form.Item>
      <Form.Item label="工具参数 (JSON)" name="toolParams">
        <Input.TextArea rows={4} placeholder='{"query": "{{input}}", "limit": 10}' className="font-mono text-sm" />
      </Form.Item>
    </>
  );

  const renderNodeConfig = () => {
    if (!node) return null;
    switch (node.data.nodeType) {
      case NodeType.LLM:
        return renderLLMConfig();
      case NodeType.HTTP:
        return renderHTTPConfig();
      case NodeType.CONDITION:
        return renderConditionConfig();
      case NodeType.TOOL:
        return renderToolConfig();
      default:
        return <div className="text-gray-500 text-sm py-4 text-center">此节点无需额外配置</div>;
    }
  };

  const renderAdvancedConfig = () => (
    <>
      <Form.Item label="超时时间" name="timeoutMs">
        <InputNumber min={1000} max={300000} step={1000} className="w-full" addonAfter="毫秒" />
      </Form.Item>
      <Form.Item label="需要人工审核" name="requiresHumanReview" valuePropName="checked">
        <Switch />
      </Form.Item>
      <Form.Item noStyle shouldUpdate={(prev, curr) => prev.requiresHumanReview !== curr.requiresHumanReview}>
        {({ getFieldValue }) =>
          getFieldValue('requiresHumanReview') && (
            <Form.Item label="审核说明" name="reviewDescription">
              <Input.TextArea rows={2} placeholder="描述需要审核的内容..." />
            </Form.Item>
          )
        }
      </Form.Item>
      <div className="grid grid-cols-2 gap-4">
        <Form.Item label="最大重试次数" name="maxRetries">
          <InputNumber min={0} max={5} className="w-full" />
        </Form.Item>
        <Form.Item label="重试延迟" name="retryDelayMs">
          <InputNumber min={100} max={60000} step={100} className="w-full" addonAfter="ms" />
        </Form.Item>
      </div>
    </>
  );

  const renderVariablesConfig = () => (
    <div className="space-y-4">
      <div>
        <div className="text-sm font-medium text-slate-200 mb-2">输入变量</div>
        <Form.Item name="inputs" noStyle>
          <Input.TextArea rows={4} placeholder='{"userInput": "{{start.output}}"}' className="font-mono text-sm" />
        </Form.Item>
      </div>
      <div>
        <div className="text-sm font-medium text-slate-200 mb-2">输出变量</div>
        <Form.Item name="outputs" noStyle>
          <Input.TextArea rows={4} placeholder='{"result": "output"}' className="font-mono text-sm" />
        </Form.Item>
      </div>
    </div>
  );

  const collapseItems = [
    { key: 'basic', label: <span className="flex items-center gap-2"><FileText className="w-4 h-4" />基本配置</span>, children: (
      <>
        <Form.Item label="节点名称" name="label" rules={[{ required: true, message: '请输入节点名称' }]}>
          <Input placeholder="输入节点名称" />
        </Form.Item>
        <Form.Item label="节点描述" name="description">
          <Input.TextArea rows={2} placeholder="描述此节点的功能..." />
        </Form.Item>
      </>
    ) },
    { key: 'config', label: <span className="flex items-center gap-2"><Settings className="w-4 h-4" />节点配置</span>, children: renderNodeConfig() },
    { key: 'advanced', label: <span className="flex items-center gap-2"><Zap className="w-4 h-4" />高级配置</span>, children: renderAdvancedConfig() },
    { key: 'variables', label: <span className="flex items-center gap-2"><Variable className="w-4 h-4" />输入/输出变量</span>, children: renderVariablesConfig() }
  ];

  if (!node || !typeConfig) {
    return (
      <aside className="workflow-props-panel w-[420px] border-l flex-shrink-0 flex items-center justify-center p-6">
        <div className="text-center text-slate-400">
          <Settings className="w-7 h-7 mx-auto mb-2" />
          <p className="text-sm">选中一个节点后在此编辑配置</p>
        </div>
      </aside>
    );
  }

  const Icon = typeConfig.icon;
  const globalVars = executionContext?.globalVariables || {};

  return (
    <aside className="workflow-props-panel w-[420px] border-l flex-shrink-0 flex flex-col">
      <div className={`flex items-center justify-between px-4 py-3 border-b ${typeConfig.bgColor} ${typeConfig.borderColor}`}>
        <div className="flex items-center gap-3">
          <div className={`p-2 rounded-lg bg-slate-950/60 ${typeConfig.color}`}>
            <Icon className="w-5 h-5" />
          </div>
          <div>
            <div className="font-semibold text-slate-100">{node.data.label}</div>
            <div className="text-xs text-slate-400">{typeConfig.label}</div>
          </div>
        </div>
        <button onClick={onClose} className="workflow-toolbar-btn p-2 rounded-lg transition-colors">
          <X className="w-5 h-5 text-slate-300" />
        </button>
      </div>

      <div className="px-4 pt-3">
        <div className="workflow-props-tabs">
          <button type="button" className={tab === 'config' ? 'active' : ''} onClick={() => setTab('config')}>
            <Settings className="w-4 h-4" />
            配置
          </button>
          <button type="button" className={tab === 'debug' ? 'active' : ''} onClick={() => setTab('debug')}>
            <Bug className="w-4 h-4" />
            调试
          </button>
          <button type="button" className={tab === 'variables' ? 'active' : ''} onClick={() => setTab('variables')}>
            <Database className="w-4 h-4" />
            变量
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        {tab === 'config' && (
          <Form form={form} layout="vertical" className="space-y-0 workflow-props-form">
            <Collapse
              activeKey={activeKeys}
              onChange={(keys) => setActiveKeys(keys as string[])}
              items={collapseItems}
              bordered={false}
              expandIconPosition="end"
              className="bg-transparent"
            />
          </Form>
        )}

        {tab === 'debug' && (
          <div className="space-y-3">
            <div className="rounded-lg border border-slate-700 bg-slate-900/70 p-3">
              <div className="text-sm text-slate-200 mb-2">节点调试</div>
              <div className="text-xs text-slate-400 mb-3">
                使用 DEBUG 模式触发执行，并在日志中观察此节点行为。
              </div>
              <Button
                type="primary"
                icon={<Play className="w-4 h-4" />}
                onClick={() => onStepRun?.(node.id)}
                disabled={isExecuting}
              >
                单步调试此节点
              </Button>
            </div>
            <div className="rounded-lg border border-slate-700 bg-slate-950/85 p-3">
              <div className="text-sm text-slate-200 mb-2">节点日志</div>
              <div className="max-h-64 overflow-y-auto font-mono text-xs space-y-1 text-slate-300">
                {nodeExecutionLogs.length === 0 && <div className="text-slate-500">暂无节点日志</div>}
                {nodeExecutionLogs.map((log, idx) => (
                  <div key={`${node.id}-${idx}`}>{log}</div>
                ))}
              </div>
            </div>
          </div>
        )}

        {tab === 'variables' && (
          <div className="space-y-3">
            <div className="rounded-lg border border-slate-700 bg-slate-950/85 p-3">
              <div className="text-sm text-slate-200 mb-2">节点输入模板</div>
              <pre className="text-xs text-slate-300 whitespace-pre-wrap break-all">{prettyJson(node.data.inputs)}</pre>
            </div>
            <div className="rounded-lg border border-slate-700 bg-slate-950/85 p-3">
              <div className="text-sm text-slate-200 mb-2">节点输出映射</div>
              <pre className="text-xs text-slate-300 whitespace-pre-wrap break-all">{prettyJson(node.data.outputs)}</pre>
            </div>
            <div className="rounded-lg border border-slate-700 bg-slate-950/85 p-3">
              <div className="text-sm text-slate-200 mb-2">执行全局变量快照</div>
              <pre className="text-xs text-slate-300 whitespace-pre-wrap break-all">{prettyJson(globalVars)}</pre>
            </div>
          </div>
        )}
      </div>

      {tab === 'config' && (
        <div className="border-t border-slate-700 bg-slate-900/80 px-4 py-3">
          <div className="flex items-center justify-between">
            <Space>
              <Popconfirm
                title="确定要删除这个节点吗?"
                description="删除后无法恢复"
                onConfirm={() => {
                  onDelete(node.id);
                  onClose();
                }}
                okText="删除"
                cancelText="取消"
                okButtonProps={{ danger: true }}
              >
                <Button danger icon={<Trash2 className="w-4 h-4" />}>删除</Button>
              </Popconfirm>
              {onDuplicate && <Button icon={<Copy className="w-4 h-4" />} onClick={() => onDuplicate(node.id)}>复制</Button>}
            </Space>
            <Button type="primary" icon={<Save className="w-4 h-4" />} onClick={handleSave}>保存</Button>
          </div>
        </div>
      )}
    </aside>
  );
}
