import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { clusterService } from '../services/clusters';
import type { JoinClusterRequest, PatchClusterRequest } from '../types';

const KEYS = { all: ['clusters'] as const, detail: (id: string) => ['clusters', id] as const };

export const useClusters = () =>
  useQuery({ queryKey: KEYS.all, queryFn: clusterService.getAll });

export const useCluster = (id: string) =>
  useQuery({ queryKey: KEYS.detail(id), queryFn: () => clusterService.getById(id), enabled: !!id });

export const useCreateCluster = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: JoinClusterRequest) => clusterService.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useUpdateCluster = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: JoinClusterRequest }) =>
      clusterService.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const usePatchCluster = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: PatchClusterRequest }) =>
      clusterService.patch(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useDeleteCluster = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => clusterService.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useUploadKubeconfig = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, file }: { id: string; file: File }) =>
      clusterService.uploadKubeconfig(id, file),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};
