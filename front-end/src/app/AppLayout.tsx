import { Link, NavLink, Outlet } from 'react-router-dom';
import {
  Cpu,
  LogOut,
  Package,
  Settings,
  Shield,
  UserCircle,
  Users,
  Wrench,
} from 'lucide-react';
import { useAuth, useLogoutRedirect } from '@/lib/auth';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';

const userLinks = [
  { to: '/app/submit', label: 'Submit' },
  { to: '/app/workloads', label: 'My Workloads' },
  { to: '/app/resources', label: 'Resources' },
  { to: '/app/profile', label: 'Profile' },
];

const adminLinks = [
  { to: '/admin/users', label: 'Users', icon: Users },
  { to: '/admin/models', label: 'Models', icon: Package },
  { to: '/admin/llm-configs', label: 'LLM Configs', icon: Wrench },
  { to: '/admin/environments', label: 'Environments', icon: Cpu },
  { to: '/admin/workloads', label: 'Workloads', icon: Shield },
  { to: '/admin/gpus', label: 'GPUs', icon: Cpu },
  { to: '/admin/settings', label: 'Settings', icon: Settings },
];

export function AppLayout() {
  const { user } = useAuth();
  const logout = useLogoutRedirect();

  return (
    <div className="flex min-h-screen">
      <aside className="w-64 border-r bg-white">
        <div className="border-b p-4">
          <Link to="/app/workloads" className="text-lg font-semibold">
            GPU Hub
          </Link>
          <p className="text-xs text-muted-foreground">Control Plane</p>
        </div>
        <nav className="space-y-1 p-3">
          {userLinks.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `block rounded px-3 py-2 text-sm ${isActive ? 'bg-slate-900 text-white' : 'hover:bg-slate-100'}`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
        {user?.role === 'ADMIN' ? (
          <div className="border-t p-3">
            <p className="mb-2 text-xs font-semibold uppercase text-muted-foreground">Admin</p>
            <nav className="space-y-1">
              {adminLinks.map((link) => {
                const Icon = link.icon;
                return (
                  <NavLink
                    key={link.to}
                    to={link.to}
                    className={({ isActive }) =>
                      `flex items-center gap-2 rounded px-3 py-2 text-sm ${isActive ? 'bg-cyan-700 text-white' : 'hover:bg-slate-100'}`
                    }
                  >
                    <Icon size={14} />
                    {link.label}
                  </NavLink>
                );
              })}
            </nav>
          </div>
        ) : null}
      </aside>

      <div className="flex-1">
        <header className="flex h-14 items-center justify-between border-b bg-white px-5">
          <div className="text-sm text-muted-foreground">GPU Workload Submission & Management Platform</div>
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 text-sm">
              <UserCircle size={16} />
              <span>{user?.username}</span>
            </div>
            <Badge variant={user?.role === 'ADMIN' ? 'info' : 'default'}>{user?.role}</Badge>
            <Button variant="ghost" size="sm" onClick={logout}>
              <LogOut size={14} className="mr-1" /> Logout
            </Button>
          </div>
        </header>
        <main className="p-5">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
