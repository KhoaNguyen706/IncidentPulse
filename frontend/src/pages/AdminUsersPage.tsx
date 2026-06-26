import { useEffect, useState, type FormEvent } from 'react';
import { apiFetch } from '../api/client';
import type { User, UserRole, UserTeam } from '../types';
import { ALL_ROLES, ALL_TEAMS } from '../types';

export function AdminUsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<UserRole>('ENGINEER');
  const [team, setTeam] = useState<UserTeam>('BACKEND');
  const [submitting, setSubmitting] = useState(false);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const data = await apiFetch<User[]>('/api/v1/users');
      setUsers(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await apiFetch<User>('/api/v1/users', {
        method: 'POST',
        body: { name, email, username, password, active: true, role, team },
      });
      setShowForm(false);
      setName('');
      setEmail('');
      setUsername('');
      setPassword('');
      await loadUsers();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create user');
    } finally {
      setSubmitting(false);
    }
  };

  const deactivateUser = async (id: number) => {
    try {
      await apiFetch(`/api/v1/users/${id}`, {
        method: 'PATCH',
        body: { active: false },
      });
      await loadUsers();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to deactivate user');
    }
  };

  const deleteUser = async (id: number) => {
    if (!confirm('Delete this user permanently?')) return;
    try {
      await apiFetch(`/api/v1/users/${id}`, { method: 'DELETE' });
      await loadUsers();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete user');
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Users</h1>
        <button type="button" className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Cancel' : 'Add user'}
        </button>
      </div>

      {error && <p className="error">{error}</p>}

      {showForm && (
        <div className="card form-card">
          <form onSubmit={handleCreate} className="form">
            <label>
              Name
              <input value={name} onChange={(e) => setName(e.target.value)} required />
            </label>
            <label>
              Email
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </label>
            <label>
              Username
              <input value={username} onChange={(e) => setUsername(e.target.value)} required />
            </label>
            <label>
              Password
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </label>
            <label>
              Role
              <select value={role} onChange={(e) => setRole(e.target.value as UserRole)}>
                {ALL_ROLES.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Team
              <select value={team} onChange={(e) => setTeam(e.target.value as UserTeam)}>
                {ALL_TEAMS.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </label>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              Create user
            </button>
          </form>
        </div>
      )}

      {loading ? (
        <p className="muted">Loading users...</p>
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Username</th>
                <th>Email</th>
                <th>Role</th>
                <th>Team</th>
                <th>Active</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id ?? u.username}>
                  <td className="mono">{u.id ?? '—'}</td>
                  <td>{u.username}</td>
                  <td>{u.email}</td>
                  <td>{u.role}</td>
                  <td>{u.team}</td>
                  <td>{u.active ? 'Yes' : 'No'}</td>
                  <td className="actions-cell">
                    {u.active && u.id != null && (
                      <button
                        type="button"
                        className="btn btn-ghost btn-sm"
                        onClick={() => deactivateUser(u.id!)}
                      >
                        Deactivate
                      </button>
                    )}
                    {u.id != null && (
                      <button
                        type="button"
                        className="btn btn-ghost btn-sm"
                        onClick={() => deleteUser(u.id!)}
                      >
                        Delete
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
