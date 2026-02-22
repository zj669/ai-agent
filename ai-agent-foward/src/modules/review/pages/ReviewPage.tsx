import { useEffect, useState } from 'react'
import { Table, Button, Tag, Modal, Input, message, Space, Empty, Typography } from 'antd'
import { CheckOutlined, CloseOutlined } from '@ant-design/icons'
import { getPendingReviews, resumeReview, type PendingReview } from '../../../shared/api/adapters/reviewAdapter'

const { TextArea } = Input
const { Title } = Typography

function ReviewPage() {
  const [reviews, setReviews] = useState<PendingReview[]>([])
  const [loading, setLoading] = useState(true)
  const [actionModal, setActionModal] = useState<{ review: PendingReview; approved: boolean } | null>(null)
  const [comment, setComment] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const loadReviews = async () => {
    setLoading(true)
    try {
      const data = await getPendingReviews()
      setReviews(data)
    } catch {
      message.error('加载审核列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void loadReviews() }, [])

  const handleAction = (review: PendingReview, approved: boolean) => {
    setActionModal({ review, approved })
    setComment('')
  }

  const handleSubmit = async () => {
    if (!actionModal) return
    setSubmitting(true)
    try {
      await resumeReview({
        executionId: actionModal.review.executionId,
        nodeId: actionModal.review.nodeId,
        approved: actionModal.approved,
        comment: comment || undefined
      })
      message.success(actionModal.approved ? '已批准' : '已拒绝')
      setActionModal(null)
      void loadReviews()
    } catch {
      message.error('操作失败')
    } finally {
      setSubmitting(false)
    }
  }

  const columns = [
    {
      title: 'Agent',
      dataIndex: 'agentName',
      key: 'agentName',
      render: (v: string) => v || '-'
    },
    {
      title: '节点',
      dataIndex: 'nodeName',
      key: 'nodeName',
      render: (v: string) => <Tag color="blue">{v || '-'}</Tag>
    },
    {
      title: '内容',
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
      width: 300
    },
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (v: string) => {
        try { return new Date(v).toLocaleString('zh-CN', { hour12: false }) } catch { return v }
      }
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_: unknown, record: PendingReview) => (
        <Space>
          <Button type="primary" size="small" icon={<CheckOutlined />} onClick={() => handleAction(record, true)}>
            批准
          </Button>
          <Button danger size="small" icon={<CloseOutlined />} onClick={() => handleAction(record, false)}>
            拒绝
          </Button>
        </Space>
      )
    }
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>待审核列表</Title>
        <Button onClick={() => void loadReviews()} loading={loading}>刷新</Button>
      </div>

      <Table
        columns={columns}
        dataSource={reviews}
        rowKey={(r) => `${r.executionId}-${r.nodeId}`}
        loading={loading}
        locale={{ emptyText: <Empty description="暂无待审核项" /> }}
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title={actionModal?.approved ? '确认批准' : '确认拒绝'}
        open={!!actionModal}
        onCancel={() => setActionModal(null)}
        onOk={() => void handleSubmit()}
        confirmLoading={submitting}
        okText={actionModal?.approved ? '批准' : '拒绝'}
        okButtonProps={actionModal?.approved ? {} : { danger: true }}
      >
        <p>执行ID: {actionModal?.review.executionId}</p>
        <p>节点: {actionModal?.review.nodeName}</p>
        <TextArea
          rows={3}
          placeholder="备注（可选）"
          value={comment}
          onChange={(e) => setComment(e.target.value)}
        />
      </Modal>
    </div>
  )
}

export default ReviewPage
