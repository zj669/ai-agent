import { useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Form, Input, Button, Checkbox, Typography, message } from 'antd'
import { LockOutlined, MailOutlined, RobotOutlined } from '@ant-design/icons'
import { saveAccessToken } from '../auth'
import { login } from '../../shared/api/adapters/authAdapter'

const { Title, Text } = Typography

interface LoginFormValues {
  email: string
  password: string
  rememberMe: boolean
}

const pageStyle: React.CSSProperties = {
  minHeight: '100vh',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: 'linear-gradient(135deg, #1677ff 0%, #722ed1 100%)',
  position: 'relative',
  overflow: 'hidden',
}

const floatingBaseStyle: React.CSSProperties = {
  position: 'absolute',
  borderRadius: '50%',
  opacity: 0.08,
  background: '#fff',
}

const cardStyle: React.CSSProperties = {
  width: '100%',
  maxWidth: 420,
  background: '#fff',
  borderRadius: 16,
  boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
  padding: 40,
  position: 'relative',
  zIndex: 1,
}

const logoCircleStyle: React.CSSProperties = {
  width: 72,
  height: 72,
  borderRadius: '50%',
  background: 'linear-gradient(135deg, #1677ff 0%, #722ed1 100%)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  margin: '0 auto 16px',
  boxShadow: '0 4px 12px rgba(22,119,255,0.3)',
}

const floatingElements: React.CSSProperties[] = [
  { ...floatingBaseStyle, width: 300, height: 300, top: '-5%', left: '-8%', animation: 'floatA 8s ease-in-out infinite' },
  { ...floatingBaseStyle, width: 200, height: 200, bottom: '-4%', right: '-6%', animation: 'floatB 10s ease-in-out infinite' },
  { ...floatingBaseStyle, width: 120, height: 120, top: '40%', right: '10%', opacity: 0.05, animation: 'floatA 12s ease-in-out infinite reverse' },
  { ...floatingBaseStyle, width: 80, height: 80, bottom: '20%', left: '12%', opacity: 0.06, animation: 'floatB 9s ease-in-out infinite reverse' },
]

const keyframesStyle = `
@keyframes floatA {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(5deg); }
}
@keyframes floatB {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(15px) rotate(-5deg); }
}
`

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const rememberedEmail = useMemo(() => localStorage.getItem('rememberedEmail') ?? '', [])
  const [loading, setLoading] = useState(false)
  const [form] = Form.useForm<LoginFormValues>()

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
    <div style={pageStyle}>
      <style>{keyframesStyle}</style>
      {floatingElements.map((style, i) => (
        <div key={i} style={style} />
      ))}

      <div style={cardStyle}>
        {/* Logo */}
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div style={logoCircleStyle}>
            <RobotOutlined style={{ fontSize: 36, color: '#fff' }} />
          </div>
          <Title level={3} style={{ marginBottom: 4, marginTop: 0 }}>AI Agent 平台</Title>
          <Text type="secondary">智能工作流编排平台</Text>
        </div>

        {/* Form */}
        <Form
          form={form}
          onFinish={handleFinish}
          initialValues={{ email: rememberedEmail, password: '', rememberMe: false }}
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
            <Button type="primary" htmlType="submit" block size="large" loading={loading}>
              登 录
            </Button>
          </Form.Item>
        </Form>

        <div style={{ textAlign: 'center' }}>
          <Text type="secondary">还没有账号？</Text>
          <Link to="/register">立即注册</Link>
        </div>
      </div>
    </div>
  )
}