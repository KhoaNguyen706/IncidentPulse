import { useEffect, useState } from 'react';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import { apiFetch } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import type { OnCallShift } from '../types';
import './Layout.css';

interface LayoutProps {
  liveConnected?: boolean;
}

export function Layout({ liveConnected }: LayoutProps) {
  const { user, logout, isAdmin, canUpdateStatus } = useAuth();
  const navigate = useNavigate();
  const [myShift, setMyShift] = useState<OnCallShift | null>(null);

  useEffect(() => {
    if (!canUpdateStatus) return;
    apiFetch<OnCallShift | null>('/api/v1/on-call/me')
      .then(setMyShift)
      .catch(() => setMyShift(null));
  }, [canUpdateStatus, user?.username]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="layout">
      <header className="header">
        <div className="header-left">
          <Link to="/incidents" className="logo">
            IncidentPulse
          </Link>
          {liveConnected && <span className="live-badge">LIVE</span>}
          {myShift?.status === 'ACTIVE' && (
            <Link to="/my-incident" className="on-call-badge">
              ON CALL until {new Date(myShift.endAt).toLocaleString()}
            </Link>
          )}
        </div>
        <nav className="nav">
          <Link to="/incidents">Incidents</Link>
          <Link to="/my-incident">My Incidents</Link>
          <Link to="/incidents/new">New</Link>
          <Link to="/integrations">Integrations</Link>
          <Link to="/oncall">On-Call</Link>
          {isAdmin && <Link to="/admin/users">Users</Link>}
          {isAdmin && <Link to="/admin/oncall">Manage On-Call</Link>}
        </nav>
        <div className="header-right">
          <span className="user-meta">
            {user?.username}
            <span className="role-tag">{user?.role}</span>
          </span>
          <button type="button" className="btn btn-ghost" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>
      <main className="main">
        <Outlet />
      </main>
    </div>
  );
}
