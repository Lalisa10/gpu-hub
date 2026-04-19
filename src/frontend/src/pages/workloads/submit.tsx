import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/shared/page-header';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,

} from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useAuth } from '@/contexts/auth-context';
import { useCreateWorkload } from '@/api/hooks/use-workloads';
import { useProjects } from '@/api/hooks/use-projects';
import { useTeams } from '@/api/hooks/use-teams';
import { useClusters } from '@/api/hooks/use-clusters';
import { X, Plus } from 'lucide-react';

interface NotebookExtra {
  pvcSize: string;
  gpuType: string;
}

interface EnvVar {
  key: string;
  value: string;
}

interface LlmInferenceExtra {
  modelSource: string;
  vllmParams: string;
  replicaCount: number;
  envVars: EnvVar[];
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="mb-4 rounded-xl border border-border/60 bg-white p-6 dark:bg-card">
      <h3 className="mb-4 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
        {title}
      </h3>
      {children}
    </div>
  );
}

const INPUT_CLS = 'bg-secondary text-[13px] focus-visible:ring-sky-500/40';
const LABEL_CLS = 'text-[11px] font-medium uppercase tracking-wide text-muted-foreground';

export default function SubmitWorkloadPage() {
  const { user, teamMemberships, isAdmin } = useAuth();
  const navigate = useNavigate();
  const createWorkload = useCreateWorkload();
  const { data: projects = [] } = useProjects();
  const { data: teams = [] } = useTeams();
  const { data: clusters = [] } = useClusters();

  const [tab, setTab] = useState('notebook');
  const [projectId, setProjectId] = useState('');
  const [name, setName] = useState('');
  const [image, setImage] = useState('');
  const [gpuCount, setGpuCount] = useState(1);
  const [cpuRequest, setCpuRequest] = useState(2);
  const [memRequest, setMemRequest] = useState(4096);
  const [error, setError] = useState('');

  const [notebook, setNotebook] = useState<NotebookExtra>({ pvcSize: '10Gi', gpuType: '' });
  const [llm, setLlm] = useState<LlmInferenceExtra>({
    modelSource: '',
    vllmParams: '',
    replicaCount: 1,
    envVars: [],
  });

  const myTeamIds = isAdmin ? teams.map((t) => t.id) : teamMemberships.map((m) => m.teamId);
  const accessibleProjects = projects.filter((p) => myTeamIds.includes(p.teamId));
  const selectedProject = projects.find((p) => p.id === projectId);

  const addEnvVar = () =>
    setLlm((prev) => ({ ...prev, envVars: [...prev.envVars, { key: '', value: '' }] }));
  const removeEnvVar = (i: number) =>
    setLlm((prev) => ({ ...prev, envVars: prev.envVars.filter((_, idx) => idx !== i) }));
  const updateEnvVar = (i: number, field: 'key' | 'value', val: string) =>
    setLlm((prev) => ({
      ...prev,
      envVars: prev.envVars.map((ev, idx) => (idx === i ? { ...ev, [field]: val } : ev)),
    }));

  const handleSubmit = async () => {
    if (!projectId || !name || !image || !user) return;
    setError('');
    const extra = tab === 'notebook' ? JSON.stringify(notebook) : JSON.stringify(llm);
    try {
      await createWorkload.mutateAsync({
        projectId,
        clusterId: selectedProject!.clusterId,
        submittedById: user.id,
        workloadType: tab === 'notebook' ? 'notebook' : 'llm_inference',
        name,
        image,
        requestedGpu: gpuCount,
        requestedCpu: cpuRequest,
        requestedMemory: memRequest,
        extra,
      });
      navigate('/workloads');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to submit workload');
    }
  };

  const isNotebookValid = !!name && !!projectId && !!image;
  const isLlmValid = !!name && !!projectId && !!image && !!llm.modelSource;

  return (
    <div>
      <PageHeader title="Submit Workload" description="Launch a notebook or LLM inference service" />

      <div className="mx-auto max-w-3xl">
        <Tabs value={tab} onValueChange={setTab}>
          <TabsList className="mb-6 w-full">
            <TabsTrigger value="notebook" className="flex-1">Notebook</TabsTrigger>
            <TabsTrigger value="llm" className="flex-1">LLM Inference</TabsTrigger>
          </TabsList>

          {/* Section 1 – General (shared) */}
          <Section title="General">
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label className={LABEL_CLS}>Workload Name *</Label>
                  <Input
                    className={INPUT_CLS}
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="my-workload"
                  />
                </div>
                <div className="space-y-1.5">
                  <Label className={LABEL_CLS}>Docker Image *</Label>
                  <Input
                    className={INPUT_CLS}
                    value={image}
                    onChange={(e) => setImage(e.target.value)}
                    placeholder={tab === 'notebook' ? 'jupyter/scipy-notebook:latest' : 'vllm/vllm-openai:latest'}
                  />
                  <p className="text-[11px] text-muted-foreground">Must be accessible from the cluster registry</p>
                </div>
              </div>
              <div className="space-y-1.5">
                <Label className={LABEL_CLS}>Project *</Label>
                <Select value={projectId} onValueChange={(v) => v && setProjectId(v)}>
                  <SelectTrigger className={INPUT_CLS}>
                    {projectId ? (
                      <span>{accessibleProjects.find((p) => p.id === projectId)?.name ?? '—'}</span>
                    ) : (
                      <span className="text-muted-foreground">Select project</span>
                    )}
                  </SelectTrigger>
                  <SelectContent>
                    {accessibleProjects.map((p) => {
                      const team = teams.find((t) => t.id === p.teamId);
                      const cluster = clusters.find((c) => c.id === p.clusterId);
                      const label = team && cluster
                        ? `${p.name} (${team.name} · ${cluster.name})`
                        : p.name;
                      return (
                        <SelectItem key={p.id} value={p.id}>
                          {label}
                        </SelectItem>
                      );
                    })}
                  </SelectContent>
                </Select>
              </div>
            </div>
          </Section>

          {/* Section 2 – Resources (shared) */}
          <Section title="Resources">
            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-1.5">
                <Label className={LABEL_CLS}>GPU Count</Label>
                <Input
                  className={INPUT_CLS}
                  type="number"
                  min={0}
                  value={gpuCount}
                  onChange={(e) => setGpuCount(+e.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label className={LABEL_CLS}>CPU Request (cores)</Label>
                <Input
                  className={INPUT_CLS}
                  type="number"
                  min={0}
                  step={0.5}
                  value={cpuRequest}
                  onChange={(e) => setCpuRequest(+e.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label className={LABEL_CLS}>Memory Request (MiB)</Label>
                <Input
                  className={INPUT_CLS}
                  type="number"
                  min={0}
                  value={memRequest}
                  onChange={(e) => setMemRequest(+e.target.value)}
                />
              </div>
            </div>
          </Section>

          {/* Notebook-specific */}
          <TabsContent value="notebook">
            <Section title="Notebook Configuration">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label className={LABEL_CLS}>PVC Size</Label>
                  <Input
                    className={INPUT_CLS}
                    value={notebook.pvcSize}
                    onChange={(e) => setNotebook({ ...notebook, pvcSize: e.target.value })}
                    placeholder="10Gi"
                  />
                </div>
                <div className="space-y-1.5">
                  <Label className={LABEL_CLS}>GPU Type (optional)</Label>
                  <Input
                    className={INPUT_CLS}
                    value={notebook.gpuType}
                    onChange={(e) => setNotebook({ ...notebook, gpuType: e.target.value })}
                    placeholder="nvidia.com/gpu"
                  />
                </div>
              </div>
            </Section>
          </TabsContent>

          {/* LLM-specific */}
          <TabsContent value="llm">
            <Section title="LLM Inference Configuration">
              <div className="space-y-4">
                <div className="space-y-1.5">
                  <Label className={LABEL_CLS}>Model Source *</Label>
                  <Input
                    className={INPUT_CLS}
                    value={llm.modelSource}
                    onChange={(e) => setLlm({ ...llm, modelSource: e.target.value })}
                    placeholder="meta-llama/Llama-2-7b-chat-hf"
                  />
                  <p className="text-[11px] text-muted-foreground">
                    HuggingFace model ID or MinIO path (s3://bucket/model)
                  </p>
                </div>
                <div className="space-y-1.5">
                  <Label className={LABEL_CLS}>vLLM Serve Parameters</Label>
                  <Textarea
                    className={`${INPUT_CLS} font-mono`}
                    value={llm.vllmParams}
                    onChange={(e) => setLlm({ ...llm, vllmParams: e.target.value })}
                    placeholder="--max-model-len 4096 --tensor-parallel-size 2"
                    rows={3}
                  />
                  <p className="text-[11px] text-muted-foreground">
                    Extra CLI flags passed to <code className="font-mono">vllm serve</code>
                  </p>
                </div>
                <div className="w-1/3 space-y-1.5">
                  <Label className={LABEL_CLS}>Replica Count</Label>
                  <Input
                    className={INPUT_CLS}
                    type="number"
                    min={1}
                    value={llm.replicaCount}
                    onChange={(e) => setLlm({ ...llm, replicaCount: +e.target.value })}
                  />
                </div>
              </div>
            </Section>

            <Section title="Environment Variables">
              {llm.envVars.length > 0 && (
                <div className="mb-3 space-y-2">
                  <div className="grid grid-cols-[1fr_1fr_2rem] gap-2">
                    <span className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">Key</span>
                    <span className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">Value</span>
                    <span />
                  </div>
                  {llm.envVars.map((ev, i) => (
                    <div key={i} className="grid grid-cols-[1fr_1fr_2rem] items-center gap-2">
                      <Input
                        className="bg-secondary font-mono text-[13px] text-blue-600 focus-visible:ring-sky-500/40 dark:text-blue-400"
                        value={ev.key}
                        onChange={(e) => updateEnvVar(i, 'key', e.target.value)}
                        placeholder="ENV_KEY"
                      />
                      <Input
                        className={INPUT_CLS}
                        value={ev.value}
                        onChange={(e) => updateEnvVar(i, 'value', e.target.value)}
                        placeholder="value"
                      />
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 shrink-0 text-muted-foreground hover:text-destructive"
                        onClick={() => removeEnvVar(i)}
                      >
                        <X className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  ))}
                </div>
              )}
              <button
                type="button"
                onClick={addEnvVar}
                className="flex w-full items-center justify-center gap-1.5 rounded-lg border border-dashed border-border py-2.5 text-[12px] text-muted-foreground transition-colors hover:border-foreground/30 hover:text-foreground"
              >
                <Plus className="h-3.5 w-3.5" />
                Add variable
              </button>
            </Section>
          </TabsContent>
        </Tabs>

        {error && <p className="mt-2 text-sm text-destructive">{error}</p>}

        <div className="mt-2 flex justify-end gap-3 pb-10">
          <Button variant="outline" onClick={() => navigate('/workloads')}>
            Cancel
          </Button>
          {tab === 'notebook' ? (
            <Button
              onClick={handleSubmit}
              disabled={createWorkload.isPending || !isNotebookValid}
            >
              {createWorkload.isPending ? 'Launching...' : 'Launch Notebook →'}
            </Button>
          ) : (
            <Button
              onClick={handleSubmit}
              disabled={createWorkload.isPending || !isLlmValid}
            >
              {createWorkload.isPending ? 'Deploying...' : 'Deploy inference service →'}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
