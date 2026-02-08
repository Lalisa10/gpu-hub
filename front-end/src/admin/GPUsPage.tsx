import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table';
import { ConfirmAction } from '@/components/ui/confirm-action';

export function AdminGPUsPage() {
  const queryClient = useQueryClient();
  const gpusQuery = useQuery({ queryKey: ['gpus'], queryFn: api.gpus.list });

  const killMutation = useMutation({
    mutationFn: api.workloads.kill,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['gpus'] });
      queryClient.invalidateQueries({ queryKey: ['workloads'] });
    },
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle>GPU Fleet (Monitoring-lite)</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <THead>
            <TR>
              <TH>GPU</TH>
              <TH>Node</TH>
              <TH>Model</TH>
              <TH>Allocated</TH>
              <TH>Owner</TH>
              <TH>Workload</TH>
              <TH>Utilization</TH>
              <TH>Memory Usage</TH>
              <TH>Actions</TH>
            </TR>
          </THead>
          <TBody>
            {gpusQuery.data?.map((gpu) => (
              <TR key={gpu.id}>
                <TD>{gpu.id}</TD>
                <TD>{gpu.nodeName}</TD>
                <TD>
                  {gpu.model} {gpu.memoryGB}GB
                </TD>
                <TD>{gpu.allocated ? 'Yes' : 'No'}</TD>
                <TD>{gpu.owner ?? '-'}</TD>
                <TD>{gpu.workloadId ?? '-'}</TD>
                <TD>
                  <div className="h-2 w-28 overflow-hidden rounded bg-slate-200">
                    <div
                      className="h-full bg-cyan-700"
                      style={{ width: `${Math.min(100, gpu.utilization ?? 0)}%` }}
                    />
                  </div>
                  <span className="text-xs">{gpu.utilization ?? 0}%</span>
                </TD>
                <TD>
                  {gpu.memUsedGB ?? 0} / {gpu.memoryGB} GB
                </TD>
                <TD>
                  {gpu.workloadId ? (
                    <ConfirmAction
                      title="Kill workload from GPU row"
                      description="This kills the workload currently attached to this GPU."
                      triggerLabel="Kill"
                      triggerVariant="destructive"
                      actionLabel="Kill"
                      onConfirm={() => killMutation.mutate(gpu.workloadId!)}
                    />
                  ) : (
                    '-'
                  )}
                </TD>
              </TR>
            ))}
          </TBody>
        </Table>
      </CardContent>
    </Card>
  );
}
