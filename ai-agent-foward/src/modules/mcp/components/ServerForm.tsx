import { Form, Input, Select, Button, Space } from 'antd'
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons'
import type { McpServerConfig, ServerType } from '../types/mcp'

const { TextArea } = Input

interface Props {
  initialName?: string
  initialValues?: Partial<McpServerConfig>
  onSubmit: (values: { name: string; formData: McpServerConfig; description: string }) => void
  onCancel: () => void
  loading?: boolean
}

export default function ServerForm({ initialName, initialValues, onSubmit, onCancel, loading }: Props) {
  const [form] = Form.useForm()

  const handleFinish = (values: {
    name: string
    serverType: ServerType
    description?: string
    command?: string
    args?: string[]
    env?: { key: string; value: string }[]
    url?: string
    headers?: { key: string; value: string }[]
    endpoint?: string
  }) => {
    const config: McpServerConfig = {
      type: values.serverType,
    }

    if (values.serverType === 'stdio') {
      config.command = values.command
      config.args = values.args?.filter(Boolean)
      config.env = values.env?.reduce<Record<string, string>>((acc, { key, value }) => {
        if (key && value) acc[key] = value
        return acc
      }, {})
    } else {
      config.url = values.url
      config.headers = values.headers?.reduce<Record<string, string>>((acc, { key, value }) => {
        if (key && value) acc[key] = value
        return acc
      }, {})
      if (values.serverType === 'http') {
        config.endpoint = values.endpoint
      }
    }

    onSubmit({ name: values.name, formData: config, description: values.description || '' })
  }

  return (
    <Form
      form={form}
      layout="vertical"
      onFinish={handleFinish}
      initialValues={{
        name: initialName || '',
        serverType: initialValues?.type as ServerType || 'stdio',
        command: initialValues?.command,
        args: initialValues?.args?.join(' '),
        description: '',
      }}
    >
      <Form.Item
        name="name"
        label="服务器名称"
        rules={[{ required: true, message: '请输入服务器名称' }]}
      >
        <Input placeholder="例如: Filesystem Server" />
      </Form.Item>

      <Form.Item
        name="serverType"
        label="传输协议"
        rules={[{ required: true, message: '请选择传输协议' }]}
      >
        <Select>
          <Select.Option value="stdio">stdio（标准输入输出）</Select.Option>
          <Select.Option value="sse">SSE（Server-Sent Events）</Select.Option>
          <Select.Option value="http">HTTP / Streamable-HTTP</Select.Option>
        </Select>
      </Form.Item>

      <Form.Item noStyle shouldUpdate={(prev, curr) => prev.serverType !== curr.serverType}>
        {({ getFieldValue }) => {
          const type = getFieldValue('serverType')
          return (
            <>
              {type === 'stdio' && (
                <>
                  <Form.Item
                    name="command"
                    label="命令"
                    rules={[{ required: true, message: '请输入命令' }]}
                  >
                    <Input placeholder="例如: npx" />
                  </Form.Item>
                  <Form.Item
                    name="args"
                    label="参数（空格分隔）"
                  >
                    <Input placeholder="例如: -y @anthropic/mcp-server" />
                  </Form.Item>
                  <Form.Item label="环境变量">
                    <Form.List name="env">
                      {(fields, { add, remove }) => (
                        <>
                          {fields.map(({ key, name }) => (
                            <Space key={key} align="baseline">
                              <Form.Item name={[name, 'key']} noStyle>
                                <Input placeholder="Key" style={{ width: 160 }} />
                              </Form.Item>
                              <Form.Item name={[name, 'value']} noStyle>
                                <Input placeholder="Value" style={{ width: 200 }} />
                              </Form.Item>
                              <Button
                                type="text"
                                danger
                                icon={<MinusCircleOutlined />}
                                onClick={() => remove(name)}
                              />
                            </Space>
                          ))}
                          <Button
                            type="link"
                            icon={<PlusOutlined />}
                            onClick={() => add({ key: '', value: '' })}
                          >
                            添加环境变量
                          </Button>
                        </>
                      )}
                    </Form.List>
                  </Form.Item>
                </>
              )}

              {(type === 'sse' || type === 'http') && (
                <>
                  <Form.Item
                    name="url"
                    label="服务器 URL"
                    rules={[{ required: true, message: '请输入服务器 URL' }]}
                  >
                    <Input placeholder="https://mcp.example.com/sse 或 https://mcp.example.com/mcp" />
                  </Form.Item>
                  <Form.Item label="请求头">
                    <Form.List name="headers">
                      {(fields, { add, remove }) => (
                        <>
                          {fields.map(({ key, name }) => (
                            <Space key={key} align="baseline">
                              <Form.Item name={[name, 'key']} noStyle>
                                <Input placeholder="Header Name" style={{ width: 160 }} />
                              </Form.Item>
                              <Form.Item name={[name, 'value']} noStyle>
                                <Input placeholder="Header Value" style={{ width: 200 }} />
                              </Form.Item>
                              <Button
                                type="text"
                                danger
                                icon={<MinusCircleOutlined />}
                                onClick={() => remove(name)}
                              />
                            </Space>
                          ))}
                          <Button
                            type="link"
                            icon={<PlusOutlined />}
                            onClick={() => add({ key: '', value: '' })}
                          >
                            添加请求头
                          </Button>
                        </>
                      )}
                    </Form.List>
                  </Form.Item>
                  {type === 'http' && (
                    <Form.Item
                      name="endpoint"
                      label="MCP Endpoint 路径"
                      extra="MCP 服务的 HTTP endpoint，默认为 /mcp"
                    >
                      <Input placeholder="/mcp" />
                    </Form.Item>
                  )}
                </>
              )}
            </>
          )
        }}
      </Form.Item>

      <Form.Item name="description" label="描述">
        <TextArea rows={3} placeholder="可选，描述该服务器的用途..." />
      </Form.Item>

      <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
        <Space>
          <Button onClick={onCancel}>取消</Button>
          <Button type="primary" htmlType="submit" loading={loading}>
            确定
          </Button>
        </Space>
      </Form.Item>
    </Form>
  )
}
