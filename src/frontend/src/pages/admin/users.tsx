import { useState } from 'react';
import { PageHeader } from '@/components/shared/page-header';
import { DataTable } from '@/components/shared/data-table';
import { ConfirmDialog } from '@/components/shared/confirm-dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useUsers, useCreateUser, useDeleteUser, usePatchUser } from '@/api/hooks/use-users';
import type { UserDto, CreateUserRequest, GlobalRole } from '@/api/types';
import type { Column } from '@/components/shared/data-table';
import { Plus, Trash2 } from 'lucide-react';

export default function UsersPage() {
  const { data: users = [], isLoading } = useUsers();
  const createUser = useCreateUser();
  const deleteUser = useDeleteUser();
  const patchUser = usePatchUser();

  const [showCreate, setShowCreate] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<UserDto | null>(null);
  const [form, setForm] = useState<CreateUserRequest>({
    username: '',
    email: '',
    password: '',
    globalRole: 'USER',
  });

  const columns: Column<UserDto>[] = [
    { header: 'Username', accessor: 'username' },
    { header: 'Email', accessor: 'email' },
    { header: 'Full Name', accessor: (u) => u.fullName ?? '-' },
    {
      header: 'Role',
      accessor: (u) => (
        <Badge variant={u.globalRole === 'ADMIN' ? 'default' : 'secondary'}>
          {u.globalRole}
        </Badge>
      ),
    },
    {
      header: 'Active',
      accessor: (u) => (
        <Button
          variant="ghost"
          size="sm"
          onClick={(e) => {
            e.stopPropagation();
            patchUser.mutate({ id: u.id, data: { isActive: !u.isActive } });
          }}
        >
          <Badge variant={u.isActive ? 'default' : 'destructive'}>
            {u.isActive ? 'Yes' : 'No'}
          </Badge>
        </Button>
      ),
    },
    {
      header: '',
      accessor: (u) => (
        <Button
          variant="ghost"
          size="sm"
          onClick={(e) => {
            e.stopPropagation();
            setDeleteTarget(u);
          }}
        >
          <Trash2 className="h-4 w-4 text-destructive" />
        </Button>
      ),
      className: 'w-12',
    },
  ];

  return (
    <div>
      <PageHeader
        title="Users"
        description="Manage platform users"
        action={
          <Button onClick={() => setShowCreate(true)}>
            <Plus className="mr-2 h-4 w-4" /> Create User
          </Button>
        }
      />

      <DataTable columns={columns} data={users} isLoading={isLoading} />

      <Dialog open={showCreate} onOpenChange={setShowCreate}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create User</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Username</Label>
              <Input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label>Email</Label>
              <Input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label>Full Name</Label>
              <Input value={form.fullName ?? ''} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label>Password</Label>
              <Input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label>Global Role</Label>
              <Select
                value={form.globalRole}
                onValueChange={(v) => v && setForm({ ...form, globalRole: v as GlobalRole })}
              >
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="USER">User</SelectItem>
                  <SelectItem value="ADMIN">Admin</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button
              onClick={async () => {
                await createUser.mutateAsync(form);
                setShowCreate(false);
                setForm({ username: '', email: '', password: '', globalRole: 'USER' });
              }}
              disabled={createUser.isPending || !form.username || !form.email || !form.password}
            >
              Create
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={() => setDeleteTarget(null)}
        title="Delete User"
        description={`Delete user "${deleteTarget?.username}"?`}
        onConfirm={async () => {
          if (deleteTarget) {
            await deleteUser.mutateAsync(deleteTarget.id);
            setDeleteTarget(null);
          }
        }}
        loading={deleteUser.isPending}
      />
    </div>
  );
}
