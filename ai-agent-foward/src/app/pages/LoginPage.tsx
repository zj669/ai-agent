import { useMemo, useState, useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Form, Input, Button, Checkbox, Typography, message } from 'antd'
import { LockOutlined, MailOutlined, RobotOutlined } from '@ant-design/icons'
import { isAuthenticated, saveAccessToken } from '../auth'
import { login } from '../../shared/api/adapters/authAdapter'
import WorkflowAnimation from '../components/WorkflowAnimation'

const { Title, Text } = Typography

interface LoginFormValues {
  email: string
  password: string
  rememberMe: boolean
}

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const rememberedEmail = useMemo(() => localStorage.getItem('rememberedEmail') ?? '', [])
  const [loading, setLoading] = useState(false)
  const [form] = Form.useForm<LoginFormValues>()

  useEffect(() => {
    if (isAuthenticated()) {
      const searchParams = new URLSearchParams(location.search)
      const redirect = searchParams.get('redirect')
      const target = redirect && redirect.startsWith('/') ? redirect : '/dashboard'
      navigate(target, { replace: true })
    }
  }, [navigate, location.search])

  const handleFinish = async (values: LoginFormValues) => {
    setLoading(true)
    try {
      const session = await login({ email: values.email, password: values.password })
      saveAccessToken(session.token, values.rememberMe)

      if (values.rememberMe) {
        localStorage.setItem('rememberedEmail', values.email)
      } else {
        localStorage.removeItem('rememberedEmail')
      }

      const searchParams = new URLSearchParams(location.search)
      const redirect = searchParams.get('redirect')
      const target = redirect && redirect.startsWith('/') ? redirect : '/dashboard'
      navigate(target, { replace: true })
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '登录失败，请重试'
      message.error(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex' }}>
      {/* 左侧动画 */}
      <div style={{ flex: 1, minWidth: 0, display: 'flex' }}>
        <WorkflowAnimation />
      </div>

      {/* 右侧表单 */}
      <div style={{
        width: 560, flexShrink: 0,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        background: '#fff', padding: '40px 60px',
      }}>
        <div style={{ width: '100%', maxWidth: 380 }}>
          {/* Logo */}
          <div style={{ textAlign: 'center', marginBottom: 32 }}>
            <div style={{
              width: 56, height: 56, borderRadius: 14,
              background: '#2970FF',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              margin: '0 auto 16px',
            }}>
              <RobotOutlined style={{ fontSize: 28, color: '#fff' }} />
            </div>
            <Title level={3} style={{ marginBottom: 4, marginTop: 0, color: '#101828' }}>AI Agent</Title>
            <Text style={{ color: '#667085' }}>智能工作流编排平台</Text>
          </div>

          {/* Form */}
          <Form
            form={form}
            onFinish={handleFinish}
            initialValues={{ email: rememberedEmail, password: '', rememberMe: !!rememberedEmail }}
            size="large"
            layout="vertical"
            requiredMark={false}
          >
            <Form.Item
              name="email"
              rules={[
                { required: true, message: '请输入邮箱' },
                { type: 'email', message: '请输入有效的邮箱地址' },
              ]}
            >
              <Input prefix={<MailOutlined />} placeholder="请输入邮箱" />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" />
            </Form.Item>

            <Form.Item style={{ marginBottom: 24 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Form.Item name="rememberMe" valuePropName="checked" noStyle>
                  <Checkbox>记住我</Checkbox>
                </Form.Item>
                <Link to="/forgot-password" style={{ fontSize: 14 }}>忘记密码?</Link>
              </div>
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" block size="large" loading={loading}
                style={{ background: '#2970FF', borderColor: '#2970FF' }}>
                登 录
              </Button>
            </Form.Item>
          </Form>

          <div style={{ textAlign: 'center' }}>
            <Text style={{ color: '#667085' }}>还没有账号？</Text>
            <Link to="/register">立即注册</Link>
          </div>
        </div>
      </div>
    </div>
  )
}
