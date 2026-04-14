import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { teamClusterService } from '../services/team-clusters';
import type { CreateTeamClusterRequest, UpdateTeamClusterRequest } from '../types';

const KEYS = { all: ['team-clusters'] as const };

export const useTeamClusters = () =>
  useQuery({ queryKey: KEYS.all, queryFn: teamClusterService.getAll });

export const useCreateTeamCluster = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTeamClusterRequest) => teamClusterService.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useUpdateTeamCluster = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateTeamClusterRequest }) =>
      teamClusterService.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useDeleteTeamCluster = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => teamClusterService.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};
