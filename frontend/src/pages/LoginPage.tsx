import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApi, type Role } from '../context/ApiContext';

export default function LoginPage() {
  const { api, login } = useApi();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await api.post<{ token: string; username: string; role: Role }>('/auth/login', {
        username,
        password,
      });
      login(res.token, res.username, res.role);
      navigate(roleHome(res.role));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="page narrow">
      <h1>Sign in</h1>
      <form onSubmit={submit} className="stack">
        <input
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Username"
          autoComplete="username"
        />
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Password"
          autoComplete="current-password"
        />
        {error && <p className="error">{error}</p>}
        <button className="btn primary" type="submit" disabled={busy}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </section>
  );
}

export function roleHome(role: Role | null): string {
  switch (role) {
    case 'ADMIN':
      return '/admin';
    case 'AGENT':
      return '/agent';
    case 'CUSTOMER':
      return '/marketplace';
    default:
      return '/';
  }
}
