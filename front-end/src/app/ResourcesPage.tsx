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
