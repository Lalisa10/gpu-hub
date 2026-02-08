import * as AlertDialog from '@radix-ui/react-alert-dialog';
import { Button } from './button';

interface ConfirmActionProps {
  title: string;
  description: string;
  actionLabel?: string;
  triggerLabel: string;
  triggerVariant?: 'default' | 'outline' | 'ghost' | 'destructive' | 'secondary';
  onConfirm: () => void;
}

export function ConfirmAction({
  title,
  description,
  actionLabel = 'Confirm',
  triggerLabel,
  triggerVariant = 'outline',
  onConfirm,
}: ConfirmActionProps) {
  return (
    <AlertDialog.Root>
      <AlertDialog.Trigger asChild>
        <Button size="sm" variant={triggerVariant}>
          {triggerLabel}
        </Button>
      </AlertDialog.Trigger>
      <AlertDialog.Portal>
        <AlertDialog.Overlay className="fixed inset-0 z-40 bg-black/40" />
        <AlertDialog.Content className="fixed left-1/2 top-1/2 z-50 w-[min(92vw,420px)] -translate-x-1/2 -translate-y-1/2 rounded-lg border bg-white p-5 shadow-lg">
          <AlertDialog.Title className="text-base font-semibold">{title}</AlertDialog.Title>
          <AlertDialog.Description className="mt-2 text-sm text-muted-foreground">
            {description}
          </AlertDialog.Description>
          <div className="mt-4 flex justify-end gap-2">
            <AlertDialog.Cancel asChild>
              <Button variant="outline">Cancel</Button>
            </AlertDialog.Cancel>
            <AlertDialog.Action asChild>
              <Button variant="destructive" onClick={onConfirm}>
                {actionLabel}
              </Button>
            </AlertDialog.Action>
          </div>
        </AlertDialog.Content>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  );
}
