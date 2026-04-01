# Frontend Component Guidelines

> React component patterns and conventions for the AI Agent Platform.

---

## Overview

- **React**: 19.2 (latest features: use(), actions, etc.)
- **UI Library**: Ant Design 6.3 (primary component library)
- **State**: Zustand 5.0 (global), React state (local)
- **Styling**: Tailwind CSS 3.4 + `clsx` + `tailwind-merge` for conditional classes
- **Flow Editor**: @xyflow/react 12.10
- **Forms**: react-hook-form (where used)

---

## Component Patterns

### Functional Components Only

All components are functional with hooks. No class components.

```tsx
// ✅ Correct — functional component
import { useState, useEffect } from 'react'

export default function AgentListPage() {
  const [agents, setAgents] = useState<AgentSummary[]>([])
  // ...
}
```

### Page Component Pattern

Pages are top-level route components in `modules/{mod}/pages/`:

```tsx
// modules/agent/pages/AgentListPage.tsx
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Card, Tag, Space, Row, Col, Empty, Spin, message } from 'antd'
import { PlusOutlined, EditOutlined } from '@ant-design/icons'
import { getAgentList, createAgent, type AgentSummary } from '../../../shared/api/adapters/agentAdapter'

export default function AgentListPage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [agents, setAgents] = useState<AgentSummary[]>([])

  useEffect(() => {
    loadAgents()
  }, [])

  async function loadAgents() {
    setLoading(true)
    try {
      const list = await getAgentList()
      setAgents(list)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      {/* Ant Design components */}
    </div>
  )
}
```

---

## State Management

### Local State (React useState)

For component-scoped state — loading, form inputs, UI toggles:

```tsx
const [loading, setLoading] = useState(false)
const [searchTerm, setSearchTerm] = useState('')
```

### Global State (Zustand)

For cross-component shared state. Store files in `modules/{mod}/stores/`:

```tsx
// modules/workflow/stores/useEditorStore.ts
import { create } from 'zustand'

interface EditorState {
  nodes: Node[]
  edges: Edge[]
  selectedNodeId: string | null
  setSelectedNode: (id: string | null) => void
  // ...
}

export const useEditorStore = create<EditorState>((set) => ({
  nodes: [],
  edges: [],
  selectedNodeId: null,
  setSelectedNode: (id) => set({ selectedNodeId: id }),
}))
```

Usage:

```tsx
function NodeSelector() {
  const selectedNodeId = useEditorStore((s) => s.selectedNodeId)
  // Use selector for optimal re-renders
}
```

---

## API Integration

### Layered API Architecture

```
Component → Module API Service → Shared Adapter → HTTP Client → Backend
```

### Shared API Adapter Pattern

Adapters in `shared/api/adapters/` handle request/response mapping:

```tsx
// shared/api/adapters/dashboardAdapter.ts
import { apiClient, type ApiClientLike } from '../client'
import { unwrapResponse, type ApiResponse } from '../response'

export interface DashboardStats {
  agentCount: number
  publishedAgentCount: number
  conversationCount: number
}

export async function getDashboardStats(
  client: ApiClientLike = apiClient
): Promise<DashboardStats> {
  const response = await client.get<ApiResponse<DashboardStats>>('/api/dashboard/stats')
  return unwrapResponse(response)
}
```

### Key Patterns:
- **Default client parameter** — enables dependency injection for testing
- **`unwrapResponse()`** — extracts `data` from `{ code, message, data }` wrapper
- **TypeScript interfaces** — for request/response types

### Module API Service Pattern

Thin wrapper in `modules/{mod}/api/` if needed:

```tsx
// modules/agent/api/agentService.ts
import { createAgent as createAgentApi, getAgentList } from '../../../shared/api/adapters/agentAdapter'

export async function fetchAgentList() {
  return getAgentList()
}

export async function createAgent() {
  const id = await createAgentApi({ name: '未命名 Agent' })
  return { id: String(id) }
}
```

---

## Error Handling (Frontend)

### API Error Normalization

All API errors go through `shared/api/errorMapper.ts`:

```tsx
export type NormalizedErrorCode =
  | 'TOKEN_EXPIRED'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'RATE_LIMITED'
  | 'TIMEOUT'
  | 'NETWORK_ERROR'
  | 'UNKNOWN_ERROR'

export interface NormalizedApiError {
  code: NormalizedErrorCode
  message: string
  status?: number
}
```

### HTTP Client Interceptors

`shared/api/httpClient.ts` handles:
- **Request**: Attach JWT token from localStorage/sessionStorage
- **Response**: Auto-clear token + redirect on 401

```tsx
instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken')
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})
```

### Toast Notifications

Use `shared/feedback/toast.ts` for lightweight toasts:

```tsx
import { showToast } from '../../../shared/feedback/toast'

showToast('保存成功', 'success')
showToast('操作失败', 'error')
```

Or Ant Design's `message` API for heavier notifications.

---

## Styling

### Tailwind CSS + Ant Design

- **Layout/spacing**: Tailwind utilities
- **UI components**: Ant Design (Button, Card, Table, Modal, etc.)
- **Conditional classes**: `clsx` + `tailwind-merge`

```tsx
import clsx from 'clsx'

<div className={clsx('p-4 rounded-lg', isActive && 'bg-blue-50 border-blue-200')}>
```

### Constants for Dynamic Styles

```tsx
const AVATAR_COLORS = [
  '#1677ff', '#52c41a', '#faad14', '#eb2f96',
  '#722ed1', '#13c2c2', '#fa541c', '#2f54eb',
]

const statusMap: Record<string, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: 'default' },
  PUBLISHED: { label: '已发布', color: 'green' },
}
```

---

## Testing

### Unit Tests (Vitest)

Test files co-located in `__tests__/` directories:

```tsx
// modules/agent/__tests__/agent.create-to-workflow.test.tsx
import { render, screen, waitFor } from '@testing-library/react'
import { vi } from 'vitest'

vi.mock('../../../shared/api/adapters/agentAdapter', () => ({
  createAgent: mockCreateAgent,
  getAgentList: () => mockGetAgentList(),
}))

describe('agent create to workflow', () => {
  it('navigates to workflow after creation', async () => {
    // ...
  })
})
```

### Key Testing Patterns:
- **Mock API adapters** — never hit real APIs in unit tests
- **Use `vi.mock()` with `vi.hoisted()`** — for clean mock setup
- **Inject mock clients** — adapters accept optional `client` parameter
- **Use `@testing-library/react`** — for component testing

---

## SSE / Streaming

For real-time features (chat, workflow execution, swarm), use custom hooks:

```tsx
// modules/swarm/hooks/useAgentStream.ts
export function useAgentStream(agentId: number) {
  // EventSource-based SSE connection
  // Handles reconnection, error states
}
```

---

## Common Mistakes

1. **❌ Importing from `app/frontend/`** — Use `ai-agent-foward/` only
2. **❌ Direct Axios calls in components** — Use shared API adapters
3. **❌ Missing TypeScript types** — Always type API responses and props
4. **❌ Storing auth tokens insecurely** — Use httpClient's built-in interceptors
5. **❌ Not using Ant Design components** — Don't reinvent basic UI patterns
6. **❌ Global state for local concerns** — Use `useState` for component-scoped state
7. **❌ Missing error handling on API calls** — Always handle loading/error states
