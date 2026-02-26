import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function ProfilePage() {
  const { user } = useAuth();
  const quotaQuery = useQuery({ queryKey: ['effective-quota'], queryFn: api.quotas.effectiveForMe });

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Profile</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm">
          <p>
            <strong>Username:</strong> {user?.username}
          </p>
          <p>
            <strong>Email:</strong> {user?.email}
          </p>
          <p>
            <strong>Role:</strong> {user?.role}
          </p>
          <p>
            <strong>Team ID:</strong> {user?.teamId}
          </p>
          <p>
            <strong>Active:</strong> {user?.active ? 'Yes' : 'No'}
          </p>
        </CardContent>
      </Card>

      {quotaQuery.data ? (
        <Card>
          <CardHeader>
            <CardTitle>Team Access</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            <p>
              <strong>Team:</strong> {quotaQuery.data.team.name}
            </p>
            <p>
              <strong>Description:</strong> {quotaQuery.data.team.description}
            </p>
            <p>
              <strong>Running Quota:</strong> {quotaQuery.data.usage.runningWorkloads} /{' '}
              {quotaQuery.data.effectiveLimits.maxRunningWorkloads}
            </p>
            <p>
              <strong>GPU Quota:</strong> {quotaQuery.data.usage.usedGPU} / {quotaQuery.data.effectiveLimits.maxGPU}
            </p>
          </CardContent>
        </Card>
      ) : null}
    </div>
  );
}
