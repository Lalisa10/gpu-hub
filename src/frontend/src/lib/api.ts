import type {
  EffectiveQuotaView,
  Environment,
  GPU,
  LLMConfigSchema,
  LoginRequest,
  LoginResponse,
  Model,
  Quota,
  Team,
  TeamDetailResponse,
  TeamMember,
  TeamSummary,
  User,
  Workload,
} from './types';

const API_BASE = '/api';

export class APIError extends Error {
  status: number;
  details?: unknown;

  constructor(message: string, status: number, details?: unknown) {
    super(message);
    this.status = status;
    this.details = details;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = localStorage.getItem('access_token');
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
  });

  if (!response.ok) {
    const body = (await response.json().catch(() => ({ message: 'Request failed' }))) as {
      message?: string;
      details?: unknown;
    };
    throw new APIError(body.message ?? 'Request failed', response.status, body.details);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export const api = {
  login: (payload: LoginRequest) =>
    request<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  me: () => request<User>('/me'),

  workloads: {
    list: () => request<Workload[]>('/workloads'),
    get: (id: string) => request<Workload>(`/workloads/${id}`),
    create: (payload: Partial<Workload> & { name: string; type: Workload['type'] }) =>
      request<Workload>('/workloads', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    stop: (id: string) => request<Workload>(`/workloads/${id}/stop`, { method: 'POST' }),
    kill: (id: string) => request<Workload>(`/workloads/${id}/kill`, { method: 'POST' }),
  },

  models: {
    list: () => request<Model[]>('/models'),
    create: (payload: Partial<Model>) =>
      request<Model>('/models', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    patch: (id: string, payload: Partial<Model>) =>
      request<Model>(`/models/${id}`, {
        method: 'PATCH',
        body: JSON.stringify(payload),
      }),
  },

  llmConfigs: {
    list: () => request<LLMConfigSchema[]>('/llm-configs'),
    create: (payload: Partial<LLMConfigSchema>) =>
      request<LLMConfigSchema>('/llm-configs', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    patch: (id: string, payload: Partial<LLMConfigSchema>) =>
      request<LLMConfigSchema>(`/llm-configs/${id}`, {
        method: 'PATCH',
        body: JSON.stringify(payload),
      }),
  },

  environments: {
    list: () => request<Environment[]>('/environments'),
    create: (payload: Partial<Environment>) =>
      request<Environment>('/environments', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    patch: (id: string, payload: Partial<Environment>) =>
      request<Environment>(`/environments/${id}`, {
        method: 'PATCH',
        body: JSON.stringify(payload),
      }),
  },

  users: {
    list: () => request<User[]>('/users'),
    create: (payload: Partial<User>) =>
      request<User>('/users', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    patch: (id: string, payload: Partial<User>) =>
      request<User>(`/users/${id}`, {
        method: 'PATCH',
        body: JSON.stringify(payload),
      }),
  },

  teams: {
    list: () => request<TeamSummary[]>('/teams'),
    get: (id: string) => request<TeamDetailResponse>(`/teams/${id}`),
    create: (payload: Partial<Team>) =>
      request<Team>('/teams', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    patch: (id: string, payload: Partial<Team>) =>
      request<Team>(`/teams/${id}`, {
        method: 'PATCH',
        body: JSON.stringify(payload),
      }),
    addMember: (teamId: string, payload: Pick<TeamMember, 'userId' | 'role'>) =>
      request<TeamMember>(`/teams/${teamId}/members`, {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    removeMember: (teamId: string, userId: string) =>
      request<void>(`/teams/${teamId}/members/${userId}`, {
        method: 'DELETE',
      }),
  },

  quotas: {
    list: (scope?: 'TEAM' | 'USER') => request<Quota[]>(`/quotas${scope ? `?scope=${scope}` : ''}`),
    create: (payload: Partial<Quota>) =>
      request<Quota>('/quotas', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    patch: (id: string, payload: Partial<Quota>) =>
      request<Quota>(`/quotas/${id}`, {
        method: 'PATCH',
        body: JSON.stringify(payload),
      }),
    effectiveForMe: () => request<EffectiveQuotaView>('/quotas/effective'),
    usage: (scope: 'TEAM' | 'USER', scopeId: string) =>
      request<EffectiveQuotaView['usage']>(`/quotas/usage?scope=${scope}&scopeId=${scopeId}`),
  },

  gpus: {
    list: () => request<GPU[]>('/gpus'),
  },
};
