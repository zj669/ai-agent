import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Space, Tag, message, Modal, Input } from 'antd';
import { Plus, Edit, Play, Trash2, Copy } from 'lucide-react';
import { agentService } from '../services/agentService';
import { AgentSummary, AgentStatus } from '../types/agent';

/**
 * 工作流列表页面
 */
export function WorkflowListPage() {
  const navigate = useNavigate();
  const [agents, setAgents] = useState<AgentSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');

  // 加载 Agent 列表
  useEffect(() => {
    loadAgents();
  }, []);

  const loadAgents = async () => {
    setLoading(true);
    try {
      const data = await agentService.listAgents();
      setAgents(data);
    } catch (error: any) {
      message.error(`加载失败: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  // 过滤 Agent
  const filteredAgents = agents.filter((agent) =>
    agent.name.toLowerCase().includes(searchText.toLowerCase())
  );

  // 编辑工作流
  const handleEdit = (agentId: number) => {
    navigate(`/workflows/${agentId}`);
  };

  // 执行工作流
  const handleExecute = (agentId: number) => {
    navigate(`/workflows/${agentId}`);
  };

  // 复制工作流
  const handleCopy = async (agentId: number) => {
    try {
      // TODO: 实现复制功能
      message.success('工作流已复制');
      loadAgents();
    } catch (error: any) {
      message.error(`复制失败: ${error.message}`);
    }
  };

  // 删除工作流
  const handleDelete = (agentId: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个工作流吗？此操作不可恢复。',
      okText: '确定',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await agentService.deleteAgent(agentId);
          message.success('工作流已删除');
          loadAgents();
        } catch (error: any) {
          message.error(`删除失败: ${error.message}`);
        }
      }
    });
  };

  // 表格列定义
  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: AgentSummary) => (
        <div>
          <div className="font-medium">{text}</div>
          {record.description && (
            <div className="text-sm text-gray-500">{record.description}</div>
          )}
        </div>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: AgentStatus) => {
        const statusConfig = {
          [AgentStatus.DRAFT]: { color: 'default', text: '草稿' },
          [AgentStatus.PUBLISHED]: { color: 'success', text: '已发布' },
          [AgentStatus.ARCHIVED]: { color: 'error', text: '已归档' }
        };
        const config = statusConfig[status];
        return <Tag color={config.color}>{config.text}</Tag>;
      }
    },
    {
      title: '版本',
      dataIndex: 'publishedVersionId',
      key: 'publishedVersionId',
      width: 100,
      render: (versionId: number | undefined) => versionId || '-'
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 180,
      render: (time: string) => new Date(time).toLocaleString('zh-CN')
    },
    {
      title: '操作',
      key: 'actions',
      width: 250,
      render: (_: any, record: AgentSummary) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<Edit className="w-4 h-4" />}
            onClick={() => handleEdit(record.id)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            icon={<Play className="w-4 h-4" />}
            onClick={() => handleExecute(record.id)}
          >
            执行
          </Button>
          <Button
            type="link"
            size="small"
            icon={<Copy className="w-4 h-4" />}
            onClick={() => handleCopy(record.id)}
          >
            复制
          </Button>
          <Button
            type="link"
            size="small"
            danger
            icon={<Trash2 className="w-4 h-4" />}
            onClick={() => handleDelete(record.id)}
          >
            删除
          </Button>
        </Space>
      )
    }
  ];

  return (
    <div className="p-6">
      {/* 页面标题 */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-800">工作流管理</h1>
        <p className="text-gray-600 mt-1">管理和编排 AI Agent 工作流</p>
      </div>

      {/* 操作栏 */}
      <div className="mb-4 flex items-center justify-between">
        <Input.Search
          placeholder="搜索工作流名称"
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          style={{ width: 300 }}
        />

        <Button
          type="primary"
          icon={<Plus className="w-4 h-4" />}
          onClick={() => navigate('/agents/create')}
        >
          创建工作流
        </Button>
      </div>

      {/* 工作流列表 */}
      <Table
        columns={columns}
        dataSource={filteredAgents}
        rowKey="id"
        loading={loading}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 个工作流`
        }}
      />
    </div>
  );
}
