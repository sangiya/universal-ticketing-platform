import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';
import {
  Alert,
  EmptyState,
  PageHeader,
  Skeleton,
  StatCard,
} from '../components/UI';
import { useToast } from '../components/Toast';

interface Ticket {
  id: number;
  requestRef: string;
  subject: string;
  category: string;
  priority: string;
  status: string;
  slaDueAt: string;
}

const CATEGORIES = [
  { key: 'REFUND', icon: '💸' },
  { key: 'BOOKING', icon: '🎟️' },
  { key: 'PAYMENT', icon: '💳' },
  { key: 'TECHNICAL', icon: '⚙️' },
  { key: 'ACCOUNT', icon: '👤' },
  { key: 'OTHER', icon: '💬' },
];

const PRIORITIES = [
  { key: 'LOW', label: 'Low', color: 'info' },
  { key: 'MEDIUM', label: 'Medium', color: 'info' },
  { key: 'HIGH', label: 'High', color: 'warn' },
  { key: 'CRITICAL', label: 'Critical', color: 'danger' },
];

function statusVariant(s: string): string {
  const v = s.toLowerCase();
  if (v === 'open' || v === 'pending' || v === 'in_progress' || v === 'reserved') return 'info';
  if (v === 'resolved' || v === 'closed' || v === 'confirmed' || v === 'paid') return 'success';
  if (v === 'escalated') return 'warn';
  if (v === 'rejected' || v === 'cancelled') return 'danger';
  return 'default';
}

function priorityVariant(p: string): string {
  const v = p.toLowerCase();
  if (v === 'critical' || v === 'high') return 'danger';
  if (v === 'medium') return 'warn';
  return 'info';
}

