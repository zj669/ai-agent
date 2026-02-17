import { Button, Input, Table, Space, Tag, Tooltip } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  RocketOutlined,
  SearchOutlined
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAgentList } from '../hooks/useAgentList';
import { AgentSummary, AgentStatus } from '../types/agent';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';

export const AgentListPage: React.FC = () => {
  const navigate = useNavigate();
  const {
    agents,
    loading,
    searchText,
    setSearchText,
    deleteAgent,
    publishAgent
  } = useAgentList();

  const getStatusTag = (status: AgentStatus) => {
    const statusConfig = {
      [AgentStatus.DRAFT]: { color: 'default', text: '草稿' },
      [AgentStatus.PUBLISHED]: { color: 'success', text: '已发布' },
      [AgentStatus.ARCHIVED]: { color: 'warning', text: '已归档' }
    };
    const config = statusConfig[status] || { color: 'default', text: status };
    return <Tag color={config.color}>{config.text}</Tag>;
  };

  const columns: ColumnsType<AgentSummary> = [
    {
      title: 'Icon',
      dataIndex: 'icon',
      key: 'icon',
      width: 60,
      render: (icon: string) => (
        <div style={{
          width: 40,
          height: 40,
          borderRadius: '8px',
          background: icon || '#f0f0f0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: '20px'
        }}>
          {icon ? icon : '🤖'}
        </div>
      )
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: AgentSummary) => (
        <a onClick={() => navigate(`/agents/${record.id}/workflow`)}>
          {name}
        </a>
      )
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: AgentStatus) => getStatusTag(status)
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 180,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm:ss')
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: any, record: AgentSummary) => (
        <Space size="small">
          <Tooltip title="编辑">
            <Button
              type="text"
              icon={<EditOutlined />}
              onClick={() => navigate(`/agents/${record.id}/workflow`)}
            />
          </Tooltip>

          {record.status === AgentStatus.DRAFT && (
            <Tooltip title="发布">
              <Button
                type="text"
                icon={<RocketOutlined />}
                onClick={() => publishAgent(record.id, record.name)}
              />
            </Tooltip>
          )}

          <Tooltip title="删除">
            <Button
              type="text"
              danger
              icon={<DeleteOutlined />}
              onClick={() => deleteAgent(record.id, record.name)}
            />
          </Tooltip>
        </Space>
      )
    }
  ];

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h2 style={{ margin: 0 }}>Agent 管理</h2>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => navigate('/agents/create')}
          >
            创建 Agent
          </Button>
        </div>

        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Input
            placeholder="搜索 Agent 名称或描述"
            prefix={<SearchOutlined />}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            style={{ width: 300 }}
            allowClear
          />

          <Table
            columns={columns}
            dataSource={agents}
            rowKey="id"
            loading={loading}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 条`
            }}
          />
        </Space>
      </div>
    </div>
  );
};
