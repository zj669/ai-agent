import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Form, Input, Button, Typography, message, Steps } from 'antd'
import { LockOutlined, MailOutlined, RobotOutlined, UserOutlined, SafetyOutlined } from '@ant-design/icons'
import { isAuthenticated, saveAccessToken } from '../auth'
import { sendEmailCode, register } from '../../shared/api/adapters/authAdapter'
import WorkflowAnimation from '../components/WorkflowAnimation'

const { Title, Text } = Typography

export default function RegisterPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState(0)
  const [email, setEmail] = useState('')
  const [sendingCode, setSendingCode] = useState(false)
  const [countdown, setCountdown] = useState(0)
  const [loading, setLoading] = useState(false)
  const countdownTimerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const [form] = Form.useForm()

  // 已登录用户直接跳转
  useEffect(() => {
    if (isAuthenticated()) {
      navigate('/dashboard', { replace: true })
    }
  }, [navigate])

  useEffect(() => {
    return () => {
      if (countdownTimerRef.current) {
        clearInterval(countdownTimerRef.current)
      }
    }
  }, [])

  const startCountdown = () => {
    if (countdownTimerRef.current) {
      clearInterval(countdownTimerRef.current)
    }
    setCountdown(60)
    countdownTimerRef.current = setInterval(() => {
      setCountdown(prev => {
        if (prev <= 1) {
          if (countdownTimerRef.current) {
            clearInterval(countdownTimerRef.current)
            countdownTimerRef.current = null
          }
          return 0
        }
        return prev - 1
      })
    }, 1000)
  }

  const handleSendCode = async () => {
    const emailValue = form.getFieldValue('email')
    if (!emailValue) {
      message.warning('请输入邮箱')
      return
    }

    // 方案A：点击后立即进入第二步并启动倒计时，避免卡在发送阶段
    setEmail(emailValue)
    setStep(1)
    startCountdown()

    setSendingCode(true)
    try {
      await sendEmailCode(emailValue)
      message.success('验证码已发送')
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '发送失败，请重试'
      message.error(msg)
    } finally {
      setSendingCode(false)
    }
  }

  const handleRegister = async () => {
    const values = await form.validateFields()
    if (values.password !== values.confirmPassword) {
      message.error('两次密码不一致')
      return
    }
    setLoading(true)
    try {
      const session = await register({
        email,
        code: values.code,
        password: values.password,
        username: values.username || undefined,
      })
      saveAccessToken(session.token, true)
      message.success('注册成功')
      navigate('/dashboard', { replace: true })
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '注册失败，请重试'
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
        overflowY: 'auto',
      }}>
        <div style={{ width: '100%', maxWidth: 380 }}>
          <div style={{ textAlign: 'center', marginBottom: 24 }}>
            <div style={{
              width: 56, height: 56, borderRadius: 14,
              background: '#2970FF',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              margin: '0 auto 16px',
            }}>
              <RobotOutlined style={{ fontSize: 28, color: '#fff' }} />
            </div>
            <Title level={3} style={{ marginBottom: 4, marginTop: 0, color: '#101828' }}>创建账号</Title>
            <Text style={{ color: '#667085' }}>AI Agent 智能工作流编排平台</Text>
          </div>

        <Steps
          current={step}
          size="small"
          style={{ marginBottom: 24 }}
          items={[
            { title: '输入邮箱' },
            { title: '验证注册' },
          ]}
        />

        <Form form={form} layout="vertical" requiredMark={false} size="large">
          {step === 0 && (
            <>
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
                <Button type="primary" block loading={sendingCode} onClick={handleSendCode}>
                  发送验证码
                </Button>
              </Form.Item>
            </>
          )}

          {step === 1 && (
            <>
              <div style={{ marginBottom: 16, padding: '8px 12px', background: '#f6f8fa', borderRadius: 8 }}>
                <Text type="secondary">验证码已发送至 </Text>
                <Text strong>{email}</Text>
              </div>

              <Form.Item
                name="code"
                rules={[{ required: true, message: '请输入验证码' }]}
              >
                <Input
                  prefix={<SafetyOutlined />}
                  placeholder="请输入6位验证码"
                  maxLength={6}
                  suffix={
                    <Button
                      type="link"
                      size="small"
                      disabled={countdown > 0}
                      loading={sendingCode}
                      onClick={handleSendCode}
                      style={{ padding: 0 }}
                    >
                      {countdown > 0 ? `${countdown}s` : '重新发送'}
                    </Button>
                  }
                />
              </Form.Item>

              <Form.Item name="username">
                <Input prefix={<UserOutlined />} placeholder="用户名（可选）" />
              </Form.Item>

              <Form.Item
                name="password"
                rules={[
                  { required: true, message: '请输入密码' },
                  { min: 8, message: '密码至少8位' },
                ]}
              >
                <Input.Password prefix={<LockOutlined />} placeholder="请输入密码（至少8位）" />
              </Form.Item>

              <Form.Item
                name="confirmPassword"
                rules={[{ required: true, message: '请确认密码' }]}
              >
                <Input.Password prefix={<LockOutlined />} placeholder="确认密码" />
              </Form.Item>

              <Form.Item>
                <Button type="primary" block loading={loading} onClick={handleRegister}>
                  注 册
                </Button>
              </Form.Item>
            </>
          )}
        </Form>

        <div style={{ textAlign: 'center' }}>
          <Text style={{ color: '#667085' }}>已有账号？</Text>
          <Link to="/login">立即登录</Link>
        </div>
        </div>
      </div>
    </div>
  )
}
