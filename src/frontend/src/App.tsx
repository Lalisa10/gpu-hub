import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster, toast } from 'sonner';
import { AuthProvider } from '@/contexts/auth-context';
import { AppLayout, RequireAdmin } from '@/components/layout/app-layout';
import { getErrorMessage } from '@/lib/error';
import LoginPage from '@/pages/login';
import DashboardPage from '@/pages/admin/dashboard';
import ClustersPage from '@/pages/admin/clusters';
import PoliciesPage from '@/pages/admin/policies';
import TeamsPage from '@/pages/admin/teams';
import ProjectsPage from '@/pages/admin/projects';
import UsersPage from '@/pages/admin/users';
import MyTeamsPage from '@/pages/my-teams';
import SubmitWorkloadPage from '@/pages/workloads/submit';
import WorkloadListPage from '@/pages/workloads/list';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
    },
    mutations: {
      onError: (error) => {
        toast.error(getErrorMessage(error));
      },
    },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route element={<AppLayout />}>
              <Route element={<RequireAdmin />}>
                <Route path="/" element={<DashboardPage />} />
                <Route path="/clusters" element={<ClustersPage />} />
                <Route path="/policies" element={<PoliciesPage />} />
                <Route path="/teams" element={<TeamsPage />} />
                <Route path="/projects" element={<ProjectsPage />} />
                <Route path="/users" element={<UsersPage />} />
              </Route>
              <Route path="/my-teams" element={<MyTeamsPage />} />
              <Route path="/workloads" element={<WorkloadListPage />} />
              <Route path="/workloads/new" element={<SubmitWorkloadPage />} />
              <Route path="*" element={<Navigate to="/workloads" replace />} />
            </Route>
          </Routes>
        </AuthProvider>
      </BrowserRouter>
      <Toaster position="top-right" richColors closeButton />
    </QueryClientProvider>
  );
}
