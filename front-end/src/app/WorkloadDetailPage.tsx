import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { StatusBadge } from '@/components/StatusBadge';
import { Skeleton } from '@/components/ui/skeleton';

export function WorkloadDetailPage() {
  const { id = '' } = useParams();
  const query = useQuery({ queryKey: ['workload', id], queryFn: () => api.workloads.get(id), enabled: !!id });

  if (query.isLoading) {
    return <Skeleton className="h-40" />;
  }

  if (!query.data) {
    return <p className="text-sm text-muted-foreground">Workload not found.</p>;
  }

  const w = query.data;

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>{w.name}</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-2 text-sm md:grid-cols-2">
          <p>
            <strong>ID:</strong> {w.id}
          </p>
          <p>
            <strong>Type:</strong> {w.type}
          </p>
          <p>
            <strong>Status:</strong> <StatusBadge status={w.status} />
          </p>
          <p>
            <strong>Owner:</strong> {w.ownerName}
          </p>
          <p>
            <strong>Node:</strong> {w.node ?? '-'}
          </p>
          <p>
            <strong>GPU IDs:</strong> {w.gpuIds?.join(', ') ?? '-'}
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Resources</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-2 text-sm md:grid-cols-4">
          <p>
            <strong>GPU:</strong> {w.resources.gpu}
          </p>
          <p>
            <strong>GPU Model:</strong> {w.resources.gpuModel ?? '-'}
          </p>
          <p>
            <strong>CPU:</strong> {w.resources.cpu}
          </p>
          <p>
            <strong>Memory:</strong> {w.resources.memGB} GB
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Runtime & Config Values</CardTitle>
        </CardHeader>
        <CardContent>
          <pre className="overflow-x-auto rounded bg-slate-900 p-3 text-xs text-slate-100">
            {JSON.stringify(w.runtime, null, 2)}
          </pre>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Usage</CardTitle>
        </CardHeader>
        <CardContent className="text-sm">
          GPU Utilization: {w.usage.gpuUtil ?? '-'}% | GPU Memory Used: {w.usage.gpuMem ?? '-'} GB
        </CardContent>
      </Card>
    </div>
  );
}
