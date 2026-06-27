import { useEffect, useState } from 'react';
import { apiFetch } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { OnCallStatusCard } from '../components/OnCallPanel';
import { IncidentTable } from '../components/IncidentTable';
import type { Incident, OnCallShift } from '../types';

export function MyIncidentPage() {
  const { user } = useAuth();
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [myShift, setMyShift] = useState<OnCallShift | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([
      apiFetch<Incident[]>('/api/v1/incident/me'),
      apiFetch<OnCallShift | null>('/api/v1/on-call/me'),
    ])
      .then(([assigned, shift]) => {
        setIncidents(assigned);
        setMyShift(shift);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : 'Failed to load your workspace');
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className="muted">Loading...</p>;

  return (
    <div>
      <h1 className="page-title">My Incidents</h1>
      <p className="muted">
        Your on-call window and incidents assigned to{' '}
        <strong>{user?.username ?? 'you'}</strong>.
      </p>

      {error && <p className="error">{error}</p>}

      <OnCallStatusCard
        title="Your on-call window"
        shift={myShift}
        emptyMessage="You are not currently scheduled for on-call."
        highlight={myShift?.status === 'ACTIVE'}
      />

      <div className="card" style={{ marginTop: '1rem' }}>
        <h2 className="section-title">Assigned incidents</h2>
        {!error && incidents.length === 0 ? (
          <p className="muted">No incidents currently assigned to you.</p>
        ) : (
          <IncidentTable incidents={incidents} />
        )}
      </div>
    </div>
  );
}
