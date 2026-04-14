import client from '../client';
import type { CreateUserRequest, PatchUserRequest, UpdateUserRequest, UserDto } from '../types';

const URL = '/users';

export const userService = {
  getAll: () => client.get<UserDto[]>(URL).then((r) => r.data),
  getById: (id: string) => client.get<UserDto>(`${URL}/${id}`).then((r) => r.data),
  create: (data: CreateUserRequest) => client.post<UserDto>(URL, data).then((r) => r.data),
  update: (id: string, data: UpdateUserRequest) =>
    client.put<UserDto>(`${URL}/${id}`, data).then((r) => r.data),
  patch: (id: string, data: PatchUserRequest) =>
    client.patch<UserDto>(`${URL}/${id}`, data).then((r) => r.data),
  delete: (id: string) => client.delete(`${URL}/${id}`),
};
