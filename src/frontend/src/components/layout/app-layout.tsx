import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '@/contexts/auth-context';
import { AppSidebar } from './app-sidebar';
import { useTeamMembers } from '@/api/hooks/use-team-members';
import { useEffect } from 'react';

export function AppLayout() {
  const { user, isLoading, setTeamMemberships } = useAuth();

  // Load team memberships for the current user
  const { data: allMembers } = useTeamMembers();

  useEffect(() => {
    if (allMembers && user) {
      const mine = allMembers.filter((m) => m.userId === user.id);
      setTeamMemberships(mine);
    }
  }, [allMembers, user, setTeamMemberships]);

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <p className="text-muted-foreground">Loading...</p>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="flex h-screen overflow-hidden">
      <AppSidebar />
      <main className="flex-1 overflow-y-auto bg-muted/30 p-6">
        <Outlet />
      </main>
    </div>
  );
}

export function RequireAdmin() {
  const { isAdmin, isLoading } = useAuth();
  if (isLoading) return null;
  if (!isAdmin) return <Navigate to="/workloads" replace />;
  return <Outlet />;
}
