import { Badge } from '@/components/ui/badge';
import type { ClusterStatus, DataSourceStatus, WorkloadStatus } from '@/api/types';

const workloadVariants: Record<WorkloadStatus, string> = {
  pending: 'bg-yellow-100 text-yellow-800',
  running: 'bg-green-100 text-green-800',
  succeeded: 'bg-emerald-100 text-emerald-800',
  failed: 'bg-red-100 text-red-800',
  preempted: 'bg-orange-100 text-orange-800',
  cancelled: 'bg-gray-100 text-gray-800',
};

const clusterVariants: Record<ClusterStatus, string> = {
  ACTIVE: 'bg-black text-white',
  INACTIVE: 'bg-gray-200 text-gray-600',
  MAINTENANCE: 'bg-red-100 text-red-700',
};

const dataSourceVariants: Record<DataSourceStatus, string> = {
  formating: 'bg-amber-100 text-amber-800',
  formated: 'bg-emerald-100 text-emerald-800',
};

export function WorkloadStatusBadge({ status }: { status: WorkloadStatus }) {
  return <Badge className={workloadVariants[status]}>{status}</Badge>;
}

export function ClusterStatusBadge({ status }: { status: ClusterStatus }) {
  return <Badge className={clusterVariants[status]}>{status.toLowerCase()}</Badge>;
}

export function DataSourceStatusBadge({ status }: { status: DataSourceStatus }) {
  return <Badge className={dataSourceVariants[status]}>{status}</Badge>;
}
