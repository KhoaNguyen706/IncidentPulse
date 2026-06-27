import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiFetch } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { OnCallStatusCard } from '../components/OnCallPanel';
import type { OnCallShift } from '../types';

export function OnCallPage() {
  const { isAdmin, canUpdateStatus } = useAuth();
  const [current, setCurrent] = useState<OnCallShift | null>(null);
  const [myShift, setMyShift] = useState<OnCallShift | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [currentShift, activeForMe] = await Promise.all([
        apiFetch<OnCallShift | null>('/api/v1/on-call'),
        canUpdateStatus
          ? apiFetch<OnCallShift | null>('/api/v1/on-call/me')
          : Promise.resolve(null),
      ]);
      setCurrent(currentShift);
      setMyShift(activeForMe);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load on-call status');
    } finally {
      setLoading(false);
    }
  }, [canUpdateStatus]);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) return <p className="muted">Loading on-call status...</p>;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">On-Call</h1>
        <button type="button" className="btn btn-ghost" onClick={load}>
          Refresh
        </button>
      </div>

      <p className="muted">
        Current on-call assignment for the team. New incidents are routed to this engineer.
      </p>

      {error && <p className="error">{error}</p>}

      <OnCallStatusCard
        title="Who is on call now"
        shift={current}
        emptyMessage="No engineer is on call right now."
        highlight={current?.status === 'ACTIVE'}
      />

      {canUpdateStatus && (
        <div style={{ marginTop: '1rem' }}>
          <OnCallStatusCard
            title="Your on-call window"
            shift={myShift}
            emptyMessage="You are not currently on call."
            highlight={myShift?.status === 'ACTIVE'}
          />
        </div>
      )}

      {isAdmin && (
        <div className="card" style={{ marginTop: '1rem' }}>
          <p>
            Need to schedule or change on-call?{' '}
            <Link to="/admin/oncall" className="btn btn-primary btn-sm">
              Manage on-call schedule
            </Link>
          </p>
        </div>
      )}
    </div>
  );
}
