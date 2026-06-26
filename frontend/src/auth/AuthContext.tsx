import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import {
  apiFetch,
  apiLogin,
  apiLogout,
  getAccessToken,
} from '../api/client';
import type { User, UserRole } from '../types';

interface AuthContextValue {
  user: User | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
  isAdmin: boolean;
  canUpdateStatus: boolean;
  role: UserRole | null;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshUser = useCallback(async () => {
    const token = getAccessToken();
    if (!token) {
      setUser(null);
      return;
    }
    try {
      const me = await apiFetch<User>('/api/v1/users/me');
      setUser(me);
    } catch {
      setUser(null);
    }
  }, []);

  useEffect(() => {
    refreshUser().finally(() => setLoading(false));
  }, [refreshUser]);

  const login = useCallback(
    async (username: string, password: string) => {
      await apiLogin(username, password);
      await refreshUser();
    },
    [refreshUser],
  );

  const logout = useCallback(() => {
    apiLogout();
    setUser(null);
  }, []);

  const role = user?.role ?? null;
  const isAdmin = role === 'ADMIN';
  const canUpdateStatus = role === 'ADMIN' || role === 'ENGINEER';

  const value = useMemo(
    () => ({
      user,
      loading,
      login,
      logout,
      refreshUser,
      isAdmin,
      canUpdateStatus,
      role,
    }),
    [user, loading, login, logout, refreshUser, isAdmin, canUpdateStatus, role],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
