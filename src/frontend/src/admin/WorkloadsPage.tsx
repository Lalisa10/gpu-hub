import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Select } from '@/components/ui/select';
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table';
import { StatusBadge } from '@/components/StatusBadge';
import { ConfirmAction } from '@/components/ui/confirm-action';
import { Badge } from '@/components/ui/badge';

export function AdminWorkloadsPage() {
  const [statusFilter, setStatusFilter] = useState('all');
  const [typeFilter, setTypeFilter] = useState('all');
  const [ownerFilter, setOwnerFilter] = useState('all');

  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ['workloads'], queryFn: api.workloads.list });

  const stopMutation = useMutation({
    mutationFn: api.workloads.stop,
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['workloads'] }),
  });
  const killMutation = useMutation({
    mutationFn: api.workloads.kill,
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: ['workloads'] });
      const previous = queryClient.getQueryData<Awaited<ReturnType<typeof api.workloads.list>>>(['workloads']);
      if (previous) {
        queryClient.setQueryData(
          ['workloads'],
          previous.map((w) => (w.id === id ? { ...w, status: 'Failed' as const } : w)),
        );
      }
      return { previous };
    },
    onError: (_err, _id, context) => {
      if (context?.previous) queryClient.setQueryData(['workloads'], context.previous);
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['workloads'] }),
  });

  const owners = useMemo(
    () => Array.from(new Set((query.data ?? []).map((w) => w.ownerName))),
    [query.data],
  );

  const filtered = useMemo(
    () =>
      (query.data ?? []).filter((w) => {
        if (statusFilter !== 'all' && w.status !== statusFilter) return false;
        if (typeFilter !== 'all' && w.type !== typeFilter) return false;
        if (ownerFilter !== 'all' && w.ownerName !== ownerFilter) return false;
        return true;
      }),
    [query.data, ownerFilter, statusFilter, typeFilter],
  );

  return (
    <Card>
      <CardHeader>
        <CardTitle>Admin Workloads</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
          <Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="all">All statuses</option>
            <option value="Pending">Pending</option>
            <option value="Running">Running</option>
            <option value="Succeeded">Succeeded</option>
            <option value="Failed">Failed</option>
            <option value="Stopped">Stopped</option>
            <option value="Preempted">Preempted</option>
          </Select>
          <Select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
            <option value="all">All types</option>
            <option value="infer">infer</option>
            <option value="train">train</option>
            <option value="research">research</option>
          </Select>
          <Select value={ownerFilter} onChange={(e) => setOwnerFilter(e.target.value)}>
            <option value="all">All owners</option>
            {owners.map((owner) => (
              <option key={owner} value={owner}>
                {owner}
              </option>
            ))}
          </Select>
        </div>

        <Table>
          <THead>
            <TR>
              <TH>Name</TH>
              <TH>Team</TH>
              <TH>Owner</TH>
              <TH>Type</TH>
              <TH>Status</TH>
              <TH>Quota</TH>
              <TH>Config</TH>
              <TH>Usage</TH>
              <TH>Actions</TH>
            </TR>
          </THead>
          <TBody>
            {filtered.map((w) => (
              <TR key={w.id}>
                <TD>
                  <Link className="text-cyan-700 hover:underline" to={`/app/workloads/${w.id}`}>
                    {w.name}
                  </Link>
                </TD>
                <TD>{w.teamName}</TD>
                <TD>{w.ownerName}</TD>
                <TD>{w.type}</TD>
                <TD>
                  <StatusBadge status={w.status} />
                </TD>
                <TD>
                  {w.quotaViolations?.length ? (
                    <Badge variant="danger">{w.quotaViolations.length} issue(s)</Badge>
                  ) : (
                    <Badge variant="success">OK</Badge>
                  )}
                </TD>
                <TD>
                  <pre className="max-w-[320px] overflow-x-auto rounded bg-slate-100 p-2 text-xs">
                    {JSON.stringify(w.runtime.configValues ?? {}, null, 2)}
                  </pre>
                </TD>
                <TD>
                  GPU: {w.usage.gpuUtil ?? '-'}% | MEM: {w.usage.gpuMem ?? '-'}GB
                </TD>
                <TD className="space-y-2">
                  <ConfirmAction
                    title="Stop workload"
                    description="Stop this workload gracefully."
                    triggerLabel="Stop"
                    onConfirm={() => stopMutation.mutate(w.id)}
                  />
                  <ConfirmAction
                    title="Kill workload"
                    description="Force kill. This is destructive and immediate."
                    triggerLabel="Kill"
                    triggerVariant="destructive"
                    actionLabel="Kill"
                    onConfirm={() => killMutation.mutate(w.id)}
                  />
                </TD>
              </TR>
            ))}
          </TBody>
        </Table>
      </CardContent>
    </Card>
  );
}
