import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiFetch } from '../api/client';
import { SeverityBadge, StatusBadge } from '../components/StatusBadge';
import type { Incident } from '../types';

export function MyIncidentPage() {
  const [incident, setIncident] = useState<Incident | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    apiFetch<Incident>('/api/v1/incident/me')
      .then(setIncident)
      .catch((err) => {
        setError(err instanceof Error ? err.message : 'No assigned incident');
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className="muted">Loading...</p>;

  return (
    <div>
      <h1 className="page-title">My Incident</h1>
      {error || !incident ? (
        <div className="card">
          <p className="muted">No incident currently assigned to you.</p>
        </div>
      ) : (
        <div className="card">
          <div className="page-header">
            <h2>
              <span className="mono">#{incident.id}</span> {incident.title}
            </h2>
            <div className="badges-row">
              <StatusBadge status={incident.status} />
              <SeverityBadge severity={incident.severity} />
            </div>
          </div>
          <p>{incident.message}</p>
          <Link to={`/incidents/${incident.id}`} state={{ incident }} className="btn btn-primary">
            View details
          </Link>
        </div>
      )}
    </div>
  );
}
