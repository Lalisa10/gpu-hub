import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import type { GlobalRole, JwtPayload, TeamMemberDto, TeamRole } from '@/api/types';
import { authService } from '@/api/services/auth';

interface AuthUser {
  id: string;
  username: string;
  globalRole: GlobalRole;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAdmin: boolean;
  teamMemberships: TeamMemberDto[];
  setTeamMemberships: (m: TeamMemberDto[]) => void;
  getTeamRole: (teamId: string) => TeamRole | null;
  isTeamLead: (teamId: string) => boolean;
  login: (accessToken: string, refreshToken: string) => void;
  logout: () => Promise<void>;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function decodeJwt(token: string): JwtPayload | null {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload));
  } catch {
    return null;
  }
}

function userFromToken(token: string): AuthUser | null {
  const payload = decodeJwt(token);
  if (!payload) return null;
  if (payload.exp * 1000 < Date.now()) return null;
  return { id: payload.sub, username: payload.username, globalRole: payload.role };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [teamMemberships, setTeamMemberships] = useState<TeamMemberDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('access_token');
    if (token) {
      const u = userFromToken(token);
      setUser(u);
    }
    setIsLoading(false);
  }, []);

  const login = useCallback((accessToken: string, refreshToken: string) => {
    localStorage.setItem('access_token', accessToken);
    localStorage.setItem('refresh_token', refreshToken);
    setUser(userFromToken(accessToken));
  }, []);

  const logout = useCallback(async () => {
    const rt = localStorage.getItem('refresh_token');
    if (rt) {
      try {
        await authService.logout(rt);
      } catch {
        // ignore
      }
    }
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    setUser(null);
    setTeamMemberships([]);
  }, []);

  const getTeamRole = useCallback(
    (teamId: string): TeamRole | null => {
      const m = teamMemberships.find((tm) => tm.teamId === teamId);
      return m?.role ?? null;
    },
    [teamMemberships],
  );

  const isTeamLead = useCallback(
    (teamId: string) => getTeamRole(teamId) === 'TEAM_LEAD',
    [getTeamRole],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAdmin: user?.globalRole === 'ADMIN',
      teamMemberships,
      setTeamMemberships,
      getTeamRole,
      isTeamLead,
      login,
      logout,
      isLoading,
    }),
    [user, teamMemberships, getTeamRole, isTeamLead, login, logout, isLoading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
