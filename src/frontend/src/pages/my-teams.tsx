import { useState } from 'react';
import { PageHeader } from '@/components/shared/page-header';
import { DataTable } from '@/components/shared/data-table';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useAuth } from '@/contexts/auth-context';
import { useTeams } from '@/api/hooks/use-teams';
import { useTeamMembers, useCreateTeamMember, useDeleteTeamMember } from '@/api/hooks/use-team-members';
import { useTeamClusters } from '@/api/hooks/use-team-clusters';
import { useProjects, useCreateProject } from '@/api/hooks/use-projects';
import { useWorkloads } from '@/api/hooks/use-workloads';
import { useUsers } from '@/api/hooks/use-users';
import { useClusters } from '@/api/hooks/use-clusters';
import { usePolicies } from '@/api/hooks/use-policies';
import { WorkloadStatusBadge } from '@/components/shared/status-badge';
import type { TeamDto, TeamMemberDto, ProjectDto, WorkloadDto, WorkloadStatus, CreateProjectRequest, TeamRole } from '@/api/types';
import type { Column } from '@/components/shared/data-table';
import { Plus, Trash2, UserPlus } from 'lucide-react';

export default function MyTeamsPage() {
  const { user, teamMemberships, isAdmin } = useAuth();
  const { data: teams = [] } = useTeams();
  const { data: allMembers = [] } = useTeamMembers();
  const { data: teamClusters = [] } = useTeamClusters();
  const { data: allProjects = [] } = useProjects();
  const { data: allWorkloads = [] } = useWorkloads();
  const { data: users = [] } = useUsers();
  const { data: clusters = [] } = useClusters();
  const { data: policies = [] } = usePolicies();
  const createMember = useCreateTeamMember();
  const deleteMember = useDeleteTeamMember();
  const createProject = useCreateProject();

  const [selectedTeam, setSelectedTeam] = useState<TeamDto | null>(null);
  const [showAddMember, setShowAddMember] = useState(false);
  const [showCreateProject, setShowCreateProject] = useState(false);
  const [memberForm, setMemberForm] = useState({ userId: '', role: 'MEMBER' as TeamRole });
  const [projectForm, setProjectForm] = useState<CreateProjectRequest>({
    teamId: '',
    clusterId: '',
    policyId: '',
    name: '',
  });

  // Teams where user is a lead (or all if admin)
  const myTeamIds = isAdmin
    ? teams.map((t) => t.id)
    : teamMemberships.filter((m) => m.role === 'TEAM_LEAD').map((m) => m.teamId);
  const myTeams = teams.filter((t) => myTeamIds.includes(t.id));

  const teamMembers = selectedTeam ? allMembers.filter((m) => m.teamId === selectedTeam.id) : [];
  const teamProjects = selectedTeam ? allProjects.filter((p) => p.teamId === selectedTeam.id) : [];
  const teamProjectIds = teamProjects.map((p) => p.id);
  const teamWorkloads = allWorkloads.filter((w) => teamProjectIds.includes(w.projectId));

  const availableClusters = selectedTeam
    ? clusters.filter((c) => teamClusters.some((tc) => tc.teamId === selectedTeam.id && tc.clusterId === c.id))
    : [];

  const teamColumns: Column<TeamDto>[] = [
    { header: 'Team', accessor: 'name' },
    { header: 'Members', accessor: (t) => allMembers.filter((m) => m.teamId === t.id).length },
    { header: 'Projects', accessor: (t) => allProjects.filter((p) => p.teamId === t.id).length },
  ];

  const memberColumns: Column<TeamMemberDto>[] = [
    { header: 'User', accessor: (m) => users.find((u) => u.id === m.userId)?.username ?? m.userId.slice(0, 8) },
    { header: 'Role', accessor: (m) => <Badge variant={m.role === 'TEAM_LEAD' ? 'default' : 'secondary'}>{m.role}</Badge> },
    {
      header: '',
      accessor: (m) =>
        m.userId !== user?.id ? (
          <Button variant="ghost" size="sm" onClick={() => deleteMember.mutate({ teamId: m.teamId, userId: m.userId })}>
            <Trash2 className="h-4 w-4 text-destructive" />
          </Button>
        ) : null,
      className: 'w-12',
    },
  ];

  const projectColumns: Column<ProjectDto>[] = [
    { header: 'Name', accessor: 'name' },
    { header: 'Cluster', accessor: (p) => clusters.find((c) => c.id === p.clusterId)?.name ?? '-' },
    { header: 'Policy', accessor: (p) => policies.find((pol) => pol.id === p.policyId)?.name ?? '-' },
  ];

  const workloadColumns: Column<WorkloadDto>[] = [
    { header: 'Name', accessor: 'name' },
    { header: 'Project', accessor: (w) => allProjects.find((p) => p.id === w.projectId)?.name ?? '-' },
    { header: 'Submitted By', accessor: (w) => users.find((u) => u.id === w.submittedById)?.username ?? '-' },
    { header: 'Status', accessor: (w) => <WorkloadStatusBadge status={w.status as WorkloadStatus} /> },
    { header: 'GPU', accessor: (w) => String(w.requestedGpu) },
  ];

  return (
    <div>
      <PageHeader title="My Teams" description="Teams you lead" />

      <div className="grid gap-6 lg:grid-cols-[300px_1fr]">
        <DataTable columns={teamColumns} data={myTeams} onRowClick={setSelectedTeam} emptyMessage="No teams" />

        {selectedTeam ? (
          <Card>
            <CardHeader>
              <CardTitle>{selectedTeam.name}</CardTitle>
            </CardHeader>
            <CardContent>
              <Tabs defaultValue="members">
                <TabsList>
                  <TabsTrigger value="members">Members</TabsTrigger>
                  <TabsTrigger value="projects">Projects</TabsTrigger>
                  <TabsTrigger value="workloads">Workloads</TabsTrigger>
                </TabsList>

                <TabsContent value="members" className="space-y-3">
                  <div className="flex justify-end">
                    <Button size="sm" onClick={() => setShowAddMember(true)}>
                      <UserPlus className="mr-2 h-4 w-4" /> Add Member
                    </Button>
                  </div>
                  <DataTable columns={memberColumns} data={teamMembers} />
                </TabsContent>

                <TabsContent value="projects" className="space-y-3">
                  <div className="flex justify-end">
                    <Button
                      size="sm"
                      onClick={() => {
                        setProjectForm({ teamId: selectedTeam.id, clusterId: '', policyId: '', name: '' });
                        setShowCreateProject(true);
                      }}
                    >
                      <Plus className="mr-2 h-4 w-4" /> Create Project
                    </Button>
                  </div>
                  <DataTable columns={projectColumns} data={teamProjects} />
                </TabsContent>

                <TabsContent value="workloads">
                  <DataTable columns={workloadColumns} data={teamWorkloads} emptyMessage="No workloads" />
                </TabsContent>
              </Tabs>
            </CardContent>
          </Card>
        ) : (
          <div className="flex h-64 items-center justify-center rounded-lg border bg-card text-muted-foreground">
            Select a team
          </div>
        )}
      </div>

      {/* Add Member Dialog */}
      <Dialog open={showAddMember} onOpenChange={setShowAddMember}>
        <DialogContent>
          <DialogHeader><DialogTitle>Add Member</DialogTitle></DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>User</Label>
              <Select value={memberForm.userId} onValueChange={(v) => v && setMemberForm({ ...memberForm, userId: v })}>
                <SelectTrigger><SelectValue placeholder="Select user" /></SelectTrigger>
                <SelectContent>
                  {users.filter((u) => !teamMembers.some((m) => m.userId === u.id)).map((u) => (
                    <SelectItem key={u.id} value={u.id}>{u.username}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Role</Label>
              <Select value={memberForm.role} onValueChange={(v) => v && setMemberForm({ ...memberForm, role: v as TeamRole })}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="MEMBER">Member</SelectItem>
                  <SelectItem value="TEAM_LEAD">Team Lead</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowAddMember(false)}>Cancel</Button>
            <Button
              onClick={async () => {
                if (selectedTeam) {
                  await createMember.mutateAsync({ userId: memberForm.userId, teamId: selectedTeam.id, role: memberForm.role });
                  setShowAddMember(false);
                  setMemberForm({ userId: '', role: 'MEMBER' });
                }
              }}
              disabled={!memberForm.userId}
            >
              Add
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Create Project Dialog */}
      <Dialog open={showCreateProject} onOpenChange={setShowCreateProject}>
        <DialogContent>
          <DialogHeader><DialogTitle>Create Project</DialogTitle></DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Name</Label>
              <Input value={projectForm.name} onChange={(e) => setProjectForm({ ...projectForm, name: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label>Cluster</Label>
              <Select
                value={projectForm.clusterId}
                onValueChange={(v) => v && setProjectForm({ ...projectForm, clusterId: v, policyId: '' })}
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
                value={projectForm.policyId}
                onValueChange={(v) => v && setProjectForm({ ...projectForm, policyId: v })}
                disabled={!projectForm.clusterId}
              >
                <SelectTrigger><SelectValue placeholder="Select policy" /></SelectTrigger>
                <SelectContent>
                  {policies.filter((p) => p.clusterId === projectForm.clusterId).map((p) => (
                    <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Description</Label>
              <Textarea
                value={projectForm.description ?? ''}
                onChange={(e) => setProjectForm({ ...projectForm, description: e.target.value })}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowCreateProject(false)}>Cancel</Button>
            <Button
              onClick={async () => {
                await createProject.mutateAsync(projectForm);
                setShowCreateProject(false);
              }}
              disabled={!projectForm.name || !projectForm.clusterId || !projectForm.policyId}
            >
              Create
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
