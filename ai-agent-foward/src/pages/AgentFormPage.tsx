import { useEffect } from 'react';
import { Form, Input, Button, Space, Spin } from 'antd';
import { SaveOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { useAgentForm } from '../hooks/useAgentForm';
import { CreateAgentRequest, UpdateAgentRequest } from '../types/agent';

const { TextArea } = Input;

export const AgentFormPage: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const agentId = id ? parseInt(id, 10) : undefined;
  const isEditMode = !!agentId;

  const [form] = Form.useForm();
  const { agent, loading, submitting, createAgent, updateAgent } = useAgentForm(agentId);

  useEffect(() => {
    if (agent) {
      form.setFieldsValue({
        name: agent.name,
        description: agent.description,
        icon: agent.icon,
        graphJson: agent.graphJson
      });
    }
  }, [agent, form]);

  const handleSubmit = async (values: any) => {
    try {
      if (isEditMode && agent) {
        const data: UpdateAgentRequest = {
          id: agent.id,
          name: values.name,
          description: values.description,
          icon: values.icon,
          graphJson: values.graphJson,
          version: agent.version
        };
        await updateAgent(data);
      } else {
        const data: CreateAgentRequest = {
          name: values.name,
          description: values.description,
          icon: values.icon
        };
        await createAgent(data);
      }
    } catch (error) {
      // Error handled in hook
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '100px 0' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Space>
            <Button
              type="text"
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate('/agents')}
            />
            <h2 style={{ margin: 0 }}>{isEditMode ? '编辑 Agent' : '创建 Agent'}</h2>
          </Space>
          <Button
            type="primary"
            icon={<SaveOutlined />}
            loading={submitting}
            onClick={() => form.submit()}
          >
            保存
          </Button>
        </div>

        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          style={{ maxWidth: 800 }}
        >
          <Form.Item
            label="名称"
            name="name"
            rules={[{ required: true, message: '请输入 Agent 名称' }]}
          >
            <Input placeholder="请输入 Agent 名称" />
          </Form.Item>

          <Form.Item
            label="描述"
            name="description"
          >
            <TextArea
              placeholder="请输入 Agent 描述"
              rows={3}
            />
          </Form.Item>

          <Form.Item
            label="图标"
            name="icon"
            tooltip="可以使用 Emoji 或图标 URL"
          >
            <Input placeholder="例如：🤖 或图标 URL" />
          </Form.Item>

          {isEditMode && (
            <Form.Item
              label="工作流配置"
              name="graphJson"
              tooltip="工作流的 JSON 配置（通常通过可视化编辑器生成）"
            >
              <TextArea
                placeholder="工作流 JSON 配置"
                rows={10}
                style={{ fontFamily: 'monospace' }}
              />
            </Form.Item>
          )}
        </Form>
      </div>

      {isEditMode && agent && (
        <div style={{ marginTop: 16, padding: 16, backgroundColor: '#fafafa', borderRadius: 8 }}>
          <h3 style={{ marginTop: 0 }}>版本信息</h3>
          <Space direction="vertical">
            <div>
              <strong>当前版本：</strong> v{agent.version}
            </div>
            {agent.publishedVersionId && (
              <div>
                <strong>已发布版本 ID：</strong> {agent.publishedVersionId}
              </div>
            )}
            <div>
              <strong>状态：</strong> {agent.status === 0 ? '草稿' : agent.status === 1 ? '已发布' : '已归档'}
            </div>
          </Space>
        </div>
      )}
    </div>
  );
};
