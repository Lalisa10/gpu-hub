import { Badge } from './ui/badge';
import type { WorkloadStatus } from '@/lib/types';

export function StatusBadge({ status }: { status: WorkloadStatus }) {
  if (status === 'Running') return <Badge variant="success">{status}</Badge>;
  if (status === 'Pending') return <Badge variant="warning">{status}</Badge>;
  if (status === 'Succeeded') return <Badge variant="info">{status}</Badge>;
  if (status === 'Failed') return <Badge variant="danger">{status}</Badge>;
  return <Badge>{status}</Badge>;
}
