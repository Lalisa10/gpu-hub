import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table';

export function ResourcesPage() {
  const gpusQuery = useQuery({ queryKey: ['gpus'], queryFn: api.gpus.list });
  const quotaQuery = useQuery({ queryKey: ['effective-quota'], queryFn: api.quotas.effectiveForMe });

  const chartData = useMemo(() => {
    const byModel = new Map<string, { model: string; total: number; allocated: number }>();
    for (const gpu of gpusQuery.data ?? []) {
      const row = byModel.get(gpu.model) ?? { model: gpu.model, total: 0, allocated: 0 };
      row.total += 1;
      if (gpu.allocated) row.allocated += 1;
      byModel.set(gpu.model, row);
    }
    return Array.from(byModel.values());
  }, [gpusQuery.data]);

  return (
    <div className="space-y-4">
      {quotaQuery.data ? (
        <Card>
          <CardHeader>
            <CardTitle>Team Quota & Usage</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3 md:grid-cols-2">
            {[
              {
                key: 'Running workloads',
                used: quotaQuery.data.usage.runningWorkloads,
                max: quotaQuery.data.effectiveLimits.maxRunningWorkloads,
                unit: '',
              },
              {
                key: 'GPU',
                used: quotaQuery.data.usage.usedGPU,
                max: quotaQuery.data.effectiveLimits.maxGPU,
                unit: '',
              },
              {
                key: 'CPU',
                used: quotaQuery.data.usage.usedCPU,
                max: quotaQuery.data.effectiveLimits.maxCPU,
                unit: '',
              },
              {
                key: 'Memory',
                used: quotaQuery.data.usage.usedMemoryGB,
                max: quotaQuery.data.effectiveLimits.maxMemoryGB,
                unit: 'GB',
              },
            ].map((item) => {
              const percent = Math.min(100, Math.round((item.used / Math.max(item.max, 1)) * 100));
              return (
                <div key={item.key} className="rounded border p-3">
                  <div className="mb-1 flex items-center justify-between text-sm">
                    <span>{item.key}</span>
                    <span>
                      {item.used} / {item.max} {item.unit}
                    </span>
                  </div>
                  <div className="h-2 overflow-hidden rounded bg-slate-200">
                    <div
                      className={`${percent >= 90 ? 'bg-red-600' : percent >= 70 ? 'bg-amber-500' : 'bg-cyan-700'} h-full`}
                      style={{ width: `${percent}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </CardContent>
        </Card>
      ) : null}

      <Card>
        <CardHeader>
          <CardTitle>GPU Availability</CardTitle>
        </CardHeader>
        <CardContent className="h-72">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="model" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="allocated" name="Allocated">
                {chartData.map((_, index) => (
                  <Cell key={index} fill="#0e7490" />
                ))}
              </Bar>
              <Bar dataKey="total" name="Total" fill="#cbd5e1" />
            </BarChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>GPU Inventory</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <THead>
              <TR>
                <TH>GPU</TH>
                <TH>Node</TH>
                <TH>Model</TH>
                <TH>Allocated</TH>
                <TH>Workload</TH>
                <TH>Utilization</TH>
              </TR>
            </THead>
            <TBody>
              {gpusQuery.data?.map((gpu) => (
                <TR key={gpu.id}>
                  <TD>{gpu.id}</TD>
                  <TD>{gpu.nodeName}</TD>
                  <TD>
                    {gpu.model} ({gpu.memoryGB}GB)
                  </TD>
                  <TD>{gpu.allocated ? 'Yes' : 'No'}</TD>
                  <TD>{gpu.workloadId ?? '-'}</TD>
                  <TD>{gpu.utilization ?? 0}%</TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
