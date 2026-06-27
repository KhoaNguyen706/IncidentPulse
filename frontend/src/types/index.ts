export type IncidentStatus = 'OPENED' | 'INVESTIGATING' | 'RESOLVED' | 'CLOSED';
export type IncidentSeverity = 'SEV1' | 'SEV2' | 'SEV3' | 'SEV4';
export type UserRole = 'ADMIN' | 'ENGINEER' | 'VIEWER' | 'MANAGER';
export type UserTeam =
  | 'PAYMENT'
  | 'API'
  | 'SERVICE'
  | 'SYSTEM'
  | 'FRONTEND'
  | 'BACKEND'
  | 'SECURITY'
  | 'NETWORK';

export type ActionType =
  | 'CREATED'
  | 'ASSIGNED'
  | 'ACKNOWLEDGED'
  | 'STATUS_CHANGED'
  | 'COMMENTED'
  | 'RESOLVED'
  | 'CLOSED';

export interface ApiResponse<T> {
  code: number;
  success: boolean;
  data: T;
  now: string;
  message: string;
}

export interface User {
  id?: number;
  name: string;
  username: string;
  email: string;
  active: boolean;
  role: UserRole;
  team: UserTeam;
  createdAt?: string;
}

export interface Incident {
  id: number;
  title: string;
  createdBy: User;
  assignedTo: User;
  status: IncidentStatus;
  severity: IncidentSeverity;
  message: string;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface IncidentHistory {
  actionType: ActionType;
  fromStatus: IncidentStatus | null;
  toStatus: IncidentStatus | null;
  actorUsername: string;
  message: string;
  createdAt: string;
}

export interface AuthTokens {
  access: string;
  refresh: string;
  success: boolean;
  createdAt: string;
}

export interface OnCallShift {
  id?: number;
  user: User;
  startedAt: string;
  endAt: string;
  status?: 'ACTIVE' | 'UPCOMING' | 'ENDED';
}

export interface IncidentEvent {
  id: number;
  type: 'CREATED' | 'STATUS_CHANGED';
  status: IncidentStatus;
  fromStatus?: IncidentStatus;
  toStatus?: IncidentStatus;
  actor: string;
  occurredAt: string;
}

export const STATUS_TRANSITIONS: Record<IncidentStatus, IncidentStatus[]> = {
  OPENED: ['INVESTIGATING'],
  INVESTIGATING: ['RESOLVED'],
  RESOLVED: ['CLOSED', 'INVESTIGATING'],
  CLOSED: [],
};

export const ALL_STATUSES: IncidentStatus[] = [
  'OPENED',
  'INVESTIGATING',
  'RESOLVED',
  'CLOSED',
];

export const ALL_SEVERITIES: IncidentSeverity[] = ['SEV1', 'SEV2', 'SEV3', 'SEV4'];

export const ALL_ROLES: UserRole[] = ['ADMIN', 'ENGINEER', 'VIEWER', 'MANAGER'];

export const ALL_TEAMS: UserTeam[] = [
  'PAYMENT',
  'API',
  'SERVICE',
  'SYSTEM',
  'FRONTEND',
  'BACKEND',
  'SECURITY',
  'NETWORK',
];
