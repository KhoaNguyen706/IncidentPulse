import { useCallback, useEffect, useState } from 'react';
import { apiFetch } from '../api/client';
import { IncidentTable } from '../components/IncidentTable';
import { useIncidentFeed } from '../ws/useIncidentFeed';
import type {
  Incident,
  IncidentEvent,
  IncidentSeverity,
  IncidentStatus,
  PageResponse,
} from '../types';
import { ALL_SEVERITIES, ALL_STATUSES } from '../types';

export function IncidentsPage() {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [statusFilter, setStatusFilter] = useState<IncidentStatus | ''>('');
  const [severityFilter, setSeverityFilter] = useState<IncidentSeverity | ''>('');
  const [assigneeFilter, setAssigneeFilter] = useState('');

  const loadIncidents = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams({
        page: String(page),
        size: '20',
        sort: 'createdAt,desc',
      });
      if (statusFilter) params.set('status', statusFilter);
      if (severityFilter) params.set('severity', severityFilter);
      if (assigneeFilter.trim()) params.set('assignee', assigneeFilter.trim());

      const data = await apiFetch<PageResponse<Incident>>(
        `/api/v1/incident?${params.toString()}`,
      );
      setIncidents(data.content);
      setTotalPages(data.totalPages);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load incidents');
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter, severityFilter, assigneeFilter]);

  useEffect(() => {
    loadIncidents();
  }, [loadIncidents]);

  const handleEvent = useCallback(
    (event: IncidentEvent) => {
      setIncidents((prev) => {
        const idx = prev.findIndex((i) => i.id === event.id);
        if (event.type === 'CREATED' && idx === -1 && page === 0) {
          loadIncidents();
          return prev;
        }
        if (idx === -1) return prev;
        const updated = [...prev];
        updated[idx] = {
          ...updated[idx],
          status: event.status,
          updatedAt: event.occurredAt,
        };
        return updated;
      });
    },
    [page, loadIncidents],
  );

  const { connected } = useIncidentFeed({ onEvent: handleEvent });

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Incidents</h1>
        {connected && <span className="live-badge">LIVE</span>}
      </div>

      <div className="filters card">
        <label>
          Status
          <select
            value={statusFilter}
            onChange={(e) => {
              setPage(0);
              setStatusFilter(e.target.value as IncidentStatus | '');
            }}
          >
            <option value="">All</option>
            {ALL_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
        <label>
          Severity
          <select
            value={severityFilter}
            onChange={(e) => {
              setPage(0);
              setSeverityFilter(e.target.value as IncidentSeverity | '');
            }}
          >
            <option value="">All</option>
            {ALL_SEVERITIES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
        <label>
          Assignee
          <input
            type="text"
            placeholder="username"
            value={assigneeFilter}
            onChange={(e) => {
              setPage(0);
              setAssigneeFilter(e.target.value);
            }}
          />
        </label>
        <button type="button" className="btn btn-ghost" onClick={loadIncidents}>
          Refresh
        </button>
      </div>

      {error && <p className="error">{error}</p>}
      {loading ? (
        <p className="muted">Loading incidents...</p>
      ) : (
        <>
          <IncidentTable incidents={incidents} />
          <div className="pagination">
            <button
              type="button"
              className="btn btn-ghost"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              Previous
            </button>
            <span className="muted">
              Page {page + 1} of {Math.max(totalPages, 1)}
            </span>
            <button
              type="button"
              className="btn btn-ghost"
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </button>
          </div>
        </>
      )}
    </div>
  );
}
