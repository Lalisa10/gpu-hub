import client from '../client';
import type { ClusterDto, JoinClusterRequest, PatchClusterRequest } from '../types';

const URL = '/clusters';

export const clusterService = {
  getAll: () => client.get<ClusterDto[]>(URL).then((r) => r.data),
  getById: (id: string) => client.get<ClusterDto>(`${URL}/${id}`).then((r) => r.data),
  create: (data: JoinClusterRequest) => client.post<ClusterDto>(URL, data).then((r) => r.data),
  update: (id: string, data: JoinClusterRequest) =>
    client.put<ClusterDto>(`${URL}/${id}`, data).then((r) => r.data),
  patch: (id: string, data: PatchClusterRequest) =>
    client.patch<ClusterDto>(`${URL}/${id}`, data).then((r) => r.data),
  delete: (id: string) => client.delete(`${URL}/${id}`),
  uploadKubeconfig: (id: string, file: File) => {
    const form = new FormData();
    form.append('file', file);
    // Do NOT set Content-Type manually — Axios sets multipart/form-data with boundary automatically
    return client.post<ClusterDto>(`${URL}/${id}/kubeconfig`, form).then((r) => r.data);
  },
};
