# Frontend Development Guidelines

> Best practices for frontend development in this project (React 19 + Vite + TypeScript).

---

## Overview

This directory contains guidelines for frontend development on the AI Agent Platform.
The frontend lives in `ai-agent-foward/` (NOT `app/frontend/` which is legacy).

---

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| React | 19.2 | UI framework |
| Vite | 7.1 | Build tool & dev server |
| TypeScript | ~5.9 | Type safety |
| Ant Design | 6.3 | UI component library |
| @xyflow/react | 12.10 | Workflow canvas (flow editor) |
| Zustand | 5.0 | State management |
| Tailwind CSS | 3.4 | Utility-first CSS |
| Axios | 1.11 | HTTP client |
| React Router DOM | 7.13 | Client-side routing |
| react-hook-form | (if used) | Form management |
| Vitest | 3.2 | Unit testing |
| Playwright | 1.55 | E2E testing |

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | Module organization and file layout | ✅ Filled |
| [Component Guidelines](./component-guidelines.md) | React component patterns | ✅ Filled |
| [Quality Guidelines](./quality-guidelines.md) | Code standards, testing, forbidden patterns | ✅ Filled |

---

## Pre-Development Checklist

Before starting frontend work, read these files:

1. **Always read**: `directory-structure.md` — understand module layout
2. **Building components**: `component-guidelines.md` — React patterns
3. **Before commit**: `quality-guidelines.md` — standards and testing

---

## Quick Reference

```bash
cd ai-agent-foward

npm install          # Install dependencies
npm run dev          # Start dev server (http://localhost:5173)
npm run build        # Production build
npm run test         # Run unit tests (Vitest)
npm run test:e2e     # Run E2E tests (Playwright)
npm run lint         # ESLint check
npm run typecheck    # TypeScript check (tsc --noEmit)
```

---

**Language**: All documentation should be written in **English**.
