import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { userService } from '../services/users';
import type { CreateUserRequest, PatchUserRequest, UpdateUserRequest } from '../types';

const KEYS = { all: ['users'] as const, detail: (id: string) => ['users', id] as const };

export const useUsers = () =>
  useQuery({ queryKey: KEYS.all, queryFn: userService.getAll });

export const useUser = (id: string) =>
  useQuery({ queryKey: KEYS.detail(id), queryFn: () => userService.getById(id), enabled: !!id });

export const useCreateUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateUserRequest) => userService.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useUpdateUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateUserRequest }) =>
      userService.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const usePatchUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: PatchUserRequest }) =>
      userService.patch(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useDeleteUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => userService.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};
