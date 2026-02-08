import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function AdminSettingsPage() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Platform Settings</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2 text-sm">
        <p>Control-plane settings placeholder.</p>
        <p>Use this area for cluster defaults, admission policies, and preemption policy tuning.</p>
      </CardContent>
    </Card>
  );
}
