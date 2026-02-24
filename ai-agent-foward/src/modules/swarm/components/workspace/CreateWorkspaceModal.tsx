import { Modal, Form, Input } from 'antd'

interface Props {
  open: boolean
  onCancel: () => void
  onOk: (name: string) => Promise<void>
}

export default function CreateWorkspaceModal({ open, onCancel, onOk }: Props) {
  const [form] = Form.useForm()

  const handleOk = async () => {
    const values = await form.validateFields()
    await onOk(values.name)
    form.resetFields()
  }

  return (
    <Modal title="新建 Workspace" open={open} onOk={handleOk} onCancel={onCancel} destroyOnClose>
      <Form form={form} layout="vertical">
        <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
          <Input placeholder="如：项目A协作" autoFocus />
        </Form.Item>
      </Form>
    </Modal>
  )
}