export default function SupportPage() {
  const { api, authenticated } = useApi();
  const { push } = useToast();
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);
  const [subject, setSubject] = useState('');
  const [category, setCategory] = useState('OTHER');
  const [priority, setPriority] = useState('MEDIUM');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<unknown[]>('/support/tickets/me');
      setTickets((data as unknown as Ticket[]) ?? []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load tickets');
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => {
    if (authenticated) void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authenticated]);

  const openTicket = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.post('/support/tickets', {
        subject,
        category,
        priority,
        description,
      });
      setSubject('');
      setDescription('');
      push('Support ticket opened — we will get back to you shortly.', 'success');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to open ticket');
    } finally {
      setBusy(false);
    }
  };

  if (!authenticated) {
    return (
      <section className="page">
        <PageHeader
          title="Support · 24/7"
          subtitle="Get help from our global support team. Tickets auto-escalate if they approach their SLA."
        />
        <EmptyState
          icon="💬"
          title="Sign in to raise a support ticket"
          description="Tickets are tied to your account so we can track and resolve them quickly."
          action={
            <Link to="/login" className="btn primary">
              Sign in
            </Link>
          }
        />
      </section>
    );
  }

  const open = tickets.filter((t) => ['OPEN', 'PENDING', 'ESCALATED', 'IN_PROGRESS'].includes(t.status)).length;
  const resolved = tickets.filter((t) => ['RESOLVED', 'CLOSED'].includes(t.status)).length;

  return (
    <section className="page">
      <PageHeader
        title="Support · 24/7"
        subtitle="Raise a ticket and our team will pick it up. Tickets auto-escalate if they approach their SLA."
        actions={
          <a
            href="https://docs.ticketmesh.com"
            className="btn"
            target="_blank"
            rel="noopener noreferrer"
          >
            📚 Knowledge base
          </a>
        }
      />

      <div className="stats">
        <StatCard label="Total tickets" value={tickets.length} icon="🎫" />
        <StatCard label="Open" value={open} icon="📨" variant="warning" />
        <StatCard label="Resolved" value={resolved} icon="✓" variant="success" />
        <StatCard
          label="SLA"
          value={tickets[0] ? new Date(tickets[0].slaDueAt).toLocaleDateString() : '—'}
          icon="⏱"
          variant="info"
        />
      </div>

      <div className="grid-2">
        <form className="card" onSubmit={openTicket}>
          <h2 style={{ marginBottom: 'var(--space-3)' }}>Open a new ticket</h2>
          <div className="form">
            <div className="field">
              <label htmlFor="sup-subject">Subject</label>
              <input
                id="sup-subject"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                placeholder="Brief summary of the issue"
                required
                maxLength={140}
              />
            </div>
            <div className="row">
              <div className="field">
                <label htmlFor="sup-category">Category</label>
                <select
                  id="sup-category"
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                >
                  {CATEGORIES.map((c) => (
                    <option key={c.key} value={c.key}>
                      {c.icon} {c.key}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label htmlFor="sup-priority">Priority</label>
                <select
                  id="sup-priority"
                  value={priority}
                  onChange={(e) => setPriority(e.target.value)}
                >
                  {PRIORITIES.map((p) => (
                    <option key={p.key} value={p.key}>
                      {p.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="field">
              <label htmlFor="sup-desc">Description</label>
              <textarea
                id="sup-desc"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Add as much detail as possible — order refs, screenshots, steps to reproduce…"
                rows={5}
              />
            </div>
            {error && (
              <Alert kind="danger" title="Could not open ticket">
                {error}
              </Alert>
            )}
            <button className="btn primary block" type="submit" disabled={busy || !subject.trim()}>
              {busy ? (
                <>
                  <span className="spinner" />
                  Opening…
                </>
              ) : (
                '📨 Open ticket'
              )}
            </button>
          </div>
        </form>

        <div className="card" style={{ background: 'var(--gradient-soft)', borderColor: 'transparent' }}>
          <h2 style={{ marginBottom: 'var(--space-3)' }}>Common topics</h2>
          <div className="stack">
            <a href="#" className="card compact" style={{ display: 'block', textDecoration: 'none' }}>
              <strong>💸 Refund & cancellation policy</strong>
              <p className="muted fs-sm" style={{ margin: '0.25rem 0 0' }}>
                When you can cancel, how long refunds take, partial-refund rules.
              </p>
            </a>
            <a href="#" className="card compact" style={{ display: 'block', textDecoration: 'none' }}>
              <strong>🎟️ My ticket isn't working</strong>
              <p className="muted fs-sm" style={{ margin: '0.25rem 0 0' }}>
                QR code issues, gate errors, name mismatches.
              </p>
            </a>
            <a href="#" className="card compact" style={{ display: 'block', textDecoration: 'none' }}>
              <strong>💳 Payment failed</strong>
              <p className="muted fs-sm" style={{ margin: '0.25rem 0 0' }}>
                Card declined, 3DS authentication, retry guidance.
              </p>
            </a>
            <a href="#" className="card compact" style={{ display: 'block', textDecoration: 'none' }}>
              <strong>👤 Account & security</strong>
              <p className="muted fs-sm" style={{ margin: '0.25rem 0 0' }}>
                Password resets, 2FA, suspicious activity, PII access.
              </p>
            </a>
          </div>
        </div>
      </div>

      <div className="section-title">
        <h2>My tickets</h2>
        <span className="muted">{tickets.length} total</span>
      </div>

      {error && <Alert kind="danger">{error}</Alert>}

      {loading ? (
        <div className="card">
          <Skeleton lines={4} />
        </div>
      ) : tickets.length === 0 ? (
        <EmptyState
          icon="🎫"
          title="No tickets yet"
          description="When you open a ticket it will appear here with its SLA timer and current status."
        />
      ) : (
        <div className="grid">
          {tickets.map((t) => {
            const cat = CATEGORIES.find((c) => c.key === t.category);
            return (
              <article className="card" key={t.id}>
                <div className="row between center" style={{ marginBottom: 'var(--space-2)' }}>
                  <strong>{cat?.icon ?? '💬'} {t.subject}</strong>
                  <span className={`badge ${statusVariant(t.status)}`}>{t.status}</span>
                </div>
                <p className="muted fs-sm" style={{ margin: '0 0 var(--space-3)' }}>
                  <code className="tag">{t.requestRef}</code>
                </p>
                <div className="row tight" style={{ flexWrap: 'wrap' }}>
                  <span className={`badge ${priorityVariant(t.priority)}`}>
                    {t.priority}
                  </span>
                  <span className="tag outline">⏱ SLA {new Date(t.slaDueAt).toLocaleString()}</span>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}
