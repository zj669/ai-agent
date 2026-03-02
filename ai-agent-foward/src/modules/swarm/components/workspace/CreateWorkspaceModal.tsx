import { useState, useEffect } from 'react'
import { Modal, Form, Input, Select, Spin } from 'antd'
import { getLlmConfigs, type LlmConfig } from '../../../llm-config/api/llmConfigService'

interface Props {
  open: boolean
  onCancel: () => void
  onOk: (name: string, llmConfigId: number) => Promise<void>
}

export default function CreateWorkspaceModal({ open, onCancel, onOk }: Props) {
  const [form] = Form.useForm()
  const [configs, setConfigs] = useState<LlmConfig[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!open) return
    setLoading(true)
    getLlmConfigs()
      .then(setConfigs)
      .finally(() => setLoading(false))
  }, [open])

  const handleOk = async () => {
    const values = await form.validateFields()
    await onOk(values.name, values.llmConfigId)
    form.resetFields()
  }

  return (
    <Modal title="新建 Workspace" open={open} onOk={handleOk} onCancel={onCancel} destroyOnClose>
      <Spin spinning={loading}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="如：项目A协作" autoFocus />
          </Form.Item>
          <Form.Item name="llmConfigId" label="模型配置" rules={[{ required: true, message: '请选择模型配置' }]}>
            <Select placeholder="选择 API 模型">
              {configs.map(c => (
                <Select.Option key={c.id} value={c.id}>
                  {c.name} ({c.provider} / {c.model})
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Spin>
    </Modal>
  )
}
