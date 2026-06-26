import type { IncidentHistory } from '../types';

interface HistoryTimelineProps {
  history: IncidentHistory[];
}

export function HistoryTimeline({ history }: HistoryTimelineProps) {
  if (history.length === 0) {
    return <p className="muted">No history yet.</p>;
  }

  return (
    <ul className="timeline">
      {history.map((entry, i) => (
        <li key={`${entry.createdAt}-${i}`} className="timeline-item">
          <div className="timeline-meta">
            <span className="mono">{entry.actionType}</span>
            <span className="muted">{new Date(entry.createdAt).toLocaleString()}</span>
          </div>
          <div className="timeline-body">
            <span className="muted">{entry.actorUsername}</span>
            {entry.fromStatus && entry.toStatus && (
              <span>
                {' '}
                {entry.fromStatus} → {entry.toStatus}
              </span>
            )}
            {entry.message && <p className="timeline-msg">{entry.message}</p>}
          </div>
        </li>
      ))}
    </ul>
  );
}
