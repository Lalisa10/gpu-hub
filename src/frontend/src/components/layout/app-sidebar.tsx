import { Link, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Server,
  Shield,
  Users,
  FolderKanban,
  Boxes,
  Play,
  List,
  LogOut,
  ChevronDown,
  Database,
  HardDrive,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuth } from '@/contexts/auth-context';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Separator } from '@/components/ui/separator';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';

interface NavItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
}

const adminNav: NavItem[] = [
  { label: 'Dashboard', href: '/', icon: LayoutDashboard },
  { label: 'Clusters', href: '/clusters', icon: Server },
  { label: 'Policies', href: '/policies', icon: Shield },
  { label: 'Teams', href: '/teams', icon: Users },
  { label: 'Projects', href: '/projects', icon: FolderKanban },
  { label: 'Users', href: '/users', icon: Users },
];

const commonNav: NavItem[] = [
  { label: 'My Workloads', href: '/workloads', icon: List },
  { label: 'Submit Workload', href: '/workloads/new', icon: Play },
  { label: 'Data Sources', href: '/data-sources', icon: Database },
  { label: 'Data Volumes', href: '/data-volumes', icon: HardDrive },
];

const teamLeadNav: NavItem[] = [
  { label: 'My Teams', href: '/my-teams', icon: Boxes },
];

export function AppSidebar() {
  const { user, isAdmin, logout, teamMemberships } = useAuth();
  const location = useLocation();
  const hasTeamLeadRole = teamMemberships.some((m) => m.role === 'TEAM_LEAD');

  const NavLink = ({ item }: { item: NavItem }) => {
    const active = location.pathname === item.href;
    return (
      <Link
        to={item.href}
        className={cn(
          'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
          active
            ? 'bg-sidebar-accent text-sidebar-accent-foreground'
            : 'text-sidebar-foreground hover:bg-sidebar-accent/50',
        )}
      >
        <item.icon className="h-4 w-4" />
        {item.label}
      </Link>
    );
  };

  return (
    <div className="flex h-screen w-64 flex-col border-r bg-sidebar">
      <div className="flex h-14 items-center border-b px-4">
        <Link to="/" className="flex items-center gap-2">
          <Boxes className="h-6 w-6 text-sidebar-primary" />
          <span className="text-lg font-bold text-sidebar-primary">GPU Hub</span>
        </Link>
      </div>

      <ScrollArea className="flex-1 px-3 py-4">
        {isAdmin && (
          <div className="space-y-1">
            <p className="px-3 text-xs font-semibold uppercase text-muted-foreground">Admin</p>
            {adminNav.map((item) => (
              <NavLink key={item.href} item={item} />
            ))}
          </div>
        )}

        {(hasTeamLeadRole || isAdmin) && (
          <>
            <Separator className="my-4" />
            <div className="space-y-1">
              <p className="px-3 text-xs font-semibold uppercase text-muted-foreground">
                Team Management
              </p>
              {teamLeadNav.map((item) => (
                <NavLink key={item.href} item={item} />
              ))}
            </div>
          </>
        )}

        <Separator className="my-4" />
        <div className="space-y-1">
          <p className="px-3 text-xs font-semibold uppercase text-muted-foreground">Workloads</p>
          {commonNav.map((item) => (
            <NavLink key={item.href} item={item} />
          ))}
        </div>
      </ScrollArea>

      <div className="border-t p-3">
        <DropdownMenu>
          <DropdownMenuTrigger className="flex w-full items-center justify-between rounded-md px-3 py-2 text-sm font-medium hover:bg-sidebar-accent/50 transition-colors">
            <span className="truncate">{user?.username}</span>
            <ChevronDown className="h-4 w-4 opacity-50" />
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuItem disabled className="text-xs text-muted-foreground">
              Role: {user?.globalRole}
            </DropdownMenuItem>
            <DropdownMenuItem onClick={logout}>
              <LogOut className="mr-2 h-4 w-4" />
              Sign out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>
  );
}
