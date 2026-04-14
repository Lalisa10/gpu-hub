import { useRef, useState } from 'react';
import { PageHeader } from '@/components/shared/page-header';
import { DataTable } from '@/components/shared/data-table';
import { ClusterStatusBadge } from '@/components/shared/status-badge';
import { ConfirmDialog } from '@/components/shared/confirm-dialog';
import { Button } from '@/components/ui/button';
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
import {
  useClusters,
  useCreateCluster,
  useDeleteCluster,
  useUploadKubeconfig,
} from '@/api/hooks/use-clusters';
import type { ClusterDto, ClusterStatus } from '@/api/types';
import type { Column } from '@/components/shared/data-table';
import { Plus, Trash2, Upload } from 'lucide-react';
import { toast } from 'sonner';

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

  const columns: Column<ClusterDto>[] = [
    { header: 'Name', accessor: 'name' },
    {
      header: 'Status',
      accessor: (c) => <ClusterStatusBadge status={c.status as ClusterStatus} />,
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
              and the cluster's <code className="rounded bg-muted px-1 py-0.5 text-xs">kubeconfigRef</code> will be
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
    </div>
  );
}
