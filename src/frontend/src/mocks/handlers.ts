import { delay, http, HttpResponse } from 'msw';
import type {
  EffectiveQuotaView,
  Quota,
  QuotaUsage,
  QuotaViolation,
  Role,
  Team,
  TeamDetailResponse,
  TeamSummary,
  User,
  Workload,
} from '@/lib/types';
import {
  environments,
  gpus,
  llmConfigs,
  models,
  quotas,
  teamMembers,
  teams,
  users,
  workloads,
} from './data';

const randomId = (prefix: string) => `${prefix}-${Math.random().toString(36).slice(2, 8)}`;
const ACTIVE_WORKLOAD_STATUSES: Workload['status'][] = ['Pending', 'Running'];

function maybeFail() {
  return Math.random() < 0.05;
}

function getTokenUser(request: Request): User | undefined {
  const auth = request.headers.get('Authorization');
  const token = auth?.replace('Bearer ', '').trim();
  if (!token) return undefined;
  const username = token.replace('-token', '');
  return users.find((u) => u.username === username);
}

function ensureRole(request: Request, role: Role) {
  const user = getTokenUser(request);
  if (!user || !user.active) {
    return { error: HttpResponse.json({ message: 'Unauthorized' }, { status: 401 }) };
  }
  if (user.role !== role) {
    return { error: HttpResponse.json({ message: 'Forbidden' }, { status: 403 }) };
  }
  return { user };
}

function getTeamByUserId(userId: string) {
  const user = users.find((u) => u.id === userId);
  if (!user) return undefined;
  return teams.find((t) => t.id === user.teamId);
}

function findQuota(scope: 'TEAM' | 'USER', scopeId: string) {
  return quotas.find((q) => q.scope === scope && q.scopeId === scopeId) ?? null;
}

function buildDefaultLimits(): Quota['limits'] {
  return {
    maxRunningWorkloads: 2,
    maxGPU: 2,
    maxCPU: 16,
    maxMemoryGB: 64,
  };
}

function buildUsageForUser(user: User): QuotaUsage {
  const active = workloads.filter(
    (w) => w.ownerId === user.id && ACTIVE_WORKLOAD_STATUSES.includes(w.status),
  );
  return {
    runningWorkloads: active.length,
    usedGPU: active.reduce((sum, w) => sum + w.resources.gpu, 0),
    usedCPU: active.reduce((sum, w) => sum + w.resources.cpu, 0),
    usedMemoryGB: active.reduce((sum, w) => sum + w.resources.memGB, 0),
  };
}

function buildUsageForTeam(teamId: string): QuotaUsage {
  const active = workloads.filter(
    (w) => w.teamId === teamId && ACTIVE_WORKLOAD_STATUSES.includes(w.status),
  );
  return {
    runningWorkloads: active.length,
    usedGPU: active.reduce((sum, w) => sum + w.resources.gpu, 0),
    usedCPU: active.reduce((sum, w) => sum + w.resources.cpu, 0),
    usedMemoryGB: active.reduce((sum, w) => sum + w.resources.memGB, 0),
  };
}

function mergeLimits(teamLimits: Quota['limits'], userLimits?: Quota['limits']): Quota['limits'] {
  if (!userLimits) return teamLimits;
  const combinedGpuByModel = { ...(teamLimits.maxGPUByModel ?? {}) };
  Object.entries(userLimits.maxGPUByModel ?? {}).forEach(([model, userModelLimit]) => {
    const teamModelLimit = combinedGpuByModel[model];
    combinedGpuByModel[model] =
      teamModelLimit === undefined ? userModelLimit : Math.min(teamModelLimit, userModelLimit);
  });

  return {
    maxRunningWorkloads: Math.min(teamLimits.maxRunningWorkloads, userLimits.maxRunningWorkloads),
    maxGPU: Math.min(teamLimits.maxGPU, userLimits.maxGPU),
    maxCPU: Math.min(teamLimits.maxCPU, userLimits.maxCPU),
    maxMemoryGB: Math.min(teamLimits.maxMemoryGB, userLimits.maxMemoryGB),
    maxGPUByModel: Object.keys(combinedGpuByModel).length > 0 ? combinedGpuByModel : undefined,
  };
}

