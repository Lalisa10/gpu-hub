import { PageHeader } from '@/components/shared/page-header';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useWorkloads } from '@/api/hooks/use-workloads';
import { useClusters } from '@/api/hooks/use-clusters';
import { useTeams } from '@/api/hooks/use-teams';
import { useProjects } from '@/api/hooks/use-projects';
import { useUsers } from '@/api/hooks/use-users';
import { DataTable } from '@/components/shared/data-table';
import { WorkloadStatusBadge } from '@/components/shared/status-badge';
import type { Column } from '@/components/shared/data-table';
import type { WorkloadDto, WorkloadStatus } from '@/api/types';
import { Server, Users, FolderKanban, Play } from 'lucide-react';

export default function DashboardPage() {
  const { data: workloads = [], isLoading: wLoading } = useWorkloads();
  const { data: clusters = [] } = useClusters();
  const { data: teams = [] } = useTeams();
  const { data: projects = [] } = useProjects();
  const { data: users = [] } = useUsers();

  const activeWorkloads = workloads.filter((w) =>
    ['pending', 'queued', 'running'].includes(w.status),
  );

  const stats = [
    { label: 'Clusters', value: clusters.length, icon: Server },
    { label: 'Teams', value: teams.length, icon: Users },
    { label: 'Projects', value: projects.length, icon: FolderKanban },
    { label: 'Active Workloads', value: activeWorkloads.length, icon: Play },
  ];

  const workloadColumns: Column<WorkloadDto>[] = [
    { header: 'Name', accessor: 'name' },
    {
      header: 'Project',
      accessor: (w) => projects.find((p) => p.id === w.projectId)?.name ?? w.projectId.slice(0, 8),
    },
    {
      header: 'Submitted By',
      accessor: (w) =>
        users.find((u) => u.id === w.submittedById)?.username ?? w.submittedById.slice(0, 8),
    },
    {
      header: 'Status',
      accessor: (w) => <WorkloadStatusBadge status={w.status as WorkloadStatus} />,
    },
    { header: 'GPU', accessor: (w) => String(w.requestedGpu) },
    { header: 'CPU', accessor: (w) => String(w.requestedCpu) },
    { header: 'Memory (MiB)', accessor: (w) => String(w.requestedMemory) },
    {
      header: 'Uptime',
      accessor: (w) => {
        if (!w.startedAt) return '-';
        const ms = Date.now() - new Date(w.startedAt).getTime();
        const mins = Math.floor(ms / 60000);
        if (mins < 60) return `${mins}m`;
        return `${Math.floor(mins / 60)}h ${mins % 60}m`;
      },
    },
  ];

  return (
    <div>
      <PageHeader title="Dashboard" description="Platform monitoring overview" />

      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((s) => (
          <Card key={s.label}>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">{s.label}</CardTitle>
              <s.icon className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{s.value}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      <h2 className="mb-3 text-lg font-semibold">Active Workloads</h2>
      <DataTable
        columns={workloadColumns}
        data={activeWorkloads}
        isLoading={wLoading}
        emptyMessage="No active workloads"
      />
    </div>
  );
}
