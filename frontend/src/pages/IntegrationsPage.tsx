import { useState, type FormEvent } from 'react';
import { apiFetch, getApiBase } from '../api/client';
import { RoleGate } from '../components/RoleGate';
import { SeverityBadge, StatusBadge } from '../components/StatusBadge';
import type { Incident, IncidentSeverity } from '../types';
import { ALL_SEVERITIES } from '../types';

export function IntegrationsPage() {
  const apiBase = getApiBase() || window.location.origin;
  const webhookUrl = `${apiBase}/api/v1/webhook/alert`;

  const examplePayload = JSON.stringify(
    {
      source: 'uptimerobot',
      externalId: 'monitor-12345',
      title: 'API gateway down',
      severity: 'SEV1',
      message: 'HTTP 500 for 3 consecutive checks',
    },
    null,
    2,
  );

  const [source, setSource] = useState('demo-monitor');
  const [externalId, setExternalId] = useState(`demo-${Date.now()}`);
  const [title, setTitle] = useState('Simulated monitor alert');
  const [severity, setSeverity] = useState<IncidentSeverity>('SEV2');
  const [message, setMessage] = useState('Triggered from admin UI');
  const [resolve, setResolve] = useState(false);
  const [result, setResult] = useState<Incident | null>(null);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSimulate = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    setResult(null);
    try {
      const body: Record<string, string> = {
        source,
        externalId,
        title,
        severity,
        message,
      };
      if (resolve) body.status = 'resolved';

      const incident = await apiFetch<Incident>('/api/v1/webhook/simulate', {
        method: 'POST',
        body,
      });
      setResult(incident);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Simulation failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <h1 className="page-title">Integrations</h1>

      <div className="card">
        <h2 className="section-title">Webhook ingestion</h2>
        <p className="muted">
          External monitors (UptimeRobot, Grafana, Prometheus Alertmanager) POST alerts to
          IncidentPulse. Duplicate alerts with the same <code>(source, externalId)</code> are
          deduplicated. Send <code>&quot;status&quot;: &quot;resolved&quot;</code> to auto-resolve.
        </p>
        <dl className="detail-list">
          <dt>Endpoint</dt>
          <dd>
            <code className="code-block">POST {webhookUrl}</code>
          </dd>
          <dt>Header</dt>
          <dd>
            <code>X-API-Key: &lt;your-webhook-secret&gt;</code>
          </dd>
        </dl>
        <pre className="code-block">{examplePayload}</pre>
      </div>

      <RoleGate roles={['ADMIN']}>
        <div className="card">
          <h2 className="section-title">Simulate monitor alert</h2>
          <p className="muted">
            Admin-only demo. Uses JWT instead of the webhook API key — real monitors call the
            public endpoint above.
          </p>
          <form onSubmit={handleSimulate} className="form">
            <label>
              Source
              <input value={source} onChange={(e) => setSource(e.target.value)} required />
            </label>
            <label>
              External ID
              <input
                value={externalId}
                onChange={(e) => setExternalId(e.target.value)}
                required
              />
            </label>
            <label>
              Title
              <input value={title} onChange={(e) => setTitle(e.target.value)} required />
            </label>
            <label>
              Severity
              <select
                value={severity}
                onChange={(e) => setSeverity(e.target.value as IncidentSeverity)}
              >
                {ALL_SEVERITIES.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Message
              <textarea value={message} onChange={(e) => setMessage(e.target.value)} rows={3} />
            </label>
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={resolve}
                onChange={(e) => setResolve(e.target.checked)}
              />
              Send as resolved (auto-resolve existing incident)
            </label>
            {error && <p className="error">{error}</p>}
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Sending...' : 'Simulate alert'}
            </button>
          </form>
          {result && (
            <div className="result-box">
              <p>
                Incident <span className="mono">#{result.id}</span> — {result.title}
              </p>
              <div className="badges-row">
                <StatusBadge status={result.status} />
                <SeverityBadge severity={result.severity} />
              </div>
              <p className="muted">
                Check the Incidents dashboard — it should appear live via WebSocket.
              </p>
            </div>
          )}
        </div>
      </RoleGate>
    </div>
  );
}
