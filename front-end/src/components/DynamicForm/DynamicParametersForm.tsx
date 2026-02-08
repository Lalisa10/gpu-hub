import { z } from 'zod';
import { useMemo } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import type { ConfigParameter } from '@/lib/types';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select } from '@/components/ui/select';

interface DynamicParametersFormProps {
  parameters: ConfigParameter[];
  initialValues?: Record<string, unknown>;
  onSubmit: (values: Record<string, unknown>) => void;
  submitLabel?: string;
}

function buildSchema(parameters: ConfigParameter[]) {
  const shape: Record<string, z.ZodTypeAny> = {};
  for (const p of parameters) {
    let field: z.ZodTypeAny;
    if (p.type === 'number') field = z.coerce.number();
    else if (p.type === 'boolean') field = z.enum(['true', 'false']).transform((v) => v === 'true');
    else if (p.type === 'enum') field = z.string().min(1);
    else field = z.string().min(1);

    if (!p.required) {
      field = field.optional();
    }

    shape[p.key] = field;
  }
  return z.object(shape);
}

export function DynamicParametersForm({
  parameters,
  initialValues,
  onSubmit,
  submitLabel = 'Apply',
}: DynamicParametersFormProps) {
  const schema = useMemo(() => buildSchema(parameters), [parameters]);

  const defaultValues = useMemo(() => {
    const values: Record<string, unknown> = {};
    for (const p of parameters) {
      const init = initialValues?.[p.key];
      values[p.key] = init ?? p.default ?? (p.type === 'boolean' ? 'false' : '');
    }
    return values;
  }, [initialValues, parameters]);

  const form = useForm<Record<string, unknown>>({
    resolver: zodResolver(schema),
    defaultValues,
  });

  return (
    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
      {parameters.map((p) => {
        const error = form.formState.errors[p.key]?.message as string | undefined;
        return (
          <div key={p.key}>
            <label className="mb-1 block text-sm font-medium">
              {p.key}
              {!p.required ? <span className="ml-1 text-xs text-muted-foreground">(optional)</span> : null}
            </label>
            {p.type === 'enum' ? (
              <Select {...form.register(p.key)}>
                <option value="">Select value</option>
                {p.enumValues?.map((v) => (
                  <option key={v} value={v}>
                    {v}
                  </option>
                ))}
              </Select>
            ) : p.type === 'boolean' ? (
              <Select {...form.register(p.key)}>
                <option value="true">true</option>
                <option value="false">false</option>
              </Select>
            ) : (
              <Input type={p.type === 'number' ? 'number' : 'text'} {...form.register(p.key)} />
            )}
            <p className="mt-1 text-xs text-muted-foreground">{p.description}</p>
            {error ? <p className="text-xs text-red-600">{error}</p> : null}
          </div>
        );
      })}
      <Button type="submit">{submitLabel}</Button>
    </form>
  );
}
