import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { projectService } from '../services/projects';
import type { CreateProjectRequest, UpdateProjectRequest } from '../types';

const KEYS = { all: ['projects'] as const, detail: (id: string) => ['projects', id] as const };

export const useProjects = () =>
  useQuery({ queryKey: KEYS.all, queryFn: projectService.getAll });

export const useProject = (id: string) =>
  useQuery({ queryKey: KEYS.detail(id), queryFn: () => projectService.getById(id), enabled: !!id });

export const useCreateProject = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateProjectRequest) => projectService.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useUpdateProject = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateProjectRequest }) =>
      projectService.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};

export const useDeleteProject = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => projectService.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEYS.all }),
  });
};
