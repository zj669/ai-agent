export type ServerStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'ERROR';

export type ServerType = 'stdio' | 'sse' | 'http';

export interface McpServer {
  id: number;
  userId: number;
  name: string;
  serverType: ServerType;
  config: McpServerConfig;
  enabled: boolean;
  status: ServerStatus;
  statusDesc: string;
  description?: string;
  createTime: string;
  updateTime: string;
}

export interface McpServerConfig {
  type?: string;
  command?: string;
  args?: string[];
  env?: Record<string, string>;
  url?: string;
  headers?: Record<string, string>;
  endpoint?: string;
}

export interface McpTool {
  serverId: number;
  serverName: string;
  toolName: string;
  fullName: string;
  description: string;
  inputSchema: string;
}

export interface CreateServerPayload {
  name: string;
  serverType: ServerType;
  configJson: string;
  enabled?: boolean;
  description?: string;
}

export interface UpdateServerPayload {
  name: string;
  serverType: ServerType;
  configJson: string;
  enabled?: boolean;
  description?: string;
}
