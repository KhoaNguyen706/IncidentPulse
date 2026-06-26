import type { IncidentSeverity, IncidentStatus } from '../types';

export function StatusBadge({ status }: { status: IncidentStatus }) {
  return <span className="badge badge-status">{status}</span>;
}

export function SeverityBadge({ severity }: { severity: IncidentSeverity }) {
  const weight =
    severity === 'SEV1' ? 'sev1' : severity === 'SEV2' ? 'sev2' : 'sev3';
  return <span className={`badge badge-severity ${weight}`}>{severity}</span>;
}
