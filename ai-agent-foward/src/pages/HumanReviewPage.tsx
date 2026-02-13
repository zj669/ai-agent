import { useState } from 'react';
import { Card, Table, Space, Button, Select, Tag, Modal, Input, Typography, Descriptions } from 'antd';
import { ReloadOutlined, CheckCircleOutlined, CloseCircleOutlined, EyeOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useHumanReview } from '../hooks/useHumanReview';
import { HumanReviewTask } from '../types/humanReview';
import dayjs from 'dayjs';

const { Option } = Select;
const { TextArea } = Input;
const { Title, Text } = Typography;

export const HumanReviewPage: React.FC = () => {
  const {
    tasks,
    loading,
    statusFilter,
    setStatusFilter,
    approveTask,
    rejectTask,
    refreshTasks
  } = useHumanReview();

  const [reviewModalVisible, setReviewModalVisible] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [currentTask, setCurrentTask] = useState<HumanReviewTask | null>(null);
  const [reviewAction, setReviewAction] = useState<'approve' | 'reject'>('approve');
  const [reviewComment, setReviewComment] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleApprove = (task: HumanReviewTask) => {
    setCurrentTask(task);
    setReviewAction('approve');
    setReviewComment('');
    setReviewModalVisible(true);
  };

  const handleReject = (task: HumanReviewTask) => {
    setCurrentTask(task);
    setReviewAction('reject');
    setReviewComment('');
    setReviewModalVisible(true);
  };

  const handleViewDetail = (task: HumanReviewTask) => {
    setCurrentTask(task);
    setDetailModalVisible(true);
  };

  const handleSubmitReview = async () => {
    if (!currentTask) return;

    setSubmitting(true);
    try {
      if (reviewAction === 'approve') {
        await approveTask(currentTask.taskId, reviewComment);
      } else {
        await rejectTask(currentTask.taskId, reviewComment);
      }
      setReviewModalVisible(false);
      setReviewComment('');
      setCurrentTask(null);
    } catch (error) {
      // Error handled in hook
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusTag = (status: string) => {
    const statusConfig = {
      PENDING: { color: 'processing', text: '待审核' },
      APPROVED: { color: 'success', text: '已批准' },
      REJECTED: { color: 'error', text: '已拒绝' }
    };
    const config = statusConfig[status as keyof typeof statusConfig] || { color: 'default', text: status };
    return <Tag color={config.color}>{config.text}</Tag>;
  };

  const columns: ColumnsType<HumanReviewTask> = [
    {
      title: '任务ID',
      dataIndex: 'taskId',
      key: 'taskId',
      width: 200,
      ellipsis: true,
      render: (text) => <Text copyable style={{ fontSize: '12px' }}>{text}</Text>
    },
    {
      title: '工作流执行ID',
      dataIndex: 'executionId',
      key: 'executionId',
      width: 200,
      ellipsis: true,
      render: (text) => <Text copyable style={{ fontSize: '12px' }}>{text}</Text>
    },
    {
      title: '节点ID',
      dataIndex: 'nodeId',
      key: 'nodeId',
      width: 150,
      ellipsis: true
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => getStatusTag(status)
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (text) => dayjs(text).format('YYYY-MM-DD HH:mm:ss')
    },
    {
      title: '审核时间',
      dataIndex: 'reviewedAt',
      key: 'reviewedAt',
      width: 180,
      render: (text) => text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-'
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            详情
          </Button>
          {record.status === 'PENDING' && (
            <>
              <Button
                type="primary"
                size="small"
                icon={<CheckCircleOutlined />}
                onClick={() => handleApprove(record)}
              >
                批准
              </Button>
              <Button
                danger
                size="small"
                icon={<CloseCircleOutlined />}
                onClick={() => handleReject(record)}
              >
                拒绝
              </Button>
            </>
          )}
        </Space>
      )
    }
  ];

  return (
    <div style={{ padding: '24px' }}>
      <Card
        title={
          <Space>
            <Title level={4} style={{ margin: 0 }}>人工审核队列</Title>
            <Tag color="blue">{tasks.length} 个任务</Tag>
          </Space>
        }
        extra={
          <Space>
            <Select
              placeholder="状态筛选"
              style={{ width: 150 }}
              value={statusFilter}
              onChange={setStatusFilter}
              allowClear
            >
              <Option value="PENDING">待审核</Option>
              <Option value="APPROVED">已批准</Option>
              <Option value="REJECTED">已拒绝</Option>
            </Select>
            <Button
              icon={<ReloadOutlined />}
              onClick={refreshTasks}
              loading={loading}
            >
              刷新
            </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={tasks}
          rowKey="taskId"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条记录`
          }}
          scroll={{ x: 1200 }}
        />
      </Card>

      {/* 审核操作 Modal */}
      <Modal
        title={reviewAction === 'approve' ? '批准审核' : '拒绝审核'}
        open={reviewModalVisible}
        onOk={handleSubmitReview}
        onCancel={() => {
          setReviewModalVisible(false);
          setReviewComment('');
        }}
        confirmLoading={submitting}
        okText="确认"
        cancelText="取消"
        width={600}
      >
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="任务ID">{currentTask?.taskId}</Descriptions.Item>
            <Descriptions.Item label="工作流执行ID">{currentTask?.executionId}</Descriptions.Item>
            <Descriptions.Item label="节点ID">{currentTask?.nodeId}</Descriptions.Item>
          </Descriptions>

          <div>
            <Text strong style={{ display: 'block', marginBottom: '8px' }}>
              {reviewAction === 'approve' ? '审核意见（可选）' : '拒绝原因（必填）'}
            </Text>
            <TextArea
              rows={4}
              placeholder={reviewAction === 'approve' ? '请输入审核意见...' : '请输入拒绝原因...'}
              value={reviewComment}
              onChange={(e) => setReviewComment(e.target.value)}
              maxLength={500}
              showCount
            />
          </div>
        </Space>
      </Modal>

      {/* 任务详情 Modal */}
      <Modal
        title="任务详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>
        ]}
        width={800}
      >
        {currentTask && (
          <Space direction="vertical" style={{ width: '100%' }} size="large">
            <Descriptions column={2} bordered>
              <Descriptions.Item label="任务ID" span={2}>
                <Text copyable>{currentTask.taskId}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="工作流执行ID" span={2}>
                <Text copyable>{currentTask.executionId}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="节点ID">{currentTask.nodeId}</Descriptions.Item>
              <Descriptions.Item label="状态">{getStatusTag(currentTask.status)}</Descriptions.Item>
              <Descriptions.Item label="创建时间">
                {dayjs(currentTask.createdAt).format('YYYY-MM-DD HH:mm:ss')}
              </Descriptions.Item>
              <Descriptions.Item label="审核时间">
                {currentTask.reviewedAt ? dayjs(currentTask.reviewedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
              </Descriptions.Item>
              {currentTask.reviewerId && (
                <Descriptions.Item label="审核人ID">{currentTask.reviewerId}</Descriptions.Item>
              )}
              {currentTask.reviewComment && (
                <Descriptions.Item label="审核意见" span={2}>
                  {currentTask.reviewComment}
                </Descriptions.Item>
              )}
            </Descriptions>

            <div>
              <Title level={5}>输入数据</Title>
              <pre style={{
                background: '#f5f5f5',
                padding: '12px',
                borderRadius: '4px',
                maxHeight: '200px',
                overflow: 'auto'
              }}>
                {JSON.stringify(currentTask.inputData, null, 2)}
              </pre>
            </div>

            {currentTask.outputData && (
              <div>
                <Title level={5}>输出数据</Title>
                <pre style={{
                  background: '#f5f5f5',
                  padding: '12px',
                  borderRadius: '4px',
                  maxHeight: '200px',
                  overflow: 'auto'
                }}>
                  {JSON.stringify(currentTask.outputData, null, 2)}
                </pre>
              </div>
            )}
          </Space>
        )}
      </Modal>
    </div>
  );
};
