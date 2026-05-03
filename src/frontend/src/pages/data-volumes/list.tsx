import { PageHeader } from '@/components/shared/page-header';
import { DataTable } from '@/components/shared/data-table';
import { Badge } from '@/components/ui/badge';
import { useMyDataVolumes } from '@/api/hooks/use-data-volumes';
import { useTeams } from '@/api/hooks/use-teams';
import { useClusters } from '@/api/hooks/use-clusters';
import type { DataVolumeDto } from '@/api/types';
import type { Column } from '@/components/shared/data-table';

export default function DataVolumesListPage() {
  const { data: volumes = [], isLoading } = useMyDataVolumes();
  const { data: teams = [] } = useTeams();
  const { data: clusters = [] } = useClusters();

  const teamName = (id: string) => teams.find((t) => t.id === id)?.name ?? '—';
  const clusterName = (id: string) => clusters.find((c) => c.id === id)?.name ?? '—';

  const columns: Column<DataVolumeDto>[] = [
    { header: 'PVC Name', accessor: 'pvcName' },
    { header: 'Team', accessor: (v) => teamName(v.teamId) },
    { header: 'Cluster', accessor: (v) => clusterName(v.clusterId) },
    {
      header: 'Type',
      accessor: (v) => (
        <Badge
          className={
            v.volumeType === 'source'
              ? 'bg-blue-100 text-blue-800'
              : 'bg-gray-100 text-gray-700'
          }
        >
          {v.volumeType}
        </Badge>
      ),
    },
    {
      header: 'Created',
      accessor: (v) => new Date(v.createdAt).toLocaleString(),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Data Volumes"
        description="Persistent volumes available to your teams. Source volumes are auto-provisioned by Data Sources; dynamic volumes are reserved for ad-hoc workload storage."
      />

      <DataTable columns={columns} data={volumes} isLoading={isLoading} />
    </div>
  );
}
