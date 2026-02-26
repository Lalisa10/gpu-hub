import type {
  Environment,
  GPU,
  LLMConfigSchema,
  Model,
  Quota,
  Team,
  TeamMember,
  User,
  Workload,
} from '@/lib/types';

const now = Date.now();
const ago = (h: number) => new Date(now - h * 60 * 60 * 1000).toISOString();

export const teams: Team[] = [
  {
    id: 't-platform',
    name: 'Platform Ops',
    description: 'Cluster administration and platform services.',
    ownerId: 'u-admin',
    createdAt: ago(1200),
    active: true,
  },
  {
    id: 't-ml-apps',
    name: 'ML Apps',
    description: 'Product inference and model experimentation.',
    ownerId: 'u-alice',
    createdAt: ago(900),
    active: true,
  },
  {
    id: 't-research',
    name: 'Research Sandbox',
    description: 'Ad-hoc exploratory experiments.',
    ownerId: 'u-bob',
    createdAt: ago(400),
    active: false,
  },
];

export const users: User[] = [
  {
    id: 'u-admin',
    username: 'admin',
    email: 'admin@gpuhub.local',
    role: 'ADMIN',
    active: true,
    teamId: 't-platform',
    createdAt: ago(720),
  },
  {
    id: 'u-alice',
    username: 'alice',
    email: 'alice@gpuhub.local',
    role: 'USER',
    active: true,
    teamId: 't-ml-apps',
    createdAt: ago(480),
  },
  {
    id: 'u-bob',
    username: 'bob',
    email: 'bob@gpuhub.local',
    role: 'USER',
    active: true,
    teamId: 't-ml-apps',
    createdAt: ago(300),
  },
];

export const teamMembers: TeamMember[] = [
  { teamId: 't-platform', userId: 'u-admin', role: 'ADMIN', joinedAt: ago(700) },
  { teamId: 't-ml-apps', userId: 'u-alice', role: 'ADMIN', joinedAt: ago(450) },
  { teamId: 't-ml-apps', userId: 'u-bob', role: 'MEMBER', joinedAt: ago(200) },
];

export const models: Model[] = [
  {
    id: 'm-llama-8b',
    name: 'Llama-3.1-8B-Instruct',
    family: 'LLaMA',
    size: '8B',
    formats: ['hf', 'awq'],
    defaultConfigId: 'cfg-vllm-default-v2',
    enabled: true,
  },
  {
    id: 'm-qwen-14b',
    name: 'Qwen2.5-14B',
    family: 'Qwen',
    size: '14B',
    formats: ['hf', 'gguf'],
    defaultConfigId: 'cfg-vllm-fast-v1',
    enabled: true,
  },
  {
    id: 'm-custom',
    name: 'Custom-Research-7B',
    family: 'Custom',
    size: '7B',
    formats: ['hf'],
    defaultConfigId: 'cfg-vllm-default-v2',
    enabled: false,
  },
];

export const llmConfigs: LLMConfigSchema[] = [
  {
    id: 'cfg-vllm-default-v2',
    name: 'vLLM Balanced',
    description: 'Balanced latency and memory footprint for general inference.',
    target: 'vllm',
    version: 2,
    active: true,
    parameters: [
      {
        key: 'tensor_parallel_size',
        type: 'number',
        required: true,
        default: 1,
        description: 'Number of GPUs to shard tensor weights across.',
      },
      {
        key: 'max_model_len',
        type: 'number',
        required: true,
        default: 4096,
        description: 'Maximum context window.',
      },
      {
        key: 'gpu_memory_utilization',
        type: 'number',
        required: true,
        default: 0.9,
        description: 'Fraction of GPU memory allocator may reserve.',
      },
      {
        key: 'enable_chunked_prefill',
        type: 'boolean',
        required: false,
        default: false,
        description: 'Enable chunked prefill for long prompts.',
      },
    ],
  },
  {
    id: 'cfg-vllm-fast-v1',
    name: 'vLLM Low-Latency',
    description: 'Lower throughput but optimized first-token latency.',
    target: 'vllm',
    version: 1,
    active: true,
    parameters: [
      {
        key: 'tensor_parallel_size',
        type: 'number',
        required: true,
        default: 2,
        description: 'Parallelism for model execution.',
      },
      {
        key: 'dtype',
        type: 'enum',
        required: true,
        default: 'bfloat16',
        description: 'Execution precision.',
        enumValues: ['float16', 'bfloat16', 'float32'],
      },
    ],
  },
];

