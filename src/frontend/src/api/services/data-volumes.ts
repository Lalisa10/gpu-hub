import client from '../client';
import type { DataVolumeDto } from '../types';

const URL = '/data-volumes';

export const dataVolumeService = {
  getAll: (teamId?: string) =>
    client
      .get<DataVolumeDto[]>(URL, { params: teamId ? { teamId } : {} })
      .then((r) => r.data),
  getMine: () => client.get<DataVolumeDto[]>(`${URL}/my`).then((r) => r.data),
  getById: (id: string) => client.get<DataVolumeDto>(`${URL}/${id}`).then((r) => r.data),
};
