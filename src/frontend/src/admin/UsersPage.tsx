import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table';
import { Input } from '@/components/ui/input';
import { Select } from '@/components/ui/select';
import { Button } from '@/components/ui/button';
import { ConfirmAction } from '@/components/ui/confirm-action';

const schema = z.object({
  username: z.string().min(2),
  email: z.string().email(),
  role: z.enum(['USER', 'ADMIN']),
  teamId: z.string().min(1),
});

export function AdminUsersPage() {
  const queryClient = useQueryClient();
  const usersQuery = useQuery({ queryKey: ['admin-users'], queryFn: api.users.list });
  const teamsQuery = useQuery({ queryKey: ['teams'], queryFn: api.teams.list });
  const form = useForm<z.infer<typeof schema>>({
    resolver: zodResolver(schema),
    defaultValues: { username: '', email: '', role: 'USER', teamId: '' },
  });

  const createMutation = useMutation({
    mutationFn: api.users.create,
    onSuccess: () => {
      form.reset();
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
  });

  const patchMutation = useMutation({
    mutationFn: ({
      id,
      payload,
    }: {
      id: string;
      payload: { active?: boolean; role?: 'USER' | 'ADMIN'; teamId?: string };
    }) =>
      api.users.patch(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      queryClient.invalidateQueries({ queryKey: ['teams'] });
      queryClient.invalidateQueries({ queryKey: ['team-detail'] });
    },
  });

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Create User</CardTitle>
        </CardHeader>
        <CardContent>
          <form
            className="grid grid-cols-1 gap-3 md:grid-cols-5"
            onSubmit={form.handleSubmit((v) => createMutation.mutate(v))}
          >
            <Input placeholder="username" {...form.register('username')} />
            <Input placeholder="email" {...form.register('email')} />
            <Select {...form.register('role')}>
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </Select>
            <Select {...form.register('teamId')}>
              <option value="">Assign team</option>
              {teamsQuery.data?.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </Select>
            <Button type="submit">Create</Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Users</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <THead>
              <TR>
                <TH>Username</TH>
                <TH>Email</TH>
                <TH>Role</TH>
                <TH>Team</TH>
                <TH>Active</TH>
                <TH>Actions</TH>
              </TR>
            </THead>
            <TBody>
              {usersQuery.data?.map((u) => (
                <TR key={u.id}>
                  <TD>{u.username}</TD>
                  <TD>{u.email}</TD>
                  <TD>{u.role}</TD>
                  <TD>
                    <Select
                      value={u.teamId}
                      onChange={(e) => patchMutation.mutate({ id: u.id, payload: { teamId: e.target.value } })}
                    >
                      {teamsQuery.data?.map((t) => (
                        <option key={t.id} value={t.id}>
                          {t.name}
                        </option>
                      ))}
                    </Select>
                  </TD>
                  <TD>{u.active ? 'Yes' : 'No'}</TD>
                  <TD className="space-x-2">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => patchMutation.mutate({ id: u.id, payload: { role: u.role === 'ADMIN' ? 'USER' : 'ADMIN' } })}
                    >
                      Toggle Role
                    </Button>
                    <ConfirmAction
                      title="Update active state"
                      description="This toggles user activation and login access."
                      triggerLabel={u.active ? 'Deactivate' : 'Activate'}
                      triggerVariant={u.active ? 'destructive' : 'outline'}
                      actionLabel={u.active ? 'Deactivate' : 'Activate'}
                      onConfirm={() => patchMutation.mutate({ id: u.id, payload: { active: !u.active } })}
                    />
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
