import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import './Layout.css';

interface LayoutProps {
  liveConnected?: boolean;
}

export function Layout({ liveConnected }: LayoutProps) {
  const { user, logout, isAdmin, canUpdateStatus } = useAuth();
  const navigate = useNavigate();

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
        </div>
        <nav className="nav">
          <Link to="/incidents">Incidents</Link>
          <Link to="/my-incident">My Incident</Link>
          <Link to="/incidents/new">New</Link>
          <Link to="/integrations">Integrations</Link>
          {isAdmin && <Link to="/admin/users">Users</Link>}
          {(isAdmin || canUpdateStatus) && <Link to="/admin/on-call">On-Call</Link>}
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
