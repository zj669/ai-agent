import { Button, Form, Input, InputNumber, Select, Space, Switch, Typography, Alert } from 'antd';
import type { NodeTemplate } from '../../types/execution';
import type { WorkflowCanvasNode } from '../../types/workflow';

interface NodePropertiesPanelProps {
  node: WorkflowCanvasNode | null;
  template?: NodeTemplate;
  hasUnknownType?: boolean;
  onChange: (nodeId: string, patch: Partial<WorkflowCanvasNode['data']>) => void;
  onDelete: () => void;
}

const inferFieldType = (field: any): 'string' | 'number' | 'boolean' | 'enum' | 'json' => {
  const type = String(field?.type || '').toLowerCase();
  if (type.includes('bool') || type === 'switch') return 'boolean';
  if (type.includes('number') || type.includes('int') || type.includes('float') || type.includes('double')) {
    return 'number';
  }
  if (type === 'select' || type === 'enum' || (field?.options && Array.isArray(field.options) && field.options.length > 0)) {
    return 'enum';
  }
  if (!type || type === 'string' || type === 'text' || type === 'textarea') return 'string';
  return 'json';
};

const safeParseJson = (text: string) => {
  try {
    return { ok: true, value: JSON.parse(text) };
  } catch {
    return { ok: false };
  }
};

export const NodePropertiesPanel: React.FC<NodePropertiesPanelProps> = ({
  node,
  template,
  hasUnknownType,
  onChange,
  onDelete
}) => {
  if (!node) {
    return (
      <div style={{ width: 340, borderLeft: '1px solid #f0f0f0', padding: 16 }}>
        <Typography.Text type="secondary">请选择节点以编辑配置</Typography.Text>
      </div>
    );
  }

  const fieldGroups = template?.configFieldGroups || [];

  const renderField = (field: any) => {
    const fieldType = inferFieldType(field);
    const value = node.data.userConfig?.[field.key];

    if (fieldType === 'number') {
      return (
        <InputNumber
          style={{ width: '100%' }}
          value={typeof value === 'number' ? value : value !== undefined ? Number(value) : undefined}
          onChange={(next) =>
            onChange(node.id, {
              userConfig: {
                [field.key]: next
              }
            })
          }
          placeholder={field.placeholder}
        />
      );
    }

    if (fieldType === 'boolean') {
      return (
        <Switch
          checked={Boolean(value)}
          onChange={(checked) =>
            onChange(node.id, {
              userConfig: {
                [field.key]: checked
              }
            })
          }
        />
      );
    }

    if (fieldType === 'enum') {
      const options = Array.isArray(field.options)
        ? field.options.map((option: any) => {
            if (option && typeof option === 'object') {
              return {
                label: String(option.label ?? option.name ?? option.value),
                value: option.value ?? option.key ?? option.label
              };
            }
            return { label: String(option), value: option };
          })
        : [];

      return (
        <Select
          allowClear
          options={options}
          value={value}
          onChange={(next) =>
            onChange(node.id, {
              userConfig: {
                [field.key]: next
              }
            })
          }
          placeholder={field.placeholder || '请选择'}
        />
      );
    }

    if (fieldType === 'json') {
      return (
        <Input.TextArea
          value={typeof value === 'string' ? value : JSON.stringify(value ?? {}, null, 2)}
          rows={4}
          onChange={(e) => {
            const text = e.target.value;
            const parsed = safeParseJson(text);
            onChange(node.id, {
              userConfig: {
                [field.key]: parsed.ok ? parsed.value : text
              }
            });
          }}
          placeholder={field.placeholder || '请输入 JSON'}
        />
      );
    }

    return (
      <Input
        value={value ?? ''}
        onChange={(e) =>
          onChange(node.id, {
            userConfig: {
              [field.key]: e.target.value
            }
          })
        }
        placeholder={field.placeholder}
      />
    );
  };

  return (
    <div style={{ width: 340, borderLeft: '1px solid #f0f0f0', padding: 16, overflowY: 'auto' }}>
      <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>节点配置</div>

      {hasUnknownType ? (
        <Alert
          style={{ marginBottom: 12 }}
          type="warning"
          showIcon
          message="该节点类型未在元数据中注册"
          description="可查看和修改节点名称，但保存会被阻断，请联系后端补齐 metadata。"
        />
      ) : null}

      {!template && !hasUnknownType ? (
        <Alert
          style={{ marginBottom: 12 }}
          type="warning"
          showIcon
          message="未找到节点模板"
          description="仅允许编辑节点名称，配置项暂不可编辑。"
        />
      ) : null}

      <Form layout="vertical">
        <Form.Item label="节点名称" required>
          <Input
            value={node.data.nodeName}
            onChange={(e) => onChange(node.id, { nodeName: e.target.value })}
          />
        </Form.Item>

        <Form.Item label="节点类型">
          <Input value={node.data.rawNodeType || node.data.nodeType} disabled />
        </Form.Item>

        {(fieldGroups || []).map((group) => (
          <div key={group.groupKey || group.groupName || 'default'} style={{ marginBottom: 4 }}>
            {group.groupName ? (
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {group.groupName}
              </Typography.Text>
            ) : null}
            {(group.fields || []).map((field) => (
              <Form.Item
                key={field.key}
                label={field.label || field.key}
                required={Boolean(field.required)}
                tooltip={field.description}
              >
                {renderField(field)}
              </Form.Item>
            ))}
          </div>
        ))}
      </Form>

      <Space>
        <Button danger onClick={onDelete}>
          删除节点
        </Button>
      </Space>
    </div>
  );
};
