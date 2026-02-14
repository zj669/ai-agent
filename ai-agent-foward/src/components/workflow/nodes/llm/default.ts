import type { LlmNodeData } from './types';

export const defaultLlmNodeData: LlmNodeData = {
  label: 'LLM',
  nodeType: 'llm',
  config: {
    model: 'gpt-4o-mini',
    prompt: ''
  }
};
