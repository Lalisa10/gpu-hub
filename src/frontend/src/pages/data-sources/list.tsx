import { useEffect, useState } from 'react';
import { PageHeader } from '@/components/shared/page-header';
import { DataTable } from '@/components/shared/data-table';
import { DataSourceStatusBadge } from '@/components/shared/status-badge';
import { ConfirmDialog } from '@/components/shared/confirm-dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
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
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
} from '@/components/ui/select';
import { useAuth } from '@/contexts/auth-context';
import {
  useMyDataSources,
  useCreateDataSource,
  useDeleteDataSource,
  useDataSourceStatusStream,
  useDataSourceJobLogsStream,
} from '@/api/hooks/use-data-sources';
import { useTeams } from '@/api/hooks/use-teams';
import { useClusters } from '@/api/hooks/use-clusters';
import { useTeamClusters } from '@/api/hooks/use-team-clusters';
import type { DataSourceDto, DataSourceStatus } from '@/api/types';
import type { Column } from '@/components/shared/data-table';
import { Plus, Trash2, Eye, Loader2 } from 'lucide-react';

export default function DataSourcesListPage() {
  const { user, isAdmin, teamMemberships } = useAuth();
  const { data: sources = [], isLoading } = useMyDataSources();
  const { data: teams = [] } = useTeams();
  const { data: clusters = [] } = useClusters();
  const { data: teamClusters = [] } = useTeamClusters();
  const deleteSource = useDeleteDataSource();

  const [addOpen, setAddOpen] = useState(false);
  const [detailTarget, setDetailTarget] = useState<DataSourceDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<DataSourceDto | null>(null);

  const { data: streamedStatus } = useDataSourceStatusStream(
    detailTarget?.id ?? null,
    !!detailTarget,
  );
  const activeSource = streamedStatus ?? detailTarget;

  const { data: streamedLogs } = useDataSourceJobLogsStream(
    detailTarget?.id ?? null,
    !!detailTarget,
  );

  const teamName = (id: string) => teams.find((t) => t.id === id)?.name ?? '—';
  const clusterName = (id: string) => clusters.find((c) => c.id === id)?.name ?? '—';

  const columns: Column<DataSourceDto>[] = [
    { header: 'PVC Name', accessor: 'pvcName' },
    { header: 'Team', accessor: (s) => teamName(s.teamId) },
    { header: 'Cluster', accessor: (s) => clusterName(s.clusterId) },
    { header: 'Bucket', accessor: (s) => <span className="font-mono text-xs">{s.bucketUrl}</span> },
    {
      header: 'Status',
      accessor: (s) => <DataSourceStatusBadge status={s.status as DataSourceStatus} />,
    },
    {
      header: 'Created',
      accessor: (s) => new Date(s.createdAt).toLocaleString(),
    },
    {
      header: 'Actions',
      accessor: (s) => (
        <div className="flex gap-1">
          <Button
            variant="ghost"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              setDetailTarget(s);
            }}
          >
            <Eye className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              setDeleteTarget(s);
            }}
          >
            <Trash2 className="h-4 w-4 text-destructive" />
          </Button>
        </div>
      ),
      className: 'w-32',
    },
  ];

  const myTeamIds = isAdmin ? teams.map((t) => t.id) : teamMemberships.map((m) => m.teamId);
  const allowedTeams = teams.filter((t) => myTeamIds.includes(t.id));

  return (
    <div>
      <PageHeader
        title="Data Sources"
        description="Register S3 buckets and migrate them onto JuiceFS volumes"
        action={
          <Button onClick={() => setAddOpen(true)}>
            <Plus className="mr-2 h-4 w-4" /> Add Data Source
          </Button>
        }
      />

      <DataTable columns={columns} data={sources} isLoading={isLoading} />

      <Sheet open={!!detailTarget} onOpenChange={(o) => !o && setDetailTarget(null)}>
        <SheetContent
          side="right"
          className="flex flex-col gap-0 p-0 data-[side=right]:w-full data-[side=right]:sm:max-w-none data-[side=right]:md:w-2/5 data-[side=right]:md:min-w-[540px]"
        >
          <SheetHeader className="border-b px-6 py-5">
            <SheetTitle className="text-lg">
              Data Source:{' '}
              <span className="font-mono text-muted-foreground">{activeSource?.pvcName}</span>
            </SheetTitle>
            {activeSource && (
              <div className="mt-2 flex flex-wrap items-center gap-2 text-xs">
                <DataSourceStatusBadge status={activeSource.status as DataSourceStatus} />
                <span className="text-muted-foreground">
                  {teamName(activeSource.teamId)} / {clusterName(activeSource.clusterId)}
                </span>
              </div>
            )}
          </SheetHeader>

          {activeSource && (
            <div className="flex-1 space-y-6 overflow-y-auto px-6 py-5 text-sm">
              <section>
                <h3 className="mb-2 font-heading text-sm font-medium">Connection</h3>
                <div className="space-y-2 rounded-md border bg-background px-3 py-2.5 text-xs">
                  <Detail label="Bucket URL" value={activeSource.bucketUrl} mono />
                  <Detail label="Access Key" value={activeSource.accessKey} mono />
                  <Detail
                    label="Created"
                    value={new Date(activeSource.createdAt).toLocaleString()}
                  />
                </div>
              </section>

              <section>
                <h3 className="mb-2 font-heading text-sm font-medium">Migration Job Logs</h3>
                <pre className="max-h-96 min-h-32 overflow-auto rounded-md bg-muted p-3 text-xs">
                  {streamedLogs && streamedLogs.length > 0 ? (
                    streamedLogs
                  ) : (
                    <span className="text-muted-foreground">
                      {activeSource.status === 'formated'
                        ? '(migration completed)'
                        : 'Waiting for migration pod...'}
                    </span>
                  )}
                </pre>
              </section>
            </div>
          )}

          {activeSource && (
            <SheetFooter className="border-t px-6 py-4">
              <div className="flex justify-end gap-2">
                <Button variant="outline" size="sm" onClick={() => setDetailTarget(null)}>
                  Close
                </Button>
              </div>
            </SheetFooter>
          )}
        </SheetContent>
      </Sheet>

      <AddDataSourceDialog
        open={addOpen}
        onOpenChange={setAddOpen}
        allowedTeams={allowedTeams}
        clusters={clusters}
        teamClusters={teamClusters}
        userId={user?.id ?? ''}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={() => setDeleteTarget(null)}
        title="Delete Data Source"
        description={`Delete data source "${deleteTarget?.pvcName}"? This removes the JuiceFS volume and all attached resources. Workloads still mounting it must be detached first.`}
        onConfirm={async () => {
          if (deleteTarget) {
            await deleteSource.mutateAsync(deleteTarget.id);
            setDeleteTarget(null);
          }
        }}
        loading={deleteSource.isPending}
      />
    </div>
  );
}

