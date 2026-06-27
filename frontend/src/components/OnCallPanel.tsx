import type { OnCallShift } from '../types';

function formatRange(shift: OnCallShift): string {
  return `${new Date(shift.startedAt).toLocaleString()} — ${new Date(shift.endAt).toLocaleString()}`;
}

interface OnCallStatusCardProps {
  title: string;
  shift: OnCallShift | null;
  emptyMessage: string;
  highlight?: boolean;
}

export function OnCallStatusCard({
  title,
  shift,
  emptyMessage,
  highlight = false,
}: OnCallStatusCardProps) {
  return (
    <div className={`card ${highlight ? 'on-call-highlight' : ''}`}>
      <h2 className="section-title">{title}</h2>
      {shift ? (
        <>
          <p>
            <strong>{shift.user?.username}</strong>
            {shift.user?.team ? ` (${shift.user.team})` : ''}
            {shift.status && (
              <span className={`shift-status shift-status-${shift.status.toLowerCase()}`}>
                {shift.status}
              </span>
            )}
          </p>
          <p className="muted">{formatRange(shift)}</p>
        </>
      ) : (
        <p className="muted">{emptyMessage}</p>
      )}
    </div>
  );
}

interface OnCallScheduleTableProps {
  shifts: OnCallShift[];
}

export function OnCallScheduleTable({ shifts }: OnCallScheduleTableProps) {
  if (shifts.length === 0) {
    return <p className="muted">No on-call shifts scheduled yet.</p>;
  }

  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>Engineer</th>
            <th>Team</th>
            <th>Start</th>
            <th>End</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {shifts.map((shift) => (
            <tr key={shift.id ?? `${shift.user?.username}-${shift.startedAt}`}>
              <td>{shift.user?.username ?? '—'}</td>
              <td>{shift.user?.team ?? '—'}</td>
              <td className="muted">{new Date(shift.startedAt).toLocaleString()}</td>
              <td className="muted">{new Date(shift.endAt).toLocaleString()}</td>
              <td>
                {shift.status ? (
                  <span className={`shift-status shift-status-${shift.status.toLowerCase()}`}>
                    {shift.status}
                  </span>
                ) : (
                  '—'
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
