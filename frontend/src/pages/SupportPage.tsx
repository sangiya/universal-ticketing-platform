import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useApi } from '../context/ApiContext';

interface Ticket {
  id: number;
  requestRef: string;
  subject: string;
  category: string;
  priority: string;
  status: string;
  slaDueAt: string;
}

export default function SupportPage() {
  const { api, authenticated } = useApi();
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [subject, setSubject] = useState('');
  const [category, setCategory] = useState('OTHER');
  const [priority, setPriority] = useState('MEDIUM');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const data = await api.get<unknown[]>('/support/tickets/me');
      setTickets((data as unknown as Ticket[]) ?? []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load tickets');
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
      await api.post('/support/tickets?tenant=global', {
        subject,
        category,
        priority,
        description,
      });
      setSubject('');
      setDescription('');
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
        <h1>Support (24/7)</h1>
        <p className="muted">Sign in to raise a support ticket.</p>
      </section>
    );
  }

  return (
    <section className="page">
      <h1>Support — 24/7</h1>
      <p className="muted">
        Raise a ticket and our team will pick it up. Tickets auto-escalate if they approach
        their SLA.
      </p>

      <form className="stack card" onSubmit={openTicket}>
        <input
          value={subject}
          onChange={(e) => setSubject(e.target.value)}
          placeholder="Subject"
          required
        />
        <div className="row">
          <select value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="REFUND">Refund</option>
            <option value="BOOKING">Booking</option>
            <option value="PAYMENT">Payment</option>
            <option value="OTHER">Other</option>
          </select>
          <select value={priority} onChange={(e) => setPriority(e.target.value)}>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>
        </div>
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Describe the issue…"
          rows={3}
        />
        {error && <p className="error">{error}</p>}
        <button className="btn primary" type="submit" disabled={busy}>
          {busy ? 'Opening…' : 'Open ticket'}
        </button>
      </form>

      <h2>My tickets</h2>
      <div className="grid">
        {tickets.map((t) => (
          <article className="card" key={t.id}>
            <h3>{t.requestRef}</h3>
            <p>{t.subject}</p>
            <p className="muted">
              {t.category} · {t.priority} · SLA {new Date(t.slaDueAt).toLocaleString()}
            </p>
            <span className={`badge ${t.status.toLowerCase()}`}>{t.status}</span>
          </article>
        ))}
        {tickets.length === 0 && <p className="muted">No tickets yet.</p>}
      </div>
    </section>
  );
}
