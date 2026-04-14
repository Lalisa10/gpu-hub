import client from '../client';
import type {
  CreateTeamMemberRequest,
  TeamMemberDto,
  UpdateTeamMemberRequest,
} from '../types';

const URL = '/team-members';

export const teamMemberService = {
  getAll: () => client.get<TeamMemberDto[]>(URL).then((r) => r.data),
  getById: (teamId: string, userId: string) =>
    client.get<TeamMemberDto>(`${URL}/${teamId}/${userId}`).then((r) => r.data),
  create: (data: CreateTeamMemberRequest) =>
    client.post<TeamMemberDto>(URL, data).then((r) => r.data),
  update: (teamId: string, userId: string, data: UpdateTeamMemberRequest) =>
    client.put<TeamMemberDto>(`${URL}/${teamId}/${userId}`, data).then((r) => r.data),
  delete: (teamId: string, userId: string) => client.delete(`${URL}/${teamId}/${userId}`),
};
