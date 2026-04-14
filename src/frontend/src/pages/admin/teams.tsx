import { useState } from 'react';
import { PageHeader } from '@/components/shared/page-header';
import { DataTable } from '@/components/shared/data-table';
import { ConfirmDialog } from '@/components/shared/confirm-dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
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
import { useTeams, useCreateTeam, useDeleteTeam } from '@/api/hooks/use-teams';
import {
  useTeamMembers,
  useCreateTeamMember,
  useUpdateTeamMember,
  useDeleteTeamMember,
} from '@/api/hooks/use-team-members';
import {
  useTeamClusters,
  useCreateTeamCluster,
  useDeleteTeamCluster,
} from '@/api/hooks/use-team-clusters';
import { useUsers } from '@/api/hooks/use-users';
import { useClusters } from '@/api/hooks/use-clusters';
import { usePolicies } from '@/api/hooks/use-policies';
import type { TeamDto, TeamMemberDto, TeamClusterDto, TeamRole } from '@/api/types';
import type { Column } from '@/components/shared/data-table';
import { Plus, Trash2, UserPlus, Server } from 'lucide-react';

export default function TeamsPage() {
  const { data: teams = [], isLoading } = useTeams();
  const { data: members = [] } = useTeamMembers();
  const { data: teamClusters = [] } = useTeamClusters();
  const { data: users = [] } = useUsers();
  const { data: clusters = [] } = useClusters();
  const { data: policies = [] } = usePolicies();

  const createTeam = useCreateTeam();
  const deleteTeam = useDeleteTeam();
  const createMember = useCreateTeamMember();
  const updateMember = useUpdateTeamMember();
  const deleteMember = useDeleteTeamMember();
  const createTC = useCreateTeamCluster();
  const deleteTC = useDeleteTeamCluster();

  const [showCreate, setShowCreate] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<TeamDto | null>(null);
  const [selectedTeam, setSelectedTeam] = useState<TeamDto | null>(null);
  const [showAddMember, setShowAddMember] = useState(false);
  const [showAddCluster, setShowAddCluster] = useState(false);
  const [teamForm, setTeamForm] = useState({ name: '', description: '' });
  const [memberForm, setMemberForm] = useState({ userId: '', role: 'MEMBER' as TeamRole });
  const [tcForm, setTcForm] = useState({ clusterId: '', policyId: '', namespace: '' });

  const teamColumns: Column<TeamDto>[] = [
    { header: 'Name', accessor: 'name' },
    { header: 'Description', accessor: (t) => t.description ?? '-' },
    {
      header: 'Members',
      accessor: (t) => members.filter((m) => m.teamId === t.id).length,
    },
    {
      header: 'Clusters',
      accessor: (t) => teamClusters.filter((tc) => tc.teamId === t.id).length,
    },
    {
      header: '',
      accessor: (t) => (
        <Button
          variant="ghost"
          size="sm"
          onClick={(e) => {
            e.stopPropagation();
            setDeleteTarget(t);
          }}
        >
          <Trash2 className="h-4 w-4 text-destructive" />
        </Button>
      ),
      className: 'w-12',
    },
  ];

  const teamMembers = selectedTeam ? members.filter((m) => m.teamId === selectedTeam.id) : [];
  const teamClustersList = selectedTeam
    ? teamClusters.filter((tc) => tc.teamId === selectedTeam.id)
    : [];

  const memberColumns: Column<TeamMemberDto>[] = [
    {
      header: 'User',
      accessor: (m) => users.find((u) => u.id === m.userId)?.username ?? m.userId.slice(0, 8),
    },
    {
      header: 'Role',
      accessor: (m) => (
        <Select
          value={m.role}
          onValueChange={(v) =>
            updateMember.mutate({
              teamId: m.teamId,
              userId: m.userId,
              data: { role: v as TeamRole },
            })
          }
        >
          <SelectTrigger className="w-32">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="MEMBER">Member</SelectItem>
            <SelectItem value="TEAM_LEAD">Team Lead</SelectItem>
          </SelectContent>
        </Select>
      ),
    },
    {
      header: '',
      accessor: (m) => (
        <Button
          variant="ghost"
          size="sm"
          onClick={() => deleteMember.mutate({ teamId: m.teamId, userId: m.userId })}
        >
          <Trash2 className="h-4 w-4 text-destructive" />
        </Button>
      ),
      className: 'w-12',
    },
  ];

  const tcColumns: Column<TeamClusterDto>[] = [
    {
      header: 'Cluster',
      accessor: (tc) => clusters.find((c) => c.id === tc.clusterId)?.name ?? tc.clusterId.slice(0, 8),
    },
    {
      header: 'Policy',
      accessor: (tc) => policies.find((p) => p.id === tc.policyId)?.name ?? tc.policyId.slice(0, 8),
    },
    { header: 'Namespace', accessor: 'namespace' },
    {
      header: '',
      accessor: (tc) => (
        <Button variant="ghost" size="sm" onClick={() => deleteTC.mutate(tc.id)}>
          <Trash2 className="h-4 w-4 text-destructive" />
        </Button>
      ),
      className: 'w-12',
    },
  ];

  const normalizeNamespace = (value: string) => value.trim().toLowerCase();

  return (
    <div>
      <PageHeader
        title="Teams"
        description="Manage teams, members, and cluster assignments"
        action={
          <Button onClick={() => setShowCreate(true)}>
            <Plus className="mr-2 h-4 w-4" /> Create Team
          </Button>
        }
      />

      <div className="grid gap-6 lg:grid-cols-[1fr_1.5fr]">
        <div>
          <DataTable
            columns={teamColumns}
            data={teams}
            isLoading={isLoading}
            onRowClick={setSelectedTeam}
          />
        </div>

        <div>
          {selectedTeam ? (
            <div className="rounded-lg border bg-card p-4">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold">{selectedTeam.name}</h2>
                <Badge variant="outline">{selectedTeam.description ?? 'No description'}</Badge>
              </div>

              <Tabs defaultValue="members">
                <TabsList>
                  <TabsTrigger value="members">Members</TabsTrigger>
                  <TabsTrigger value="clusters">Clusters</TabsTrigger>
                </TabsList>

                <TabsContent value="members" className="space-y-3">
                  <div className="flex justify-end">
                    <Button size="sm" onClick={() => setShowAddMember(true)}>
                      <UserPlus className="mr-2 h-4 w-4" /> Add Member
                    </Button>
                  </div>
                  <DataTable columns={memberColumns} data={teamMembers} />
                </TabsContent>

                <TabsContent value="clusters" className="space-y-3">
                  <div className="flex justify-end">
                    <Button size="sm" onClick={() => setShowAddCluster(true)}>
                      <Server className="mr-2 h-4 w-4" /> Assign Cluster
                    </Button>
                  </div>
                  <DataTable columns={tcColumns} data={teamClustersList} />
                </TabsContent>
              </Tabs>
            </div>
          ) : (
            <div className="flex h-64 items-center justify-center rounded-lg border bg-card text-muted-foreground">
              Select a team to view details
            </div>
          )}
        </div>
      </div>

      {/* Create Team Dialog */}
      <Dialog open={showCreate} onOpenChange={setShowCreate}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create Team</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Name</Label>
              <Input value={teamForm.name} onChange={(e) => setTeamForm({ ...teamForm, name: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label>Description</Label>
              <Textarea
                value={teamForm.description}
                onChange={(e) => setTeamForm({ ...teamForm, description: e.target.value })}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button
              onClick={async () => {
                const normalizedTeamName = teamForm.name.trim();
                await createTeam.mutateAsync({ ...teamForm, name: normalizedTeamName });
                setShowCreate(false);
                setTeamForm({ name: '', description: '' });
              }}
              disabled={createTeam.isPending || !teamForm.name.trim()}
            >
              Create
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Add Member Dialog */}
      <Dialog open={showAddMember} onOpenChange={setShowAddMember}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add Member</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>User</Label>
              <Select value={memberForm.userId} onValueChange={(v) => v && setMemberForm({ ...memberForm, userId: v })}>
                <SelectTrigger>
                  <SelectValue placeholder="Select user" />
                </SelectTrigger>
                <SelectContent>
                  {users
                    .filter((u) => !teamMembers.some((m) => m.userId === u.id))
                    .map((u) => (
                      <SelectItem key={u.id} value={u.id}>
                        {u.username} ({u.email})
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Role</Label>
              <Select
                value={memberForm.role}
                onValueChange={(v) => v && setMemberForm({ ...memberForm, role: v as TeamRole })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
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
                  await createMember.mutateAsync({
                    userId: memberForm.userId,
                    teamId: selectedTeam.id,
                    role: memberForm.role,
                  });
                  setShowAddMember(false);
                  setMemberForm({ userId: '', role: 'MEMBER' });
                }
              }}
              disabled={createMember.isPending || !memberForm.userId}
            >
              Add
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Assign Cluster Dialog */}
      <Dialog open={showAddCluster} onOpenChange={setShowAddCluster}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Assign Cluster</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Cluster</Label>
              <Select value={tcForm.clusterId} onValueChange={(v) => v && setTcForm({ ...tcForm, clusterId: v, policyId: '' })}>
                <SelectTrigger>
                  <SelectValue placeholder="Select cluster" />
                </SelectTrigger>
                <SelectContent>
                  {clusters.map((c) => (
                    <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Policy</Label>
              <Select value={tcForm.policyId} onValueChange={(v) => v && setTcForm({ ...tcForm, policyId: v })}>
                <SelectTrigger>
                  <SelectValue placeholder="Select policy" />
                </SelectTrigger>
                <SelectContent>
                  {policies
                    .filter((p) => p.clusterId === tcForm.clusterId)
                    .map((p) => (
                      <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
                    ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Namespace</Label>
              <Input
                value={tcForm.namespace}
                onChange={(e) => setTcForm({ ...tcForm, namespace: e.target.value })}
                onBlur={() => setTcForm((prev) => ({ ...prev, namespace: normalizeNamespace(prev.namespace) }))}
                placeholder="team-namespace"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowAddCluster(false)}>Cancel</Button>
            <Button
              onClick={async () => {
                if (selectedTeam) {
                  const normalizedNamespace = normalizeNamespace(tcForm.namespace);
                  await createTC.mutateAsync({
                    teamId: selectedTeam.id,
                    ...tcForm,
                    namespace: normalizedNamespace,
                  });
                  setShowAddCluster(false);
                  setTcForm({ clusterId: '', policyId: '', namespace: '' });
                }
              }}
              disabled={createTC.isPending || !tcForm.clusterId || !tcForm.policyId || !tcForm.namespace.trim()}
            >
              Assign
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={() => setDeleteTarget(null)}
        title="Delete Team"
        description={`Delete team "${deleteTarget?.name}"?`}
        onConfirm={async () => {
          if (deleteTarget) {
            await deleteTeam.mutateAsync(deleteTarget.id);
            setDeleteTarget(null);
            if (selectedTeam?.id === deleteTarget.id) setSelectedTeam(null);
          }
        }}
        loading={deleteTeam.isPending}
      />
    </div>
  );
}
