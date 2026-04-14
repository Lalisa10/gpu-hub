import client from '../client';
import type { CreateTeamRequest, TeamDto, UpdateTeamRequest } from '../types';

const URL = '/teams';

export const teamService = {
  getAll: () => client.get<TeamDto[]>(URL).then((r) => r.data),
  getById: (id: string) => client.get<TeamDto>(`${URL}/${id}`).then((r) => r.data),
  create: (data: CreateTeamRequest) => client.post<TeamDto>(URL, data).then((r) => r.data),
  update: (id: string, data: UpdateTeamRequest) =>
    client.put<TeamDto>(`${URL}/${id}`, data).then((r) => r.data),
  delete: (id: string) => client.delete(`${URL}/${id}`),
};
