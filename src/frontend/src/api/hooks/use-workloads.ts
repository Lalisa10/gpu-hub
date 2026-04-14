import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workloadService } from '../services/workloads';
import type { CreateWorkloadRequest } from '../types';

const KEYS = {
  all: ['workloads'] as const,
  detail: (id: string) => ['workloads', id] as const,
};

export const useWorkloads = () =>
  useQuery({ queryKey: KEYS.all, queryFn: workloadService.getAll, refetchInterval: 10_000 });

export const useWorkload = (id: string) =>
  useQuery({
    queryKey: KEYS.detail(id),
    queryFn: () => workloadService.getById(id),
    enabled: !!id,
    refetchInterval: 5_000,
  });

export const useCreateWorkload = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateWorkloadRequest) => workloadService.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const usePatchWorkload = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Record<string, unknown> }) =>
      workloadService.patch(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useDeleteWorkload = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => workloadService.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};
