import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { api } from '@/lib/api';
import type { ResourceValues } from '@/components/ResourceSelector';
import { ResourceSelector } from '@/components/ResourceSelector';
import { DynamicParametersForm } from '@/components/DynamicForm/DynamicParametersForm';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Select } from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import type { WorkloadType } from '@/lib/types';

const trainSchema = z.object({
  name: z.string().min(2),
  image: z.string().min(3),
  command: z.string().min(2),
});

const researchSchema = z.object({
  name: z.string().min(2),
  environmentId: z.string().min(1),
});

export function SubmitPage() {
  const [type, setType] = useState<WorkloadType>('infer');
  const [resourceValues, setResourceValues] = useState<ResourceValues>({ gpu: 1, cpu: 8, memGB: 32 });
  const [inferName, setInferName] = useState('Inference Workload');
  const [modelId, setModelId] = useState('');
  const [configSchemaId, setConfigSchemaId] = useState('');
  const queryClient = useQueryClient();

  const modelsQuery = useQuery({ queryKey: ['models'], queryFn: api.models.list });
  const configsQuery = useQuery({ queryKey: ['llm-configs'], queryFn: api.llmConfigs.list });
  const envsQuery = useQuery({ queryKey: ['environments'], queryFn: api.environments.list });

  const workloadMutation = useMutation({
    mutationFn: api.workloads.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workloads'] });
    },
  });

  const activeSchema = useMemo(
    () => configsQuery.data?.find((cfg) => cfg.id === configSchemaId) ?? configsQuery.data?.[0],
    [configSchemaId, configsQuery.data],
  );

  const trainForm = useForm<z.infer<typeof trainSchema>>({
    resolver: zodResolver(trainSchema),
    defaultValues: {
      name: 'Train Job',
      image: 'ghcr.io/gpu-hub/train:latest',
      command: 'python train.py --epochs 5',
    },
  });

  const researchForm = useForm<z.infer<typeof researchSchema>>({
    resolver: zodResolver(researchSchema),
    defaultValues: {
      name: 'Research Notebook',
      environmentId: '',
    },
  });

  const submitInfer = async (params: Record<string, unknown>) => {
    const selectedModelId = modelId || modelsQuery.data?.[0]?.id;
    const selectedConfigSchemaId = configSchemaId || activeSchema?.id;
    await workloadMutation.mutateAsync({
      name: inferName,
      type: 'infer',
      resources: resourceValues,
      runtime: {
        modelId: selectedModelId,
        configSchemaId: selectedConfigSchemaId,
        configValues: params,
      },
    });
  };

  const submitTrain = trainForm.handleSubmit(async (values) => {
    await workloadMutation.mutateAsync({
      name: values.name,
      type: 'train',
      resources: resourceValues,
      runtime: {
        command: values.command,
        environmentId: values.image,
      },
    });
  });

  const submitResearch = researchForm.handleSubmit(async (values) => {
    await workloadMutation.mutateAsync({
      name: values.name,
      type: 'research',
      resources: resourceValues,
      runtime: {
        environmentId: values.environmentId,
      },
    });
  });

  const latest = workloadMutation.data;

  return (
    <div className="space-y-5">
      <Card>
        <CardHeader>
          <CardTitle>Workload Submission</CardTitle>
          <CardDescription>Step 1: Select workload type</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex gap-2">
            {(['infer', 'train', 'research'] as const).map((t) => (
              <Button key={t} variant={type === t ? 'default' : 'outline'} onClick={() => setType(t)}>
                {t.toUpperCase()}
              </Button>
            ))}
          </div>
        </CardContent>
      </Card>

      {type === 'infer' ? (
        <Card>
          <CardHeader>
            <CardTitle>Inference Submission</CardTitle>
            <CardDescription>Dynamic parameters generated from selected schema</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <label className="block text-sm">
              Workload Name
              <Input value={inferName} onChange={(e) => setInferName(e.target.value)} />
            </label>

            <label className="block text-sm">
              Model
              <Select value={modelId} onChange={(e) => setModelId(e.target.value)}>
                <option value="">Auto-select model</option>
                {modelsQuery.data?.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name} ({m.size})
                  </option>
                ))}
              </Select>
            </label>

            <label className="block text-sm">
              Config Schema
              <Select value={configSchemaId} onChange={(e) => setConfigSchemaId(e.target.value)}>
                <option value="">Auto-select schema</option>
                {configsQuery.data?.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} v{c.version}
                  </option>
                ))}
              </Select>
            </label>

            <div>
              <p className="mb-2 text-sm font-medium">Resources</p>
              <ResourceSelector value={resourceValues} onChange={setResourceValues} />
            </div>

            {activeSchema ? (
              <DynamicParametersForm
                parameters={activeSchema.parameters}
                submitLabel={workloadMutation.isPending ? 'Submitting...' : 'Submit Inference Workload'}
                onSubmit={submitInfer}
              />
            ) : (
              <p className="text-sm text-muted-foreground">No active config schema available.</p>
            )}
          </CardContent>
        </Card>
      ) : null}

      {type === 'train' ? (
        <Card>
          <CardHeader>
            <CardTitle>Training Submission</CardTitle>
            <CardDescription>Basic placeholder flow with resources + command</CardDescription>
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={submitTrain}>
              <label className="block text-sm">
                Name
                <Input {...trainForm.register('name')} />
              </label>
              <label className="block text-sm">
                Container Image
                <Input {...trainForm.register('image')} />
              </label>
              <label className="block text-sm">
                Command
                <Textarea {...trainForm.register('command')} />
              </label>
              <ResourceSelector value={resourceValues} onChange={setResourceValues} />
              <Button type="submit" disabled={workloadMutation.isPending}>
                Submit Training Workload
              </Button>
            </form>
          </CardContent>
        </Card>
      ) : null}

      {type === 'research' ? (
        <Card>
          <CardHeader>
            <CardTitle>Research Environment Submission</CardTitle>
            <CardDescription>Provision notebook/container environment</CardDescription>
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={submitResearch}>
              <label className="block text-sm">
                Name
                <Input {...researchForm.register('name')} />
              </label>
              <label className="block text-sm">
                Environment
                <Select {...researchForm.register('environmentId')}>
                  <option value="">Select environment</option>
                  {envsQuery.data?.map((env) => (
                    <option key={env.id} value={env.id}>
                      {env.name} ({env.type})
                    </option>
                  ))}
                </Select>
              </label>
              <ResourceSelector value={resourceValues} onChange={setResourceValues} />
              <Button type="submit" disabled={workloadMutation.isPending}>
                Submit Research Workload
              </Button>
            </form>
          </CardContent>
        </Card>
      ) : null}

      {latest?.type === 'research' && latest.notebookUrl ? (
        <Card className="border-emerald-300 bg-emerald-50">
          <CardContent className="p-4 text-sm">
            Your environment is ready {'->'}{' '}
            <a className="text-emerald-700 underline" href={latest.notebookUrl} target="_blank" rel="noreferrer">
              Open Notebook
            </a>
          </CardContent>
        </Card>
      ) : null}
    </div>
  );
}