function buildEffectiveQuota(user: User): EffectiveQuotaView {
  const team = teams.find((t) => t.id === user.teamId)!;
  const teamQuota = findQuota('TEAM', team.id);
  const userQuota = findQuota('USER', user.id);
  const teamLimits = teamQuota?.limits ?? buildDefaultLimits();
  const effectiveLimits = mergeLimits(teamLimits, userQuota?.limits);
  const teamBurst = teamQuota?.burst ?? { allowBurst: false };
  const userBurst = userQuota?.burst;

  return {
    team,
    userId: user.id,
    teamQuota,
    userQuota,
    effectiveLimits,
    effectiveBurst: userBurst ?? teamBurst,
    usage: buildUsageForUser(user),
  };
}

function validateRequestedQuota(
  user: User,
  req: { gpu: number; gpuModel?: string; cpu: number; memGB: number },
): QuotaViolation[] {
  const effective = buildEffectiveQuota(user);
  const violations: QuotaViolation[] = [];
  const runningAfter = effective.usage.runningWorkloads + 1;
  const gpuAfter = effective.usage.usedGPU + req.gpu;
  const cpuAfter = effective.usage.usedCPU + req.cpu;
  const memAfter = effective.usage.usedMemoryGB + req.memGB;

  if (runningAfter > effective.effectiveLimits.maxRunningWorkloads) {
    violations.push({
      limit: 'maxRunningWorkloads',
      scope: 'USER',
      message: `Running workload limit exceeded (${runningAfter}/${effective.effectiveLimits.maxRunningWorkloads}).`,
      suggestion: 'Wait for active workloads to finish or ask admin for a higher limit.',
    });
  }

  const maxGPU = effective.effectiveLimits.maxGPU;
  const burstGPU = effective.effectiveBurst.allowBurst ? effective.effectiveBurst.burstGPU ?? 0 : 0;
  if (gpuAfter > maxGPU + burstGPU) {
    violations.push({
      limit: 'maxGPU',
      scope: 'USER',
      message: `GPU quota exceeded (${gpuAfter}/${maxGPU}${burstGPU ? ` + burst ${burstGPU}` : ''}).`,
      suggestion: 'Reduce GPU count, wait for capacity, or contact admin for quota change.',
    });
  }

  if (req.gpuModel && effective.effectiveLimits.maxGPUByModel?.[req.gpuModel] !== undefined) {
    const activeModelGpu = workloads
      .filter(
        (w) =>
          w.ownerId === user.id &&
          ACTIVE_WORKLOAD_STATUSES.includes(w.status) &&
          w.resources.gpuModel === req.gpuModel,
      )
      .reduce((sum, w) => sum + w.resources.gpu, 0);
    const modelAfter = activeModelGpu + req.gpu;
    const modelMax = effective.effectiveLimits.maxGPUByModel[req.gpuModel]!;
    if (modelAfter > modelMax) {
      violations.push({
        limit: 'maxGPUByModel',
        scope: 'USER',
        message: `GPU model quota exceeded for ${req.gpuModel} (${modelAfter}/${modelMax}).`,
        suggestion: 'Select a different GPU model or request model-specific quota increase.',
      });
    }
  }

  if (cpuAfter > effective.effectiveLimits.maxCPU) {
    violations.push({
      limit: 'maxCPU',
      scope: 'USER',
      message: `CPU quota exceeded (${cpuAfter}/${effective.effectiveLimits.maxCPU}).`,
      suggestion: 'Lower CPU request or contact admin.',
    });
  }

  if (memAfter > effective.effectiveLimits.maxMemoryGB) {
    violations.push({
      limit: 'maxMemoryGB',
      scope: 'USER',
      message: `Memory quota exceeded (${memAfter}/${effective.effectiveLimits.maxMemoryGB} GB).`,
      suggestion: 'Lower memory request or contact admin.',
    });
  }

  return violations;
}

