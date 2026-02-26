import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from 'react-router-dom';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table';

const schema = z.object({
  name: z.string().min(2),
  description: z.string().min(2),
});

export function AdminTeamsPage() {
  const queryClient = useQueryClient();
  const teamsQuery = useQuery({ queryKey: ['teams'], queryFn: api.teams.list });
  const form = useForm<z.infer<typeof schema>>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', description: '' },
  });

  const createMutation = useMutation({
    mutationFn: api.teams.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams'] });
      form.reset();
    },
  });

  const patchMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => api.teams.patch(id, { active }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['teams'] }),
  });

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Create Team</CardTitle>
        </CardHeader>
        <CardContent>
          <form
            className="grid grid-cols-1 gap-3 md:grid-cols-3"
            onSubmit={form.handleSubmit((v) => createMutation.mutate(v))}
          >
            <Input placeholder="Team name" {...form.register('name')} />
            <Input placeholder="Description" {...form.register('description')} />
            <Button type="submit">Create Team</Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Teams</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <THead>
              <TR>
                <TH>Team</TH>
                <TH>Members</TH>
                <TH>Active Workloads</TH>
                <TH>GPU Usage</TH>
                <TH>Status</TH>
                <TH>Actions</TH>
              </TR>
            </THead>
            <TBody>
              {teamsQuery.data?.map((team) => (
                <TR key={team.id}>
                  <TD>
                    <Link to={`/admin/teams/${team.id}`} className="text-cyan-700 hover:underline">
                      {team.name}
                    </Link>
                    <p className="text-xs text-muted-foreground">{team.description}</p>
                  </TD>
                  <TD>{team.membersCount}</TD>
                  <TD>{team.activeWorkloads}</TD>
                  <TD>{team.usedGPU} GPU</TD>
                  <TD>{team.active ? 'Active' : 'Disabled'}</TD>
                  <TD>
                    <Button
                      size="sm"
                      variant={team.active ? 'destructive' : 'outline'}
                      onClick={() => patchMutation.mutate({ id: team.id, active: !team.active })}
                    >
                      {team.active ? 'Disable Team' : 'Enable Team'}
                    </Button>
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
