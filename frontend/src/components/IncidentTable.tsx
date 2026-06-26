import { useNavigate } from 'react-router-dom';
import type { Incident } from '../types';
import { SeverityBadge, StatusBadge } from './StatusBadge';

interface IncidentTableProps {
  incidents: Incident[];
}

export function IncidentTable({ incidents }: IncidentTableProps) {
  const navigate = useNavigate();

  if (incidents.length === 0) {
    return <p className="muted">No incidents found.</p>;
  }

  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Title</th>
            <th>Status</th>
            <th>Severity</th>
            <th>Assignee</th>
            <th>Created</th>
          </tr>
        </thead>
        <tbody>
          {incidents.map((inc) => (
            <tr
              key={inc.id}
              className="clickable-row"
              onClick={() => navigate(`/incidents/${inc.id}`, { state: { incident: inc } })}
            >
              <td className="mono">#{inc.id}</td>
              <td>{inc.title}</td>
              <td>
                <StatusBadge status={inc.status} />
              </td>
              <td>
                <SeverityBadge severity={inc.severity} />
              </td>
              <td>{inc.assignedTo?.username ?? '—'}</td>
              <td className="muted">{new Date(inc.createdAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
