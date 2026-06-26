import type { ReactNode } from 'react';
import { useAuth } from '../auth/AuthContext';
import type { UserRole } from '../types';

interface RoleGateProps {
  roles: UserRole[];
  children: ReactNode;
  fallback?: ReactNode;
}

export function RoleGate({ roles, children, fallback = null }: RoleGateProps) {
  const { role } = useAuth();
  if (!role || !roles.includes(role)) return <>{fallback}</>;
  return <>{children}</>;
}
