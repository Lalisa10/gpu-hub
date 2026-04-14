import axios from 'axios';

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const rawData = error.response?.data as { message?: string; error?: string; details?: string } | string | undefined;
    if (typeof rawData === 'string' && rawData.trim()) return rawData;
    const data = typeof rawData === 'object' && rawData !== null ? rawData : undefined;
    if (data?.details) return data.details;
    if (data?.message) return data.message;
    if (data?.error) return data.error;
    const status = error.response?.status;
    if (status === 403) return 'Access denied (403)';
    if (status === 401) return 'Unauthorized (401)';
    if (status === 404) return 'Resource not found (404)';
    if (status === 500) return 'Server error (500)';
    return error.message;
  }
  if (error instanceof Error) return error.message;
  return 'An unexpected error occurred';
}
