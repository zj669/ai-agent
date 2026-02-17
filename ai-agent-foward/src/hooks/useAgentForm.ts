import { useState, useEffect } from 'react';
import { message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { agentService } from '../services/agentService';
import { AgentDetail, CreateAgentRequest, UpdateAgentRequest } from '../types/agent';

export const useAgentForm = (agentId?: number) => {
  const navigate = useNavigate();
  const [agent, setAgent] = useState<AgentDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (agentId) {
      fetchAgent();
    }
  }, [agentId]);

  const fetchAgent = async () => {
    if (!agentId) return;

    setLoading(true);
    try {
      const data = await agentService.getAgent(agentId);
      setAgent(data);
    } catch (error: any) {
      message.error(error.response?.data?.message || '获取 Agent 详情失败');
    } finally {
      setLoading(false);
    }
  };

  const createAgent = async (data: CreateAgentRequest) => {
    setSubmitting(true);
    try {
      const id = await agentService.createAgent(data);
      message.success('创建成功');
      navigate(`/agents/${id}/workflow`);
    } catch (error: any) {
      message.error(error.response?.data?.message || '创建失败');
      throw error;
    } finally {
      setSubmitting(false);
    }
  };

  const updateAgent = async (data: UpdateAgentRequest) => {
    setSubmitting(true);
    try {
      await agentService.updateAgent(data);
      message.success('保存成功');
      fetchAgent();
    } catch (error: any) {
      message.error(error.response?.data?.message || '保存失败');
      throw error;
    } finally {
      setSubmitting(false);
    }
  };

  return {
    agent,
    loading,
    submitting,
    fetchAgent,
    createAgent,
    updateAgent
  };
};
