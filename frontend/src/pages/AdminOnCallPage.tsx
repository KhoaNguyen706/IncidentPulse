import { useEffect, useState, type FormEvent } from 'react';
import { apiFetch } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { RoleGate } from '../components/RoleGate';
import type { OnCallShift, User } from '../types';

function toLocalInput(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function defaultShiftTimes() {
  const start = new Date();
  const end = new Date(start.getTime() + 7 * 24 * 60 * 60 * 1000);
  return { start: toLocalInput(start.toISOString()), end: toLocalInput(end.toISOString()) };
}

export function AdminOnCallPage() {
  const { isAdmin } = useAuth();
  const [current, setCurrent] = useState<OnCallShift | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const defaults = defaultShiftTimes();
  const [userId, setUserId] = useState('');
  const [startedAt, setStartedAt] = useState(defaults.start);
  const [endAt, setEndAt] = useState(defaults.end);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      if (isAdmin) {
        const [shift, allUsers] = await Promise.all([
          apiFetch<OnCallShift>('/api/v1/on-call'),
          apiFetch<User[]>('/api/v1/users'),
        ]);
        setCurrent(shift);
        setUsers(allUsers);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load on-call data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [isAdmin]);

  const createShift = async (path: string) => {
    setSubmitting(true);
    setError('');
    setSuccess('');
    try {
      const shift = await apiFetch<OnCallShift>(path, {
        method: 'POST',
        body: {
          startedAt: new Date(startedAt).toISOString(),
          endAt: new Date(endAt).toISOString(),
        },
      });
      setCurrent(shift);
      setSuccess('On-call shift created.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create shift');
    } finally {
      setSubmitting(false);
    }
  };

  const handleAdminSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!userId) return;
    await createShift(`/api/v1/on-call/${userId}`);
  };

  const handleSelfSubmit = async (e: FormEvent) => {
    e.preventDefault();
    await createShift('/api/v1/on-call/me');
  };

  if (loading) return <p className="muted">Loading on-call data...</p>;

  return (
    <div>
      <h1 className="page-title">On-Call</h1>

      {current && (
        <div className="card">
          <h2 className="section-title">Current on-call</h2>
          <p>
            <strong>{current.user?.username}</strong> ({current.user?.team})
          </p>
          <p className="muted">
            {new Date(current.startedAt).toLocaleString()} —{' '}
            {new Date(current.endAt).toLocaleString()}
          </p>
        </div>
      )}

      {!current && (
        <div className="card">
          <p className="muted">No active on-call shift configured.</p>
        </div>
      )}

      {error && <p className="error">{error}</p>}
      {success && <p className="success">{success}</p>}

      <RoleGate roles={['ADMIN']}>
        <div className="card form-card">
          <h2 className="section-title">Assign on-call shift (Admin)</h2>
          <form onSubmit={handleAdminSubmit} className="form">
            <label>
              Engineer
              <select value={userId} onChange={(e) => setUserId(e.target.value)} required>
                <option value="">Select user</option>
                {users
                  .filter((u) => u.role === 'ENGINEER' || u.role === 'ADMIN')
                  .map((u) => (
                    <option key={u.id ?? u.username} value={u.id}>
                      {u.username} ({u.role})
                    </option>
                  ))}
              </select>
            </label>
            <label>
              Start
              <input
                type="datetime-local"
                value={startedAt}
                onChange={(e) => setStartedAt(e.target.value)}
                required
              />
            </label>
            <label>
              End
              <input
                type="datetime-local"
                value={endAt}
                onChange={(e) => setEndAt(e.target.value)}
                required
              />
            </label>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              Create shift
            </button>
          </form>
        </div>
      </RoleGate>

      <RoleGate roles={['ENGINEER', 'ADMIN']}>
        <div className="card form-card">
          <h2 className="section-title">Self-service on-call</h2>
          <form onSubmit={handleSelfSubmit} className="form">
            <label>
              Start
              <input
                type="datetime-local"
                value={startedAt}
                onChange={(e) => setStartedAt(e.target.value)}
                required
              />
            </label>
            <label>
              End
              <input
                type="datetime-local"
                value={endAt}
                onChange={(e) => setEndAt(e.target.value)}
                required
              />
            </label>
            <button type="submit" className="btn btn-ghost" disabled={submitting}>
              Set my on-call window
            </button>
          </form>
        </div>
      </RoleGate>
    </div>
  );
}