function summarizeTeam(team: Team): TeamSummary {
  const membersCount = teamMembers.filter((m) => m.teamId === team.id).length;
  const active = workloads.filter((w) => w.teamId === team.id && ACTIVE_WORKLOAD_STATUSES.includes(w.status));
  return {
    ...team,
    membersCount,
    activeWorkloads: active.length,
    usedGPU: active.reduce((sum, w) => sum + w.resources.gpu, 0),
  };
}

function buildTeamDetail(teamId: string): TeamDetailResponse | null {
  const team = teams.find((t) => t.id === teamId);
  if (!team) return null;

  const members = teamMembers
    .filter((m) => m.teamId === teamId)
    .map((m) => {
      const user = users.find((u) => u.id === m.userId)!;
      return {
        ...m,
        username: user.username,
        email: user.email,
        userRole: user.role,
        active: user.active,
      };
    });

  const activeWorkloads = workloads.filter(
    (w) => w.teamId === teamId && ACTIVE_WORKLOAD_STATUSES.includes(w.status),
  );

  return {
    team,
    members,
    activeWorkloads,
    quota: findQuota('TEAM', teamId),
    usage: buildUsageForTeam(teamId),
  };
}

async function withLatency() {
  await delay(300 + Math.random() * 700);
  if (maybeFail()) {
    return HttpResponse.json({ message: 'Random transient API error' }, { status: 500 });
  }
  return null;
}

