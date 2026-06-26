import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { AdminRoute, ProtectedRoute } from './auth/ProtectedRoute';
import { Layout } from './components/Layout';
import { LiveFeedProvider, useLiveFeed } from './ws/LiveFeedContext';
import { AdminOnCallPage } from './pages/AdminOnCallPage';
import { AdminUsersPage } from './pages/AdminUsersPage';
import { CreateIncidentPage } from './pages/CreateIncidentPage';
import { IncidentDetailPage } from './pages/IncidentDetailPage';
import { IncidentsPage } from './pages/IncidentsPage';
import { IntegrationsPage } from './pages/IntegrationsPage';
import { LoginPage } from './pages/LoginPage';
import { MyIncidentPage } from './pages/MyIncidentPage';

function AppLayout() {
  const { connected } = useLiveFeed();
  return <Layout liveConnected={connected} />;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<LiveFeedProvider><AppLayout /></LiveFeedProvider>}>
              <Route index element={<Navigate to="/incidents" replace />} />
              <Route path="incidents" element={<IncidentsPage />} />
              <Route path="incidents/new" element={<CreateIncidentPage />} />
              <Route path="incidents/:id" element={<IncidentDetailPage />} />
              <Route path="my-incident" element={<MyIncidentPage />} />
              <Route path="integrations" element={<IntegrationsPage />} />
              <Route element={<AdminRoute />}>
                <Route path="admin/users" element={<AdminUsersPage />} />
              </Route>
              <Route path="admin/on-call" element={<AdminOnCallPage />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/incidents" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