export const environments: Environment[] = [
  {
    id: 'env-jupyter-pytorch',
    name: 'Jupyter + PyTorch 2.4',
    type: 'notebook',
    image: 'nvcr.io/nvidia/pytorch:24.09-py3',
    description: 'Notebook environment with CUDA and PyTorch tooling.',
    enabled: true,
  },
  {
    id: 'env-container-base',
    name: 'Base Inference Container',
    type: 'container',
    image: 'ghcr.io/gpu-hub/inference:latest',
    description: 'Minimal container for model serving workloads.',
    enabled: true,
  },
];

export const workloads: Workload[] = [
  {
    id: 'w-101',
    name: 'Customer Support Inference',
    type: 'infer',
    ownerId: 'u-alice',
    ownerName: 'alice',
    teamId: 't-ml-apps',
    teamName: 'ML Apps',
    status: 'Running',
    createdAt: ago(6),
    startedAt: ago(5),
    resources: { gpu: 1, gpuModel: 'NVIDIA A100', cpu: 8, memGB: 32 },
    runtime: {
      modelId: 'm-llama-8b',
      configSchemaId: 'cfg-vllm-default-v2',
      configValues: {
        tensor_parallel_size: 1,
        max_model_len: 4096,
        gpu_memory_utilization: 0.9,
        enable_chunked_prefill: false,
      },
    },
    node: 'node-01',
    gpuIds: ['gpu-1'],
    usage: { gpuUtil: 78, gpuMem: 29 },
  },
  {
    id: 'w-102',
    name: 'Qwen Fine-Tune',
    type: 'train',
    ownerId: 'u-bob',
    ownerName: 'bob',
    teamId: 't-ml-apps',
    teamName: 'ML Apps',
    status: 'Pending',
    createdAt: ago(1),
    resources: { gpu: 2, gpuModel: 'NVIDIA H100', cpu: 16, memGB: 64 },
    runtime: { command: 'python train.py --epochs 3' },
    usage: {},
    quotaViolations: [
      {
        limit: 'maxGPU',
        scope: 'USER',
        message: 'User GPU override limit exceeded (2 requested, limit 1).',
        suggestion: 'Reduce GPU count or request higher user quota from admin.',
      },
    ],
  },
  {
    id: 'w-103',
    name: 'Tokenization Research',
    type: 'research',
    ownerId: 'u-admin',
    ownerName: 'admin',
    teamId: 't-platform',
    teamName: 'Platform Ops',
    status: 'Succeeded',
    createdAt: ago(20),
    startedAt: ago(19),
    finishedAt: ago(2),
    resources: { gpu: 1, cpu: 8, memGB: 24 },
    runtime: { environmentId: 'env-jupyter-pytorch' },
    notebookUrl: 'https://notebook.gpuhub.local/lab/tree/w-103',
    usage: { gpuUtil: 55, gpuMem: 18 },
  },
];

export const quotas: Quota[] = [
  {
    id: 'q-team-ml-apps',
    scope: 'TEAM',
    scopeId: 't-ml-apps',
    limits: {
      maxRunningWorkloads: 3,
      maxGPU: 3,
      maxGPUByModel: {
        'NVIDIA H100': 1,
        'NVIDIA A100': 3,
      },
      maxCPU: 32,
      maxMemoryGB: 128,
    },
    burst: { allowBurst: true, burstGPU: 1 },
    enforced: true,
    updatedAt: ago(3),
  },
  {
    id: 'q-team-platform',
    scope: 'TEAM',
    scopeId: 't-platform',
    limits: {
      maxRunningWorkloads: 5,
      maxGPU: 4,
      maxCPU: 64,
      maxMemoryGB: 256,
    },
    burst: { allowBurst: false },
    enforced: true,
    updatedAt: ago(4),
  },
  {
    id: 'q-user-alice',
    scope: 'USER',
    scopeId: 'u-alice',
    limits: {
      maxRunningWorkloads: 2,
      maxGPU: 1,
      maxGPUByModel: {
        'NVIDIA H100': 0,
      },
      maxCPU: 16,
      maxMemoryGB: 64,
    },
    burst: { allowBurst: false },
    enforced: true,
    updatedAt: ago(2),
  },
];

export const gpus: GPU[] = [
  {
    id: 'gpu-1',
    nodeName: 'node-01',
    model: 'NVIDIA A100',
    memoryGB: 40,
    allocated: true,
    workloadId: 'w-101',
    owner: 'alice',
    utilization: 78,
    memUsedGB: 29,
  },
  {
    id: 'gpu-2',
    nodeName: 'node-01',
    model: 'NVIDIA A100',
    memoryGB: 40,
    allocated: false,
    utilization: 0,
    memUsedGB: 0,
  },
  {
    id: 'gpu-3',
    nodeName: 'node-02',
    model: 'NVIDIA H100',
    memoryGB: 80,
    allocated: true,
    workloadId: 'w-102',
    owner: 'bob',
    utilization: 85,
    memUsedGB: 64,
  },
];
