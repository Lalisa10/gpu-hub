import client from '../client';
import type { CreateWorkloadRequest, PodDto, WorkloadDto } from '../types';

const URL = '/workloads';

export const workloadService = {
  getAll: () => client.get<WorkloadDto[]>(URL).then((r) => r.data),
  getById: (id: string) => client.get<WorkloadDto>(`${URL}/${id}`).then((r) => r.data),
  create: (data: CreateWorkloadRequest) =>
    client.post<WorkloadDto>(URL, data).then((r) => r.data),
  patch: (id: string, data: Partial<WorkloadDto>) =>
    client.patch<WorkloadDto>(`${URL}/${id}`, data).then((r) => r.data),
  delete: (id: string) => client.delete(`${URL}/${id}`),
  getPods: (id: string) => client.get<PodDto[]>(`${URL}/${id}/pods`).then((r) => r.data),
  getPodLogs: (id: string, podName: string) =>
    client.get<string>(`${URL}/${id}/pods/${podName}/logs`).then((r) => r.data),
};
