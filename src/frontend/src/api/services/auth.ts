import client from '../client';
import type { LoginRequest, TokenResponse } from '../types';

export const authService = {
  login: (data: LoginRequest) =>
    client.post<TokenResponse>('/auth/login', data).then((r) => r.data),

  refresh: (refreshToken: string) =>
    client.post<TokenResponse>('/auth/refresh', { refreshToken }).then((r) => r.data),

  logout: (refreshToken: string) =>
    client.delete('/auth/logout', { data: { refreshToken } }),
};
