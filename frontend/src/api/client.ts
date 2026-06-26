const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

const ACCESS_KEY = 'ip_access';
const REFRESH_KEY = 'ip_refresh';

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

  const json = await res.json();
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
    if (token) headers['Authorization'] = `Bearer ${token}`;
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

  const rawText = await res.text();
  // #region agent log
  fetch('http://127.0.0.1:7757/ingest/a8176d9f-bf56-41d6-adcb-168fdd18384c',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'00adae'},body:JSON.stringify({sessionId:'00adae',location:'client.ts:apiFetch:post',message:'api response',data:{path,status:res.status,bodyPreview:rawText.slice(0,120),origin:window.location.origin},timestamp:Date.now(),hypothesisId:'A'})}).catch(()=>{});
  // #endregion
  let json: { success?: boolean; data?: T; message?: string };
  try {
    json = JSON.parse(rawText);
  } catch {
    throw new Error(rawText || `Request failed (${res.status})`);
  }

  if (!res.ok || json.success === false) {
    throw new Error(json.message ?? `Request failed (${res.status})`);
  }

  return json.data as T;
}

export async function apiLogin(username: string, password: string) {
  const url = `${API_BASE}/api/v1/auth/login`;
  // #region agent log
  fetch('http://127.0.0.1:7757/ingest/a8176d9f-bf56-41d6-adcb-168fdd18384c',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'00adae'},body:JSON.stringify({sessionId:'00adae',location:'client.ts:apiLogin:pre',message:'login attempt',data:{url,origin:window.location.origin,apiBase:API_BASE||'(same-origin)'},timestamp:Date.now(),hypothesisId:'A'})}).catch(()=>{});
  // #endregion
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  const rawText = await res.text();
  // #region agent log
  fetch('http://127.0.0.1:7757/ingest/a8176d9f-bf56-41d6-adcb-168fdd18384c',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'00adae'},body:JSON.stringify({sessionId:'00adae',location:'client.ts:apiLogin:post',message:'login response',data:{status:res.status,contentType:res.headers.get('content-type'),bodyPreview:rawText.slice(0,120),origin:window.location.origin},timestamp:Date.now(),hypothesisId:'A'})}).catch(()=>{});
  // #endregion
  let json: { success?: boolean; data?: { access: string; refresh: string }; message?: string };
  try {
    json = JSON.parse(rawText);
  } catch {
    throw new Error(rawText || `Login failed (${res.status})`);
  }
  if (!res.ok || !json.success) {
    throw new Error(json.message ?? 'Login failed');
  }
  setTokens(json.data!.access, json.data!.refresh);
  return json.data;
}

export function apiLogout(): void {
  clearTokens();
}
