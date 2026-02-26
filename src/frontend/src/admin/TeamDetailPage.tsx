import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Select } from '@/components/ui/select';
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table';

export function AdminTeamDetailPage() {
  const { id = '' } = useParams();
  const queryClient = useQueryClient();
  const [selectedUserId, setSelectedUserId] = useState('');

  const teamQuery = useQuery({ queryKey: ['team-detail', id], queryFn: () => api.teams.get(id), enabled: !!id });
  const usersQuery = useQuery({ queryKey: ['admin-users'], queryFn: api.users.list });

  const addMemberMutation = useMutation({
    mutationFn: ({ teamId, userId }: { teamId: string; userId: string }) =>
      api.teams.addMember(teamId, { userId, role: 'MEMBER' }),
    onSuccess: () => {
      setSelectedUserId('');
      queryClient.invalidateQueries({ queryKey: ['team-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['teams'] });
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
  });

  const removeMemberMutation = useMutation({
    mutationFn: ({ teamId, userId }: { teamId: string; userId: string }) => api.teams.removeMember(teamId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['team-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['teams'] });
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
  });

  const candidateUsers = useMemo(() => {
    const current = new Set(teamQuery.data?.members.map((m) => m.userId) ?? []);
    return (usersQuery.data ?? []).filter((u) => !current.has(u.id));
  }, [teamQuery.data?.members, usersQuery.data]);

  if (!teamQuery.data) return <p className="text-sm text-muted-foreground">Loading team detail...</p>;

  const detail = teamQuery.data;
  const q = detail.quota;

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>{detail.team.name}</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-2 text-sm md:grid-cols-2">
          <p>
            <strong>Description:</strong> {detail.team.description}
          </p>
          <p>
            <strong>Status:</strong> {detail.team.active ? 'Active' : 'Disabled'}
          </p>
          <p>
            <strong>Active Workloads:</strong> {detail.usage.runningWorkloads}
          </p>
          <p>
            <strong>GPU Usage:</strong> {detail.usage.usedGPU}
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Members</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="flex gap-2">
            <Select value={selectedUserId} onChange={(e) => setSelectedUserId(e.target.value)}>
              <option value="">Assign user to team</option>
              {candidateUsers.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.username} ({u.email})
                </option>
              ))}
            </Select>
            <Button
              disabled={!selectedUserId}
              onClick={() => addMemberMutation.mutate({ teamId: detail.team.id, userId: selectedUserId })}
            >
              Assign
            </Button>
          </div>

          <Table>
            <THead>
              <TR>
                <TH>User</TH>
                <TH>Email</TH>
                <TH>Team Role</TH>
                <TH>Global Role</TH>
                <TH>Actions</TH>
              </TR>
            </THead>
            <TBody>
              {detail.members.map((member) => (
                <TR key={member.userId}>
                  <TD>{member.username}</TD>
                  <TD>{member.email}</TD>
                  <TD>{member.role}</TD>
                  <TD>{member.userRole}</TD>
                  <TD>
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => removeMemberMutation.mutate({ teamId: detail.team.id, userId: member.userId })}
                    >
                      Remove
                    </Button>
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Team Quota Summary</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-2 text-sm md:grid-cols-2">
          <p>
            <strong>Running:</strong> {detail.usage.runningWorkloads} / {q?.limits.maxRunningWorkloads ?? '-'}
          </p>
          <p>
            <strong>GPU:</strong> {detail.usage.usedGPU} / {q?.limits.maxGPU ?? '-'}
          </p>
          <p>
            <strong>CPU:</strong> {detail.usage.usedCPU} / {q?.limits.maxCPU ?? '-'}
          </p>
          <p>
            <strong>Memory:</strong> {detail.usage.usedMemoryGB} / {q?.limits.maxMemoryGB ?? '-'} GB
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Active Workloads</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <THead>
              <TR>
                <TH>Name</TH>
                <TH>Owner</TH>
                <TH>Type</TH>
                <TH>Status</TH>
                <TH>GPU</TH>
              </TR>
            </THead>
            <TBody>
              {detail.activeWorkloads.map((w) => (
                <TR key={w.id}>
                  <TD>{w.name}</TD>
                  <TD>{w.ownerName}</TD>
                  <TD>{w.type}</TD>
                  <TD>{w.status}</TD>
                  <TD>{w.resources.gpu}</TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
