import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { teamMemberService } from '../services/team-members';
import type { CreateTeamMemberRequest, UpdateTeamMemberRequest } from '../types';

const KEYS = { all: ['team-members'] as const };

export const useTeamMembers = () =>
  useQuery({ queryKey: KEYS.all, queryFn: teamMemberService.getAll });

export const useCreateTeamMember = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTeamMemberRequest) => teamMemberService.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useUpdateTeamMember = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      teamId,
      userId,
      data,
    }: {
      teamId: string;
      userId: string;
      data: UpdateTeamMemberRequest;
    }) => teamMemberService.update(teamId, userId, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useDeleteTeamMember = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ teamId, userId }: { teamId: string; userId: string }) =>
      teamMemberService.delete(teamId, userId),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};
