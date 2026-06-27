import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { apiFetch } from '../api/client';
import { OnCallScheduleTable } from '../components/OnCallPanel';
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
  const location = useLocation();
  const [schedule, setSchedule] = useState<OnCallShift[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const defaults = defaultShiftTimes();
  const [userId, setUserId] = useState('');
  const [startedAt, setStartedAt] = useState(defaults.start);
  const [endAt, setEndAt] = useState(defaults.end);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [allUsers, allShifts] = await Promise.all([
        apiFetch<User[]>('/api/v1/users'),
        apiFetch<OnCallShift[]>('/api/v1/on-call/shifts'),
      ]);
      setUsers(allUsers);
      setSchedule(allShifts);
      setUserId((prev) => {
        if (prev && allUsers.some((u) => String(u.id) === prev)) return prev;
        return '';
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load on-call schedule');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load, location.key]);

  const assignableUsers = users.filter(
    (u) =>
      u.active &&
      u.id != null &&
      (u.role === 'ENGINEER' || u.role === 'ADMIN'),
  );

  const shiftBody = () => ({
    startedAt: new Date(startedAt).toISOString(),
    endAt: new Date(endAt).toISOString(),
  });

  const createShift = async (path: string, successMessage: string) => {
    setSubmitting(true);
    setError('');
    setSuccess('');
    try {
      await apiFetch<OnCallShift>(path, {
        method: 'POST',
        body: shiftBody(),
      });
      setSuccess(successMessage);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create shift');
    } finally {
      setSubmitting(false);
    }
  };

  const handleAssignEngineer = async (e: FormEvent) => {
    e.preventDefault();
    if (!userId) return;
    await createShift(
      `/api/v1/on-call/${userId}`,
      'On-call shift assigned. The engineer has been emailed.',
    );
  };

  const handleAssignSelf = async (e: FormEvent) => {
    e.preventDefault();
    await createShift(
      '/api/v1/on-call/me',
      'You are now scheduled for on-call. A confirmation email has been sent.',
    );
  };

  if (loading) return <p className="muted">Loading schedule...</p>;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Manage On-Call</h1>
        <div className="header-actions">
          <Link to="/oncall" className="btn btn-ghost">
            View current on-call
          </Link>
          <button type="button" className="btn btn-ghost" onClick={load}>
            Refresh
          </button>
        </div>
      </div>

      <p className="muted">
        Assign an engineer to an on-call window, or schedule yourself. The assignee receives an
        email notification.
      </p>

      {error && <p className="error">{error}</p>}
      {success && <p className="success">{success}</p>}

      <div className="card" style={{ marginTop: '1rem' }}>
        <h2 className="section-title">On-call schedule</h2>
        <OnCallScheduleTable shifts={schedule} />
      </div>

      <div className="detail-grid">
        <div className="card form-card">
          <h2 className="section-title">Assign engineer</h2>
          <form onSubmit={handleAssignEngineer} className="form">
            <label>
              Engineer
              <select value={userId} onChange={(e) => setUserId(e.target.value)} required>
                <option value="">Select engineer</option>
                {assignableUsers.map((u) => (
                  <option key={u.id} value={String(u.id)}>
                    {u.username} ({u.role}) — {u.email}
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
              Assign engineer
            </button>
          </form>
        </div>

        <div className="card form-card">
          <h2 className="section-title">Assign yourself</h2>
          <p className="muted">Use the same start/end times as above, or change them first.</p>
          <form onSubmit={handleAssignSelf} className="form">
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
              Assign myself
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
