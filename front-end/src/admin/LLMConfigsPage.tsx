import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table';
import { Input } from '@/components/ui/input';
import { Select } from '@/components/ui/select';
import { Button } from '@/components/ui/button';

const schema = z.object({
  name: z.string().min(2),
  description: z.string().min(2),
  key: z.string().min(1),
  type: z.enum(['string', 'number', 'boolean', 'enum']),
  default: z.string().optional(),
  paramDescription: z.string().min(1),
});

export function AdminLLMConfigsPage() {
  const [enumValues, setEnumValues] = useState('');
  const queryClient = useQueryClient();
  const cfgQuery = useQuery({ queryKey: ['admin-llm-configs'], queryFn: api.llmConfigs.list });
  const form = useForm<z.infer<typeof schema>>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      description: '',
      key: 'temperature',
      type: 'number',
      default: '0.7',
      paramDescription: 'Sampling temperature',
    },
  });

  const createMutation = useMutation({
    mutationFn: api.llmConfigs.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-llm-configs'] });
      form.reset();
      setEnumValues('');
    },
  });

  const patchMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: Record<string, unknown> }) =>
      api.llmConfigs.patch(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-llm-configs'] }),
  });

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Create Config Schema (with first parameter)</CardTitle>
        </CardHeader>
        <CardContent>
          <form
            className="grid grid-cols-1 gap-3 md:grid-cols-3"
            onSubmit={form.handleSubmit((v) => {
              createMutation.mutate({
                name: v.name,
                description: v.description,
                version: 1,
                active: true,
                target: 'vllm',
                parameters: [
                  {
                    key: v.key,
                    type: v.type,
                    required: true,
                    default: v.default,
                    description: v.paramDescription,
                    enumValues: v.type === 'enum' ? enumValues.split(',').map((x) => x.trim()) : undefined,
                  },
                ],
              });
            })}
          >
            <Input placeholder="Schema name" {...form.register('name')} />
            <Input placeholder="Description" {...form.register('description')} />
            <Input placeholder="Parameter key" {...form.register('key')} />
            <Select {...form.register('type')}>
              <option value="string">string</option>
              <option value="number">number</option>
              <option value="boolean">boolean</option>
              <option value="enum">enum</option>
            </Select>
            <Input placeholder="Default value" {...form.register('default')} />
            <Input placeholder="Parameter description" {...form.register('paramDescription')} />
            <Input
              placeholder="Enum values: a,b,c"
              value={enumValues}
              onChange={(e) => setEnumValues(e.target.value)}
            />
            <Button type="submit">Create Schema</Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>LLM Config Schemas</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <THead>
              <TR>
                <TH>Name</TH>
                <TH>Version</TH>
                <TH>Active</TH>
                <TH>Parameters</TH>
                <TH>Actions</TH>
              </TR>
            </THead>
            <TBody>
              {cfgQuery.data?.map((cfg) => (
                <TR key={cfg.id}>
                  <TD>{cfg.name}</TD>
                  <TD>{cfg.version}</TD>
                  <TD>{cfg.active ? 'Yes' : 'No'}</TD>
                  <TD>
                    <pre className="max-w-[420px] overflow-x-auto rounded bg-slate-900 p-2 text-xs text-slate-100">
                      {JSON.stringify(cfg.parameters, null, 2)}
                    </pre>
                  </TD>
                  <TD className="space-y-2">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() =>
                        patchMutation.mutate({
                          id: cfg.id,
                          payload: { active: !cfg.active },
                        })
                      }
                    >
                      {cfg.active ? 'Mark Inactive' : 'Mark Active'}
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() =>
                        patchMutation.mutate({
                          id: cfg.id,
                          payload: { version: cfg.version + 1 },
                        })
                      }
                    >
                      Bump Version
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
