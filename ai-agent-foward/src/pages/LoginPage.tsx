import { useEffect, useState } from 'react';
import { Form, Input, Button, Tabs, Checkbox, message } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { authService } from '../services/authService';
import { loadCredential } from '../services/credentialStorageService';
import type { LoginRequest, RegisterRequest } from '../types/auth';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login, register } = useAuthStore();
  const [loading, setLoading] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [loginForm] = Form.useForm();
  const [registerForm] = Form.useForm();

  const redirect = searchParams.get('redirect') || '/dashboard';

  useEffect(() => {
    const credential = loadCredential();
    if (!credential) {
      return;
    }

    loginForm.setFieldsValue({
      email: credential.email,
      password: credential.password,
      rememberMe: true
    });
  }, [loginForm]);

  const handleLogin = async (values: LoginRequest) => {
    setLoading(true);
    try {
      await login(values);
      message.success('登录成功');
      navigate(redirect);
    } catch (error: any) {
      console.error('Login error:', error);

      // 检查是否是网络错误
      if (!error.response) {
        if (error.request) {
          message.error('无法连接到服务器，请检查后端服务是否启动（http://localhost:8080）');
        } else {
          message.error('请求失败：' + (error.message || '未知错误'));
        }
        return;
      }

      // 处理后端返回的错误
      const errorCode = error.response?.data?.code;
      const errorMsg = error.response?.data?.message;

      if (errorCode === 'TOO_MANY_LOGIN_ATTEMPTS') {
        message.error('登录失败次数过多,账号已被锁定15分钟,请稍后再试');
      } else if (errorCode === 'INVALID_CREDENTIALS') {
        message.error('邮箱或密码错误');
      } else if (errorCode === 'USER_DISABLED') {
        message.error('账号已被禁用,请联系管理员');
      } else if (error.response.status === 404) {
        message.error('登录接口不存在，请检查后端服务');
      } else if (error.response.status === 500) {
        message.error('服务器内部错误：' + (errorMsg || '请联系管理员'));
      } else {
        message.error(errorMsg || '登录失败,请稍后重试');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (values: RegisterRequest) => {
    setLoading(true);
    try {
      await register(values);
      message.success('注册成功');
      navigate(redirect);
    } catch (error: any) {
      console.error('Register error:', error);

      // 检查是否是网络错误
      if (!error.response) {
        if (error.request) {
          message.error('无法连接到服务器，请检查后端服务是否启动（http://localhost:8080）');
        } else {
          message.error('请求失败：' + (error.message || '未知错误'));
        }
        return;
      }

      // 处理后端返回的错误
      const errorCode = error.response?.data?.code;
      const errorMsg = error.response?.data?.message;

      if (errorCode === 'INVALID_VERIFICATION_CODE') {
        message.error('验证码无效或已过期');
      } else if (errorCode === 'EMAIL_ALREADY_REGISTERED') {
        message.error('该邮箱已被注册');
      } else if (errorCode === 'WEAK_PASSWORD') {
        message.error('密码强度不足,至少需要8位');
      } else if (error.response.status === 404) {
        message.error('注册接口不存在，请检查后端服务');
      } else if (error.response.status === 500) {
        message.error('服务器内部错误：' + (errorMsg || '请联系管理员'));
      } else {
        message.error(errorMsg || '注册失败,请稍后重试');
      }
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
      message.success('验证码已发送,5分钟内有效,请查收邮件');

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
      console.error('Send code error:', error);

      // 检查是否是网络错误
      if (!error.response) {
        if (error.request) {
          message.error('无法连接到服务器，请检查后端服务是否启动（http://localhost:8080）');
        } else {
          message.error('请求失败：' + (error.message || '未知错误'));
        }
        return;
      }

      // 处理后端返回的错误
      const errorCode = error.response?.data?.code;
      const errorMsg = error.response?.data?.message;

      if (errorCode === 'RATE_LIMITED') {
        message.error('操作过于频繁,请1分钟后再试');
      } else if (errorCode === 'INVALID_EMAIL') {
        message.error('邮箱格式不正确');
      } else if (errorCode === 'EMAIL_SEND_FAILED') {
        message.error('邮件发送失败,请稍后重试');
      } else if (error.response.status === 404) {
        message.error('发送验证码接口不存在，请检查后端服务');
      } else if (error.response.status === 500) {
        message.error('服务器内部错误：' + (errorMsg || '请联系管理员'));
      } else {
        message.error(errorMsg || '发送验证码失败');
      }
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
          AI Agent Platform
        </div>
        <div style={{ padding: 24 }}>
          <Tabs
            defaultActiveKey="login"
            centered
            items={[
            {
              key: 'login',
              label: '登录',
              children: (
                <Form
                  name="login"
                  form={loginForm}
                  onFinish={handleLogin}
                  autoComplete="off"
                  size="large"
                >
                  <Form.Item
                    name="email"
                    rules={[
                      {
                        validator: (_, value) => {
                          if (!value || !value.trim()) {
                            return Promise.reject('请输入邮箱');
                          }
                          if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(value)) {
                            return Promise.reject('邮箱格式不正确，请输入有效的邮箱地址');
                          }
                          return Promise.resolve();
                        }
                      }
                    ]}
                  >
                    <Input
                      prefix={<MailOutlined />}
                      placeholder="邮箱"
                    />
                  </Form.Item>

                  <Form.Item
                    name="password"
                    rules={[{ required: true, message: '请输入密码' }]}
                  >
                    <Input.Password
                      prefix={<LockOutlined />}
                      placeholder="密码"
                    />
                  </Form.Item>

                  <Form.Item
                    name="rememberMe"
                    valuePropName="checked"
                  >
                    <Checkbox>记住我</Checkbox>
                  </Form.Item>

                  <Form.Item>
                    <Button
                      type="primary"
                      htmlType="submit"
                      loading={loading}
                      block
                    >
                      登录
                    </Button>
                  </Form.Item>

                  <div style={{ textAlign: 'center' }}>
                    <a onClick={() => navigate('/reset-password')}>忘记密码？</a>
                  </div>
                </Form>
              )
            },
            {
              key: 'register',
              label: '注册',
              children: (
                <Form
                  name="register"
                  form={registerForm}
                  onFinish={handleRegister}
                  autoComplete="off"
                  size="large"
                >
                  <Form.Item
                    name="email"
                    rules={[
                      {
                        validator: (_, value) => {
                          if (!value || !value.trim()) {
                            return Promise.reject('请输入邮箱');
                          }
                          if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(value)) {
                            return Promise.reject('邮箱格式不正确，请输入有效的邮箱地址');
                          }
                          return Promise.resolve();
                        }
                      }
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
                            const email = registerForm.getFieldValue('email');
                            handleSendCode(email);
                          }}
                        >
                          {countdown > 0 ? `${countdown}s` : '发送验证码'}
                        </Button>
                      }
                    />
                  </Form.Item>

                  <Form.Item
                    name="password"
                    rules={[
                      { required: true, message: '请输入密码' },
                      { min: 8, message: '密码至少8位' }
                    ]}
                  >
                    <Input.Password
                      prefix={<LockOutlined />}
                      placeholder="密码（至少8位）"
                    />
                  </Form.Item>

                  <Form.Item
                    name="username"
                  >
                    <Input
                      prefix={<UserOutlined />}
                      placeholder="用户名（可选）"
                    />
                  </Form.Item>

                  <Form.Item>
                    <Button
                      type="primary"
                      htmlType="submit"
                      loading={loading}
                      block
                    >
                      注册
                    </Button>
                  </Form.Item>
                </Form>
              )
            }
          ]}
          />
        </div>
      </div>
    </div>
  );
};
