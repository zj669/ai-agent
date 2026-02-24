import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Form, Input, Button, Typography, message } from 'antd'
import { MailOutlined, RobotOutlined } from '@ant-design/icons'
import { sendEmailCode } from '../../shared/api/adapters/authAdapter'

const { Title, Text } = Typography

export default function ForgotPasswordPage() {
  const [loading, setLoading] = useState(false)
  const [sent, setSent] = useState(false)

  const handleSend = async (values: { email: string }) => {
    setLoading(true)
    try {
      await sendEmailCode(values.email)
      setSent(true)
      message.success('重置邮件已发送')
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '发送失败，请重试'
      message.error(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: '#EFF4FF',
    }}>
      <div style={{
        width: '100%',
        maxWidth: 400,
        background: '#fff',
        borderRadius: 16,
        boxShadow: '0 1px 3px rgba(16,24,40,0.1), 0 1px 2px rgba(16,24,40,0.06)',
        padding: 40,
      }}>
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div style={{
            width: 56, height: 56, borderRadius: 14,
            background: '#2970FF',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            margin: '0 auto 16px',
          }}>
            <RobotOutlined style={{ fontSize: 28, color: '#fff' }} />
          </div>
          <Title level={3} style={{ marginBottom: 4, marginTop: 0, color: '#101828' }}>忘记密码</Title>
          <Text style={{ color: '#667085' }}>输入邮箱，我们将发送重置链接</Text>
        </div>

        {sent ? (
          <div style={{ textAlign: 'center' }}>
            <Text style={{ color: '#344054' }}>重置邮件已发送，请查收邮箱</Text>
            <div style={{ marginTop: 24 }}>
              <Link to="/login">返回登录</Link>
            </div>
          </div>
        ) : (
          <Form layout="vertical" requiredMark={false} size="large" onFinish={handleSend}>
            <Form.Item
              name="email"
              rules={[
                { required: true, message: '请输入邮箱' },
                { type: 'email', message: '请输入有效的邮箱地址' },
              ]}
            >
              <Input prefix={<MailOutlined />} placeholder="请输入邮箱" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" block loading={loading}
                style={{ background: '#2970FF', borderColor: '#2970FF' }}>
                发送重置邮件
              </Button>
            </Form.Item>
            <div style={{ textAlign: 'center' }}>
              <Link to="/login">返回登录</Link>
            </div>
          </Form>
        )}
      </div>
    </div>
  )
}
