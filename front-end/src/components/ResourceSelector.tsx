import { Input } from './ui/input';
import { Select } from './ui/select';

export interface ResourceValues {
  gpu: number;
  gpuModel?: string;
  cpu: number;
  memGB: number;
}

interface ResourceSelectorProps {
  value: ResourceValues;
  onChange: (next: ResourceValues) => void;
  compact?: boolean;
}

const GPU_MODELS = ['NVIDIA A100', 'NVIDIA H100', 'NVIDIA L40S', 'NVIDIA RTX 4090'];

export function ResourceSelector({ value, onChange, compact = false }: ResourceSelectorProps) {
  return (
    <div className={compact ? 'grid grid-cols-2 gap-3' : 'grid grid-cols-1 gap-3 md:grid-cols-2'}>
      <label className="text-sm">
        GPU Count
        <Input
          type="number"
          min={0}
          value={value.gpu}
          onChange={(e) => onChange({ ...value, gpu: Number(e.target.value) })}
        />
      </label>
      <label className="text-sm">
        GPU Model (Optional)
        <Select
          value={value.gpuModel ?? ''}
          onChange={(e) => onChange({ ...value, gpuModel: e.target.value || undefined })}
        >
          <option value="">No Preference</option>
          {GPU_MODELS.map((model) => (
            <option key={model} value={model}>
              {model}
            </option>
          ))}
        </Select>
      </label>
      <label className="text-sm">
        CPU Cores
        <Input
          type="number"
          min={1}
          value={value.cpu}
          onChange={(e) => onChange({ ...value, cpu: Number(e.target.value) })}
        />
      </label>
      <label className="text-sm">
        Memory (GB)
        <Input
          type="number"
          min={1}
          value={value.memGB}
          onChange={(e) => onChange({ ...value, memGB: Number(e.target.value) })}
        />
      </label>
    </div>
  );
}
