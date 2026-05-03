import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { dataSourceService } from '../services/data-sources';
import { makeSseHook } from './sse';
import type { CreateDataSourceRequest, DataSourceDto } from '../types';

const KEYS = {
  mine: ['data-sources', 'my'] as const,
  detail: (id: string) => ['data-sources', id] as const,
};

export const useMyDataSources = () =>
  useQuery({ queryKey: KEYS.mine, queryFn: dataSourceService.getMine });

export const useDataSource = (id: string) =>
  useQuery({
    queryKey: KEYS.detail(id),
    queryFn: () => dataSourceService.getById(id),
    enabled: !!id,
  });

export const useCreateDataSource = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateDataSourceRequest) => dataSourceService.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['data-sources'] }),
  });
};

export const useDeleteDataSource = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => dataSourceService.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['data-sources'] }),
  });
};

export const useDataSourceStatusStream = makeSseHook<DataSourceDto>(
  (id) => `/api/data-sources/${id}/status/stream`,
);

export const useDataSourceJobLogsStream = makeSseHook<string>(
  (id) => `/api/data-sources/${id}/job-logs/stream`,
  { parse: (s) => s },
);
