import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { apiFetch } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { HistoryTimeline } from '../components/HistoryTimeline';
import { SeverityBadge, StatusBadge } from '../components/StatusBadge';
import { useIncidentTopicFeed } from '../ws/LiveFeedContext';
import type {
  Incident,
  IncidentEvent,
  IncidentHistory,
  IncidentStatus,
  PageResponse,
} from '../types';
import { STATUS_TRANSITIONS } from '../types';

export function IncidentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const { canUpdateStatus } = useAuth();
  const incidentId = Number(id);

  const [incident, setIncident] = useState<Incident | null>(
    (location.state as { incident?: Incident } | null)?.incident ?? null,
  );
  const [history, setHistory] = useState<IncidentHistory[]>([]);
  const [note, setNote] = useState('');
  const [loading, setLoading] = useState(!incident);
  const [error, setError] = useState('');
  const [updating, setUpdating] = useState(false);

  const loadIncident = useCallback(async () => {
    if (!incidentId) return;
    setLoading(true);
    setError('');
    try {
      const data = await apiFetch<PageResponse<Incident>>(
        `/api/v1/incident?page=0&size=100&sort=createdAt,desc`,
      );
      const found = data.content.find((i) => i.id === incidentId);
      if (found) setIncident(found);
      else setError('Incident not found');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load incident');
    } finally {
      setLoading(false);
    }
  }, [incidentId]);

  const loadHistory = useCallback(async () => {
    if (!incidentId) return;
    try {
      const data = await apiFetch<IncidentHistory[]>(
        `/api/v1/incident/${incidentId}/history`,
      );
      setHistory(data);
    } catch {
      /* history load failure is non-fatal */
    }
  }, [incidentId]);

  useEffect(() => {
    if (!incident) loadIncident();
    loadHistory();
  }, [incident, loadIncident, loadHistory]);

  const handleEvent = useCallback(
    (event: IncidentEvent) => {
      if (event.id !== incidentId) return;
      setIncident((prev) =>
        prev
          ? { ...prev, status: event.status, updatedAt: event.occurredAt }
          : prev,
      );
      loadHistory();
    },
    [incidentId, loadHistory],
  );

  useIncidentTopicFeed(incidentId, handleEvent);

  const updateStatus = async (status: IncidentStatus) => {
    if (!incidentId) return;
    setUpdating(true);
    setError('');
    try {
      const updated = await apiFetch<Incident>(
        `/api/v1/incident/${incidentId}/status`,
        {
          method: 'PATCH',
          body: { status, note: note || undefined },
        },
      );
      setIncident(updated);
      setNote('');
      await loadHistory();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Status update failed');
    } finally {
      setUpdating(false);
    }
  };

  if (loading) return <p className="muted">Loading incident...</p>;
  if (!incident) return <p className="error">{error || 'Incident not found'}</p>;

  const nextStatuses = STATUS_TRANSITIONS[incident.status] ?? [];

  return (
    <div>
      <Link to="/incidents" className="back-link">
        ← Back to incidents
      </Link>

      <div className="page-header">
        <h1 className="page-title">
          <span className="mono">#{incident.id}</span> {incident.title}
        </h1>
        <div className="badges-row">
          <StatusBadge status={incident.status} />
          <SeverityBadge severity={incident.severity} />
        </div>
      </div>

      <div className="detail-grid">
        <div className="card">
          <h2 className="section-title">Details</h2>
          <dl className="detail-list">
            <dt>Status</dt>
            <dd>{incident.status}</dd>
            <dt>Severity</dt>
            <dd>{incident.severity}</dd>
            <dt>Assignee</dt>
            <dd>{incident.assignedTo?.username ?? '—'}</dd>
            <dt>Created by</dt>
            <dd>{incident.createdBy?.username ?? '—'}</dd>
            <dt>Message</dt>
            <dd>{incident.message}</dd>
            <dt>Created</dt>
            <dd>{new Date(incident.createdAt).toLocaleString()}</dd>
            <dt>Updated</dt>
            <dd>{new Date(incident.updatedAt).toLocaleString()}</dd>
          </dl>
        </div>

        {canUpdateStatus && nextStatuses.length > 0 && (
          <div className="card">
            <h2 className="section-title">Update Status</h2>
            <label>
              Note (optional)
              <input
                type="text"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="What changed?"
              />
            </label>
            <div className="status-actions">
              {nextStatuses.map((s) => (
                <button
                  key={s}
                  type="button"
                  className="btn btn-primary"
                  disabled={updating}
                  onClick={() => updateStatus(s)}
                >
                  → {s}
                </button>
              ))}
            </div>
            {error && <p className="error">{error}</p>}
          </div>
        )}
      </div>

      <div className="card">
        <h2 className="section-title">History</h2>
        <HistoryTimeline history={history} />
      </div>
    </div>
  );
}
