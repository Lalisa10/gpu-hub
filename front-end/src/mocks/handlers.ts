import { delay, http, HttpResponse } from 'msw';
import type { Role, User, Workload } from '@/lib/types';
import { environments, gpus, llmConfigs, models, users, workloads } from './data';

const randomId = (prefix: string) => `${prefix}-${Math.random().toString(36).slice(2, 8)}`;

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
    return HttpResponse.json(workloads.filter((w) => w.ownerId === user.id));
  }),

  http.get('/api/workloads/:id', async ({ params, request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const user = getTokenUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });

    const workload = workloads.find((w) => w.id === params.id);
    if (!workload) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    if (user.role !== 'ADMIN' && workload.ownerId !== user.id) {
      return HttpResponse.json({ message: 'Forbidden' }, { status: 403 });
    }
    return HttpResponse.json(workload);
  }),

  http.post('/api/workloads', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const user = getTokenUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });

    const payload = (await request.json()) as Partial<Workload>;
    const created: Workload = {
      id: randomId('w'),
      name: payload.name ?? 'Untitled Workload',
      type: payload.type ?? 'infer',
      ownerId: user.id,
      ownerName: user.username,
      status: payload.type === 'research' ? 'Running' : 'Pending',
      createdAt: new Date().toISOString(),
      resources: payload.resources ?? { gpu: 1, cpu: 4, memGB: 16 },
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
    if (user.role !== 'ADMIN' && workload.ownerId !== user.id) {
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
    const created = {
      id: randomId('u'),
      username: payload.username ?? 'new_user',
      email: payload.email ?? 'user@gpuhub.local',
      role: payload.role ?? 'USER',
      active: true,
      createdAt: new Date().toISOString(),
    } as (typeof users)[number];
    users.unshift(created);
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
    Object.assign(user, payload);
    return HttpResponse.json(user);
  }),

  http.get('/api/gpus', async ({ request }) => {
    const flaky = await withLatency();
    if (flaky) return flaky;
    const user = getTokenUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    return HttpResponse.json(gpus);
  }),
];
