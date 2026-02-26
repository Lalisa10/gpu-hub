import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from '@/app/AppLayout';
import { AdminRoute, ProtectedRoute } from '@/app/RouteGuards';
import { LoginPage } from '@/auth/LoginPage';
import { SubmitPage } from '@/app/SubmitPage';
import { WorkloadsPage } from '@/app/WorkloadsPage';
import { WorkloadDetailPage } from '@/app/WorkloadDetailPage';
import { ResourcesPage } from '@/app/ResourcesPage';
import { ProfilePage } from '@/app/ProfilePage';
import { AdminUsersPage } from '@/admin/UsersPage';
import { AdminModelsPage } from '@/admin/ModelsPage';
import { AdminLLMConfigsPage } from '@/admin/LLMConfigsPage';
import { AdminEnvironmentsPage } from '@/admin/EnvironmentsPage';
import { AdminWorkloadsPage } from '@/admin/WorkloadsPage';
import { AdminGPUsPage } from '@/admin/GPUsPage';
import { AdminSettingsPage } from '@/admin/SettingsPage';
import { AdminTeamsPage } from '@/admin/TeamsPage';
import { AdminTeamDetailPage } from '@/admin/TeamDetailPage';
import { AdminQuotasPage } from '@/admin/QuotasPage';

export function AppRouter() {
  return (
    <Routes>
      <Route path="/auth/login" element={<LoginPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/app/workloads" replace />} />
          <Route path="/app/submit" element={<SubmitPage />} />
          <Route path="/app/workloads" element={<WorkloadsPage />} />
          <Route path="/app/workloads/:id" element={<WorkloadDetailPage />} />
          <Route path="/app/resources" element={<ResourcesPage />} />
          <Route path="/app/profile" element={<ProfilePage />} />

          <Route element={<AdminRoute />}>
            <Route path="/admin/users" element={<AdminUsersPage />} />
            <Route path="/admin/models" element={<AdminModelsPage />} />
            <Route path="/admin/llm-configs" element={<AdminLLMConfigsPage />} />
            <Route path="/admin/environments" element={<AdminEnvironmentsPage />} />
            <Route path="/admin/workloads" element={<AdminWorkloadsPage />} />
            <Route path="/admin/gpus" element={<AdminGPUsPage />} />
            <Route path="/admin/teams" element={<AdminTeamsPage />} />
            <Route path="/admin/teams/:id" element={<AdminTeamDetailPage />} />
            <Route path="/admin/quotas" element={<AdminQuotasPage />} />
            <Route path="/admin/settings" element={<AdminSettingsPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/app/workloads" replace />} />
    </Routes>
  );
}
