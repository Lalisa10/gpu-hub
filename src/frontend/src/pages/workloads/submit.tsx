import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/shared/page-header';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useAuth } from '@/contexts/auth-context';
import { useCreateWorkload } from '@/api/hooks/use-workloads';
import { useProjects } from '@/api/hooks/use-projects';
import { useTeams } from '@/api/hooks/use-teams';
import { useClusters } from '@/api/hooks/use-clusters';

interface NotebookExtra {
  dockerImage: string;
  pvcSize: string;
  gpuType: string;
}

interface LlmInferenceExtra {
  modelSource: string;
  vllmParams: string;
  replicaCount: number;
  nodePort: number;
  apiKey: string;
}

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
  const [gpuCount, setGpuCount] = useState(1);
  const [cpuRequest, setCpuRequest] = useState(2);
  const [cpuLimit, setCpuLimit] = useState(4);
  const [memRequest, setMemRequest] = useState(4096);
  const [memLimit, setMemLimit] = useState(8192);
  const [error, setError] = useState('');

  const [notebook, setNotebook] = useState<NotebookExtra>({
    dockerImage: 'jupyter/scipy-notebook:latest',
    pvcSize: '10Gi',
    gpuType: '',
  });

  const [llm, setLlm] = useState<LlmInferenceExtra>({
    modelSource: '',
    vllmParams: '',
    replicaCount: 1,
    nodePort: 30080,
    apiKey: '',
  });

  // Filter projects the user has access to
  const myTeamIds = isAdmin
    ? teams.map((t) => t.id)
    : teamMemberships.map((m) => m.teamId);
  const accessibleProjects = projects.filter((p) => myTeamIds.includes(p.teamId));

  const selectedProject = projects.find((p) => p.id === projectId);

  const handleSubmit = async () => {
    if (!projectId || !name || !user) return;
    setError('');

    const extra =
      tab === 'notebook'
        ? JSON.stringify(notebook)
        : JSON.stringify(llm);

    try {
      await createWorkload.mutateAsync({
        projectId,
        clusterId: selectedProject!.clusterId,
        submittedById: user.id,
        workloadType: tab === 'notebook' ? 'notebook' : 'llm_inference',
        priorityClass: tab === 'notebook' ? 'train' : 'inference',
        name,
        requestedGpu: gpuCount,
        requestedCpu: cpuRequest,
        requestedCpuLimit: cpuLimit,
        requestedMemory: memRequest,
        requestedMemoryLimit: memLimit,
        status: 'pending',
        extra,
      });
      navigate('/workloads');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to submit workload';
      setError(msg);
    }
  };

  return (
    <div>
      <PageHeader title="Submit Workload" description="Launch a notebook or LLM inference service" />

      <div className="mx-auto max-w-2xl">
        <Tabs value={tab} onValueChange={setTab}>
          <TabsList className="mb-4 w-full">
            <TabsTrigger value="notebook" className="flex-1">
              Notebook
            </TabsTrigger>
            <TabsTrigger value="llm" className="flex-1">
              LLM Inference
            </TabsTrigger>
          </TabsList>

          {/* Common fields */}
          <Card className="mb-4">
            <CardHeader>
              <CardTitle className="text-base">General</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Workload Name</Label>
                  <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="my-workload" />
                </div>
                <div className="space-y-2">
                  <Label>Project</Label>
                  <Select value={projectId} onValueChange={(v) => v && setProjectId(v)}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select project" />
                    </SelectTrigger>
                    <SelectContent>
                      {accessibleProjects.map((p) => {
                        const team = teams.find((t) => t.id === p.teamId);
                        const cluster = clusters.find((c) => c.id === p.clusterId);
                        return (
                          <SelectItem key={p.id} value={p.id}>
                            {p.name} ({team?.name} / {cluster?.name})
                          </SelectItem>
                        );
                      })}
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Resource requests */}
          <Card className="mb-4">
            <CardHeader>
              <CardTitle className="text-base">Resources</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>GPU Count</Label>
                  <Input type="number" min={0} value={gpuCount} onChange={(e) => setGpuCount(+e.target.value)} />
                </div>
                <div className="space-y-2">
                  <Label>CPU Request (cores)</Label>
                  <Input type="number" min={0} step={0.5} value={cpuRequest} onChange={(e) => setCpuRequest(+e.target.value)} />
                </div>
                <div className="space-y-2">
                  <Label>CPU Limit (cores)</Label>
                  <Input type="number" min={0} step={0.5} value={cpuLimit} onChange={(e) => setCpuLimit(+e.target.value)} />
                </div>
                <div className="space-y-2">
                  <Label>Memory Request (MiB)</Label>
                  <Input type="number" min={0} value={memRequest} onChange={(e) => setMemRequest(+e.target.value)} />
                </div>
                <div className="space-y-2">
                  <Label>Memory Limit (MiB)</Label>
                  <Input type="number" min={0} value={memLimit} onChange={(e) => setMemLimit(+e.target.value)} />
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Notebook specific */}
          <TabsContent value="notebook">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Notebook Configuration</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label>Docker Image</Label>
                  <Input
                    value={notebook.dockerImage}
                    onChange={(e) => setNotebook({ ...notebook, dockerImage: e.target.value })}
                    placeholder="jupyter/scipy-notebook:latest"
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>PVC Size</Label>
                    <Input
                      value={notebook.pvcSize}
                      onChange={(e) => setNotebook({ ...notebook, pvcSize: e.target.value })}
                      placeholder="10Gi"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>GPU Type (optional)</Label>
                    <Input
                      value={notebook.gpuType}
                      onChange={(e) => setNotebook({ ...notebook, gpuType: e.target.value })}
                      placeholder="nvidia.com/gpu"
                    />
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* LLM Inference specific */}
          <TabsContent value="llm">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">LLM Inference Configuration</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label>Model Source (HuggingFace ID or MinIO path)</Label>
                  <Input
                    value={llm.modelSource}
                    onChange={(e) => setLlm({ ...llm, modelSource: e.target.value })}
                    placeholder="meta-llama/Llama-2-7b-chat-hf"
                  />
                </div>
                <div className="space-y-2">
                  <Label>vLLM Serve Parameters</Label>
                  <Textarea
                    value={llm.vllmParams}
                    onChange={(e) => setLlm({ ...llm, vllmParams: e.target.value })}
                    placeholder="--max-model-len 4096 --tensor-parallel-size 2"
                  />
                </div>
                <div className="grid grid-cols-3 gap-4">
                  <div className="space-y-2">
                    <Label>Replica Count</Label>
                    <Input
                      type="number"
                      min={1}
                      value={llm.replicaCount}
                      onChange={(e) => setLlm({ ...llm, replicaCount: +e.target.value })}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>NodePort</Label>
                    <Input
                      type="number"
                      value={llm.nodePort}
                      onChange={(e) => setLlm({ ...llm, nodePort: +e.target.value })}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>API Key</Label>
                    <Input
                      value={llm.apiKey}
                      onChange={(e) => setLlm({ ...llm, apiKey: e.target.value })}
                      placeholder="Optional"
                    />
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>

        {error && <p className="mt-4 text-sm text-destructive">{error}</p>}

        <div className="mt-6 flex justify-end gap-3">
          <Button variant="outline" onClick={() => navigate('/workloads')}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} disabled={createWorkload.isPending || !name || !projectId}>
            {createWorkload.isPending ? 'Submitting...' : 'Submit Workload'}
          </Button>
        </div>
      </div>
    </div>
  );
}
