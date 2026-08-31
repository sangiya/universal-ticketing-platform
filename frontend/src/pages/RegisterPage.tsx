import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApi, type Role } from '../context/ApiContext';
import { roleHome } from './LoginPage';

export default function RegisterPage() {
  const { api, login } = useApi();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<Role>('CUSTOMER');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await api.post<{ token: string; username: string; role: Role }>('/auth/register', {
        username,
        password,
        fullName,
        email,
        role,
      });
      login(res.token, res.username, res.role);
      navigate(roleHome(res.role));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed');
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="page narrow">
      <h1>Create your account</h1>
      <p className="muted">
        Customers can book directly. Agents and shops can apply to sell after signing up.
      </p>
      <form onSubmit={submit} className="stack">
        <input
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
          placeholder="Full name"
        />
        <input
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="Email"
          type="email"
        />
        <input
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Username"
        />
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Password"
        />
        <label className="field">
          <span className="label">I am a</span>
          <select value={role} onChange={(e) => setRole(e.target.value as Role)}>
            <option value="CUSTOMER">Customer (I want to buy)</option>
            <option value="AGENT">Agent / Seller (I want to sell)</option>
          </select>
        </label>
        {error && <p className="error">{error}</p>}
        <button className="btn primary" type="submit" disabled={busy}>
          {busy ? 'Creating…' : 'Create account'}
        </button>
      </form>
    </section>
  );
}
