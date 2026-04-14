import client from '../client';
import type { CreatePolicyRequest, PolicyDto, UpdatePolicyRequest } from '../types';

const URL = '/policies';

export const policyService = {
  getAll: () => client.get<PolicyDto[]>(URL).then((r) => r.data),
  getById: (id: string) => client.get<PolicyDto>(`${URL}/${id}`).then((r) => r.data),
  create: (data: CreatePolicyRequest) => client.post<PolicyDto>(URL, data).then((r) => r.data),
  update: (id: string, data: UpdatePolicyRequest) =>
    client.put<PolicyDto>(`${URL}/${id}`, data).then((r) => r.data),
  delete: (id: string) => client.delete(`${URL}/${id}`),
};
