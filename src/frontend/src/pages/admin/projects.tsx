import { useState } from 'react';
import { PageHeader } from '@/components/shared/page-header';
import { DataTable } from '@/components/shared/data-table';
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useProjects, useCreateProject, useDeleteProject } from '@/api/hooks/use-projects';
import { useTeams } from '@/api/hooks/use-teams';
import { useClusters } from '@/api/hooks/use-clusters';
import { usePolicies } from '@/api/hooks/use-policies';
import { useTeamClusters } from '@/api/hooks/use-team-clusters';
import type { ProjectDto, CreateProjectRequest } from '@/api/types';
import type { Column } from '@/components/shared/data-table';
import { Plus, Trash2 } from 'lucide-react';

const emptyForm: CreateProjectRequest = {
  teamId: '',
  clusterId: '',
  policyId: '',
  name: '',
};

export default function ProjectsPage() {
  const { data: projects = [], isLoading } = useProjects();
  const { data: teams = [] } = useTeams();
  const { data: clusters = [] } = useClusters();
  const { data: policies = [] } = usePolicies();
  const { data: teamClusters = [] } = useTeamClusters();
  const createProject = useCreateProject();
  const deleteProject = useDeleteProject();

  const [showCreate, setShowCreate] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<ProjectDto | null>(null);
  const [form, setForm] = useState<CreateProjectRequest>(emptyForm);

  const teamName = (id: string) => teams.find((t) => t.id === id)?.name ?? id.slice(0, 8);
  const clusterName = (id: string) => clusters.find((c) => c.id === id)?.name ?? id.slice(0, 8);
  const policyName = (id: string) => policies.find((p) => p.id === id)?.name ?? id.slice(0, 8);

  // Filter clusters that the selected team is assigned to
  const availableClusters = form.teamId
    ? clusters.filter((c) => teamClusters.some((tc) => tc.teamId === form.teamId && tc.clusterId === c.id))
    : [];

  // Filter policies for the selected cluster
  const availablePolicies = form.clusterId
    ? policies.filter((p) => p.clusterId === form.clusterId)
    : [];

  const columns: Column<ProjectDto>[] = [
    { header: 'Name', accessor: 'name' },
    { header: 'Team', accessor: (p) => teamName(p.teamId) },
    { header: 'Cluster', accessor: (p) => clusterName(p.clusterId) },
    { header: 'Policy', accessor: (p) => policyName(p.policyId) },
    { header: 'Description', accessor: (p) => p.description ?? '-' },
    {
      header: '',
      accessor: (p) => (
        <Button
          variant="ghost"
          size="sm"
          onClick={(e) => {
            e.stopPropagation();
            setDeleteTarget(p);
          }}
        >
          <Trash2 className="h-4 w-4 text-destructive" />
        </Button>
      ),
      className: 'w-12',
    },
  ];

  const updateField = <K extends keyof CreateProjectRequest>(key: K, value: CreateProjectRequest[K]) =>
    setForm((f) => ({ ...f, [key]: value }));

  return (
    <div>
      <PageHeader
        title="Projects"
        description="Team projects on clusters"
        action={
          <Button onClick={() => setShowCreate(true)}>
            <Plus className="mr-2 h-4 w-4" /> Create Project
          </Button>
        }
      />

      <DataTable columns={columns} data={projects} isLoading={isLoading} />

      <Dialog open={showCreate} onOpenChange={setShowCreate}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create Project</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Name</Label>
              <Input value={form.name} onChange={(e) => updateField('name', e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label>Team</Label>
              <Select
                value={form.teamId}
                onValueChange={(v) => v && setForm({ ...form, teamId: v, clusterId: '', policyId: '' })}
              >
                <SelectTrigger><SelectValue placeholder="Select team" /></SelectTrigger>
                <SelectContent>
                  {teams.map((t) => (
                    <SelectItem key={t.id} value={t.id}>{t.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Cluster</Label>
              <Select
                value={form.clusterId}
                onValueChange={(v) => v && setForm({ ...form, clusterId: v, policyId: '' })}
                disabled={!form.teamId}
              >
                <SelectTrigger><SelectValue placeholder="Select cluster" /></SelectTrigger>
                <SelectContent>
                  {availableClusters.map((c) => (
                    <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Policy</Label>
              <Select
                value={form.policyId}
                onValueChange={(v) => v && updateField('policyId', v)}
                disabled={!form.clusterId}
              >
                <SelectTrigger><SelectValue placeholder="Select policy" /></SelectTrigger>
                <SelectContent>
                  {availablePolicies.map((p) => (
                    <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Description</Label>
              <Textarea
                value={form.description ?? ''}
                onChange={(e) => updateField('description', e.target.value)}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button
              onClick={async () => {
                await createProject.mutateAsync(form);
                setShowCreate(false);
                setForm(emptyForm);
              }}
              disabled={createProject.isPending || !form.name || !form.teamId || !form.clusterId || !form.policyId}
            >
              Create
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={() => setDeleteTarget(null)}
        title="Delete Project"
        description={`Delete project "${deleteTarget?.name}"?`}
        onConfirm={async () => {
          if (deleteTarget) {
            await deleteProject.mutateAsync(deleteTarget.id);
            setDeleteTarget(null);
          }
        }}
        loading={deleteProject.isPending}
      />
    </div>
  );
}
