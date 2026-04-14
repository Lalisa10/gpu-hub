import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { teamService } from '../services/teams';
import type { CreateTeamRequest, UpdateTeamRequest } from '../types';

const KEYS = { all: ['teams'] as const, detail: (id: string) => ['teams', id] as const };

export const useTeams = () =>
  useQuery({ queryKey: KEYS.all, queryFn: teamService.getAll });

export const useTeam = (id: string) =>
  useQuery({ queryKey: KEYS.detail(id), queryFn: () => teamService.getById(id), enabled: !!id });

export const useCreateTeam = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTeamRequest) => teamService.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useUpdateTeam = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateTeamRequest }) =>
      teamService.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useDeleteTeam = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => teamService.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};
