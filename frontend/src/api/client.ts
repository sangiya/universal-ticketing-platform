const TOKEN_KEY = 'ticketmesh_token';
const REFRESH_KEY = 'ticketmesh_refresh';
const BASE = '/api';

export interface ApiClient {
  token: string | null;
  refreshToken: string | null;
  setToken(token: string | null): void;
  setRefreshToken(token: string | null): void;
  get<T>(path: string): Promise<T>;
  post<T>(path: string, body?: unknown): Promise<T>;
  put<T>(path: string, body?: unknown): Promise<T>;
  delete<T>(path: string): Promise<T>;
  blob(path: string): Promise<Blob>;
}

export class HttpApiClient implements ApiClient {
  token: string | null = localStorage.getItem(TOKEN_KEY);
  refreshToken: string | null = localStorage.getItem(REFRESH_KEY);

  setToken(token: string | null) {
    this.token = token;
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  }

  setRefreshToken(token: string | null) {
    this.refreshToken = token;
    if (token) {
      localStorage.setItem(REFRESH_KEY, token);
    } else {
      localStorage.removeItem(REFRESH_KEY);
    }
  }

  private async request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const doRequest = (tok: string | null) => {
      const headers: Record<string, string> = { 'Content-Type': 'application/json' };
      if (tok) headers['Authorization'] = `Bearer ${tok}`;
      return fetch(`${BASE}${path}`, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined,
      });
    };

    let res = await doRequest(this.token);
    if (res.status === 401 && this.refreshToken && !path.endsWith('/refresh') && !path.endsWith('/login')) {
      // Attempt one refresh and retry the original request
      const refreshed = await this.tryRefresh();
      if (refreshed) {
        res = await doRequest(this.token);
      }
    }

    if (!res.ok) {
      let message = `Request failed (${res.status})`;
      try {
        const err = (await res.json()) as { message?: string };
        if (err.message) message = err.message;
      } catch {
        /* ignore */
      }
      if (res.status === 401) {
        // Hard logout on a 401 we couldn't refresh past
        this.setToken(null);
        this.setRefreshToken(null);
      }
      throw new Error(message);
    }
    if (res.status === 204) {
      return undefined as T;
    }
    return (await res.json()) as T;
  }

  private async tryRefresh(): Promise<boolean> {
    if (!this.refreshToken) return false;
    try {
      const res = await fetch(`${BASE}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: this.refreshToken }),
      });
      if (!res.ok) return false;
      const data = (await res.json()) as {
        accessToken?: string;
        refreshToken?: string;
      };
      if (data.accessToken) this.setToken(data.accessToken);
      if (data.refreshToken) this.setRefreshToken(data.refreshToken);
      return Boolean(data.accessToken);
    } catch {
      return false;
    }
  }

  get<T>(path: string): Promise<T> {
    return this.request<T>('GET', path);
  }

  post<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>('POST', path, body);
  }

  put<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>('PUT', path, body);
  }

  delete<T>(path: string): Promise<T> {
    return this.request<T>('DELETE', path);
  }

  async blob(path: string): Promise<Blob> {
    const headers: Record<string, string> = {};
    if (this.token) headers['Authorization'] = `Bearer ${this.token}`;
    let res = await fetch(`${BASE}${path}`, { headers });
    if (res.status === 401 && this.refreshToken && !path.endsWith('/refresh') && !path.endsWith('/login')) {
      const refreshed = await this.tryRefresh();
      if (refreshed) {
        const retryHeaders: Record<string, string> = {};
        if (this.token) retryHeaders['Authorization'] = `Bearer ${this.token}`;
        res = await fetch(`${BASE}${path}`, { headers: retryHeaders });
      }
    }
    if (!res.ok) {
      throw new Error(`Request failed (${res.status})`);
    }
    return res.blob();
  }
}
