// ─── Auth ──────────────────────────────────────────────
export interface LoginRequest {
  username: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

// ─── Enums / Constants ─────────────────────────────────
export type GlobalRole = 'ADMIN' | 'USER';
export type TeamRole = 'MEMBER' | 'TEAM_LEAD';
export type ClusterStatus = 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE';
export type WorkloadStatus =
  | 'pending'
  | 'queued'
  | 'running'
  | 'succeeded'
  | 'failed'
  | 'preempted'
  | 'cancelled';

// ─── User ──────────────────────────────────────────────
export interface UserDto {
  id: string;
  username: string;
  email: string;
  fullName: string | null;
  globalRole: GlobalRole;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  fullName?: string;
  password: string;
  globalRole: GlobalRole;
}

export interface UpdateUserRequest {
  username: string;
  email: string;
  fullName?: string;
  password?: string;
  globalRole: GlobalRole;
  isActive: boolean;
}

export interface PatchUserRequest {
  username?: string;
  email?: string;
  fullName?: string;
  password?: string;
  globalRole?: GlobalRole;
  isActive?: boolean;
}

// ─── Cluster ───────────────────────────────────────────
export interface ClusterDto {
  id: string;
  name: string;
  description: string | null;
  status: ClusterStatus;
  createdAt: string;
  updatedAt: string;
}

export interface JoinClusterRequest {
  name: string;
  description?: string;
  kubeconfigRef?: string;
}

export interface PatchClusterRequest {
  name?: string;
  description?: string;
  kubeconfigRef?: string;
  status?: ClusterStatus;
}

// ─── Policy ────────────────────────────────────────────
export interface PolicyDto {
  id: string;
  clusterId: string;
  name: string;
  description: string | null;
  priority: number;
  gpuQuota: number | null;
  cpuQuota: number | null;
  memoryQuota: number | null;
  gpuLimit: number | null;
  cpuLimit: number | null;
  memoryLimit: number | null;
  gpuOverQuotaWeight: number | null;
  cpuOverQuotaWeight: number | null;
  memoryOverQuotaWeight: number | null;
  nodeAffinity: string | null;
  gpuTypes: string[] | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePolicyRequest {
  clusterId: string;
  name: string;
  description?: string;
  priority: number;
  gpuQuota?: number;
  cpuQuota?: number;
  memoryQuota?: number;
  gpuLimit?: number;
  cpuLimit?: number;
  memoryLimit?: number;
  gpuOverQuotaWeight?: number;
  cpuOverQuotaWeight?: number;
  memoryOverQuotaWeight?: number;
  nodeAffinity?: string;
  gpuTypes?: string[];
}

export interface UpdatePolicyRequest extends CreatePolicyRequest {}

// ─── Team ──────────────────────────────────────────────
export interface TeamDto {
  id: string;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTeamRequest {
  name: string;
  description?: string;
}

export interface UpdateTeamRequest {
  name: string;
  description?: string;
}

// ─── Team Member ───────────────────────────────────────
export interface TeamMemberDto {
  userId: string;
  teamId: string;
  role: TeamRole;
  joinedAt: string;
}

export interface CreateTeamMemberRequest {
  userId: string;
  teamId: string;
  role?: TeamRole;
}

export interface UpdateTeamMemberRequest {
  role: TeamRole;
}

// ─── Team Cluster ──────────────────────────────────────
export interface TeamClusterDto {
  id: string;
  teamId: string;
  clusterId: string;
  policyId: string;
  namespace: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTeamClusterRequest {
  teamId: string;
  clusterId: string;
  policyId: string;
  namespace: string;
}

export interface UpdateTeamClusterRequest {
  teamId: string;
  clusterId: string;
  policyId: string;
  namespace: string;
}

// ─── Project ───────────────────────────────────────────
export interface ProjectDto {
  id: string;
  teamId: string;
  clusterId: string;
  policyId: string;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectRequest {
  teamId: string;
  clusterId: string;
  policyId: string;
  name: string;
  description?: string;
}

export interface UpdateProjectRequest extends CreateProjectRequest {}

// ─── Workload ──────────────────────────────────────────
export interface WorkloadDto {
  id: string;
  projectId: string;
  clusterId: string;
  submittedById: string;
  workloadType: string;
  priorityClass: string;
  name: string;
  requestedGpu: number;
  requestedCpu: number;
  requestedCpuLimit: number;
  requestedMemory: number;
  requestedMemoryLimit: number;
  status: WorkloadStatus;
  k8sNamespace: string | null;
  k8sResourceName: string | null;
  k8sResourceKind: string | null;
  queuedAt: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  extra: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateWorkloadRequest {
  projectId: string;
  clusterId: string;
  submittedById: string;
  workloadType: string;
  priorityClass: string;
  name: string;
  requestedGpu: number;
  requestedCpu: number;
  requestedCpuLimit: number;
  requestedMemory: number;
  requestedMemoryLimit: number;
  status: string;
  k8sNamespace?: string;
  k8sResourceName?: string;
  k8sResourceKind?: string;
  extra?: string;
}

// ─── API Error ─────────────────────────────────────────
export interface ApiError {
  status: number;
  error: string;
  message: string;
}

// ─── JWT decoded (minimal) ─────────────────────────────
export interface JwtPayload {
  sub: string;
  username: string;
  role: GlobalRole;
  exp: number;
}
