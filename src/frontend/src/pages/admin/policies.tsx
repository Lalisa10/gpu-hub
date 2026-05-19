import { useState } from 'react';
import { PageHeader } from '@/components/shared/page-header';
import { DataTable } from '@/components/shared/data-table';
import { ConfirmDialog } from '@/components/shared/confirm-dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
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
import { usePolicies, useCreatePolicy, useDeletePolicy } from '@/api/hooks/use-policies';
import { useClusters } from '@/api/hooks/use-clusters';
import type { PolicyDto, CreatePolicyRequest } from '@/api/types';
import type { Column } from '@/components/shared/data-table';
import { Plus, Trash2, AlertCircle, HelpCircle } from 'lucide-react';

// All numeric fields are kept as raw strings so the user can type naturally
// (no coercion on every keystroke). They are parsed to numbers at submit time.
interface FormState {
  clusterId: string;
  name: string;
  description?: string;
  gpuQuota: string;
  cpuQuota: string;
  memoryQuota: string;
  gpuLimit: string;
  cpuLimit: string;
  memoryLimit: string;
  gpuOverQuotaWeight: string;
  cpuOverQuotaWeight: string;
  memoryOverQuotaWeight: string;
  extra?: string;
}

const NUMERIC_FIELDS = [
  'gpuQuota', 'cpuQuota', 'memoryQuota',
  'gpuLimit', 'cpuLimit', 'memoryLimit',
  'gpuOverQuotaWeight', 'cpuOverQuotaWeight', 'memoryOverQuotaWeight',
] as const;

const emptyForm: FormState = {
  clusterId: '',
  name: '',
  description: '',
  gpuQuota: '',
  cpuQuota: '',
  memoryQuota: '',
  gpuLimit: '',
  cpuLimit: '',
  memoryLimit: '',
  gpuOverQuotaWeight: '',
  cpuOverQuotaWeight: '',
  memoryOverQuotaWeight: '',
  extra: '',
};

interface ValidationErrors {
  [key: string]: string;
}

/** Parse a raw input string to a number, or undefined when blank. */
const parseNum = (raw: string): number | undefined => {
  const t = raw.trim();
  if (t === '') return undefined;
  const n = Number(t);
  return Number.isNaN(n) ? undefined : n;
};

const validateForm = (form: FormState): ValidationErrors => {
  const errors: ValidationErrors = {};

  if (!form.name?.trim()) {
    errors.name = 'Policy name is required';
  }

  if (!form.clusterId) {
    errors.clusterId = 'Cluster is required';
  }

  // Validate numeric fields: must be a valid number ≥ -1 when provided
  for (const field of NUMERIC_FIELDS) {
    const raw = form[field].trim();
    if (raw === '') continue;
    const n = Number(raw);
    if (Number.isNaN(n)) {
      errors[field] = 'Must be a number';
    } else if (n < -1) {
      errors[field] = 'Must be a positive number or 0, -1';
    }
  }

  // Validate extra JSON if provided
  if (form.extra?.trim()) {
    try {
      const parsed = JSON.parse(form.extra);
      if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
        errors.extra = 'Must be a valid JSON object';
      }
    } catch {
      errors.extra = 'Invalid JSON format';
    }
  }

  return errors;
};

interface FieldGroupProps {
  label: string;
  required?: boolean;
  error?: string;
  helperText?: string;
  children: React.ReactNode;
}

function FieldGroup({ label, required, error, helperText, children }: FieldGroupProps) {
  return (
    <div className="space-y-1.5">
      <div className="flex items-center gap-1.5">
        <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
          {label}
          {required && <span className="text-destructive ml-0.5">*</span>}
        </label>
        {helperText && (
          <div title={helperText} className="cursor-help">
            <HelpCircle className="h-3.5 w-3.5 text-muted-foreground hover:text-foreground" />
          </div>
        )}
      </div>
      {children}
      {error && (
        <div className="flex items-center gap-1 text-xs text-destructive">
          <AlertCircle className="h-3 w-3" />
          <span>{error}</span>
        </div>
      )}
    </div>
  );
}

