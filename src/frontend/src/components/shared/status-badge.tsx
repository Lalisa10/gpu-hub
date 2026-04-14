import { Badge } from '@/components/ui/badge';
import type { ClusterStatus, WorkloadStatus } from '@/api/types';

const workloadVariants: Record<WorkloadStatus, string> = {
  pending: 'bg-yellow-100 text-yellow-800',
  queued: 'bg-blue-100 text-blue-800',
  running: 'bg-green-100 text-green-800',
  succeeded: 'bg-emerald-100 text-emerald-800',
  failed: 'bg-red-100 text-red-800',
  preempted: 'bg-orange-100 text-orange-800',
  cancelled: 'bg-gray-100 text-gray-800',
};

const clusterVariants: Record<ClusterStatus, string> = {
  ACTIVE: 'bg-green-100 text-green-800',
  INACTIVE: 'bg-gray-100 text-gray-800',
  MAINTENANCE: 'bg-yellow-100 text-yellow-800',
};

export function WorkloadStatusBadge({ status }: { status: WorkloadStatus }) {
  return <Badge className={workloadVariants[status]}>{status}</Badge>;
}

export function ClusterStatusBadge({ status }: { status: ClusterStatus }) {
  return <Badge className={clusterVariants[status]}>{status.toLowerCase()}</Badge>;
}
