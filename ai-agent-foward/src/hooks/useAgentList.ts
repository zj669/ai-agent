import { useState, useEffect } from 'react';
import { message, Modal } from 'antd';
import { agentService } from '../services/agentService';
import { AgentSummary } from '../types/agent';

export const useAgentList = () => {
  const [agents, setAgents] = useState<AgentSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');

  const fetchAgents = async () => {
    setLoading(true);
    try {
      const data = await agentService.listAgents();
      setAgents(data);
    } catch (error: any) {
      message.error(error.response?.data?.message || '获取 Agent 列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAgents();
  }, []);

  const deleteAgent = async (id: number, name: string) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除 Agent "${name}" 吗？此操作将删除所有版本且不可恢复。`,
      okText: '确认',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await agentService.forceDeleteAgent(id);
          message.success('删除成功');
          fetchAgents();
        } catch (error: any) {
          message.error(error.response?.data?.message || '删除失败');
        }
      }
    });
  };

  const publishAgent = async (id: number, name: string) => {
    Modal.confirm({
      title: '确认发布',
      content: `确定要发布 Agent "${name}" 吗？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          await agentService.publishAgent({ id });
          message.success('发布成功');
          fetchAgents();
        } catch (error: any) {
          message.error(error.response?.data?.message || '发布失败');
        }
      }
    });
  };

  const filteredAgents = agents.filter(agent =>
    agent.name.toLowerCase().includes(searchText.toLowerCase()) ||
    agent.description?.toLowerCase().includes(searchText.toLowerCase())
  );

  return {
    agents: filteredAgents,
    loading,
    searchText,
    setSearchText,
    fetchAgents,
    deleteAgent,
    publishAgent
  };
};
