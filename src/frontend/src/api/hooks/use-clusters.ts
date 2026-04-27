import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { clusterService } from '../services/clusters';
import type { ClusterDetailsDto, JoinClusterRequest, PatchClusterRequest } from '../types';

const KEYS = {
  all: ['clusters'] as const,
  detail: (id: string) => ['clusters', id] as const,
  detailsFull: (id: string) => ['clusters', id, 'details'] as const,
};

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

export const useClusterDetails = (id: string | null, enabled = true) =>
  useQuery({
    queryKey: KEYS.detailsFull(id ?? ''),
    queryFn: () => clusterService.getDetails(id!),
    enabled: enabled && !!id,
    staleTime: 60_000,
  });

export function useClusterDetailsStream(id: string | null, enabled: boolean) {
  const [data, setData] = useState<ClusterDetailsDto | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isError, setIsError] = useState(false);

  useEffect(() => {
    if (!enabled || !id) {
      setData(null);
      return;
    }
    const token = localStorage.getItem('access_token');
    const ctrl = new AbortController();
    setIsLoading(true);
    setIsError(false);

    (async () => {
      try {
        const res = await fetch(`/api/clusters/${id}/details/stream`, {
          headers: { Authorization: `Bearer ${token}` },
          signal: ctrl.signal,
        });
        if (!res.ok || !res.body) throw new Error('SSE connect failed');
        setIsLoading(false);
        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buf = '';
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          buf += decoder.decode(value, { stream: true });
          const lines = buf.split('\n');
          buf = lines.pop() ?? '';
          for (const line of lines) {
            if (line.startsWith('data:')) {
              try {
                setData(JSON.parse(line.slice(5).trim()));
              } catch {
                // malformed event, skip
              }
            }
          }
        }
      } catch (e) {
        if ((e as Error).name !== 'AbortError') setIsError(true);
        setIsLoading(false);
      }
    })();

    return () => ctrl.abort();
  }, [id, enabled]);

  return { data, isLoading, isError };
}
