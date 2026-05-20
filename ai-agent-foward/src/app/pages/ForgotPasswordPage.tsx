import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Form, Input, Button, Typography, message } from 'antd'
import { LockOutlined, MailOutlined, RobotOutlined, SafetyOutlined } from '@ant-design/icons'
import { resetPassword, sendEmailCode } from '../../shared/api/adapters/authAdapter'
import { getApiErrorMessage } from '../../shared/api/errorMapper'

const { Title, Text } = Typography

interface ForgotPasswordFormValues {
  email: string
  code: string
  newPassword: string
  confirmPassword: string
}

export default function ForgotPasswordPage() {
  const [sendingCode, setSendingCode] = useState(false)
  const [resetting, setResetting] = useState(false)
  const [sent, setSent] = useState(false)
  const [done, setDone] = useState(false)
  const [form] = Form.useForm<ForgotPasswordFormValues>()

  const handleSend = async () => {
    let email: string
    try {
      const values = await form.validateFields(['email'])
      email = values.email
    } catch {
      return
    }

    setSendingCode(true)
    try {
      await sendEmailCode(email)
      setSent(true)
      message.success('验证码已发送')
    } catch (err: unknown) {
      message.error(getApiErrorMessage(err, '发送失败，请重试'))
    } finally {
      setSendingCode(false)
    }
  }

  const handleReset = async () => {
    let values: ForgotPasswordFormValues
    try {
      values = await form.validateFields()
    } catch {
      return
    }

    setResetting(true)
    try {
      await resetPassword({
        email: values.email,
        code: values.code,
        newPassword: values.newPassword,
        confirmPassword: values.confirmPassword,
      })
      setDone(true)
      message.success('密码已重置，请重新登录')
    } catch (err: unknown) {
      message.error(getApiErrorMessage(err, '重置密码失败'))
    } finally {
      setResetting(false)
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
          <Text style={{ color: '#667085' }}>输入邮箱，我们将发送验证码</Text>
        </div>

        {done ? (
          <div style={{ textAlign: 'center' }}>
            <Text style={{ color: '#344054' }}>密码已重置，请使用新密码登录</Text>
            <div style={{ marginTop: 24 }}>
              <Link to="/login">返回登录</Link>
            </div>
          </div>
        ) : (
          <Form form={form} layout="vertical" requiredMark={false} size="large">
            <Form.Item
              name="email"
              rules={[
                { required: true, message: '请输入邮箱' },
                { type: 'email', message: '请输入有效的邮箱地址' },
              ]}
            >
              <Input prefix={<MailOutlined />} placeholder="请输入邮箱" disabled={sent} />
            </Form.Item>

            {sent && (
              <>
                <Form.Item
                  name="code"
                  rules={[{ required: true, message: '请输入验证码' }]}
                >
                  <Input prefix={<SafetyOutlined />} placeholder="请输入6位验证码" maxLength={6} />
                </Form.Item>

                <Form.Item
                  name="newPassword"
                  rules={[
                    { required: true, message: '请输入新密码' },
                    { min: 8, message: '密码至少8位' },
                  ]}
                >
                  <Input.Password prefix={<LockOutlined />} placeholder="请输入新密码（至少8位）" />
                </Form.Item>

                <Form.Item
                  name="confirmPassword"
                  dependencies={['newPassword']}
                  rules={[
                    { required: true, message: '请确认密码' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        if (!value || getFieldValue('newPassword') === value) return Promise.resolve()
                        return Promise.reject(new Error('两次输入的密码不一致'))
                      },
                    }),
                  ]}
                >
                  <Input.Password prefix={<LockOutlined />} placeholder="请再次输入新密码" />
                </Form.Item>
              </>
            )}

            <Form.Item>
              <Button type="primary" block loading={sent ? resetting : sendingCode} onClick={sent ? handleReset : handleSend}
                style={{ background: '#2970FF', borderColor: '#2970FF' }}>
                {sent ? '重置密码' : '发送验证码'}
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
