// lib/api.ts

const RAW_API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? '/api';
const API_BASE = RAW_API_BASE.replace(/\/+$/, '');

export function setTokens(accessToken: string, refreshToken?: string) {
  localStorage.setItem('accessToken', accessToken);
  if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
}
export function getAccessToken() { return localStorage.getItem('accessToken'); }
export function getRefreshToken() { return localStorage.getItem('refreshToken'); }
export function clearTokens() { localStorage.removeItem('accessToken'); localStorage.removeItem('refreshToken'); }

function isAbsoluteUrl(url: string) { return /^https?:\/\//i.test(url); }
function normalizePath(url: string) {
  if (isAbsoluteUrl(url)) return url;
  if (API_BASE.endsWith('/api') && url.startsWith('/api')) {
    return url.replace(/^\/api(?=\/|$)/, '') || '/';
  }
  return url;
}
function buildUrl(url: string) {
  const path = normalizePath(url);
  return isAbsoluteUrl(path) ? path : `${API_BASE}${path.startsWith('/') ? '' : '/'}${path}`;
}
function isJson(headers: Headers) {
  const c = headers.get('content-type') || '';
  return c.includes('application/json') || c.includes('+json');
}
function shouldSetJsonContentType(body: any, method?: string) {
  if (!body || method?.toUpperCase() === 'GET') return false;
  if (typeof body === 'string') return true;
  if (body instanceof FormData) return false;
  return true;
}

const REFRESH_USE_COOKIE = process.env.NEXT_PUBLIC_REFRESH_USE_COOKIE === 'true';
const DEFAULT_CREDENTIALS: RequestCredentials = 'include';
let refreshPromise: Promise<boolean> | null = null;

export async function apiFetch(url: string, options: RequestInit = {}, _isRetry = false): Promise<any> {
  const method = (options.method || 'GET').toUpperCase();
  const token = getAccessToken();

  const baseHeaders: HeadersInit = {};
  if (shouldSetJsonContentType(options.body, method)) baseHeaders['Content-Type'] = 'application/json';
  if (token) baseHeaders['Authorization'] = `Bearer ${token}`;
  const headers: HeadersInit = { ...baseHeaders, ...(options.headers || {}) };

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 20000);
  let res: Response;

  try {
    res = await fetch(buildUrl(url), {
      ...options,
      method,
      headers,
      signal: controller.signal,
      credentials: options.credentials ?? DEFAULT_CREDENTIALS,
    });
  } catch (e) {
    clearTimeout(timeout);
    if (!_isRetry) {
      await new Promise(r => setTimeout(r, 300));
      return apiFetch(url, options, true);
    }
    throw new Error('네트워크 오류가 발생했습니다.');
  } finally {
    clearTimeout(timeout);
  }

  const redirectedToLogin = res.redirected && /\/login(?:[?;].*)?$/.test(new URL(res.url, location.origin).pathname);
  const isOpaqueRedirect = (res.type === 'opaqueredirect');

  if (res.status === 401 || redirectedToLogin || isOpaqueRedirect) {
    const isRefreshCall = /\/refresh-token$/.test(normalizePath(url));
    if (!isRefreshCall) {
      if (!refreshPromise) refreshPromise = tryRefreshToken();
      const ok = await refreshPromise.finally(() => (refreshPromise = null));
      if (ok) {
        const { headers: oldHeaders, ...rest } = options;
        const nextHeaders = oldHeaders && typeof oldHeaders === 'object'
          ? Object.fromEntries(Object.entries(oldHeaders).filter(([k]) => k.toLowerCase() !== 'authorization'))
          : undefined;
        return apiFetch(url, { ...rest, headers: nextHeaders }, true);
      }
    }
    clearTokens();
    throw new Error('인증 만료 - 다시 로그인하세요.');
  }

  if (res.status === 204) return {};

  if (!res.ok) {
    let errorMsg = `API 요청 실패 (${res.status})`;
    try {
      if (isJson(res.headers)) {
        const j = await res.json();
        errorMsg = j.message || j.error || JSON.stringify(j) || errorMsg;
      } else {
        const t = await res.text();
        if (t) errorMsg = t;
      }
    } catch {}
    throw new Error(errorMsg);
  }

  if (isJson(res.headers)) return res.status === 204 ? {} : await res.json();
  const ct = res.headers.get('content-type') || '';
  if (ct.startsWith('text/')) return await res.text();
  return await res.blob();
}

async function tryRefreshToken(): Promise<boolean> {
  const refreshToken = getRefreshToken();

  // 1) 쿠키 기반
  if (REFRESH_USE_COOKIE) {
    try {
      const res = await fetch(buildUrl('/refresh-token'), {
        method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json' }
      });
      if (!res.ok) return false;
      const data = await res.json().catch(() => ({}));
      if (data?.accessToken) { setTokens(data.accessToken, data.refreshToken); return true; }
      return false;
    } catch { return false; }
  }

  // 2) 바디 기반
  for (const key of ['refreshToken', 'token'] as const) {
    try {
      const res = await fetch(buildUrl('/refresh-token'), {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, credentials: DEFAULT_CREDENTIALS,
        body: JSON.stringify({ [key]: refreshToken })
      });
      if (res.ok) {
        const data = await res.json().catch(() => ({}));
        if (data?.accessToken) { setTokens(data.accessToken, data.refreshToken); return true; }
      }
    } catch {}
  }

  // 3) 헤더 기반
  if (refreshToken) {
    try {
      const res = await fetch(buildUrl('/refresh-token'), {
        method: 'POST', credentials: DEFAULT_CREDENTIALS,
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${refreshToken}` }
      });
      if (res.ok) {
        const data = await res.json().catch(() => ({}));
        if (data?.accessToken) { setTokens(data.accessToken, data.refreshToken); return true; }
      }
    } catch {}
  }
  return false;
}

// --- API helpers ---
export function registerUser(user: any) {
  return apiFetch('/register', { method: 'POST', body: JSON.stringify(user) });
}

// ✅ 로그인: 백엔드 계약에 맞춰 { email, password }
export async function loginUser(email: string, password: string) {
  const data = await apiFetch('/login', { method: 'POST', body: JSON.stringify({ email, password }) });
  if (!data?.accessToken) throw new Error('토큰이 응답되지 않았습니다.');
  setTokens(data.accessToken, data.refreshToken);
  return data;
}

export function getMyInfo() { return apiFetch('/me'); }
export function logout() { clearTokens(); }

export async function logoutUser() {
  try {
    await apiFetch('/logout', {
      method: 'POST',
      credentials: 'include',
    });
  } catch (e) {
    console.warn('서버 로그아웃 실패:', e);
  } finally {
    clearTokens();
  }
}

export async function updateUser(data: any) {
  return apiFetch('/api/user', {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}