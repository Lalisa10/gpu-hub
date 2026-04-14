import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { policyService } from '../services/policies';
import type { CreatePolicyRequest, UpdatePolicyRequest } from '../types';

const KEYS = { all: ['policies'] as const, detail: (id: string) => ['policies', id] as const };

export const usePolicies = () =>
  useQuery({ queryKey: KEYS.all, queryFn: policyService.getAll });

export const usePolicy = (id: string) =>
  useQuery({ queryKey: KEYS.detail(id), queryFn: () => policyService.getById(id), enabled: !!id });

export const useCreatePolicy = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreatePolicyRequest) => policyService.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useUpdatePolicy = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdatePolicyRequest }) =>
      policyService.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useDeletePolicy = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => policyService.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};
