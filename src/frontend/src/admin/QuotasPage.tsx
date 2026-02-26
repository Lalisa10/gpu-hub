import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import type { Quota } from '@/lib/types';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select } from '@/components/ui/select';
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table';

function QuotaRow({
  quota,
  usage,
  onSave,
}: {
  quota: Quota;
  usage?: { runningWorkloads: number; usedGPU: number; usedCPU: number; usedMemoryGB: number };
  onSave: (id: string, payload: Partial<Quota>) => void;
}) {
  const [maxRunningWorkloads, setMaxRunningWorkloads] = useState(String(quota.limits.maxRunningWorkloads));
  const [maxGPU, setMaxGPU] = useState(String(quota.limits.maxGPU));
  const [maxCPU, setMaxCPU] = useState(String(quota.limits.maxCPU));
  const [maxMemoryGB, setMaxMemoryGB] = useState(String(quota.limits.maxMemoryGB));
  const [allowBurst, setAllowBurst] = useState(quota.burst.allowBurst ? 'yes' : 'no');
  const [burstGPU, setBurstGPU] = useState(String(quota.burst.burstGPU ?? 0));

  const overLimit =
    usage &&
    (usage.runningWorkloads > Number(maxRunningWorkloads) ||
      usage.usedGPU > Number(maxGPU) ||
      usage.usedCPU > Number(maxCPU) ||
      usage.usedMemoryGB > Number(maxMemoryGB));

  return (
    <TR className={overLimit ? 'bg-red-50' : ''}>
      <TD>
        <div className="text-xs">
          <p className="font-medium">{quota.scope}</p>
          <p className="text-muted-foreground">{quota.scopeId}</p>
        </div>
      </TD>
      <TD>
        <Input value={maxRunningWorkloads} onChange={(e) => setMaxRunningWorkloads(e.target.value)} />
      </TD>
      <TD>
        <Input value={maxGPU} onChange={(e) => setMaxGPU(e.target.value)} />
      </TD>
      <TD>
        <Input value={maxCPU} onChange={(e) => setMaxCPU(e.target.value)} />
      </TD>
      <TD>
        <Input value={maxMemoryGB} onChange={(e) => setMaxMemoryGB(e.target.value)} />
      </TD>
      <TD>
        <Select value={allowBurst} onChange={(e) => setAllowBurst(e.target.value)}>
          <option value="yes">Allow</option>
          <option value="no">Disallow</option>
        </Select>
      </TD>
      <TD>
        <Input value={burstGPU} onChange={(e) => setBurstGPU(e.target.value)} disabled={allowBurst === 'no'} />
      </TD>
      <TD className="text-xs">
        {usage ? (
          <>
            <p>Run: {usage.runningWorkloads}</p>
            <p>GPU: {usage.usedGPU}</p>
            <p>CPU: {usage.usedCPU}</p>
            <p>MEM: {usage.usedMemoryGB} GB</p>
          </>
        ) : (
          '-'
        )}
      </TD>
      <TD>
        <Button
          size="sm"
          onClick={() =>
            onSave(quota.id, {
              limits: {
                ...quota.limits,
                maxRunningWorkloads: Number(maxRunningWorkloads),
                maxGPU: Number(maxGPU),
                maxCPU: Number(maxCPU),
                maxMemoryGB: Number(maxMemoryGB),
              },
              burst: {
                allowBurst: allowBurst === 'yes',
                burstGPU: allowBurst === 'yes' ? Number(burstGPU) : undefined,
              },
            })
          }
        >
          Save
        </Button>
      </TD>
    </TR>
  );
}

export function AdminQuotasPage() {
  const [tab, setTab] = useState<'TEAM' | 'USER'>('TEAM');
  const [scopeId, setScopeId] = useState('');
  const queryClient = useQueryClient();

  const quotasQuery = useQuery({ queryKey: ['quotas', tab], queryFn: () => api.quotas.list(tab) });
  const teamsQuery = useQuery({ queryKey: ['teams'], queryFn: api.teams.list });
  const usersQuery = useQuery({ queryKey: ['admin-users'], queryFn: api.users.list });

  const usageQueries = useQuery({
    queryKey: ['quota-usages', tab, quotasQuery.data?.map((q) => q.id).join(',')],
    enabled: !!quotasQuery.data,
    queryFn: async () => {
      const entries = await Promise.all(
        (quotasQuery.data ?? []).map(async (q) => {
          const usage = await api.quotas.usage(q.scope, q.scopeId);
          return [q.id, usage] as const;
        }),
      );
      return Object.fromEntries(entries);
    },
  });

  const createMutation = useMutation({
    mutationFn: api.quotas.create,
    onSuccess: () => {
      setScopeId('');
      queryClient.invalidateQueries({ queryKey: ['quotas', tab] });
    },
  });

  const patchMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: Partial<Quota> }) => api.quotas.patch(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quotas', tab] });
      queryClient.invalidateQueries({ queryKey: ['quota-usages'] });
    },
  });

  const scopeOptions = useMemo(
    () =>
      tab === 'TEAM'
        ? (teamsQuery.data ?? []).map((t) => ({ value: t.id, label: t.name }))
        : (usersQuery.data ?? []).map((u) => ({ value: u.id, label: `${u.username} (${u.email})` })),
    [tab, teamsQuery.data, usersQuery.data],
  );

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Quota Scopes</CardTitle>
        </CardHeader>
        <CardContent className="flex gap-2">
          <Button variant={tab === 'TEAM' ? 'default' : 'outline'} onClick={() => setTab('TEAM')}>
            Team quotas
          </Button>
          <Button variant={tab === 'USER' ? 'default' : 'outline'} onClick={() => setTab('USER')}>
            User override quotas
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Create {tab} Quota</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 gap-3 md:grid-cols-4">
          <Select value={scopeId} onChange={(e) => setScopeId(e.target.value)}>
            <option value="">Select {tab === 'TEAM' ? 'team' : 'user'}</option>
            {scopeOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </Select>
          <Button
            disabled={!scopeId}
            onClick={() =>
              createMutation.mutate({
                scope: tab,
                scopeId,
                enforced: true,
                limits: {
                  maxRunningWorkloads: 2,
                  maxGPU: 2,
                  maxCPU: 16,
                  maxMemoryGB: 64,
                  maxGPUByModel: { 'NVIDIA A100': 2 },
                },
                burst: { allowBurst: false },
              })
            }
          >
            Create Quota
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{tab} Quotas</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <THead>
              <TR>
                <TH>Scope</TH>
                <TH>Max Running</TH>
                <TH>Max GPU</TH>
                <TH>Max CPU</TH>
                <TH>Max Memory</TH>
                <TH>Burst</TH>
                <TH>Burst GPU</TH>
                <TH>Live Usage</TH>
                <TH>Action</TH>
              </TR>
            </THead>
            <TBody>
              {quotasQuery.data?.map((quota) => (
                <QuotaRow
                  key={quota.id}
                  quota={quota}
                  usage={usageQueries.data?.[quota.id]}
                  onSave={(id, payload) => patchMutation.mutate({ id, payload })}
                />
              ))}
            </TBody>
          </Table>
          <p className="mt-3 text-xs text-amber-700">
            Rows highlighted in red mean enforced limits are below current usage and may block new workloads.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
