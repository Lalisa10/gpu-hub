import client from '../client';
import type {
  CreateTeamClusterRequest,
  TeamClusterDto,
  UpdateTeamClusterRequest,
} from '../types';

const URL = '/team-clusters';

export const teamClusterService = {
  getAll: () => client.get<TeamClusterDto[]>(URL).then((r) => r.data),
  getById: (id: string) => client.get<TeamClusterDto>(`${URL}/${id}`).then((r) => r.data),
  create: (data: CreateTeamClusterRequest) =>
    client.post<TeamClusterDto>(URL, data).then((r) => r.data),
  update: (id: string, data: UpdateTeamClusterRequest) =>
    client.put<TeamClusterDto>(`${URL}/${id}`, data).then((r) => r.data),
  delete: (id: string) => client.delete(`${URL}/${id}`),
};
