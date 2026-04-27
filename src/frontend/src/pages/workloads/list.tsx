import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/shared/page-header';
import { DataTable } from '@/components/shared/data-table';
import { WorkloadStatusBadge } from '@/components/shared/status-badge';
import { ConfirmDialog } from '@/components/shared/confirm-dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Sheet,
  SheetContent,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { useAuth } from '@/contexts/auth-context';
import {
  useWorkloads,
  useCancelWorkload,
  useDeleteWorkload,
  useWorkloadStatusStream,
  useWorkloadPodsStream,
  useWorkloadPodLogsStream,
} from '@/api/hooks/use-workloads';
import { useProjects } from '@/api/hooks/use-projects';
import { useClusters } from '@/api/hooks/use-clusters';
import type { PodDto, WorkloadDto, WorkloadStatus } from '@/api/types';
import type { Column } from '@/components/shared/data-table';
import {
  Plus,
  Trash2,
  Square,
  Eye,
  ExternalLink,
  Loader2,
  Terminal,
  FileText,
  Server,
} from 'lucide-react';

function podStatusVariant(status: string | null): 'default' | 'secondary' | 'outline' | 'destructive' {
  if (!status) return 'outline';
  if (status === 'Running') return 'default';
  if (['Failed', 'Error', 'CrashLoopBackOff', 'ImagePullBackOff', 'ErrImagePull'].includes(status))
    return 'destructive';
  return 'secondary';
}

function podIsTransient(status: string | null): boolean {
  if (!status) return true;
  return ['Pending', 'ContainerCreating', 'PodInitializing', 'Terminating'].includes(status);
}

