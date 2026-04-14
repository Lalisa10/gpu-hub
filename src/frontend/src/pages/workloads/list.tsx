import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/shared/page-header';
import { DataTable } from '@/components/shared/data-table';
import { WorkloadStatusBadge } from '@/components/shared/status-badge';
import { ConfirmDialog } from '@/components/shared/confirm-dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { useAuth } from '@/contexts/auth-context';
import { useWorkloads, usePatchWorkload, useDeleteWorkload } from '@/api/hooks/use-workloads';
import { useProjects } from '@/api/hooks/use-projects';
import { useClusters } from '@/api/hooks/use-clusters';
import type { WorkloadDto, WorkloadStatus } from '@/api/types';
import type { Column } from '@/components/shared/data-table';
import { Plus, Trash2, Square, Eye, ExternalLink } from 'lucide-react';

export default function WorkloadListPage() {
  const { user, isAdmin, teamMemberships } = useAuth();
  const navigate = useNavigate();
  const { data: allWorkloads = [], isLoading } = useWorkloads();
  const { data: projects = [] } = useProjects();
  const { data: clusters = [] } = useClusters();
  const patchWorkload = usePatchWorkload();
  const deleteWorkload = useDeleteWorkload();

  const [deleteTarget, setDeleteTarget] = useState<WorkloadDto | null>(null);
  const [detailTarget, setDetailTarget] = useState<WorkloadDto | null>(null);

  // Filter workloads: admin sees all, others see their own + team workloads
  const myTeamIds = teamMemberships.map((m) => m.teamId);
  const myProjectIds = projects.filter((p) => myTeamIds.includes(p.teamId)).map((p) => p.id);
  const workloads = isAdmin
    ? allWorkloads
    : allWorkloads.filter(
        (w) => w.submittedById === user?.id || myProjectIds.includes(w.projectId),
      );

  const projectName = (id: string) => projects.find((p) => p.id === id)?.name ?? id.slice(0, 8);
  const clusterName = (id: string) => clusters.find((c) => c.id === id)?.name ?? id.slice(0, 8);
  const typeName = (value: string) => value.replace(/_/g, ' ');

  const canStop = (w: WorkloadDto) =>
    ['pending', 'queued', 'running'].includes(w.status) &&
    (isAdmin || w.submittedById === user?.id);

  const handleStop = (w: WorkloadDto) => {
    patchWorkload.mutate({ id: w.id, data: { status: 'cancelled' } });
  };

  const parseExtra = (extra: string | null): Record<string, unknown> | null => {
    if (!extra) return null;
    try {
      return JSON.parse(extra);
    } catch {
      return null;
    }
  };

  const columns: Column<WorkloadDto>[] = [
    { header: 'Name', accessor: 'name' },
    { header: 'Type', accessor: (w) => typeName(w.workloadType) },
    { header: 'Project', accessor: (w) => projectName(w.projectId) },
    { header: 'Cluster', accessor: (w) => clusterName(w.clusterId) },
    {
      header: 'Status',
      accessor: (w) => <WorkloadStatusBadge status={w.status as WorkloadStatus} />,
    },
    { header: 'GPU', accessor: (w) => String(w.requestedGpu) },
    { header: 'CPU', accessor: (w) => String(w.requestedCpu) },
    { header: 'Mem (MiB)', accessor: (w) => String(w.requestedMemory) },
    {
      header: 'Actions',
      accessor: (w) => (
        <div className="flex gap-1">
          <Button variant="ghost" size="sm" onClick={(e) => { e.stopPropagation(); setDetailTarget(w); }}>
            <Eye className="h-4 w-4" />
          </Button>
          {canStop(w) && (
            <Button variant="ghost" size="sm" onClick={(e) => { e.stopPropagation(); handleStop(w); }}>
              <Square className="h-4 w-4 text-orange-600" />
            </Button>
          )}
          <Button variant="ghost" size="sm" onClick={(e) => { e.stopPropagation(); setDeleteTarget(w); }}>
            <Trash2 className="h-4 w-4 text-destructive" />
          </Button>
        </div>
      ),
      className: 'w-32',
    },
  ];

  const detailExtra = detailTarget ? parseExtra(detailTarget.extra) : null;

  return (
    <div>
      <PageHeader
        title="My Workloads"
        description="View and manage your workloads"
        action={
          <Button onClick={() => navigate('/workloads/new')}>
            <Plus className="mr-2 h-4 w-4" /> Submit Workload
          </Button>
        }
      />

      <DataTable columns={columns} data={workloads} isLoading={isLoading} />

      {/* Detail / Info Dialog */}
      <Dialog open={!!detailTarget} onOpenChange={() => setDetailTarget(null)}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Workload: {detailTarget?.name}</DialogTitle>
          </DialogHeader>
          {detailTarget && (
            <div className="space-y-4 text-sm">
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <span className="text-muted-foreground">Status:</span>{' '}
                  <WorkloadStatusBadge status={detailTarget.status as WorkloadStatus} />
                </div>
                <div>
                  <span className="text-muted-foreground">Project:</span>{' '}
                  {projectName(detailTarget.projectId)}
                </div>
                <div>
                  <span className="text-muted-foreground">Cluster:</span>{' '}
                  {clusterName(detailTarget.clusterId)}
                </div>
                <div>
                  <span className="text-muted-foreground">GPU:</span> {detailTarget.requestedGpu}
                </div>
                <div>
                  <span className="text-muted-foreground">CPU:</span> {detailTarget.requestedCpu}
                </div>
                <div>
                  <span className="text-muted-foreground">Memory:</span>{' '}
                  {detailTarget.requestedMemory} MiB
                </div>
                <div>
                  <span className="text-muted-foreground">Type:</span>{' '}
                  {typeName(detailTarget.workloadType)}
                </div>
              </div>

              {detailTarget.k8sNamespace && (
                <div>
                  <span className="text-muted-foreground">K8s:</span>{' '}
                  {detailTarget.k8sNamespace}/{detailTarget.k8sResourceName} ({detailTarget.k8sResourceKind})
                </div>
              )}

              {detailTarget.startedAt && (
                <div>
                  <span className="text-muted-foreground">Started:</span>{' '}
                  {new Date(detailTarget.startedAt).toLocaleString()}
                </div>
              )}

              {detailTarget.finishedAt && (
                <div>
                  <span className="text-muted-foreground">Finished:</span>{' '}
                  {new Date(detailTarget.finishedAt).toLocaleString()}
                </div>
              )}

              {/* Extra info - Notebook URL or LLM curl example */}
              {detailExtra && (
                <div className="rounded-md bg-muted p-3">
                  <p className="mb-2 font-medium">Extra Info</p>
                  {detailExtra.dockerImage != null && (
                    <div className="mb-1">
                      <Badge variant="outline">Notebook</Badge>
                      {detailTarget.status === 'running' && detailExtra.jupyterUrl != null && (
                        <a
                          href={String(detailExtra.jupyterUrl)}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="ml-2 inline-flex items-center gap-1 text-blue-600 underline"
                        >
                          Open JupyterLab <ExternalLink className="h-3 w-3" />
                        </a>
                      )}
                    </div>
                  )}
                  {detailExtra.modelSource != null && (
                    <div className="mb-1">
                      <Badge variant="outline">LLM Inference</Badge>
                      <p className="mt-1">Model: {String(detailExtra.modelSource)}</p>
                      {detailTarget.status === 'running' && (
                        <pre className="mt-2 overflow-x-auto rounded bg-background p-2 text-xs">
{`curl http://<node-ip>:${String(detailExtra.nodePort)}/v1/completions \\
  -H "Content-Type: application/json" \\${detailExtra.apiKey ? `\n  -H "Authorization: Bearer ${String(detailExtra.apiKey)}" \\` : ''}
  -d '{"model": "${String(detailExtra.modelSource)}", "prompt": "Hello", "max_tokens": 128}'`}
                        </pre>
                      )}
                    </div>
                  )}
                  {!detailExtra.dockerImage && !detailExtra.modelSource && (
                    <pre className="overflow-x-auto text-xs">{JSON.stringify(detailExtra, null, 2)}</pre>
                  )}
                </div>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={() => setDeleteTarget(null)}
        title="Delete Workload"
        description={`Delete workload "${deleteTarget?.name}"? This cannot be undone.`}
        onConfirm={async () => {
          if (deleteTarget) {
            await deleteWorkload.mutateAsync(deleteTarget.id);
            setDeleteTarget(null);
          }
        }}
        loading={deleteWorkload.isPending}
      />
    </div>
  );
}
