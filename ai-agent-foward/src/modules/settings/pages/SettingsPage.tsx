import { useState, useEffect } from 'react'
import { Card, Form, Input, Button, message, Spin, Descriptions, Tabs, Typography } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { apiClient } from '../../../shared/api/client'
import { unwrapResponse, type ApiResponse } from '../../../shared/api/response'

const { Title } = Typography

interface UserInfo {
  id: number
  username: string
  email: string
  avatarUrl: string | null
  phone: string | null
  status: number
  createdAt: string
}

async function getUserInfo(): Promise<UserInfo> {
  const res = await apiClient.get<ApiResponse<UserInfo>>('/client/user/info')
  return unwrapResponse(res)
}

async function updateProfile(data: { username?: string; phone?: string }): Promise<UserInfo> {
  const res = await apiClient.post<ApiResponse<UserInfo>>('/client/user/profile', data)
  return unwrapResponse(res)
}

async function resetPassword(data: { email: string; newPassword: string; code: string }): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/client/user/resetPassword', data)
}

export default function SettingsPage() {
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null)
  const [loading, setLoading] = useState(true)
  const [profileForm] = Form.useForm()
  const [passwordForm] = Form.useForm()
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    getUserInfo()
      .then((info) => {
        setUserInfo(info)
        profileForm.setFieldsValue({ username: info.username, phone: info.phone ?? '' })
      })
      .catch(() => message.error('加载用户信息失败'))
      .finally(() => setLoading(false))
  }, [profileForm])

  const handleProfileSave = async (values: { username: string; phone: string }) => {
    setSaving(true)
    try {
      const updated = await updateProfile(values)
      setUserInfo(updated)
      localStorage.setItem('userInfo', JSON.stringify(updated))
      message.success('个人信息已更新')
    } catch {
      message.error('更新失败')
    } finally {
      setSaving(false)
    }
  }

  const handlePasswordReset = async (values: { code: string; newPassword: string; confirm: string }) => {
    if (!userInfo) return
    setSaving(true)
    try {
      await resetPassword({ email: userInfo.email, newPassword: values.newPassword, code: values.code })
      message.success('密码已重置')
      passwordForm.resetFields()
    } catch {
      message.error('重置密码失败')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <Spin style={{ display: 'block', margin: '100px auto' }} />

  const tabItems = [
    {
      key: 'profile',
      label: '个人信息',
      icon: <UserOutlined />,
      children: (
        <div style={{ maxWidth: 500 }}>
          <Descriptions column={1} style={{ marginBottom: 24 }}>
            <Descriptions.Item label="邮箱">{userInfo?.email}</Descriptions.Item>
            <Descriptions.Item label="注册时间">{userInfo?.createdAt}</Descriptions.Item>
          </Descriptions>
          <Form form={profileForm} layout="vertical" onFinish={handleProfileSave}>
            <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
              <Input placeholder="请输入用户名" />
            </Form.Item>
            <Form.Item name="phone" label="手机号">
              <Input placeholder="请输入手机号" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={saving}>保存</Button>
            </Form.Item>
          </Form>
        </div>
      ),
    },
    {
      key: 'security',
      label: '安全设置',
      icon: <LockOutlined />,
      children: (
        <div style={{ maxWidth: 500 }}>
          <Form form={passwordForm} layout="vertical" onFinish={handlePasswordReset}>
            <Form.Item name="code" label="邮箱验证码" rules={[{ required: true, message: '请输入验证码' }]}>
              <Input placeholder="请输入邮箱验证码" />
            </Form.Item>
            <Form.Item name="newPassword" label="新密码" rules={[{ required: true, min: 6, message: '密码至少 6 位' }]}>
              <Input.Password placeholder="请输入新密码" />
            </Form.Item>
            <Form.Item
              name="confirm"
              label="确认密码"
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
              <Input.Password placeholder="请再次输入新密码" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={saving}>重置密码</Button>
            </Form.Item>
          </Form>
        </div>
      ),
    },
  ]

  return (
    <Card>
      <Title level={4} style={{ marginTop: 0, marginBottom: 24 }}>个人设置</Title>
      <Tabs items={tabItems} />
    </Card>
  )
}
