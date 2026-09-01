import {
  createContext,
  useContext,
  useState,
  type ReactNode,
} from 'react';
import { HttpApiClient, type ApiClient } from '../api/client';

export type Role = 'ADMIN' | 'AGENT' | 'CUSTOMER';

const ROLE_KEY = 'ticketmesh_role';
const USER_ID_KEY = 'ticketmesh_user_id';
const TENANT_ID_KEY = 'ticketmesh_tenant_id';

interface ApiContextValue {
  api: ApiClient;
  authenticated: boolean;
  username: string | null;
  role: Role | null;
  userId: number | null;
  tenantId: number | null;
  login: (
    accessToken: string,
    refreshToken: string | null,
    username: string,
    role: Role,
    userId?: number,
    tenantId?: number | null,
  ) => void;
  logout: () => void;
  clearError: () => void;
}

const ApiContext = createContext<ApiContextValue | null>(null);

export function ApiProvider({ children }: { children: ReactNode }) {
  const [api] = useState<ApiClient>(() => new HttpApiClient());
  const [username, setUsername] = useState<string | null>(
    () => localStorage.getItem('ticketmesh_user'),
  );
  const [role, setRole] = useState<Role | null>(
    () => (localStorage.getItem(ROLE_KEY) as Role | null) ?? null,
  );
  const [userId, setUserId] = useState<number | null>(() => {
    const raw = localStorage.getItem(USER_ID_KEY);
    return raw ? Number(raw) : null;
  });
  const [tenantId, setTenantId] = useState<number | null>(() => {
    const raw = localStorage.getItem(TENANT_ID_KEY);
    return raw ? Number(raw) : null;
  });

  const authenticated = Boolean(api.token && username);

  const login = (
    accessToken: string,
    refreshToken: string | null,
    user: string,
    userRole: Role,
    uId?: number,
    tId?: number | null,
  ) => {
    api.setToken(accessToken);
    if (refreshToken) api.setRefreshToken(refreshToken);
    localStorage.setItem('ticketmesh_user', user);
    localStorage.setItem(ROLE_KEY, userRole);
    if (uId !== undefined) {
      localStorage.setItem(USER_ID_KEY, String(uId));
      setUserId(uId);
    }
    if (tId !== undefined && tId !== null) {
      localStorage.setItem(TENANT_ID_KEY, String(tId));
      setTenantId(tId);
    }
    setUsername(user);
    setRole(userRole);
  };

  const logout = () => {
    api.setToken(null);
    api.setRefreshToken(null);
    localStorage.removeItem('ticketmesh_user');
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(USER_ID_KEY);
    localStorage.removeItem(TENANT_ID_KEY);
    setUsername(null);
    setRole(null);
    setUserId(null);
    setTenantId(null);
  };

  return (
    <ApiContext.Provider
      value={{ api, authenticated, username, role, userId, tenantId, login, logout, clearError: () => {} }}
    >
      {children}
    </ApiContext.Provider>
  );
}

export function useApi(): ApiContextValue {
  const ctx = useContext(ApiContext);
  if (!ctx) {
    throw new Error('useApi must be used within ApiProvider');
  }
  return ctx;
}
