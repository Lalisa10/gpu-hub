import client from '../client';
import type { CreateDataSourceRequest, DataSourceDto } from '../types';

const URL = '/data-sources';

export const dataSourceService = {
  getMine: () => client.get<DataSourceDto[]>(`${URL}/my`).then((r) => r.data),
  getById: (id: string) => client.get<DataSourceDto>(`${URL}/${id}`).then((r) => r.data),
  create: (data: CreateDataSourceRequest) =>
    client.post<DataSourceDto>(URL, data).then((r) => r.data),
  delete: (id: string) => client.delete(`${URL}/${id}`),
};
