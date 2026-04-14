import axios from 'axios';
import type { TokenResponse } from './types';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

const client = axios.create({
  baseURL: `${API_BASE}/api`,
});

// Attach access token to every request
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Refresh on 401
let refreshPromise: Promise<string> | null = null;

client.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    if (
      status === 401 &&
      !original._retry &&
      !original.url?.includes('/auth/')
    ) {
      original._retry = true;
      const refreshToken = localStorage.getItem('refresh_token');
      if (!refreshToken) {
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      // Deduplicate concurrent refresh calls
      if (!refreshPromise) {
        refreshPromise = client
          .post<TokenResponse>('/auth/refresh', { refreshToken })
          .then((res) => {
            localStorage.setItem('access_token', res.data.accessToken);
            localStorage.setItem('refresh_token', res.data.refreshToken);
            return res.data.accessToken;
          })
          .catch(() => {
            localStorage.clear();
            window.location.href = '/login';
            return '';
          })
          .finally(() => {
            refreshPromise = null;
          });
      }

      const newToken = await refreshPromise;
      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`;
        return client(original);
      }
    }
    return Promise.reject(error);
  },
);

export default client;
