import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';

interface Signal {
  id: number;
  subjectRef: string;
  risk: string;
  score: number;
  flags: string;
  createdAt: string;
}

export default function AdminPage() {
  const { api, authenticated } = useApi();
  const [signals, setSignals] = useState<Signal[]>([]);
  const [highCount, setHighCount] = useState<number>(0);
  const [health, setHealth] = useState<string>('—');
  const [errors, setErrors] = useState<string[]>([]);

  const load = useCallback(async () => {
    const nextErrors: string[] = [];
    try {
      const data = await api.get<unknown[]>(`/security/fraud/signals?tenantId=1`);
      setSignals((data as unknown as Signal[]) ?? []);
    } catch (e) {
      nextErrors.push(e instanceof Error ? e.message : 'Failed to load signals');
    }
    try {
      const c = await api.get<number>('/security/fraud/high-count');
      setHighCount(c ?? 0);
    } catch (e) {
      nextErrors.push(e instanceof Error ? e.message : 'Failed to load high count');
    }
    try {
      const h = await api.get<{ status: string }>('/health/live');
      setHealth(h.status ?? '—');
    } catch {
      setHealth('DOWN');
    }
    setErrors(nextErrors);
  }, [api]);

  useEffect(() => {
    if (authenticated) void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authenticated]);

  if (!authenticated) {
    return (
      <section className="page">
        <h1>Admin</h1>
        <p className="muted">Sign in with an admin account.</p>
        <Link className="btn primary" to="/login">
          Sign in
        </Link>
      </section>
    );
  }

  return (
    <section className="page">
      <h1>Admin Portal</h1>
      <div className="stats">
        <div className="stat card">
          <span className="value">{highCount}</span>
          <span className="label">High-risk signals</span>
        </div>
        <div className="stat card">
          <span className="value">{health}</span>
          <span className="label">Platform health</span>
        </div>
      </div>
      {errors.map((e, i) => (
        <p className="error" key={i}>
          {e}
        </p>
      ))}

      <h2>Recent fraud signals</h2>
      <table className="table">
        <thead>
          <tr>
            <th>Ref</th>
            <th>Risk</th>
            <th>Score</th>
            <th>Flags</th>
            <th>Detected</th>
          </tr>
        </thead>
        <tbody>
          {signals.map((s) => (
            <tr key={s.id}>
              <td>{s.subjectRef}</td>
              <td>
                <span className={`badge ${s.risk.toLowerCase()}`}>{s.risk}</span>
              </td>
              <td>{s.score}</td>
              <td className="muted">{s.flags}</td>
              <td>{new Date(s.createdAt).toLocaleString()}</td>
            </tr>
          ))}
          {signals.length === 0 && (
            <tr>
              <td colSpan={5} className="muted">
                No fraud signals recorded.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      <p className="muted">
        Manage tenants, approve shop applications, work the support queue and review
        flagged transactions here.
      </p>
    </section>
  );
}
