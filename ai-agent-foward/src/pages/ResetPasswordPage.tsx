import { useState } from 'react';
import { Form, Input, Button, message } from 'antd';
import { LockOutlined, MailOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { authService } from '../services/authService';
import type { ResetPasswordRequest } from '../types/auth';

export const ResetPasswordPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [countdown, setCountdown] = useState(0);

  const handleResetPassword = async (values: ResetPasswordRequest) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error('两次输入的密码不一致');
      return;
    }

    setLoading(true);
    try {
      await authService.resetPassword(values);
      message.success('密码重置成功，请使用新密码登录');
      navigate('/login');
    } catch (error: any) {
      message.error(error.response?.data?.message || '密码重置失败');
    } finally {
      setLoading(false);
    }
  };

  const handleSendCode = async (email: string) => {
    if (!email) {
      message.error('请输入邮箱地址');
      return;
    }

    setSendingCode(true);
    try {
      await authService.sendEmailCode({ email });
      message.success('验证码已发送，请查收邮件');

      // Start countdown
      setCountdown(60);
      const timer = setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) {
            clearInterval(timer);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } catch (error: any) {
      message.error(error.response?.data?.message || '发送验证码失败');
    } finally {
      setSendingCode(false);
    }
  };

  return (
    <div style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    }}>
      <div
        style={{
          width: 400,
          background: '#fff',
          borderRadius: 8,
          boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
          overflow: 'hidden'
        }}
      >
        <div style={{ padding: '24px 24px 0', textAlign: 'center', fontSize: '24px', fontWeight: 'bold' }}>
          重置密码
        </div>
        <div style={{ padding: 24 }}>
          <Form
            name="reset-password"
            onFinish={handleResetPassword}
            autoComplete="off"
            size="large"
          >
          <Form.Item
            name="email"
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入有效的邮箱地址' }
            ]}
          >
            <Input
              prefix={<MailOutlined />}
              placeholder="邮箱"
            />
          </Form.Item>

          <Form.Item
            name="code"
            rules={[{ required: true, message: '请输入验证码' }]}
          >
            <Input
              prefix={<LockOutlined />}
              placeholder="验证码"
              addonAfter={
                <Button
                  type="link"
                  size="small"
                  loading={sendingCode}
                  disabled={countdown > 0}
                  onClick={() => {
                    const form = document.querySelector('form[name="reset-password"]') as HTMLFormElement;
                    const emailInput = form?.querySelector('input[type="email"]') as HTMLInputElement;
                    handleSendCode(emailInput?.value);
                  }}
                >
                  {countdown > 0 ? `${countdown}s` : '发送验证码'}
                </Button>
              }
            />
          </Form.Item>

          <Form.Item
            name="newPassword"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, message: '密码至少6位' }
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="新密码（至少6位）"
            />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            rules={[
              { required: true, message: '请确认新密码' },
              { min: 6, message: '密码至少6位' }
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="确认新密码"
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
            >
              重置密码
            </Button>
          </Form.Item>

          <div style={{ textAlign: 'center' }}>
            <a onClick={() => navigate('/login')}>返回登录</a>
          </div>
          </Form>
        </div>
      </div>
    </div>
  );
};
