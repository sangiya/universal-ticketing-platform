import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';
import {
  Alert,
  Currency,
  EmptyState,
  PageHeader,
  StatCard,
} from '../components/UI';

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

const SUGGESTIONS = [
  'Which product domain generates the most revenue?',
  'How many orders did we get this period?',
  'What is the average order value?',
  'Which provider has the most orders?',
];

function domainIcon(type: string): string {
  switch (type) {
    case 'ROUTE': return '🚌';
    case 'ADMISSION': return '🎟️';
    case 'SERVICE': return '🛎️';
    case 'SEAT': return '💺';
    case 'PACKAGE': return '📦';
    case 'TICKET': return '🎫';
    default: return '📊';
  }
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
  const [history, setHistory] = useState<AnalystAnswer[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<TrendReport>('/analytics/trend');
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

  const ask = async (q?: string) => {
    const text = (q ?? question).trim();
    if (!text) return;
    setQuestion(text);
    setAsking(true);
    setAskError(null);
    try {
      const data = await api.post<AnalystAnswer>('/analytics/ask', { question: text });
      setAnswer(data ?? null);
      if (data) setHistory((h) => [data, ...h].slice(0, 6));
    } catch (err) {
      setAskError(err instanceof Error ? err.message : 'Failed to get an answer');
    } finally {
      setAsking(false);
    }
  };

  if (!authenticated) {
    return (
      <section className="page">
        <PageHeader title="Analytics" subtitle="Tenant revenue intelligence and an AI data analyst." />
        <EmptyState
          icon="📊"
          title="Sign in to view analytics"
          description="Trends, revenue by domain, and an AI data analyst are scoped to your account."
          action={<Link className="btn primary" to="/login">Sign in</Link>}
        />
      </section>
    );
  }

  const maxRevenue = Math.max(1, ...(trend?.domains?.map((d) => d.revenue) ?? [1]));

  return (
    <section className="page">
      <PageHeader
        title="Analytics"
        subtitle="Tenant revenue intelligence across every product domain, plus a natural-language data analyst."
        actions={
          <button
            className="btn"
            onClick={() => void load()}
            disabled={loading}
            aria-busy={loading}
          >
            {loading ? <span className="spinner" /> : '↻'} Refresh
          </button>
        }
      />

      {error && <Alert kind="danger">{error}</Alert>}

      {loading && !trend && (
        <div className="grid">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="card skeleton-card" />
          ))}
        </div>
      )}

      {trend && (
        <>
          <div className="stats">
            <StatCard
              label="Total orders"
              value={trend.totalOrders.toLocaleString()}
              icon="🧾"
            />
            <StatCard
              label="Total revenue"
              value={Number(trend.totalRevenue).toLocaleString(undefined, {
                maximumFractionDigits: 0,
              })}
              icon="💰"
              variant="success"
            />
            <StatCard
              label="Average order value"
              value={Number(trend.averageOrderValue).toLocaleString(undefined, {
                maximumFractionDigits: 0,
              })}
              icon="📈"
              variant="violet"
            />
          </div>

          <div className="grid-2" style={{ marginTop: 'var(--space-4)' }}>
            <div className="card">
              <h2 style={{ marginBottom: 'var(--space-3)' }}>Revenue by product domain</h2>
              {trend.domains.length === 0 ? (
                <p className="muted">No domain data recorded yet.</p>
              ) : (
                <div className="stack">
                  {trend.domains
                    .sort((a, b) => b.revenue - a.revenue)
                    .map((d) => {
                      const pct = Math.round((d.revenue / maxRevenue) * 100);
                      return (
                        <div key={d.productType}>
                          <div className="row between" style={{ marginBottom: 4 }}>
                            <span style={{ fontWeight: 600 }}>
                              {domainIcon(d.productType)} {d.productType}
                            </span>
                            <span className="muted fs-sm">
                              {d.orderCount} orders ·{' '}
                              <Currency amount={d.revenue} />
                            </span>
                          </div>
                          <div
                            style={{
                              height: 8,
                              borderRadius: 4,
                              background: 'var(--surface-3)',
                              overflow: 'hidden',
                            }}
                          >
                            <div
                              style={{
                                width: `${pct}%`,
                                height: '100%',
                                background: 'var(--gradient)',
                                transition: 'width 0.4s',
                              }}
                            />
                          </div>
                        </div>
                      );
                    })}
                </div>
              )}
            </div>

            <div className="card" style={{ background: 'var(--gradient-soft)', borderColor: 'transparent' }}>
              <h2 style={{ marginBottom: 'var(--space-3)' }}>
                🤖 Ask the data analyst
              </h2>
              <p className="muted fs-sm" style={{ marginBottom: 'var(--space-3)' }}>
                Plain-language questions about your sales data, answered with the
                underlying numbers.
              </p>
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  void ask();
                }}
                className="stack"
              >
                <textarea
                  rows={2}
                  value={question}
                  onChange={(e) => setQuestion(e.target.value)}
                  placeholder="e.g. Which provider has the most orders?"
                />
                <button className="btn primary" type="submit" disabled={asking || !question.trim()}>
                  {asking ? (
                    <>
                      <span className="spinner" />
                      Analysing…
                    </>
                  ) : (
                    'Ask'
                  )}
                </button>
              </form>

              {askError && (
                <Alert kind="danger" title="Could not analyse">
                  {askError}
                </Alert>
              )}

              <div className="stack tight" style={{ marginTop: 'var(--space-3)' }}>
                {SUGGESTIONS.map((s) => (
                  <button
                    key={s}
                    className="chip"
                    style={{ justifyContent: 'flex-start' }}
                    onClick={() => void ask(s)}
                    disabled={asking}
                  >
                    💡 {s}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {answer && (
            <div className="card" style={{ marginTop: 'var(--space-4)' }}>
              <div className="row between center" style={{ marginBottom: 'var(--space-3)' }}>
                <h3 style={{ margin: 0 }}>💡 {answer.question}</h3>
                <span className="tag">latest</span>
              </div>
              <p style={{ margin: 0 }}>{answer.answer}</p>
              {Object.keys(answer.data ?? {}).length > 0 && (
                <details style={{ marginTop: 'var(--space-3)' }}>
                  <summary className="muted fs-sm" style={{ cursor: 'pointer' }}>
                    View underlying data
                  </summary>
                  <pre
                    style={{
                      background: 'var(--surface-2)',
                      padding: 'var(--space-3)',
                      borderRadius: 'var(--radius-sm)',
                      fontSize: '0.8rem',
                      overflow: 'auto',
                      marginTop: 'var(--space-2)',
                    }}
                  >
                    {JSON.stringify(answer.data, null, 2)}
                  </pre>
                </details>
              )}
            </div>
          )}

          {history.length > 1 && (
            <>
              <div className="section-title">
                <h2>Recent questions</h2>
                <button className="link" onClick={() => setHistory([])}>
                  Clear
                </button>
              </div>
              <div className="stack">
                {history.slice(1).map((h, i) => (
                  <details key={i} className="card compact">
                    <summary style={{ cursor: 'pointer', fontWeight: 600 }}>
                      {h.question}
                    </summary>
                    <p style={{ margin: '0.5rem 0 0' }} className="muted fs-sm">
                      {h.answer}
                    </p>
                  </details>
                ))}
              </div>
            </>
          )}
        </>
      )}
    </section>
  );
}
