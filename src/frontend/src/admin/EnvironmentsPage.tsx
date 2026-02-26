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

const schema = z.object({
  name: z.string().min(2),
  type: z.enum(['notebook', 'container']),
  image: z.string().min(3),
  description: z.string().min(2),
});

export function AdminEnvironmentsPage() {
  const queryClient = useQueryClient();
  const envQuery = useQuery({ queryKey: ['admin-environments'], queryFn: api.environments.list });
  const form = useForm<z.infer<typeof schema>>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', type: 'container', image: '', description: '' },
  });

  const createMutation = useMutation({
    mutationFn: api.environments.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-environments'] });
      form.reset();
    },
  });

  const patchMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: Record<string, unknown> }) =>
      api.environments.patch(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-environments'] }),
  });

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Create Environment</CardTitle>
        </CardHeader>
        <CardContent>
          <form
            className="grid grid-cols-1 gap-3 md:grid-cols-5"
            onSubmit={form.handleSubmit((v) => createMutation.mutate(v))}
          >
            <Input placeholder="Name" {...form.register('name')} />
            <Select {...form.register('type')}>
              <option value="container">container</option>
              <option value="notebook">notebook</option>
            </Select>
            <Input placeholder="Image" {...form.register('image')} />
            <Input placeholder="Description" {...form.register('description')} />
            <Button type="submit">Create</Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Environments</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <THead>
              <TR>
                <TH>Name</TH>
                <TH>Type</TH>
                <TH>Image</TH>
                <TH>Enabled</TH>
                <TH>Actions</TH>
              </TR>
            </THead>
            <TBody>
              {envQuery.data?.map((env) => (
                <TR key={env.id}>
                  <TD>{env.name}</TD>
                  <TD>{env.type}</TD>
                  <TD>{env.image}</TD>
                  <TD>{env.enabled ? 'Yes' : 'No'}</TD>
                  <TD>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => patchMutation.mutate({ id: env.id, payload: { enabled: !env.enabled } })}
                    >
                      {env.enabled ? 'Disable' : 'Enable'}
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
