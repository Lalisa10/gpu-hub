import { useQuery } from '@tanstack/react-query';
import { dataVolumeService } from '../services/data-volumes';

const KEYS = {
  all: ['data-volumes'] as const,
  mine: ['data-volumes', 'my'] as const,
  byTeam: (teamId: string) => ['data-volumes', { teamId }] as const,
  detail: (id: string) => ['data-volumes', id] as const,
};

export const useDataVolumes = (teamId?: string) =>
  useQuery({
    queryKey: teamId ? KEYS.byTeam(teamId) : KEYS.all,
    queryFn: () => dataVolumeService.getAll(teamId),
    enabled: !!teamId,
  });

export const useMyDataVolumes = () =>
  useQuery({ queryKey: KEYS.mine, queryFn: dataVolumeService.getMine });

export const useDataVolume = (id: string) =>
  useQuery({
    queryKey: KEYS.detail(id),
    queryFn: () => dataVolumeService.getById(id),
    enabled: !!id,
  });
