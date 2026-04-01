# Frontend Quality Guidelines

> Code quality standards for the AI Agent Platform frontend.

---

## Overview

- **Language**: TypeScript ~5.9 (strict mode)
- **Framework**: React 19.2 + Vite 7.1
- **UI Library**: Ant Design 6.3
- **Linting**: ESLint 9 + eslint-plugin-react-hooks + react-refresh
- **Testing**: Vitest 3.2 (unit) + Playwright 1.55 (E2E)
- **Styling**: Tailwind CSS 3.4

---

## Build & Quality Commands

```bash
cd ai-agent-foward

# Development
npm run dev          # Start dev server (http://localhost:5173)

# Quality checks
npm run lint         # ESLint
npm run typecheck    # TypeScript (tsc --noEmit)
npm run test         # Unit tests (Vitest)
npm run test:watch   # Unit tests in watch mode
npm run test:e2e     # E2E tests (Playwright)

# Production
npm run build        # tsc -b && vite build
npm run preview      # Preview production build
```

**All of these must pass before commit.**

---

## Forbidden Patterns

### ❌ NEVER Do These

| Pattern | Why | Correct Alternative |
|---------|-----|---------------------|
| Class components | Outdated, no hooks support | Functional components with hooks |
| Direct `axios.get/post` in components | Bypasses error handling, untestable | Use `shared/api/adapters/` |
| `any` type | Defeats TypeScript purpose | Use proper types or `unknown` |
| `localStorage` for auth directly | Bypass interceptors | Use httpClient's built-in token management |
| `console.log` in committed code | Noise in production | Remove or use proper logging |
| CSS-in-JS (styled-components) | Not used in this project | Tailwind CSS + Ant Design |
| Barrel exports (`index.ts`) | Not used in this codebase | Direct file imports |
| `app/frontend/` directory | Legacy skeleton | Use `ai-agent-foward/` |
| Inline styles for complex layouts | Hard to maintain | Tailwind utilities |
| Non-typed API responses | Runtime errors | Type all API response interfaces |

---

## Required Patterns

### ✅ ALWAYS Do These

| Pattern | Where | Example |
|---------|-------|---------|
| TypeScript strict types | All files | Interface for API responses, component props |
| Ant Design for UI | All UI components | `Button`, `Card`, `Table`, `Modal`, `message` |
| API adapter layer | API calls | `shared/api/adapters/{mod}Adapter.ts` |
| Loading/error states | Any data fetch | `useState<boolean>(true)` + try/finally |
| `useNavigate()` for routing | Navigation | `const navigate = useNavigate()` |
| `clsx` for conditional classes | Dynamic styling | `clsx('base', condition && 'active')` |
| Test co-location | Tests | `__tests__/` inside each module |
| Mock API in tests | Unit tests | `vi.mock()` with `vi.hoisted()` |
| Default client DI | Adapters | `client: ApiClientLike = apiClient` parameter |

---

## TypeScript Conventions

### API Types

```tsx
// shared/api/response.ts
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}
```

### Component Props

```tsx
interface AgentCardProps {
  agent: AgentSummary
  onEdit: (id: string) => void
  onDelete: (id: string) => void
}

export default function AgentCard({ agent, onEdit, onDelete }: AgentCardProps) {
  // ...
}
```

### Enum-like Maps

```tsx
const statusMap: Record<string, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: 'default' },
  PUBLISHED: { label: '已发布', color: 'green' },
}
```

---

## File Organization Rules

### Import Order

1. React / React DOM
2. Third-party libraries (react-router-dom, antd, zustand)
3. Shared utilities (`../../../shared/...`)
4. Module-local imports (`../components/...`)
5. Types (if separate)

```tsx
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Card, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { getAgentList } from '../../../shared/api/adapters/agentAdapter'
import AgentCard from '../components/AgentCard'
```

### Export Pattern

- **Page components**: `export default function {Name}Page()`
- **Utility functions**: Named exports `export function {name}()`
- **Types**: Named exports `export interface {Name}`

---

## Testing Standards

### Unit Test Pattern

```tsx
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { vi, describe, it, expect, beforeEach } from 'vitest'

// Hoisted mocks
const { mockFn } = vi.hoisted(() => ({
  mockFn: vi.fn()
}))

// Module mocks
vi.mock('../../../shared/api/adapters/agentAdapter', () => ({
  getAgentList: mockFn,
}))

describe('AgentListPage', () => {
  beforeEach(() => {
    mockFn.mockReset()
  })

  it('renders agent list', async () => {
    mockFn.mockResolvedValue([{ id: 1, name: 'Test Agent' }])
    render(<AgentListPage />)
    await waitFor(() => expect(screen.getByText('Test Agent')).toBeInTheDocument())
  })
})
```

### Adapter Test Pattern (DI-based)

```tsx
import { vi } from 'vitest'

describe('dashboardAdapter', () => {
  it('returns stats', async () => {
    const client = {
      get: vi.fn().mockResolvedValue({
        data: { code: 200, message: 'ok', data: { agentCount: 5 } }
      })
    }

    const result = await getDashboardStats(client)
    expect(result.agentCount).toBe(5)
  })
})
```

### What to Test

| Priority | What | How |
|----------|------|-----|
| 🔴 Must | API adapters | Mock HTTP client, verify request/response mapping |
| 🔴 Must | Validation logic | Pure function tests |
| 🟡 Should | Key page components | Mock APIs, render, verify UI state |
| 🟡 Should | Zustand stores | Test actions and state changes |
| 🟢 Nice | UI interactions | fireEvent + waitFor |

---

## Pre-Commit Checklist

- [ ] `npm run lint` — no ESLint errors
- [ ] `npm run typecheck` — no TypeScript errors
- [ ] `npm run test` — all unit tests pass
- [ ] No `console.log` in committed code
- [ ] No `any` types (use `unknown` if truly needed)
- [ ] API calls go through adapter layer
- [ ] Loading/error states handled
- [ ] New routes added to `app/router.tsx`
- [ ] New modules follow `modules/{name}/` structure

---

## Common Mistakes

1. **❌ Not handling loading states** — Users see blank screens
2. **❌ Missing TypeScript types on API responses** — Runtime crashes
3. **❌ Testing implementation details** — Test behavior, not structure
4. **❌ Forgetting `mockReset()` in `beforeEach`** — Test pollution
5. **❌ Not using `unwrapResponse()`** — Manually extracting `.data.data`
6. **❌ Creating new UI components when Ant Design has one** — Check antd docs first
7. **❌ Large component files** — Extract sub-components, hooks, or stores
