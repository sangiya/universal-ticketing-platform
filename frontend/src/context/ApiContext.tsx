import {
  createContext,
  useContext,
  useState,
  type ReactNode,
} from 'react';
import { HttpApiClient, type ApiClient } from '../api/client';

export type Role = 'ADMIN' | 'AGENT' | 'CUSTOMER';

const ROLE_KEY = 'ticketmesh_role';

interface ApiContextValue {
  api: ApiClient;
  authenticated: boolean;
  username: string | null;
  role: Role | null;
  login: (token: string, username: string, role: Role) => void;
  logout: () => void;
}

const ApiContext = createContext<ApiContextValue | null>(null);

export function ApiProvider({ children }: { children: ReactNode }) {
  const [api] = useState<ApiClient>(() => new HttpApiClient());
  const [username, setUsername] = useState<string | null>(
    () => localStorage.getItem('ticketmesh_user')
  );
  const [role, setRole] = useState<Role | null>(
    () => (localStorage.getItem(ROLE_KEY) as Role | null) ?? null
  );

  const authenticated = Boolean(api.token && username);

  const login = (token: string, user: string, userRole: Role) => {
    api.setToken(token);
    localStorage.setItem('ticketmesh_user', user);
    localStorage.setItem(ROLE_KEY, userRole);
    setUsername(user);
    setRole(userRole);
  };

  const logout = () => {
    api.setToken(null);
    localStorage.removeItem('ticketmesh_user');
    localStorage.removeItem(ROLE_KEY);
    setUsername(null);
    setRole(null);
  };

  return (
    <ApiContext.Provider value={{ api, authenticated, username, role, login, logout }}>
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
