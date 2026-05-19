import { useRef, useState } from 'react';
import { useQueries } from '@tanstack/react-query';
import { PageHeader } from '@/components/shared/page-header';
import { DataTable } from '@/components/shared/data-table';
import { ClusterStatusBadge } from '@/components/shared/status-badge';
import { ConfirmDialog } from '@/components/shared/confirm-dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import {
  useClusters,
  useCreateCluster,
  useDeleteCluster,
  useUploadKubeconfig,
  useClusterDetailsStream,
} from '@/api/hooks/use-clusters';
import { clusterService } from '@/api/services/clusters';
import type {
  ClusterDetailsDto,
  ClusterDto,
  ClusterStatus,
  NodeInfoDto,
  NodeStatus,
} from '@/api/types';
import type { Column } from '@/components/shared/data-table';
import { Loader2, Plus, Trash2, Upload } from 'lucide-react';
import { toast } from 'sonner';

// ─── Sub-components ────────────────────────────────────

const NODE_STATUS_CLASS: Record<NodeStatus, string> = {
  Ready: 'bg-green-100 text-green-700',
  NotReady: 'bg-red-100 text-red-700',
  Pressure: 'bg-amber-100 text-amber-700',
};

function formatBytes(bytes: number): string {
  if (bytes <= 0) return '0 B';
  const units = ['B', 'KiB', 'MiB', 'GiB', 'TiB'];
  let v = bytes;
  let i = 0;
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024;
    i++;
  }
  return `${v.toFixed(v >= 100 ? 0 : 1)} ${units[i]}`;
}

function formatCpu(millis: number): string {
  const cores = millis / 1000;
  return `${cores.toFixed(cores >= 10 ? 0 : 1)} cores`;
}

function NodeCard({ node, expanded, onToggle }: {
  node: NodeInfoDto;
  expanded: boolean;
  onToggle: () => void;
}) {
  const gpuPct = node.gpuTotal > 0 ? (node.gpuAllocated / node.gpuTotal) * 100 : 0;

  return (
    <div className="overflow-hidden rounded-lg border bg-card">
      <button
        type="button"
        onClick={onToggle}
        className="flex w-full items-center gap-6 px-5 py-4 text-left transition-colors hover:bg-muted/40"
      >
        {/* Name + status */}
        <div className="flex min-w-[12rem] shrink-0 items-center gap-2.5">
          <Badge className={NODE_STATUS_CLASS[node.status]}>{node.status}</Badge>
          <span className="truncate text-sm font-medium">{node.name}</span>
        </div>

        {/* GPU utilization bar (grows) */}
        <div className="min-w-0 flex-1">
          <div className="mb-1 flex justify-between text-[11px] text-muted-foreground">
            <span>GPU Allocated</span>
            <span>
              {node.gpuAllocated} / {node.gpuTotal} GPU{node.gpuTotal !== 1 ? 's' : ''}
            </span>
          </div>
          <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
            <div
              className="h-full rounded-full bg-foreground"
              style={{ width: `${Math.min(gpuPct, 100)}%` }}
            />
          </div>
        </div>

        {/* GPU type */}
        <div className="min-w-[14rem] shrink-0 text-right">
          <p className="text-[10px] uppercase tracking-wide text-muted-foreground">GPU Type</p>
          <p className="truncate text-xs">
            {node.gpuModel ?? (node.gpuTotal > 0 ? 'Unknown' : 'No GPU')}
          </p>
        </div>
      </button>
      {expanded && <NodeResourceTable node={node} />}
    </div>
  );
}