export default function PoliciesPage() {
  const { data: policies = [], isLoading } = usePolicies();
  const { data: clusters = [] } = useClusters();
  const createPolicy = useCreatePolicy();
  const deletePolicy = useDeletePolicy();

  const [showCreate, setShowCreate] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<PolicyDto | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [errors, setErrors] = useState<ValidationErrors>({});

  const clusterName = (id: string) => clusters.find((c) => c.id === id)?.name ?? id.slice(0, 8);

  const columns: Column<PolicyDto>[] = [
    { header: 'Name', accessor: 'name' },
    { header: 'Cluster', accessor: (p) => clusterName(p.clusterId) },
    { header: 'GPU Quota', accessor: (p) => p.gpuQuota ?? '-' },
    { header: 'CPU Quota', accessor: (p) => p.cpuQuota ?? '-' },
    { header: 'Memory (MiB)', accessor: (p) => p.memoryQuota ?? '-' },
    {
      header: '',
      accessor: (p) => (
        <Button
          variant="ghost"
          size="sm"
          onClick={(e) => {
            e.stopPropagation();
            setDeleteTarget(p);
          }}
        >
          <Trash2 className="h-4 w-4 text-destructive" />
        </Button>
      ),
      className: 'w-12',
    },
  ];

  const handleCreate = async () => {
    const newErrors = validateForm(form);
    setErrors(newErrors);

    if (Object.keys(newErrors).length > 0) {
      return;
    }

    const submitData: CreatePolicyRequest = {
      clusterId: form.clusterId,
      name: form.name.trim(),
      description: form.description?.trim() || undefined,
      gpuQuota: parseNum(form.gpuQuota),
      cpuQuota: parseNum(form.cpuQuota),
      memoryQuota: parseNum(form.memoryQuota),
      gpuLimit: parseNum(form.gpuLimit),
      cpuLimit: parseNum(form.cpuLimit),
      memoryLimit: parseNum(form.memoryLimit),
      gpuOverQuotaWeight: parseNum(form.gpuOverQuotaWeight),
      cpuOverQuotaWeight: parseNum(form.cpuOverQuotaWeight),
      memoryOverQuotaWeight: parseNum(form.memoryOverQuotaWeight),
      extra: form.extra ? JSON.parse(form.extra) : undefined,
    };

    await createPolicy.mutateAsync(submitData);
    setShowCreate(false);
    setForm(emptyForm);
    setErrors({});
  };

  const updateField = <K extends keyof FormState>(key: K, value: FormState[K]) => {
    setForm((f) => ({ ...f, [key]: value }));
    // Clear error for this field when user starts editing
    if (errors[key]) {
      setErrors((e) => {
        const newErrors = { ...e };
        delete newErrors[key];
        return newErrors;
      });
    }
  };

  const isFormValid = Object.keys(validateForm(form)).length === 0 && form.name && form.clusterId;

  return (
    <div>
      <PageHeader
        title="Policies"
        description="Resource quota and limit definitions"
        action={
          <Button onClick={() => setShowCreate(true)}>
            <Plus className="mr-2 h-4 w-4" /> Add Policy
          </Button>
        }
      />

      <DataTable columns={columns} data={policies} isLoading={isLoading} />

      <Dialog open={showCreate} onOpenChange={setShowCreate}>
        <DialogContent className="w-[95vw] sm:max-w-5xl max-h-[90vh] overflow-y-auto overflow-x-hidden">
          <DialogHeader>
            <DialogTitle>Create Policy</DialogTitle>
          </DialogHeader>

          <div className="space-y-6 py-4">
            {/* Basic Info Section */}
            <div className="space-y-4">
              <h3 className="text-sm font-semibold text-foreground">Basic Information</h3>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <FieldGroup
                  label="Policy Name"
                  required
                  error={errors.name}
                  helperText="A unique name to identify this policy"
                >
                  <Input
                    placeholder="e.g., GPU-Heavy-Team"
                    value={form.name}
                    onChange={(e) => updateField('name', e.target.value)}
                    className={errors.name ? 'border-destructive' : ''}
                  />
                </FieldGroup>

                <FieldGroup
                  label="Cluster"
                  required
                  error={errors.clusterId}
                  helperText="Select the cluster this policy applies to"
                >
                  <Select value={form.clusterId} onValueChange={(v) => v && updateField('clusterId', v)}>
                    <SelectTrigger className={`w-full ${errors.clusterId ? 'border-destructive' : ''}`}>
                      <SelectValue placeholder="Select cluster">
                        {form.clusterId ? clusterName(form.clusterId) : null}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {clusters.map((c) => (
                        <SelectItem key={c.id} value={c.id}>
                          {c.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </FieldGroup>
              </div>
            </div>

            {/* Resource Quotas Section */}
            <div className="space-y-4">
              <h3 className="text-sm font-semibold text-foreground">Resource Quotas</h3>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                <FieldGroup
                  label="GPU Quota"
                  error={errors.gpuQuota}
                  helperText="Number of GPUs"
                >
                  <Input
                    type="number"
                    placeholder="-1"
                    value={form.gpuQuota ?? ''}
                    onChange={(e) => updateField('gpuQuota', e.target.value)}
                    min="0"
                    className={errors.gpuQuota ? 'border-destructive' : ''}
                  />
                </FieldGroup>

                <FieldGroup
                  label="CPU Quota"
                  error={errors.cpuQuota}
                  helperText="CPU cores"
                >
                  <Input
                    type="number"
                    placeholder="-1"
                    value={form.cpuQuota ?? ''}
                    onChange={(e) => updateField('cpuQuota', e.target.value)}
                    min="0"
                    className={errors.cpuQuota ? 'border-destructive' : ''}
                  />
                </FieldGroup>

                <FieldGroup
                  label="Memory Quota"
                  error={errors.memoryQuota}
                  helperText="Memory in MB"
                >
                  <Input
                    type="number"
                    placeholder="-1"
                    value={form.memoryQuota ?? ''}
                    onChange={(e) => updateField('memoryQuota', e.target.value)}
                    min="0"
                    className={errors.memoryQuota ? 'border-destructive' : ''}
                  />
                </FieldGroup>
              </div>
            </div>

            {/* Resource Limits Section */}
            <div className="space-y-4">
              <h3 className="text-sm font-semibold text-foreground">Resource Limits</h3>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                <FieldGroup
                  label="GPU Limit"
                  error={errors.gpuLimit}
                  helperText="Max GPUs per workload"
                >
                  <Input
                    type="number"
                    placeholder="-1"
                    value={form.gpuLimit ?? ''}
                    onChange={(e) => updateField('gpuLimit', e.target.value)}
                    min="0"
                    className={errors.gpuLimit ? 'border-destructive' : ''}
                  />
                </FieldGroup>

                <FieldGroup
                  label="CPU Limit"
                  error={errors.cpuLimit}
                  helperText="Max CPU cores per workload"
                >
                  <Input
                    type="number"
                    placeholder="-1"
                    value={form.cpuLimit ?? ''}
                    onChange={(e) => updateField('cpuLimit', e.target.value)}
                    min="0"
                    className={errors.cpuLimit ? 'border-destructive' : ''}
                  />
                </FieldGroup>

                <FieldGroup
                  label="Memory Limit"
                  error={errors.memoryLimit}
                  helperText="Max memory per workload"
                >
                  <Input
                    type="number"
                    placeholder="-1"
                    value={form.memoryLimit ?? ''}
                    onChange={(e) => updateField('memoryLimit', e.target.value)}
                    min="0"
                    className={errors.memoryLimit ? 'border-destructive' : ''}
                  />
                </FieldGroup>
              </div>
            </div>

            {/* Over-Quota Weights Section */}
            <div className="space-y-4">
              <h3 className="text-sm font-semibold text-foreground">Scheduling</h3>

              <div className="space-y-2">
                <h4 className="text-xs font-medium text-muted-foreground">Over-Quota Weights</h4>
                <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                  <FieldGroup
                    label="GPU Over-Quota Weight"
                    error={errors.gpuOverQuotaWeight}
                    helperText="Weight for over GPU quota"
                  >
                    <Input
                      type="number"
                      placeholder="1"
                      value={form.gpuOverQuotaWeight ?? ''}
                      onChange={(e) =>
                        updateField('gpuOverQuotaWeight', e.target.value)
                      }
                      min="0"
                      step="0.1"
                      className={errors.gpuOverQuotaWeight ? 'border-destructive' : ''}
                    />
                  </FieldGroup>

                  <FieldGroup
                    label="CPU Over-Quota Weight"
                    error={errors.cpuOverQuotaWeight}
                    helperText="Weight for over CPU quota"
                  >
                    <Input
                      type="number"
                      placeholder="1"
                      value={form.cpuOverQuotaWeight ?? ''}
                      onChange={(e) =>
                        updateField('cpuOverQuotaWeight', e.target.value)
                      }
                      min="0"
                      step="0.1"
                      className={errors.cpuOverQuotaWeight ? 'border-destructive' : ''}
                    />
                  </FieldGroup>

                  <FieldGroup
                    label="Memory Over-Quota Weight"
                    error={errors.memoryOverQuotaWeight}
                    helperText="Weight for over memory quota"
                  >
                    <Input
                      type="number"
                      placeholder="1"
                      value={form.memoryOverQuotaWeight ?? ''}
                      onChange={(e) =>
                        updateField('memoryOverQuotaWeight', e.target.value)
                      }
                      min="0"
                      step="0.1"
                      className={errors.memoryOverQuotaWeight ? 'border-destructive' : ''}
                    />
                  </FieldGroup>
                </div>
              </div>
            </div>

            {/* Advanced Settings Section */}
            <div className="space-y-4">
              <h3 className="text-sm font-semibold text-foreground">Advanced Settings</h3>

              <FieldGroup
                label="Extra (JSON)"
                error={errors.extra}
                helperText="Free-form JSON for scheduler hints, node affinity, or other extensions. Leave empty if not needed."
              >
                <Textarea
                  placeholder={'{\n  "nodeAffinity": {\n    "requiredDuringSchedulingIgnoredDuringExecution": {\n      "nodeSelectorTerms": []\n    }\n  }\n}'}
                  value={form.extra ?? ''}
                  onChange={(e) => updateField('extra', e.target.value)}
                  className={`font-mono text-xs ${errors.extra ? 'border-destructive' : ''}`}
                  rows={4}
                />
              </FieldGroup>

              <FieldGroup
                label="Description"
                helperText="Optional details about this policy"
              >
                <Textarea
                  placeholder="Describe the purpose and constraints of this policy..."
                  value={form.description ?? ''}
                  onChange={(e) => updateField('description', e.target.value)}
                  rows={3}
                />
              </FieldGroup>
            </div>
          </div>

          <DialogFooter className="flex justify-between border-t pt-4">
            <Button variant="outline" onClick={() => {
              setShowCreate(false);
              setForm(emptyForm);
              setErrors({});
            }}>
              Cancel
            </Button>
            <Button
              onClick={handleCreate}
              disabled={createPolicy.isPending || !isFormValid}
            >
              {createPolicy.isPending ? (
                <>Creating...</>
              ) : (
                <>Create Policy</>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={() => setDeleteTarget(null)}
        title="Delete Policy"
        description={`Delete policy "${deleteTarget?.name}"?`}
        onConfirm={async () => {
          if (deleteTarget) {
            await deletePolicy.mutateAsync(deleteTarget.id);
            setDeleteTarget(null);
          }
        }}
        loading={deletePolicy.isPending}
      />
    </div>
  );
}