function Detail({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="grid grid-cols-[7rem_1fr] gap-2">
      <span className="text-[10px] uppercase tracking-wide text-muted-foreground">{label}</span>
      <span className={mono ? 'truncate font-mono text-xs' : 'truncate'}>{value}</span>
    </div>
  );
}

interface AddDialogProps {
  open: boolean;
  onOpenChange: (o: boolean) => void;
  allowedTeams: { id: string; name: string }[];
  clusters: { id: string; name: string }[];
  teamClusters: { teamId: string; clusterId: string }[];
  userId: string;
}

function AddDataSourceDialog({
  open,
  onOpenChange,
  allowedTeams,
  clusters,
  teamClusters,
  userId,
}: AddDialogProps) {
  const createSource = useCreateDataSource();
  const [teamId, setTeamId] = useState('');
  const [clusterId, setClusterId] = useState('');
  const [pvcName, setPvcName] = useState('');
  const [bucketUrl, setBucketUrl] = useState('');
  const [accessKey, setAccessKey] = useState('');
  const [secretKey, setSecretKey] = useState('');
  const [sourcePath, setSourcePath] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (!open) {
      setTeamId('');
      setClusterId('');
      setPvcName('');
      setBucketUrl('');
      setAccessKey('');
      setSecretKey('');
      setSourcePath('');
      setError('');
    }
  }, [open]);

  useEffect(() => {
    setClusterId('');
  }, [teamId]);

  const teamClusterIds = teamClusters
    .filter((tc) => tc.teamId === teamId)
    .map((tc) => tc.clusterId);
  const allowedClusters = clusters.filter((c) => teamClusterIds.includes(c.id));

  const valid =
    !!teamId &&
    !!clusterId &&
    !!pvcName.trim() &&
    !!bucketUrl.trim() &&
    !!accessKey.trim() &&
    !!secretKey.trim();

  const submit = async () => {
    if (!valid) return;
    if (!/^[a-z0-9]([-a-z0-9]*[a-z0-9])?$/.test(pvcName)) {
      setError(
        'PVC name must be lowercase alphanumeric or "-", start and end with alphanumeric.',
      );
      return;
    }
    setError('');
    try {
      await createSource.mutateAsync({
        clusterId,
        teamId,
        createdById: userId,
        pvcName: pvcName.trim(),
        bucketUrl: bucketUrl.trim(),
        accessKey: accessKey.trim(),
        secretKey: secretKey.trim(),
        sourcePath: sourcePath.trim() || undefined,
      });
      onOpenChange(false);
    } catch (e) {
      const msg = (() => {
        const err = e as { response?: { data?: { message?: string } }; message?: string };
        return err?.response?.data?.message ?? err?.message ?? 'Failed to create data source';
      })();
      setError(msg);
    }
  };

  const INPUT_CLS = 'bg-secondary text-[13px] focus-visible:ring-sky-500/40';
  const LABEL_CLS = 'text-[11px] font-medium uppercase tracking-wide text-muted-foreground';

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-[90vw] sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Add Data Source</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label className={LABEL_CLS}>Team *</Label>
              <Select value={teamId} onValueChange={(v) => v && setTeamId(v)}>
                <SelectTrigger className={INPUT_CLS}>
                  {teamId ? (
                    <span>{allowedTeams.find((t) => t.id === teamId)?.name ?? '—'}</span>
                  ) : (
                    <span className="text-muted-foreground">Select team</span>
                  )}
                </SelectTrigger>
                <SelectContent>
                  {allowedTeams.map((t) => (
                    <SelectItem key={t.id} value={t.id}>
                      {t.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label className={LABEL_CLS}>Cluster *</Label>
              <Select
                value={clusterId}
                onValueChange={(v) => v && setClusterId(v)}
                disabled={!teamId}
              >
                <SelectTrigger className={INPUT_CLS}>
                  {clusterId ? (
                    <span>{allowedClusters.find((c) => c.id === clusterId)?.name ?? '—'}</span>
                  ) : (
                    <span className="text-muted-foreground">
                      {teamId ? 'Select cluster' : 'Pick a team first'}
                    </span>
                  )}
                </SelectTrigger>
                <SelectContent>
                  {allowedClusters.map((c) => (
                    <SelectItem key={c.id} value={c.id}>
                      {c.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-1.5">
            <Label className={LABEL_CLS}>PVC Name *</Label>
            <Input
              className={INPUT_CLS}
              value={pvcName}
              onChange={(e) => setPvcName(e.target.value)}
              placeholder="my-dataset"
            />
            <p className="text-[11px] text-muted-foreground">
              Lowercase letters, digits, and hyphens. Must be unique in the team's namespace.
            </p>
          </div>

          <div className="space-y-1.5">
            <Label className={LABEL_CLS}>Bucket URL *</Label>
            <Input
              className={INPUT_CLS}
              value={bucketUrl}
              onChange={(e) => setBucketUrl(e.target.value)}
              placeholder="http://minio.example.com:9000/my-bucket"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label className={LABEL_CLS}>Access Key *</Label>
              <Input
                className={INPUT_CLS}
                value={accessKey}
                onChange={(e) => setAccessKey(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label className={LABEL_CLS}>Secret Key *</Label>
              <Input
                className={INPUT_CLS}
                type="password"
                value={secretKey}
                onChange={(e) => setSecretKey(e.target.value)}
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label className={LABEL_CLS}>Source Path (optional)</Label>
            <Input
              className={INPUT_CLS}
              value={sourcePath}
              onChange={(e) => setSourcePath(e.target.value)}
              placeholder="subdir/inside/bucket"
            />
            <p className="text-[11px] text-muted-foreground">
              Leave empty to copy the entire bucket.
            </p>
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={submit} disabled={!valid || createSource.isPending}>
            {createSource.isPending && <Loader2 className="mr-1.5 h-4 w-4 animate-spin" />}
            Create
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