function NodeResourceTable({ node }: { node: NodeInfoDto }) {
  return (
    <div className="border-t bg-muted/30 px-4 py-3">
      <table className="w-full text-xs">
        <thead>
          <tr className="border-b text-muted-foreground">
            <th className="pb-2 text-left font-medium">Resource</th>
            <th className="pb-2 text-right font-medium">Request</th>
            <th className="pb-2 text-right font-medium">Limit</th>
            <th className="pb-2 text-right font-medium">Capacity</th>
          </tr>
        </thead>
        <tbody>
          <tr className="border-b last:border-0">
            <td className="py-2 font-medium">CPU</td>
            <td className="py-2 text-right">{formatCpu(node.cpuRequestMillis)}</td>
            <td className="py-2 text-right">{formatCpu(node.cpuLimitMillis)}</td>
            <td className="py-2 text-right">{formatCpu(node.cpuCapacityMillis)}</td>
          </tr>
          <tr className="border-b last:border-0">
            <td className="py-2 font-medium">RAM</td>
            <td className="py-2 text-right">{formatBytes(node.ramRequestBytes)}</td>
            <td className="py-2 text-right">{formatBytes(node.ramLimitBytes)}</td>
            <td className="py-2 text-right">{formatBytes(node.ramCapacityBytes)}</td>
          </tr>
          <tr>
            <td className="py-2 font-medium">GPU</td>
            <td className="py-2 text-right">{node.gpuAllocated} Units</td>
            <td className="py-2 text-right">{node.gpuAllocated} Units</td>
            <td className="py-2 text-right">{node.gpuTotal} Units</td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}

function formatAge(startedAt: string | null): string {
  if (!startedAt) return '—';
  const mins = Math.floor((Date.now() - new Date(startedAt).getTime()) / 60_000);
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

function ClusterDetailBody({ details }: { details: ClusterDetailsDto }) {
  const [expandedNode, setExpandedNode] = useState<string | null>(null);

  return (
    <div className="space-y-8 px-8 py-6">
      {/* Section 1: Nodes */}
      <section>
        <div className="mb-3 flex items-baseline justify-between">
          <h3 className="text-sm font-semibold">Nodes</h3>
          <span className="text-xs text-muted-foreground">
            {details.gpusInUse} / {details.gpusTotal} GPUs in use
          </span>
        </div>
        {details.nodes.length === 0 ? (
          <p className="text-xs text-muted-foreground">No nodes found.</p>
        ) : (
          <div className="space-y-3">
            {details.nodes.map((n) => (
              <NodeCard
                key={n.name}
                node={n}
                expanded={expandedNode === n.name}
                onToggle={() => setExpandedNode(expandedNode === n.name ? null : n.name)}
              />
            ))}
          </div>
        )}
      </section>

      {/* Section 2: Active Workloads */}
      <section>
        <h3 className="mb-3 text-sm font-semibold">Active Workloads</h3>
        {details.activeWorkloads.length === 0 ? (
          <p className="text-xs text-muted-foreground">No active workloads.</p>
        ) : (
          <table className="w-full text-xs">
            <thead>
              <tr className="border-b text-muted-foreground">
                <th className="pb-2 text-left font-medium">Name</th>
                <th className="pb-2 text-left font-medium">Type</th>
                <th className="pb-2 text-left font-medium">GPUs</th>
                <th className="pb-2 text-left font-medium">Status</th>
                <th className="pb-2 text-left font-medium">Age</th>
              </tr>
            </thead>
            <tbody>
              {details.activeWorkloads.map((w) => (
                <tr key={w.id} className="border-b last:border-0">
                  <td className="py-2 font-medium">{w.name}</td>
                  <td className="py-2 capitalize">{w.workloadType.replace('_', ' ')}</td>
                  <td className="py-2">{w.requestedGpu ?? '—'}</td>
                  <td className="py-2 capitalize">{w.status}</td>
                  <td className="py-2 text-muted-foreground">{formatAge(w.startedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}

// ─── Page ──────────────────────────────────────────────

export default function ClustersPage() {
  const { data: clusters = [], isLoading } = useClusters();
  const createCluster = useCreateCluster();
  const deleteCluster = useDeleteCluster();
  const uploadKubeconfig = useUploadKubeconfig();

  const [showCreate, setShowCreate] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<ClusterDto | null>(null);
  const [uploadTarget, setUploadTarget] = useState<ClusterDto | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [createFile, setCreateFile] = useState<File | null>(null);
  const [form, setForm] = useState({
    name: '',
    description: '',
  });
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [drawerCluster, setDrawerCluster] = useState<ClusterDto | null>(null);
  const { data: clusterDetails, isLoading: detailsLoading, isError: detailsError } =
    useClusterDetailsStream(drawerCluster?.id ?? null, !!drawerCluster);

  // Background fetch for GPU Summary column.
  // retry:false — a cluster with a broken kubeconfig should fail fast, not
  // retry and double the wait.
  const gpuQueries = useQueries({
    queries: clusters.map((c) => ({
      queryKey: ['clusters', c.id, 'details'],
      queryFn: () => clusterService.getDetails(c.id),
      staleTime: 60_000,
      retry: false,
    })),
  });
  const gpuSummaryMap = Object.fromEntries(
    clusters.map((c, i) => {
      const d = gpuQueries[i]?.data;
      return [c.id, d ? `${d.gpusInUse} / ${d.gpusTotal} GPUs in use` : '—'];
    }),
  );

  const columns: Column<ClusterDto>[] = [
    {
      header: 'Name',
      accessor: (c) => (
        <button
          className="text-left font-medium underline-offset-2 hover:underline"
          onClick={() => setDrawerCluster(c)}
        >
          {c.name}
        </button>
      ),
    },
    {
      header: 'Status',
      accessor: (c) => <ClusterStatusBadge status={c.status as ClusterStatus} />,
    },
    {
      header: 'GPU Summary',
      accessor: (c) => (
        <span className="text-sm text-muted-foreground">{gpuSummaryMap[c.id]}</span>
      ),
    },
    { header: 'Description', accessor: (c) => c.description ?? '-' },
    {
      header: '',
      accessor: (c) => (
        <div className="flex gap-1">
          <Button
            variant="ghost"
            size="sm"
            title="Replace kubeconfig"
            onClick={(e) => {
              e.stopPropagation();
              setUploadTarget(c);
              setSelectedFile(null);
            }}
          >
            <Upload className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              setDeleteTarget(c);
            }}
          >
            <Trash2 className="h-4 w-4 text-destructive" />
          </Button>
        </div>
      ),
      className: 'w-20',
    },
  ];

  const handleCreate = async () => {
    if (!form.name || !createFile) return;
    try {
      await createCluster.mutateAsync({
        meta: { name: form.name, description: form.description || undefined },
        file: createFile,
      });
      toast.success(`Cluster "${form.name}" created`);
      setShowCreate(false);
      setForm({ name: '', description: '' });
      setCreateFile(null);
    } catch {
      // Validation/connection error — keep the dialog open so the user can retry.
      // The global mutation onError handler already shows the message toast.
    }
  };

  const handleUpload = async () => {
    if (!uploadTarget || !selectedFile) return;
    try {
      await uploadKubeconfig.mutateAsync({ id: uploadTarget.id, file: selectedFile });
      toast.success(`Kubeconfig replaced for "${uploadTarget.name}"`);
      setUploadTarget(null);
      setSelectedFile(null);
    } catch {
      // Validation/connection error — keep the dialog open for retry.
      // The global mutation onError handler already shows the message toast.
    }
  };

  return (
    <div>
      <PageHeader
        title="Clusters"
        description="Manage Kubernetes clusters"
        action={
          <Button onClick={() => setShowCreate(true)}>
            <Plus className="mr-2 h-4 w-4" /> Add Cluster
          </Button>
        }
      />

      <DataTable columns={columns} data={clusters} isLoading={isLoading} />

      {/* Create Cluster Dialog */}
      <Dialog open={showCreate} onOpenChange={setShowCreate}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Register Cluster</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Name</Label>
              <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label>Kubeconfig file *</Label>
              <Input
                type="file"
                accept=".yaml,.yml,.conf,.kubeconfig"
                onChange={(e) => setCreateFile(e.target.files?.[0] ?? null)}
              />
              {createFile ? (
                <p className="text-xs text-muted-foreground">
                  {createFile.name} ({(createFile.size / 1024).toFixed(1)} KB)
                </p>
              ) : (
                <p className="text-xs text-muted-foreground">
                  Required — the cluster connection is verified before it is created.
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label>Description</Label>
              <Textarea
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowCreate(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleCreate}
              disabled={createCluster.isPending || !form.name || !createFile}
            >
              {createCluster.isPending ? 'Verifying & creating...' : 'Create'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Upload Kubeconfig Dialog */}
      <Dialog open={!!uploadTarget} onOpenChange={() => setUploadTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Replace Kubeconfig — {uploadTarget?.name}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <p className="text-sm text-muted-foreground">
              The new kubeconfig is verified against the cluster before it replaces the
              current one — an invalid or unreachable config is rejected and the existing
              kubeconfig is kept.
            </p>
            <div className="space-y-2">
              <Label>Kubeconfig file</Label>
              <Input
                ref={fileInputRef}
                type="file"
                accept=".yaml,.yml,.conf,.kubeconfig"
                onChange={(e) => setSelectedFile(e.target.files?.[0] ?? null)}
              />
              {selectedFile && (
                <p className="text-xs text-muted-foreground">
                  {selectedFile.name} ({(selectedFile.size / 1024).toFixed(1)} KB)
                </p>
              )}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setUploadTarget(null)}>
              Cancel
            </Button>
            <Button onClick={handleUpload} disabled={!selectedFile || uploadKubeconfig.isPending}>
              {uploadKubeconfig.isPending ? 'Verifying & replacing...' : 'Replace'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={() => setDeleteTarget(null)}
        title="Delete Cluster"
        description={`Are you sure you want to delete cluster "${deleteTarget?.name}"? This will remove all associated resources.`}
        onConfirm={async () => {
          if (deleteTarget) {
            await deleteCluster.mutateAsync(deleteTarget.id);
            setDeleteTarget(null);
          }
        }}
        loading={deleteCluster.isPending}
      />

      {/* Cluster Detail Drawer */}
      <Sheet open={!!drawerCluster} onOpenChange={(o) => !o && setDrawerCluster(null)}>
        <SheetContent
          side="right"
          className="flex flex-col gap-0 p-0 data-[side=right]:w-[80vw] data-[side=right]:sm:max-w-none data-[side=right]:lg:max-w-[1800px]"
        >
          <SheetHeader className="border-b px-6 py-5">
            <div className="flex items-center gap-3">
              <SheetTitle className="text-lg">{drawerCluster?.name}</SheetTitle>
              {drawerCluster && (
                <ClusterStatusBadge status={drawerCluster.status as ClusterStatus} />
              )}
            </div>
          </SheetHeader>
          <div className="flex-1 overflow-y-auto">
            {detailsLoading && !clusterDetails && (
              <div className="flex items-center justify-center gap-2 py-16 text-xs text-muted-foreground">
                <Loader2 className="h-4 w-4 animate-spin" /> Loading cluster details...
              </div>
            )}
            {detailsError && (
              <p className="py-16 text-center text-xs text-destructive">
                Failed to load cluster details.
              </p>
            )}
            {clusterDetails && <ClusterDetailBody details={clusterDetails} />}
          </div>
        </SheetContent>
      </Sheet>
    </div>
  );
}
