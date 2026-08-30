import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';

interface DomainStat {
  productType: string;
  orderCount: number;
  revenue: number;
}

interface TrendReport {
  tenantId: number | null;
  totalOrders: number;
  totalRevenue: number;
  averageOrderValue: number;
  domains: DomainStat[];
}

interface AnalystAnswer {
  question: string;
  answer: string;
  data: Record<string, unknown>;
}

export default function AnalyticsPage() {
  const { api, authenticated } = useApi();
  const [trend, setTrend] = useState<TrendReport | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState<AnalystAnswer | null>(null);
  const [asking, setAsking] = useState(false);
  const [askError, setAskError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<TrendReport>('/analytics/trend?tenantId=1');
      setTrend(data ?? null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load analytics');
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => {
    if (authenticated) void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authenticated]);

  const ask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim()) return;
    setAsking(true);
    setAskError(null);
    try {
      const data = await api.post<AnalystAnswer>('/analytics/ask', {
        question: question.trim(),
      });
      setAnswer(data ?? null);
    } catch (err) {
      setAskError(err instanceof Error ? err.message : 'Failed to get an answer');
    } finally {
      setAsking(false);
    }
  };

  if (!authenticated) {
    return (
      <section className="page">
        <h1>Analytics</h1>
        <p className="muted">Sign in to view tenant analytics and ask the data analyst.</p>
        <Link className="btn primary" to="/login">
          Sign in
        </Link>
      </section>
    );
  }

  const fmt = (n: number | undefined | null) =>
    n == null ? '—' : Number(n).toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });

  return (
    <section className="page">
      <h1>Analytics</h1>
      <p className="muted">
        Tenant revenue intelligence across every product domain, plus a
        natural-language data analyst.
      </p>
      {error && <p className="error">{error}</p>}
      {loading && <p className="muted">Loading analytics…</p>}

      {trend && (
        <>
          <div className="stats">
            <div className="stat card">
              <span className="value">{trend.totalOrders}</span>
              <span className="label">Total orders</span>
            </div>
            <div className="stat card">
              <span className="value">{fmt(trend.totalRevenue)}</span>
              <span className="label">Total revenue</span>
            </div>
            <div className="stat card">
              <span className="value">{fmt(trend.averageOrderValue)}</span>
              <span className="label">Average order value</span>
            </div>
          </div>

          <h2>Revenue by product domain</h2>
          {trend.domains.length === 0 ? (
            <p className="muted">No domain data recorded yet.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Product type</th>
                  <th>Order count</th>
                  <th>Revenue</th>
                </tr>
              </thead>
              <tbody>
                {trend.domains.map((d) => (
                  <tr key={d.productType}>
                    <td>
                      <span className="tag">{d.productType}</span>
                    </td>
                    <td>{d.orderCount}</td>
                    <td className="currency">{fmt(d.revenue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      <h2 style={{ marginTop: '2rem' }}>Ask the data analyst</h2>
      <p className="muted">
        Ask a question in plain language, e.g. "Which product domain generates the
        most revenue?"
      </p>
      <form className="search" onSubmit={(e) => void ask(e)}>
        <textarea
          rows={2}
          value={question}
          placeholder="Ask anything about your sales data…"
          onChange={(e) => setQuestion(e.target.value)}
        />
        <button className="btn primary" type="submit" disabled={asking}>
          {asking ? 'Analysing…' : 'Ask'}
        </button>
      </form>
      {askError && <p className="error">{askError}</p>}

      {answer && (
        <div className="card" style={{ backgroundColor: '#f8fafc' }}>
          <h3>{answer.question}</h3>
          <p>{answer.answer}</p>
          {Object.keys(answer.data).length > 0 && (
            <p className="muted">
              <strong>Data:</strong> {JSON.stringify(answer.data)}
            </p>
          )}
        </div>
      )}
    </section>
  );
}
