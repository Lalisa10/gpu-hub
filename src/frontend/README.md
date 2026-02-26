# GPU Hub Control Plane (React + Vite)

Production-style frontend for a **GPU Workload Submission & Management Platform**.

## Stack

- React 18 + TypeScript
- Vite
- TailwindCSS
- shadcn-style UI primitives
- lucide-react
- react-router-dom
- TanStack Query
- react-hook-form + zod
- Recharts
- MSW (seeded mock backend)
- Auth token in `localStorage` under key `access_token`

## Run

```bash
pnpm install
pnpm dev
```

Notes:
- `postinstall` runs `msw init public --save` to generate `public/mockServiceWorker.js`.
- Default demo credentials:
  - `admin / password`
  - `alice / password`

## Architecture Overview

```txt
src/
  app/
    AppLayout.tsx
    RouteGuards.tsx
    SubmitPage.tsx
    WorkloadsPage.tsx
    WorkloadDetailPage.tsx
    ResourcesPage.tsx
    ProfilePage.tsx
  admin/
    UsersPage.tsx
    ModelsPage.tsx
    LLMConfigsPage.tsx
    EnvironmentsPage.tsx
    WorkloadsPage.tsx
    GPUsPage.tsx
    SettingsPage.tsx
  auth/
    LoginPage.tsx
  components/
    DynamicForm/
      DynamicParametersForm.tsx
    StatusBadge.tsx
    ResourceSelector.tsx
    ui/
  lib/
    api.ts
    auth.tsx
    types.ts
    utils.ts
  mocks/
    browser.ts
    handlers.ts
    data.ts
  main.tsx
  router.tsx
```

Core decisions:
- **Control plane first** UX: submit/manage workloads and admin operations, not deep observability dashboards.
- **Role-based routing** via `ProtectedRoute` + `AdminRoute`.
- **Server state** via TanStack Query with optimistic updates for stop/kill operations.
- **Dynamic inference params** generated from schema metadata from `/api/llm-configs`.

## How Config Schema Works (Dynamic Form)

- `GET /api/llm-configs` returns `LLMConfigSchema[]`.
- Each schema has `parameters[]` with:
  - `key`, `type`, `required`, `default`, `description`, optional `enumValues`
- `DynamicParametersForm` in `src/components/DynamicForm/DynamicParametersForm.tsx`:
  - Builds a **zod object schema dynamically** from `parameters`
  - Renders inputs dynamically by `type`
  - Emits `Record<string, unknown>` values on submit
- `/app/submit` (infer flow) sends these values as `runtime.configValues` in `POST /api/workloads`.

## Add a New vLLM Parameter (No Frontend Code Change)

1. Go to `/admin/llm-configs`.
2. Update/create an LLM config schema parameter entry.
3. Provide:
   - `key` (example `max_num_seqs`)
   - `type` (`number`, `boolean`, `string`, `enum`)
   - `default` and `description`
   - `enumValues` if `type=enum`
4. Mark schema active if needed.
5. On `/app/submit`, choosing that schema auto-renders the new field.

## How Admin Updates Configs Without Frontend Changes

- Admin edits schema definitions through `/admin/llm-configs`.
- The submit UI never hardcodes parameter fields; it relies on backend schema payload.
- Any schema additions/removals/changes are reflected immediately after query refresh.

## API Mocking (MSW)

Implemented endpoints:
- `POST /api/auth/login`
- `GET /api/me`
- `POST /api/workloads`
- `GET /api/workloads`
- `GET /api/workloads/:id`
- `POST /api/workloads/:id/stop`
- `POST /api/workloads/:id/kill`
- `GET/POST/PATCH` for `models`, `llm-configs`, `environments`, `users`
- `GET /api/gpus`

Mock behavior:
- Seeded domain data in `src/mocks/data.ts`
- Role-based access control checks in handlers
- Simulated latency (`300-1000ms`)
- 5% random transient error response

