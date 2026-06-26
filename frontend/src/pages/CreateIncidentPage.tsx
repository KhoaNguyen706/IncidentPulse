import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch } from '../api/client';
import type { Incident, IncidentSeverity } from '../types';
import { ALL_SEVERITIES } from '../types';

export function CreateIncidentPage() {
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [severity, setSeverity] = useState<IncidentSeverity>('SEV2');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const incident = await apiFetch<Incident>('/api/v1/incident', {
        method: 'POST',
        body: { title, severity, message },
      });
      navigate(`/incidents/${incident.id}`, { state: { incident } });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create incident');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <h1 className="page-title">New Incident</h1>
      <div className="card form-card">
        <form onSubmit={handleSubmit} className="form">
          <label>
            Title
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={200}
              required
            />
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
            <textarea
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              rows={4}
              required
            />
          </label>
          {error && <p className="error">{error}</p>}
          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Creating...' : 'Create Incident'}
            </button>
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => navigate('/incidents')}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
