import client from '../client';
import type { CreateProjectRequest, ProjectDto, UpdateProjectRequest } from '../types';

const URL = '/projects';

export const projectService = {
  getAll: () => client.get<ProjectDto[]>(URL).then((r) => r.data),
  getById: (id: string) => client.get<ProjectDto>(`${URL}/${id}`).then((r) => r.data),
  create: (data: CreateProjectRequest) => client.post<ProjectDto>(URL, data).then((r) => r.data),
  update: (id: string, data: UpdateProjectRequest) =>
    client.put<ProjectDto>(`${URL}/${id}`, data).then((r) => r.data),
  delete: (id: string) => client.delete(`${URL}/${id}`),
};
