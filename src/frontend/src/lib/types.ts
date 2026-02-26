export type Role = 'USER' | 'ADMIN';

export interface User {
  id: string;
  username: string;
  email: string;
  role: Role;
  active: boolean;
  teamId: string;
  createdAt: string;
}

export type WorkloadType = 'infer' | 'train' | 'research';
export type WorkloadStatus =
  | 'Pending'
  | 'Running'
  | 'Succeeded'
  | 'Failed'
  | 'Stopped'
  | 'Preempted';

export interface QuotaViolation {
  limit: 'maxRunningWorkloads' | 'maxGPU' | 'maxGPUByModel' | 'maxCPU' | 'maxMemoryGB';
  scope: 'TEAM' | 'USER';
  message: string;
  suggestion: string;
}

export interface Workload {
  id: string;
  name: string;
  type: WorkloadType;
  ownerId: string;
  ownerName: string;
  teamId: string;
  teamName: string;
  status: WorkloadStatus;
  createdAt: string;
  startedAt?: string;
  finishedAt?: string;
  resources: {
    gpu: number;
    gpuModel?: string;
    cpu: number;
    memGB: number;
  };
  runtime: {
    modelId?: string;
    configValues?: Record<string, unknown>;
    environmentId?: string;
    command?: string;
    configSchemaId?: string;
  };
  node?: string;
  gpuIds?: string[];
  usage: {
    gpuUtil?: number;
    gpuMem?: number;
  };
  notebookUrl?: string;
  quotaViolations?: QuotaViolation[];
}

export interface Model {
  id: string;
  name: string;
  family: 'LLaMA' | 'Qwen' | 'DeepSeek' | 'Custom';
  size: string;
  formats: Array<'hf' | 'gguf' | 'awq' | 'gptq'>;
  defaultConfigId: string;
  enabled: boolean;
}

export interface ConfigParameter {
  key: string;
  type: 'string' | 'number' | 'boolean' | 'enum';
  required: boolean;
  default?: string | number | boolean;
  description: string;
  enumValues?: string[];
}

export interface LLMConfigSchema {
  id: string;
  name: string;
  description: string;
  target: 'vllm';
  parameters: ConfigParameter[];
  version: number;
  active: boolean;
}

export interface Environment {
  id: string;
  name: string;
  type: 'notebook' | 'container';
  image: string;
  description: string;
  enabled: boolean;
}

export interface GPU {
  id: string;
  nodeName: string;
  model: string;
  memoryGB: number;
  allocated: boolean;
  workloadId?: string;
  owner?: string;
  utilization?: number;
  memUsedGB?: number;
}

export interface Team {
  id: string;
  name: string;
  description: string;
  ownerId: string;
  createdAt: string;
  active: boolean;
}

export interface TeamMember {
  teamId: string;
  userId: string;
  role: 'ADMIN' | 'MEMBER';
  joinedAt: string;
}

export type QuotaScope = 'TEAM' | 'USER';

export interface Quota {
  id: string;
  scope: QuotaScope;
  scopeId: string;
  limits: {
    maxRunningWorkloads: number;
    maxGPU: number;
    maxGPUByModel?: Record<string, number>;
    maxCPU: number;
    maxMemoryGB: number;
  };
  burst: {
    allowBurst: boolean;
    burstGPU?: number;
  };
  enforced: boolean;
  updatedAt: string;
}

export interface QuotaUsage {
  runningWorkloads: number;
  usedGPU: number;
  usedCPU: number;
  usedMemoryGB: number;
}

export interface EffectiveQuotaView {
  team: Team;
  userId: string;
  teamQuota: Quota | null;
  userQuota: Quota | null;
  effectiveLimits: Quota['limits'];
  effectiveBurst: Quota['burst'];
  usage: QuotaUsage;
}

export interface TeamDetailResponse {
  team: Team;
  members: Array<TeamMember & { username: string; email: string; userRole: Role; active: boolean }>;
  activeWorkloads: Workload[];
  quota: Quota | null;
  usage: QuotaUsage;
}

export interface TeamSummary extends Team {
  membersCount: number;
  activeWorkloads: number;
  usedGPU: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}
