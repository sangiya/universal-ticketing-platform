import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';
import {
  Alert,
  Currency,
  EmptyState,
  Modal,
  PageHeader,
  StatCard,
} from '../components/UI';

interface Ticket {
  bookingId?: number;
  bookingRef: string;
  status: string;
  seatNumber?: number;
  fare?: number;
  currency?: string;
  travelDate?: string;
  productTitle?: string;
  providerName?: string;
  quantity?: number;
}

const FILTERS = [
  { key: '', label: 'All' },
  { key: 'PAID', label: 'Paid' },
  { key: 'ISSUED', label: 'Issued' },
  { key: 'CONFIRMED', label: 'Confirmed' },
];

function statusVariant(s: string): string {
  const v = s.toLowerCase();
  if (v === 'paid' || v === 'issued' || v === 'confirmed' || v === 'active') return 'success';
  if (v === 'pending' || v === 'reserved') return 'info';
  if (v === 'cancelled' || v === 'expired' || v === 'refunded') return 'danger';
  return 'default';
}

export default function TicketsPage() {
  const { api, authenticated } = useApi();
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [filter, setFilter] = useState('');
  const [viewTicket, setViewTicket] = useState<Ticket | null>(null);
  const [qrSrc, setQrSrc] = useState<string | null>(null);
  const [qrLoading, setQrLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  const load = useCallback(
    async (q = '') => {
      setLoading(true);
      setError(null);
      try {
        const endpoint = q
          ? `/bookings/search?q=${encodeURIComponent(q)}`
          : `/bookings`;
        const [bookingData, orderData] = await Promise.all([
          api.get<unknown[]>(endpoint).catch(() => []),
          api.get<unknown[]>(`/orders/mine`).catch(() => []),
        ]);
        const bookingsList = (bookingData as unknown as Ticket[]) ?? [];
        const ordersList =
          (orderData as unknown as Array<{
            orderRef: string;
            status: string;
            productTitle: string;
            providerName: string;
            totalAmount: number;
            currencyIso: string;
            createdAt: string;
            quantity: number;
          }>) ?? [];
        const orderTickets: Ticket[] = ordersList
          .filter((o) => ['PAID', 'ISSUED', 'CONFIRMED'].includes(o.status))
          .map((o) => ({
            bookingRef: o.orderRef,
            status: o.status,
            fare: o.totalAmount,
            currency: o.currencyIso,
            travelDate: new Date(o.createdAt).toLocaleDateString(),
            productTitle: o.productTitle,
            providerName: o.providerName,
            quantity: o.quantity,
          }));
        const combined = [...bookingsList, ...orderTickets];
        if (q) {
          const lq = q.toLowerCase();
          setTickets(
            combined.filter(
              (b) =>
                b.bookingRef.toLowerCase().includes(lq) ||
                (b.productTitle && b.productTitle.toLowerCase().includes(lq)) ||
                (b.providerName && b.providerName.toLowerCase().includes(lq)) ||
                b.status.toLowerCase().includes(lq),
            ),
          );
        } else {
          setTickets(combined);
        }
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Failed to load tickets');
      } finally {
        setLoading(false);
      }
    },
    [api],
  );

  useEffect(() => {
    if (authenticated) void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authenticated]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    void load(searchQuery);
  };

  useEffect(() => {
    if (!viewTicket) {
      setQrSrc(null);
      return;
    }
    const isMarketplace = viewTicket.bookingRef.startsWith('TM-');
    const path = isMarketplace
      ? `/tickets/order/${encodeURIComponent(viewTicket.bookingRef)}/qr`
      : viewTicket.bookingId
        ? `/tickets/booking/${viewTicket.bookingId}/qr`
        : null;
    if (!path) {
      setQrSrc(null);
      return;
    }
    setQrLoading(true);
    api
      .blob(path)
      .then((b) => {
        const url = URL.createObjectURL(b);
        setQrSrc((prev) => {
          if (prev) URL.revokeObjectURL(prev);
          return url;
        });
      })
      .catch(() => setQrSrc(null))
      .finally(() => setQrLoading(false));
    return () => {
      if (qrSrc) URL.revokeObjectURL(qrSrc);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [viewTicket]);

  const copy = (text: string) => {
    navigator.clipboard.writeText(text).then(
      () => {
        setCopied(true);
        setTimeout(() => setCopied(false), 1500);
      },
      () => setCopied(false),
    );
  };

  const filtered = tickets.filter(
    (t) =>
      !filter || t.status === filter,
  );

  const active = tickets.filter((t) =>
    ['PAID', 'ISSUED', 'CONFIRMED'].includes(t.status),
  ).length;

  if (!authenticated) {
    return (
      <section className="page">
        <PageHeader
          title="My Tickets"
          subtitle="Sign in to view your bookings and tickets."
        />
        <EmptyState
          icon="🎟️"
          title="Sign in to access your tickets"
          description="Tickets are tied to your account so you can re-download or transfer them."
          action={
            <Link className="btn primary" to="/login">
              Sign in
            </Link>
          }
        />
      </section>
    );
  }

  return (
    <section className="page">
      <div className="breadcrumb">
        <Link to="/dashboard">Dashboard</Link>
        <span className="sep">›</span>
        <span>My Tickets</span>
      </div>

      <PageHeader
        title="My Tickets"
        subtitle="Show, download, or transfer every active ticket. QR codes refresh automatically."
      />

      <div className="stats">
        <StatCard label="Total tickets" value={tickets.length} icon="🎫" />
        <StatCard label="Active" value={active} icon="✓" variant="success" />
        <StatCard
          label="Latest"
          value={tickets[0] ? new Date(tickets[0].travelDate ?? '').toLocaleDateString() : '—'}
          icon="📅"
          variant="info"
        />
        <StatCard
          label="Total spent"
          value={
            tickets[0]
              ? `${tickets[0].fare ? tickets[0].fare.toFixed(0) : 0}`
              : '—'
          }
          icon="💰"
          variant="violet"
        />
      </div>

      <div className="filters">
        <form
          onSubmit={handleSearch}
          style={{ display: 'flex', gap: '0.5rem', flex: 1, minWidth: 220 }}
        >
          <input
            placeholder="🔍 Search by reference, product, or provider…"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ flex: 1 }}
          />
          <button className="btn" type="submit">
            Search
          </button>
        </form>
        <div className="chip-row">
          {FILTERS.map((f) => {
            const count = f.key
              ? tickets.filter((t) => t.status === f.key).length
              : tickets.length;
            return (
              <button
                key={f.key}
                className={`chip ${filter === f.key ? 'active' : ''}`}
                onClick={() => setFilter(f.key)}
              >
                {f.label}
                <span className="count">{count}</span>
              </button>
            );
          })}
        </div>
      </div>

      {error && <Alert kind="danger" title="Could not load tickets">{error}</Alert>}

      {loading ? (
        <div className="grid">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="card skeleton-card" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          icon="🎟️"
          title={tickets.length === 0 ? 'No tickets yet' : 'No tickets match your filters'}
          description={
            tickets.length === 0
              ? 'Browse the marketplace and book your first experience.'
              : 'Try a different status or clear the search.'
          }
          action={
            <Link to="/marketplace" className="btn primary">
              Browse marketplace
            </Link>
          }
        />
      ) : (
        <div className="grid">
          {filtered.map((b) => (
            <article className="card product-card" key={b.bookingRef}>
              <div className="product-cover">
                <span className="product-type">🎫 {b.status}</span>
                <span className="price-badge">
                  <Currency amount={b.fare} currency={b.currency} />
                </span>
              </div>
              <div className="product-body">
                <h3>{b.productTitle || b.bookingRef}</h3>
                <p className="muted fs-sm">
                  by <strong style={{ color: 'var(--primary)' }}>{b.providerName ?? '—'}</strong>
                </p>
                <div className="product-meta">
                  <span className="tag outline">📅 {b.travelDate ?? '—'}</span>
                  {b.seatNumber && <span className="tag outline">💺 Seat {b.seatNumber}</span>}
                </div>
                <div className="row tight">
                  <button className="btn primary block" onClick={() => setViewTicket(b)}>
                    View ticket
                  </button>
                  <button className="btn" onClick={() => copy(b.bookingRef)}>
                    Copy ref
                  </button>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}

      {viewTicket && (
        <Modal
          title={viewTicket.productTitle || viewTicket.bookingRef}
          description={
            viewTicket.providerName
              ? `Issued by ${viewTicket.providerName}`
              : `Reference ${viewTicket.bookingRef}`
          }
          size="md"
          onClose={() => setViewTicket(null)}
          footer={
            <>
              <button className="btn" onClick={() => copy(viewTicket.bookingRef)}>
                {copied ? '✓ Copied' : 'Copy reference'}
              </button>
              <button className="btn primary" onClick={() => setViewTicket(null)}>
                Close
              </button>
            </>
          }
        >
          <div className="grid-2" style={{ marginBottom: 'var(--space-4)' }}>
            <div className="field">
              <label>Reference</label>
              <code className="tag" style={{ fontSize: '0.85rem' }}>
                {viewTicket.bookingRef}
              </code>
            </div>
            <div className="field">
              <label>Status</label>
              <span className={`badge ${statusVariant(viewTicket.status)}`}>
                {viewTicket.status}
              </span>
            </div>
            {viewTicket.seatNumber && (
              <div className="field">
                <label>Seat</label>
                <strong>💺 {viewTicket.seatNumber}</strong>
              </div>
            )}
            {viewTicket.travelDate && (
              <div className="field">
                <label>Date</label>
                <strong>📅 {viewTicket.travelDate}</strong>
              </div>
            )}
            {viewTicket.providerName && (
              <div className="field">
                <label>Provider</label>
                <strong>{viewTicket.providerName}</strong>
              </div>
            )}
            {viewTicket.fare != null && (
              <div className="field">
                <label>Amount</label>
                <strong>
                  <Currency amount={viewTicket.fare} currency={viewTicket.currency} />
                </strong>
              </div>
            )}
          </div>

          <div
            className="card"
            style={{
              background: 'var(--surface-2)',
              textAlign: 'center',
              border: '1px solid var(--border)',
            }}
          >
            <div
              style={{
                width: 160,
                height: 160,
                margin: '0 auto var(--space-3)',
                background: '#fff',
                border: '1px solid var(--border)',
                borderRadius: 'var(--radius)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                overflow: 'hidden',
                boxShadow: 'var(--shadow-sm)',
              }}
            >
              {qrLoading ? (
                <span className="spinner" />
              ) : qrSrc ? (
                <img
                  src={qrSrc}
                  alt="QR"
                  style={{ width: '100%', height: '100%', objectFit: 'contain' }}
                />
              ) : (
                <span style={{ fontSize: '2.8rem' }}>🎫</span>
              )}
            </div>
            <p className="muted fs-sm" style={{ margin: 0 }}>
              Show this at entry · QR encodes <strong>{viewTicket.bookingRef}</strong>
            </p>
            <div
              style={{
                marginTop: 'var(--space-3)',
                fontFamily: 'monospace',
                fontSize: '0.85rem',
                letterSpacing: '0.08em',
                background: '#fff',
                display: 'inline-block',
                padding: '0.4rem 0.7rem',
                borderRadius: 'var(--radius-sm)',
                border: '1px dashed var(--border)',
              }}
            >
              {viewTicket.bookingRef}
            </div>
          </div>

          <p className="muted fs-xs text-center" style={{ marginTop: 'var(--space-3)' }}>
            Ticket issued by TicketMesh · Valid for single use · In-app support available 24/7
          </p>
        </Modal>
      )}
    </section>
  );
}
