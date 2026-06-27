const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

const ACCESS_KEY = 'ip_access';
const REFRESH_KEY = 'ip_refresh';

interface ApiEnvelope<T> {
  success?: boolean;
  data?: T;
  message?: string;
  code?: number;
}

export function getAccessToken(): string | null {
  return sessionStorage.getItem(ACCESS_KEY);
}

export function getRefreshToken(): string | null {
  return sessionStorage.getItem(REFRESH_KEY);
}

export function setTokens(access: string, refresh: string): void {
  sessionStorage.setItem(ACCESS_KEY, access);
  sessionStorage.setItem(REFRESH_KEY, refresh);
}

export function clearTokens(): void {
  sessionStorage.removeItem(ACCESS_KEY);
  sessionStorage.removeItem(REFRESH_KEY);
}

export function getApiBase(): string {
  return API_BASE;
}

export function getWsUrl(): string {
  if (API_BASE) {
    const url = new URL(API_BASE);
    const protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${url.host}/ws`;
  }
  return `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`;
}

export function getSockJsUrl(): string {
  if (API_BASE) {
    return `${API_BASE.replace(/\/$/, '')}/ws`;
  }
  return `${window.location.origin}/ws`;
}

type RequestOptions = Omit<RequestInit, 'body'> & {
  body?: unknown;
  auth?: boolean;
};

function parseApiEnvelope<T>(rawText: string, status: number): ApiEnvelope<T> {
  const trimmed = rawText.trim();
  if (!trimmed) {
    // 204 No Content or empty 200 — treat as success with no payload.
    if (status >= 200 && status < 300) {
      return { success: true, data: undefined };
    }
    throw new Error(`Empty response (${status})`);
  }
  try {
    return JSON.parse(trimmed) as ApiEnvelope<T>;
  } catch {
    throw new Error(trimmed || `Invalid response (${status})`);
  }
}

let refreshPromise: Promise<boolean> | null = null;

async function refreshAccessToken(): Promise<boolean> {
  const refresh = getRefreshToken();
  if (!refresh) return false;

  const res = await fetch(`${API_BASE}/api/v1/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: refresh }),
  });

  if (!res.ok) return false;

  const json = parseApiEnvelope<{ access: string; refresh: string }>(
    await res.text(),
    res.status,
  );
  if (json.success && json.data?.access) {
    setTokens(json.data.access, json.data.refresh ?? refresh);
    return true;
  }
  return false;
}

async function ensureRefreshed(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

export async function apiFetch<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { body, auth = true, headers: extraHeaders, ...rest } = options;

  const headers: Record<string, string> = {
    ...(extraHeaders as Record<string, string>),
  };

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  if (auth) {
    const token = getAccessToken();
    if (!token) {
      clearTokens();
      window.location.href = '/login';
      throw new Error('Unauthorized');
    }
    headers['Authorization'] = `Bearer ${token}`;
  }

  const doFetch = () =>
    fetch(`${API_BASE}${path}`, {
      ...rest,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });

  let res = await doFetch();

  if (res.status === 401 && auth) {
    const refreshed = await ensureRefreshed();
    if (refreshed) {
      const token = getAccessToken();
      if (token) headers['Authorization'] = `Bearer ${token}`;
      res = await doFetch();
    }
  }

  if (res.status === 401 && auth) {
    clearTokens();
    window.location.href = '/login';
    throw new Error('Unauthorized');
  }

  const json = parseApiEnvelope<T>(await res.text(), res.status);

  if (!res.ok || json.success === false) {
    throw new Error(json.message ?? `Request failed (${res.status})`);
  }

  return json.data as T;
}

export async function apiLogin(username: string, password: string) {
  const res = await fetch(`${API_BASE}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });

  const json = parseApiEnvelope<{ access: string; refresh: string }>(
    await res.text(),
    res.status,
  );

  if (!res.ok || !json.success || !json.data?.access) {
    throw new Error(json.message ?? 'Login failed');
  }

  setTokens(json.data.access, json.data.refresh);
  return json.data;
}

export function apiLogout(): void {
  clearTokens();
}