export const handlers = [
  http.post('/api/auth/login', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;

    const body = (await request.json()) as { username?: string; password?: string };
    const user = users.find((u) => u.username === body.username);
    if (!user || !user.active || body.password !== 'password') {
      return HttpResponse.json({ message: 'Invalid credentials' }, { status: 401 });
    }
    return HttpResponse.json({ token: `${user.username}-token`, user });
  }),

  http.get('/api/me', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const user = getTokenUser(request);
    if (!user || !user.active) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    return HttpResponse.json(user);
  }),

  http.get('/api/workloads', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const user = getTokenUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    if (user.role === 'ADMIN') return HttpResponse.json(workloads);
    return HttpResponse.json(workloads.filter((w) => w.teamId === user.teamId));
  }),

  http.get('/api/workloads/:id', async ({ params, request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const user = getTokenUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });

    const workload = workloads.find((w) => w.id === params.id);
    if (!workload) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    if (user.role !== 'ADMIN' && workload.teamId !== user.teamId) {
      return HttpResponse.json({ message: 'Forbidden' }, { status: 403 });
    }
    return HttpResponse.json(workload);
  }),

  http.post('/api/workloads', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const user = getTokenUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });

    const team = teams.find((t) => t.id === user.teamId);
    if (!team || !team.active) {
      return HttpResponse.json({ message: 'Your team is inactive. Contact admin.' }, { status: 403 });
    }

    const payload = (await request.json()) as Partial<Workload>;
    const requested = payload.resources ?? { gpu: 1, cpu: 4, memGB: 16 };
    const violations = validateRequestedQuota(user, requested);
    if (violations.length > 0) {
      return HttpResponse.json(
        {
          message: 'Quota exceeded. Adjust request before submitting.',
          details: { violations },
        },
        { status: 409 },
      );
    }

    const created: Workload = {
      id: randomId('w'),
      name: payload.name ?? 'Untitled Workload',
      type: payload.type ?? 'infer',
      ownerId: user.id,
      ownerName: user.username,
      teamId: team.id,
      teamName: team.name,
      status: payload.type === 'research' ? 'Running' : 'Pending',
      createdAt: new Date().toISOString(),
      resources: requested,
      runtime: payload.runtime ?? {},
      usage: {},
      notebookUrl:
        payload.type === 'research'
          ? `https://notebook.gpuhub.local/lab/tree/${randomId('nb')}`
          : undefined,
    };
    workloads.unshift(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.post('/api/workloads/:id/stop', async ({ params, request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const user = getTokenUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });

    const workload = workloads.find((w) => w.id === params.id);
    if (!workload) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    if (user.role !== 'ADMIN' && workload.teamId !== user.teamId) {
      return HttpResponse.json({ message: 'Forbidden' }, { status: 403 });
    }

    workload.status = 'Stopped';
    workload.finishedAt = new Date().toISOString();
    return HttpResponse.json(workload);
  }),

  http.post('/api/workloads/:id/kill', async ({ params, request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const workload = workloads.find((w) => w.id === params.id);
    if (!workload) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    workload.status = 'Failed';
    workload.finishedAt = new Date().toISOString();

    gpus.forEach((gpu) => {
      if (gpu.workloadId === workload.id) {
        gpu.allocated = false;
        gpu.workloadId = undefined;
        gpu.owner = undefined;
        gpu.utilization = 0;
        gpu.memUsedGB = 0;
      }
    });

    return HttpResponse.json(workload);
  }),

  http.get('/api/models', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const user = getTokenUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    return HttpResponse.json(user.role === 'ADMIN' ? models : models.filter((m) => m.enabled));
  }),
  http.post('/api/models', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const payload = (await request.json()) as Partial<(typeof models)[number]>;
    const created = {
      id: randomId('m'),
      name: payload.name ?? 'New Model',
      family: payload.family ?? 'Custom',
      size: payload.size ?? '7B',
      formats: payload.formats ?? ['hf'],
      defaultConfigId: payload.defaultConfigId ?? llmConfigs[0].id,
      enabled: true,
    } as (typeof models)[number];
    models.unshift(created);
    return HttpResponse.json(created, { status: 201 });
  }),
  http.patch('/api/models/:id', async ({ params, request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const payload = (await request.json()) as Partial<(typeof models)[number]>;
    const model = models.find((m) => m.id === params.id);
    if (!model) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    Object.assign(model, payload);
    return HttpResponse.json(model);
  }),

  http.get('/api/llm-configs', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const user = getTokenUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    return HttpResponse.json(user.role === 'ADMIN' ? llmConfigs : llmConfigs.filter((c) => c.active));
  }),
  http.post('/api/llm-configs', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const payload = (await request.json()) as Partial<(typeof llmConfigs)[number]>;
    const created = {
      id: randomId('cfg'),
      name: payload.name ?? 'New Config',
      description: payload.description ?? '',
      target: 'vllm',
      parameters: payload.parameters ?? [],
      version: payload.version ?? 1,
      active: true,
    } as (typeof llmConfigs)[number];
    llmConfigs.unshift(created);
    return HttpResponse.json(created, { status: 201 });
  }),
  http.patch('/api/llm-configs/:id', async ({ params, request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const payload = (await request.json()) as Partial<(typeof llmConfigs)[number]>;
    const cfg = llmConfigs.find((c) => c.id === params.id);
    if (!cfg) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    Object.assign(cfg, payload);
    return HttpResponse.json(cfg);
  }),

  http.get('/api/environments', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const user = getTokenUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    return HttpResponse.json(user.role === 'ADMIN' ? environments : environments.filter((e) => e.enabled));
  }),
  http.post('/api/environments', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const payload = (await request.json()) as Partial<(typeof environments)[number]>;
    const created = {
      id: randomId('env'),
      name: payload.name ?? 'New Environment',
      type: payload.type ?? 'container',
      image: payload.image ?? 'ghcr.io/gpu-hub/custom:latest',
      description: payload.description ?? '',
      enabled: true,
    } as (typeof environments)[number];
    environments.unshift(created);
    return HttpResponse.json(created, { status: 201 });
  }),
  http.patch('/api/environments/:id', async ({ params, request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const payload = (await request.json()) as Partial<(typeof environments)[number]>;
    const env = environments.find((e) => e.id === params.id);
    if (!env) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    Object.assign(env, payload);
    return HttpResponse.json(env);
  }),

  http.get('/api/users', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;
    return HttpResponse.json(users);
  }),
  http.post('/api/users', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const payload = (await request.json()) as Partial<(typeof users)[number]>;
    const fallbackTeam = teams[0]?.id;
    const created = {
      id: randomId('u'),
      username: payload.username ?? 'new_user',
      email: payload.email ?? 'user@gpuhub.local',
      role: payload.role ?? 'USER',
      active: true,
      teamId: payload.teamId ?? fallbackTeam,
      createdAt: new Date().toISOString(),
    } as (typeof users)[number];
    users.unshift(created);
    if (created.teamId) {
      teamMembers.push({
        teamId: created.teamId,
        userId: created.id,
        role: 'MEMBER',
        joinedAt: new Date().toISOString(),
      });
    }
    return HttpResponse.json(created, { status: 201 });
  }),
  http.patch('/api/users/:id', async ({ params, request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const payload = (await request.json()) as Partial<(typeof users)[number]>;
    const user = users.find((u) => u.id === params.id);
    if (!user) return HttpResponse.json({ message: 'Not found' }, { status: 404 });

    const previousTeam = user.teamId;
    Object.assign(user, payload);

    if (payload.teamId && payload.teamId !== previousTeam) {
      const existingMembership = teamMembers.find((m) => m.userId === user.id);
      if (existingMembership) {
        existingMembership.teamId = payload.teamId;
      } else {
        teamMembers.push({
          teamId: payload.teamId,
          userId: user.id,
          role: 'MEMBER',
          joinedAt: new Date().toISOString(),
        });
      }
    }

    return HttpResponse.json(user);
  }),

  http.get('/api/teams', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const user = getTokenUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });

    if (user.role === 'ADMIN') {
      return HttpResponse.json(teams.map((team) => summarizeTeam(team)));
    }

    const ownTeam = teams.find((t) => t.id === user.teamId);
    return HttpResponse.json(ownTeam ? [summarizeTeam(ownTeam)] : []);
  }),

  http.get('/api/teams/:id', async ({ request, params }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const user = getTokenUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });

    const detail = buildTeamDetail(params.id as string);
    if (!detail) return HttpResponse.json({ message: 'Not found' }, { status: 404 });

    if (user.role !== 'ADMIN' && detail.team.id !== user.teamId) {
      return HttpResponse.json({ message: 'Forbidden' }, { status: 403 });
    }

    return HttpResponse.json(detail);
  }),

  http.post('/api/teams', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const payload = (await request.json()) as Partial<Team>;
    const created: Team = {
      id: randomId('t'),
      name: payload.name ?? 'New Team',
      description: payload.description ?? '',
      ownerId: payload.ownerId ?? check.user.id,
      createdAt: new Date().toISOString(),
      active: true,
    };
    teams.unshift(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.patch('/api/teams/:id', async ({ request, params }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const payload = (await request.json()) as Partial<Team>;
    const team = teams.find((t) => t.id === params.id);
    if (!team) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    Object.assign(team, payload);
    return HttpResponse.json(team);
  }),

  http.post('/api/teams/:id/members', async ({ request, params }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const teamId = params.id as string;
    const team = teams.find((t) => t.id === teamId);
    if (!team) return HttpResponse.json({ message: 'Team not found' }, { status: 404 });

    const payload = (await request.json()) as { userId: string; role: 'ADMIN' | 'MEMBER' };
    const user = users.find((u) => u.id === payload.userId);
    if (!user) return HttpResponse.json({ message: 'User not found' }, { status: 404 });

    let member = teamMembers.find((m) => m.userId === payload.userId);
    if (member) {
      member.teamId = teamId;
      member.role = payload.role;
    } else {
      member = {
        teamId,
        userId: payload.userId,
        role: payload.role,
        joinedAt: new Date().toISOString(),
      };
      teamMembers.push(member);
    }
    user.teamId = teamId;

    return HttpResponse.json(member, { status: 201 });
  }),

  http.delete('/api/teams/:id/members/:userId', async ({ request, params }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const memberIndex = teamMembers.findIndex(
      (m) => m.teamId === params.id && m.userId === params.userId,
    );
    if (memberIndex === -1) {
      return HttpResponse.json({ message: 'Membership not found' }, { status: 404 });
    }

    const fallbackTeam = teams.find((t) => t.active && t.id !== params.id) ?? teams[0];
    const user = users.find((u) => u.id === params.userId);
    if (user && fallbackTeam) {
      user.teamId = fallbackTeam.id;
      teamMembers.push({
        teamId: fallbackTeam.id,
        userId: user.id,
        role: 'MEMBER',
        joinedAt: new Date().toISOString(),
      });
    }

    teamMembers.splice(memberIndex, 1);
    return HttpResponse.json({}, { status: 204 });
  }),

  http.get('/api/quotas', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;

    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const url = new URL(request.url);
    const scope = url.searchParams.get('scope');
    if (scope === 'TEAM' || scope === 'USER') {
      return HttpResponse.json(quotas.filter((q) => q.scope === scope));
    }
    return HttpResponse.json(quotas);
  }),

  http.post('/api/quotas', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;

    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const payload = (await request.json()) as Partial<Quota>;
    const created: Quota = {
      id: randomId('q'),
      scope: payload.scope ?? 'TEAM',
      scopeId: payload.scopeId ?? '',
      limits: payload.limits ?? buildDefaultLimits(),
      burst: payload.burst ?? { allowBurst: false },
      enforced: payload.enforced ?? true,
      updatedAt: new Date().toISOString(),
    };
    quotas.unshift(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.patch('/api/quotas/:id', async ({ request, params }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;

    const check = ensureRole(request, 'ADMIN');
    if (check.error) return check.error;

    const quota = quotas.find((q) => q.id === params.id);
    if (!quota) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    const payload = (await request.json()) as Partial<Quota>;
    Object.assign(quota, payload, { updatedAt: new Date().toISOString() });
    return HttpResponse.json(quota);
  }),

  http.get('/api/quotas/effective', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;

    const user = getTokenUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    return HttpResponse.json(buildEffectiveQuota(user));
  }),

  http.get('/api/quotas/usage', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;

    const user = getTokenUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });

    const url = new URL(request.url);
    const scope = url.searchParams.get('scope');
    const scopeId = url.searchParams.get('scopeId');

    if (!scope || !scopeId) {
      return HttpResponse.json({ message: 'scope and scopeId are required' }, { status: 400 });
    }

    if (scope === 'USER') {
      if (user.role !== 'ADMIN' && user.id !== scopeId) {
        return HttpResponse.json({ message: 'Forbidden' }, { status: 403 });
      }
      const target = users.find((u) => u.id === scopeId);
      if (!target) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
      return HttpResponse.json(buildUsageForUser(target));
    }

    if (scope === 'TEAM') {
      if (user.role !== 'ADMIN' && user.teamId !== scopeId) {
        return HttpResponse.json({ message: 'Forbidden' }, { status: 403 });
      }
      return HttpResponse.json(buildUsageForTeam(scopeId));
    }

    return HttpResponse.json({ message: 'Invalid scope' }, { status: 400 });
  }),

  http.get('/api/gpus', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const user = getTokenUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    return HttpResponse.json(gpus);
  }),
];
