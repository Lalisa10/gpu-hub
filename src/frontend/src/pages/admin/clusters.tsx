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
import type { ClusterDetailsDto, ClusterDto, ClusterStatus, GpuInfoDto, NodeInfoDto } from '@/api/types';
import type { Column } from '@/components/shared/data-table';
import { Loader2, Plus, Trash2, Upload } from 'lucide-react';
import { toast } from 'sonner';

// ─── Sub-components ────────────────────────────────────

function ProgressRow({ label, pct, detail }: { label: string; pct: number; detail?: string }) {
  return (
    <div>
      <div className="mb-0.5 flex justify-between text-xs text-muted-foreground">
        <span>{label}</span>
        <span>{detail ?? `${pct.toFixed(1)}%`}</span>
      </div>
      <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
        <div
          className="h-full rounded-full bg-foreground"
          style={{ width: `${Math.min(pct, 100)}%` }}
        />
      </div>
    </div>
  );
}

function NodeCard({ node }: { node: NodeInfoDto }) {
  const cpuUsedPct =
    node.cpuCapacityMillis > 0
      ? ((node.cpuCapacityMillis - node.cpuAllocatableMillis) / node.cpuCapacityMillis) * 100
      : 0;
  const ramUsedPct =
    node.ramCapacityBytes > 0
      ? ((node.ramCapacityBytes - node.ramAllocatableBytes) / node.ramCapacityBytes) * 100
      : 0;
  const ramCapGiB = (node.ramCapacityBytes / 1024 ** 3).toFixed(0);
  const ramUsedGiB = ((node.ramCapacityBytes - node.ramAllocatableBytes) / 1024 ** 3).toFixed(1);
  const cpuCores = (node.cpuCapacityMillis / 1000).toFixed(0);
  const cpuUsedCores = ((node.cpuCapacityMillis - node.cpuAllocatableMillis) / 1000).toFixed(1);

  return (
    <div className="min-w-[200px] space-y-2 rounded-lg border p-3">
      <div className="flex items-center justify-between gap-2">
        <span className="truncate text-xs font-medium">{node.name}</span>
        <Badge className={node.ready ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}>
          {node.ready ? 'Ready' : 'NotReady'}
        </Badge>
      </div>
      <ProgressRow label="CPU Allocated" pct={cpuUsedPct} detail={`${cpuUsedCores} / ${cpuCores} cores`} />
      <ProgressRow label="RAM Allocated" pct={ramUsedPct} detail={`${ramUsedGiB} / ${ramCapGiB} GiB`} />
      <p className="text-xs text-muted-foreground">
        {node.gpuTotal} GPU{node.gpuTotal !== 1 ? 's' : ''}
        {node.gpuModel ? ` · ${node.gpuModel}` : ''}
      </p>
    </div>
  );
}

function GpuCard({ gpu }: { gpu: GpuInfoDto }) {
  const statusClass =
    gpu.gpuStatus === 'In Use' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600';
  return (
    <div className="space-y-1.5 rounded-lg border p-3">
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold">GPU {gpu.index}</span>
        <Badge className={statusClass}>{gpu.gpuStatus}</Badge>
      </div>
      <p className="text-xs text-muted-foreground">{gpu.model ?? 'Unknown model'}</p>
      <p className="text-xs text-muted-foreground">{gpu.nodeName}</p>
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
  return (
    <div className="space-y-8 px-6 py-6">
      {/* Section 1: Node Overview */}
      <section>
        <h3 className="mb-3 text-sm font-semibold">Node Overview</h3>
        <div className="flex gap-3 overflow-x-auto pb-1">
          {details.nodes.length === 0 ? (
            <p className="text-xs text-muted-foreground">No nodes found.</p>
          ) : (
            details.nodes.map((n) => <NodeCard key={n.name} node={n} />)
          )}
        </div>
      </section>

      {/* Section 2: GPU Status */}
      <section>
        <h3 className="mb-3 text-sm font-semibold">GPU Status</h3>
        <div className="grid grid-cols-2 gap-3">
          {details.gpus.length === 0 ? (
            <p className="col-span-2 text-xs text-muted-foreground">No GPUs detected.</p>
          ) : (
            details.gpus.map((g) => <GpuCard key={`${g.nodeName}-${g.index}`} gpu={g} />)
          )}
        </div>
      </section>

      {/* Section 3: Active Workloads */}
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
  const [form, setForm] = useState({ name: '', description: '', kubeconfigRef: '' });
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [drawerCluster, setDrawerCluster] = useState<ClusterDto | null>(null);
  const { data: clusterDetails, isLoading: detailsLoading, isError: detailsError } =
    useClusterDetailsStream(drawerCluster?.id ?? null, !!drawerCluster);

  // Background fetch for GPU Summary column
  const gpuQueries = useQueries({
    queries: clusters.map((c) => ({
      queryKey: ['clusters', c.id, 'details'],
      queryFn: () => clusterService.getDetails(c.id),
      staleTime: 60_000,
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
            title="Upload kubeconfig"
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
    await createCluster.mutateAsync(form);
    toast.success(`Cluster "${form.name}" created`);
    setShowCreate(false);
    setForm({ name: '', description: '', kubeconfigRef: '' });
  };

  const handleUpload = async () => {
    if (!uploadTarget || !selectedFile) return;
    await uploadKubeconfig.mutateAsync({ id: uploadTarget.id, file: selectedFile });
    toast.success(`Kubeconfig uploaded for "${uploadTarget.name}"`);
    setUploadTarget(null);
    setSelectedFile(null);
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
              <Label>Kubeconfig Reference</Label>
              <Input
                value={form.kubeconfigRef}
                onChange={(e) => setForm({ ...form, kubeconfigRef: e.target.value })}
                placeholder="MinIO object key (optional — upload after creation)"
              />
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
            <Button onClick={handleCreate} disabled={createCluster.isPending || !form.name}>
              {createCluster.isPending ? 'Creating...' : 'Create'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Upload Kubeconfig Dialog */}
      <Dialog open={!!uploadTarget} onOpenChange={() => setUploadTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Upload Kubeconfig — {uploadTarget?.name}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <p className="text-sm text-muted-foreground">
              The file will be stored in MinIO under{' '}
              <code className="rounded bg-muted px-1 py-0.5 text-xs">
                clusters/{uploadTarget?.id}/kubeconfig
              </code>{' '}
              and the cluster's{' '}
              <code className="rounded bg-muted px-1 py-0.5 text-xs">kubeconfigRef</code> will be
              updated automatically.
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
              {uploadKubeconfig.isPending ? 'Uploading...' : 'Upload'}
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
        <SheetContent side="right" className="flex w-full flex-col gap-0 p-0 sm:max-w-[520px]">
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