export default function WorkloadListPage() {
  const { user, isAdmin, teamMemberships } = useAuth();
  const navigate = useNavigate();
  const { data: allWorkloads = [], isLoading } = useWorkloads();
  const { data: projects = [] } = useProjects();
  const { data: clusters = [] } = useClusters();
  const cancelWorkload = useCancelWorkload();
  const deleteWorkload = useDeleteWorkload();

  const [deleteTarget, setDeleteTarget] = useState<WorkloadDto | null>(null);
  const [detailTarget, setDetailTarget] = useState<WorkloadDto | null>(null);
  const [logsTarget, setLogsTarget] = useState<{ workloadId: string; podName: string } | null>(null);

  const {
    data: streamedStatus,
  } = useWorkloadStatusStream(detailTarget?.id ?? null, !!detailTarget);

  const activeWorkload = streamedStatus ?? detailTarget;

  const {
    data: streamedPods,
    isLoading: podsLoading,
    isError: podsError,
  } = useWorkloadPodsStream(detailTarget?.id ?? null, !!detailTarget);
  const pods = streamedPods ?? [];

  const myTeamIds = teamMemberships.map((m) => m.teamId);
  const myProjectIds = projects.filter((p) => myTeamIds.includes(p.teamId)).map((p) => p.id);
  const workloads = isAdmin
    ? allWorkloads
    : allWorkloads.filter(
        (w) => w.submittedById === user?.id || myProjectIds.includes(w.projectId),
      );

  const projectName = (id: string) => projects.find((p) => p.id === id)?.name ?? '—';
  const clusterName = (id: string) => clusters.find((c) => c.id === id)?.name ?? '—';
  const typeName = (value: string) => value.replace(/_/g, ' ');

  const canStop = (w: WorkloadDto) =>
    ['pending', 'running'].includes(w.status) && (isAdmin || w.submittedById === user?.id);

  const handleStop = (w: WorkloadDto) => {
    cancelWorkload.mutate(w.id);
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

  const detailExtra = activeWorkload ? parseExtra(activeWorkload.extra) : null;
  const activePodCount = pods.filter((p) => p.phase === 'Running' || podIsTransient(p.status)).length;

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

      {/* Slide-over detail panel */}
      <Sheet open={!!detailTarget} onOpenChange={(o) => !o && setDetailTarget(null)}>
        <SheetContent
          side="right"
          className="flex flex-col gap-0 p-0 data-[side=right]:w-full data-[side=right]:sm:max-w-none data-[side=right]:md:w-2/5 data-[side=right]:md:min-w-[540px]"
        >
          <SheetHeader className="border-b px-6 py-5">
            <SheetTitle className="text-lg">
              Details for Workload:{' '}
              <span className="text-muted-foreground">{activeWorkload?.name}</span>
            </SheetTitle>
            {activeWorkload && (
              <div className="mt-2 flex flex-wrap items-center gap-2 text-xs">
                <WorkloadStatusBadge status={activeWorkload.status as WorkloadStatus} />
                <Badge variant="outline">{typeName(activeWorkload.workloadType)}</Badge>
                <span className="text-muted-foreground">
                  {projectName(activeWorkload.projectId)} / {clusterName(activeWorkload.clusterId)}
                </span>
              </div>
            )}
          </SheetHeader>

          {activeWorkload && (
            <div className="flex-1 space-y-6 overflow-y-auto px-6 py-5 text-sm">
              {/* Summary grid */}
              <section>
                <div className="grid grid-cols-3 gap-3">
                  <SummaryStat label="GPU" value={String(activeWorkload.requestedGpu)} />
                  <SummaryStat label="CPU" value={String(activeWorkload.requestedCpu)} />
                  <SummaryStat label="Memory" value={`${activeWorkload.requestedMemory} MiB`} />
                </div>
                {(activeWorkload.startedAt || activeWorkload.finishedAt) && (
                  <div className="mt-3 grid grid-cols-2 gap-3 text-xs text-muted-foreground">
                    {activeWorkload.startedAt && (
                      <div>
                        <div className="uppercase tracking-wide text-[10px]">Started</div>
                        <div className="text-foreground">
                          {new Date(activeWorkload.startedAt).toLocaleString()}
                        </div>
                      </div>
                    )}
                    {activeWorkload.finishedAt && (
                      <div>
                        <div className="uppercase tracking-wide text-[10px]">Finished</div>
                        <div className="text-foreground">
                          {new Date(activeWorkload.finishedAt).toLocaleString()}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </section>

              {/* Pods */}
              <section>
                <div className="mb-3 flex items-center justify-between">
                  <h3 className="font-heading text-sm font-medium">Associated Pods</h3>
                  <Badge variant="outline" className="text-xs">
                    {podsLoading ? '...' : `Pods (${activePodCount} Active)`}
                  </Badge>
                </div>

                {podsLoading ? (
                  <div className="flex items-center gap-2 rounded-md border border-dashed px-4 py-6 text-xs text-muted-foreground">
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Loading pods...
                  </div>
                ) : podsError ? (
                  <div className="rounded-md border border-dashed px-4 py-6 text-xs text-destructive">
                    Failed to load pods.
                  </div>
                ) : pods.length === 0 ? (
                  <div className="rounded-md border border-dashed px-4 py-6 text-center text-xs text-muted-foreground">
                    No pods yet. The workload may still be scheduling.
                  </div>
                ) : (
                  <ul className="space-y-2">
                    {pods.map((pod) => (
                      <PodCard
                        key={pod.name}
                        pod={pod}
                        onViewLogs={() =>
                          setLogsTarget({ workloadId: activeWorkload.id, podName: pod.name })
                        }
                      />
                    ))}
                  </ul>
                )}
              </section>

              {/* Extra info */}
              {detailExtra && (
                <section>
                  <h3 className="mb-2 font-heading text-sm font-medium">Extra Info</h3>
                  <div className="rounded-md bg-muted p-3">
                    {detailExtra.dockerImage != null && (
                      <div className="mb-1">
                        <Badge variant="outline">Notebook</Badge>
                        {activeWorkload.status === 'running' && detailExtra.jupyterUrl != null && (
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
                      </div>
                    )}
                    {!detailExtra.dockerImage && !detailExtra.modelSource && (
                      <pre className="overflow-x-auto text-xs">
                        {JSON.stringify(detailExtra, null, 2)}
                      </pre>
                    )}
                  </div>
                </section>
              )}
            </div>
          )}

          {activeWorkload && (
            <SheetFooter className="border-t px-6 py-4">
              <div className="flex justify-end gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled
                  title="Terminal access not yet available"
                >
                  <Terminal className="mr-1.5 h-4 w-4" /> Launch Terminal
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={pods.length === 0}
                  onClick={() => {
                    if (pods[0]) {
                      setLogsTarget({ workloadId: activeWorkload.id, podName: pods[0].name });
                    }
                  }}
                >
                  <FileText className="mr-1.5 h-4 w-4" /> View Logs
                </Button>
              </div>
            </SheetFooter>
          )}
        </SheetContent>
      </Sheet>

      {/* Logs dialog */}
      <LogsDialog
        target={logsTarget}
        onClose={() => setLogsTarget(null)}
      />

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

function SummaryStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border bg-background px-3 py-2">
      <div className="text-[10px] uppercase tracking-wide text-muted-foreground">{label}</div>
      <div className="mt-0.5 font-mono text-sm">{value}</div>
    </div>
  );
}

function PodCard({ pod, onViewLogs }: { pod: PodDto; onViewLogs: () => void }) {
  const transient = podIsTransient(pod.status);
  return (
    <li className="group rounded-md border bg-background px-3 py-2.5">
      <div className="flex items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-2">
          <Server className="h-4 w-4 shrink-0 text-muted-foreground" />
          <div className="min-w-0">
            <div className="truncate font-mono text-xs font-medium">{pod.name}</div>
            <div className="text-[11px] text-muted-foreground">
              IP: {pod.ip ?? '—'}
              {pod.restartCount > 0 && (
                <span className="ml-2">· restarts: {pod.restartCount}</span>
              )}
            </div>
          </div>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <Badge variant={podStatusVariant(pod.status)} className="gap-1 text-[10px]">
            {transient && <Loader2 className="h-3 w-3 animate-spin" />}
            {pod.status ?? 'Unknown'}
          </Badge>
          <Button
            variant="ghost"
            size="sm"
            className="h-7 px-2 opacity-0 transition group-hover:opacity-100"
            onClick={onViewLogs}
          >
            <FileText className="h-3.5 w-3.5" />
          </Button>
        </div>
      </div>
    </li>
  );
}

function LogsDialog({
  target,
  onClose,
}: {
  target: { workloadId: string; podName: string } | null;
  onClose: () => void;
}) {
  const { data, isLoading, isError } = useWorkloadPodLogsStream(
    target?.workloadId ?? null,
    target?.podName ?? null,
    !!target,
  );

  return (
    <Dialog open={!!target} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="w-[90vw] sm:max-w-[96rem]">
        <DialogHeader>
          <DialogTitle className="font-mono text-sm">
            Logs · {target?.podName}
          </DialogTitle>
        </DialogHeader>
        {isLoading && (
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" /> Fetching logs...
          </div>
        )}
        {isError && (
          <div className="text-xs text-destructive">Failed to fetch logs.</div>
        )}
        {!isLoading && !isError && (
          <pre className="max-h-[90vh] overflow-auto rounded-md bg-muted p-3 text-xs">
            {data && data.length > 0 ? data : '(no output)'}
          </pre>
        )}
      </DialogContent>
    </Dialog>
  );
}
