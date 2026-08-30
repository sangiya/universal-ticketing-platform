import {
  createContext,
  useContext,
  useState,
  type ReactNode,
} from 'react';
import { HttpApiClient, type ApiClient } from '../api/client';

interface ApiContextValue {
  api: ApiClient;
  authenticated: boolean;
  username: string | null;
  login: (token: string, username: string) => void;
  logout: () => void;
}

const ApiContext = createContext<ApiContextValue | null>(null);

export function ApiProvider({ children }: { children: ReactNode }) {
  const [api] = useState<ApiClient>(() => new HttpApiClient());
  const [username, setUsername] = useState<string | null>(
    () => localStorage.getItem('ticketmesh_user')
  );

  const authenticated = Boolean(api.token && username);

  const login = (token: string, user: string) => {
    api.setToken(token);
    localStorage.setItem('ticketmesh_user', user);
    setUsername(user);
  };

  const logout = () => {
    api.setToken(null);
    localStorage.removeItem('ticketmesh_user');
    setUsername(null);
  };

  return (
    <ApiContext.Provider value={{ api, authenticated, username, login, logout }}>
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
