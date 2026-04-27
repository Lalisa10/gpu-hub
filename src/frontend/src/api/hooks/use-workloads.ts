import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workloadService } from '../services/workloads';
import type { CreateWorkloadRequest, PodDto, WorkloadDto } from '../types';

const KEYS = {
  all: ['workloads'] as const,
  detail: (id: string) => ['workloads', id] as const,
  pods: (id: string) => ['workloads', id, 'pods'] as const,
  logs: (id: string, podName: string) => ['workloads', id, 'pods', podName, 'logs'] as const,
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

export const useCancelWorkload = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => workloadService.cancel(id),
    onSuccess: (_data, id) => {
      qc.invalidateQueries({ queryKey: KEYS.all });
      qc.invalidateQueries({ queryKey: KEYS.detail(id) });
    },
  });
};

export const useDeleteWorkload = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => workloadService.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useWorkloadPods = (id: string | null) =>
  useQuery({
    queryKey: KEYS.pods(id ?? ''),
    queryFn: () => workloadService.getPods(id!),
    enabled: !!id,
    refetchInterval: 5_000,
  });

export const useWorkloadPodLogs = (id: string | null, podName: string | null) =>
  useQuery({
    queryKey: KEYS.logs(id ?? '', podName ?? ''),
    queryFn: () => workloadService.getPodLogs(id!, podName!),
    enabled: !!id && !!podName,
    refetchInterval: 5_000,
  });

function makeSseHook<T>(buildUrl: (id: string) => string) {
  return function useSseStream(id: string | null, enabled: boolean) {
    const [data, setData] = useState<T | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [isError, setIsError] = useState(false);

    useEffect(() => {
      if (!enabled || !id) { setData(null); return; }
      const token = localStorage.getItem('access_token');
      const ctrl = new AbortController();
      setIsLoading(true);
      setIsError(false);

      (async () => {
        try {
          const res = await fetch(buildUrl(id), {
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
                try { setData(JSON.parse(line.slice(5).trim())); } catch { /* skip malformed */ }
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
  };
}

export const useWorkloadStatusStream = makeSseHook<WorkloadDto>(
  (id) => `/api/workloads/${id}/status/stream`,
);

export const useWorkloadPodsStream = makeSseHook<PodDto[]>(
  (id) => `/api/workloads/${id}/pods/stream`,
);

export function useWorkloadPodLogsStream(
  workloadId: string | null,
  podName: string | null,
  enabled: boolean,
) {
  const [data, setData] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isError, setIsError] = useState(false);

  useEffect(() => {
    if (!enabled || !workloadId || !podName) { setData(null); return; }
    const token = localStorage.getItem('access_token');
    const ctrl = new AbortController();
    setIsLoading(true);
    setIsError(false);

    (async () => {
      try {
        const res = await fetch(`/api/workloads/${workloadId}/pods/${podName}/logs/stream`, {
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
              try { setData(JSON.parse(line.slice(5).trim())); } catch { /* skip malformed */ }
            }
          }
        }
      } catch (e) {
        if ((e as Error).name !== 'AbortError') setIsError(true);
        setIsLoading(false);
      }
    })();

    return () => ctrl.abort();
  }, [workloadId, podName, enabled]);

  return { data, isLoading, isError };
}
