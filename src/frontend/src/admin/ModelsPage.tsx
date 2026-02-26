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
  family: z.enum(['LLaMA', 'Qwen', 'DeepSeek', 'Custom']),
  size: z.string().min(2),
  defaultConfigId: z.string().min(1),
});

export function AdminModelsPage() {
  const queryClient = useQueryClient();
  const modelsQuery = useQuery({ queryKey: ['admin-models'], queryFn: api.models.list });
  const cfgQuery = useQuery({ queryKey: ['llm-configs'], queryFn: api.llmConfigs.list });

  const form = useForm<z.infer<typeof schema>>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', family: 'Custom', size: '7B', defaultConfigId: '' },
  });

  const createMutation = useMutation({
    mutationFn: api.models.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-models'] });
      form.reset();
    },
  });

  const patchMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: Record<string, unknown> }) => api.models.patch(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-models'] }),
  });

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Create Model</CardTitle>
        </CardHeader>
        <CardContent>
          <form
            className="grid grid-cols-1 gap-3 md:grid-cols-5"
            onSubmit={form.handleSubmit((v) => createMutation.mutate({ ...v, formats: ['hf'], enabled: true }))}
          >
            <Input placeholder="Model name" {...form.register('name')} />
            <Select {...form.register('family')}>
              <option>LLaMA</option>
              <option>Qwen</option>
              <option>DeepSeek</option>
              <option>Custom</option>
            </Select>
            <Input placeholder="7B" {...form.register('size')} />
            <Select {...form.register('defaultConfigId')}>
              <option value="">Select default config</option>
              {cfgQuery.data?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </Select>
            <Button type="submit">Create</Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Models</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <THead>
              <TR>
                <TH>Name</TH>
                <TH>Family</TH>
                <TH>Size</TH>
                <TH>Default Config</TH>
                <TH>Enabled</TH>
                <TH>Actions</TH>
              </TR>
            </THead>
            <TBody>
              {modelsQuery.data?.map((m) => (
                <TR key={m.id}>
                  <TD>{m.name}</TD>
                  <TD>{m.family}</TD>
                  <TD>{m.size}</TD>
                  <TD>{m.defaultConfigId}</TD>
                  <TD>{m.enabled ? 'Yes' : 'No'}</TD>
                  <TD>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => patchMutation.mutate({ id: m.id, payload: { enabled: !m.enabled } })}
                    >
                      {m.enabled ? 'Disable' : 'Enable'}
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
