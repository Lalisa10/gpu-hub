import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table';
import { Skeleton } from '@/components/ui/skeleton';
import { StatusBadge } from '@/components/StatusBadge';
import { ConfirmAction } from '@/components/ui/confirm-action';

export function WorkloadsPage() {
  const queryClient = useQueryClient();
  const workloadsQuery = useQuery({ queryKey: ['workloads'], queryFn: api.workloads.list });

  const stopMutation = useMutation({
    mutationFn: api.workloads.stop,
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: ['workloads'] });
      const previous = queryClient.getQueryData<Awaited<ReturnType<typeof api.workloads.list>>>(['workloads']);
      if (previous) {
        queryClient.setQueryData(
          ['workloads'],
          previous.map((w) => (w.id === id ? { ...w, status: 'Stopped' as const } : w)),
        );
      }
      return { previous };
    },
    onError: (_err, _id, context) => {
      if (context?.previous) queryClient.setQueryData(['workloads'], context.previous);
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['workloads'] }),
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle>My Workloads</CardTitle>
      </CardHeader>
      <CardContent>
        {workloadsQuery.isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-8" />
            <Skeleton className="h-8" />
            <Skeleton className="h-8" />
          </div>
        ) : null}

        {workloadsQuery.data && workloadsQuery.data.length === 0 ? (
          <div className="rounded border border-dashed p-8 text-center text-sm text-muted-foreground">
            No workloads yet. Submit your first workload from `/app/submit`.
          </div>
        ) : null}

        {workloadsQuery.data && workloadsQuery.data.length > 0 ? (
          <Table>
            <THead>
              <TR>
                <TH>Name</TH>
                <TH>Type</TH>
                <TH>Status</TH>
                <TH>Created</TH>
                <TH>Actions</TH>
              </TR>
            </THead>
            <TBody>
              {workloadsQuery.data.map((w) => (
                <TR key={w.id}>
                  <TD>
                    <Link className="text-cyan-700 hover:underline" to={`/app/workloads/${w.id}`}>
                      {w.name}
                    </Link>
                  </TD>
                  <TD>{w.type}</TD>
                  <TD>
                    <StatusBadge status={w.status} />
                  </TD>
                  <TD>{new Date(w.createdAt).toLocaleString()}</TD>
                  <TD>
                    {(w.status === 'Running' || w.status === 'Pending') && (
                      <ConfirmAction
                        title="Stop workload"
                        description="This action will stop execution for this workload."
                        triggerLabel="Stop"
                        onConfirm={() => stopMutation.mutate(w.id)}
                      />
                    )}
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
        ) : null}
      </CardContent>
    </Card>
  );
}
