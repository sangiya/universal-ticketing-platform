import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';

interface Booking {
  bookingRef: string;
  status: string;
  seatNumber?: number;
  fare?: number;
  travelDate?: string;
  productTitle?: string;
  providerName?: string;
}

export default function TicketsPage() {
  const { api, authenticated } = useApi();
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [viewTicket, setViewTicket] = useState<Booking | null>(null);

  const load = useCallback(async (q = '') => {
    try {
      const endpoint = q 
        ? `/bookings/search?q=${encodeURIComponent(q)}` 
        : `/bookings`;
      const [bookingData, orderData] = await Promise.all([
        api.get<unknown[]>(endpoint).catch(() => []),
        api.get<unknown[]>(`/orders/mine`).catch(() => [])
      ]);
      const bookingsList = (bookingData as unknown as Booking[]) ?? [];
      const ordersList = (orderData as unknown as Array<{orderRef:string,status:string,productTitle:string,providerName:string,totalAmount:number,currencyIso:string,createdAt:string}>) ?? [];
      const orderTickets: Booking[] = ordersList
        .filter(o => ['PAID','ISSUED','CONFIRMED'].includes(o.status))
        .map(o => ({
          bookingRef: o.orderRef,
          status: o.status,
          fare: o.totalAmount,
          travelDate: new Date(o.createdAt).toLocaleDateString(),
          productTitle: o.productTitle,
          providerName: o.providerName
        }));
      const combined = [...bookingsList, ...orderTickets];
      if (q) {
        const lq = q.toLowerCase();
        setBookings(combined.filter(b => 
          b.bookingRef.toLowerCase().includes(lq) ||
          (b.productTitle && b.productTitle.toLowerCase().includes(lq)) ||
          (b.providerName && b.providerName.toLowerCase().includes(lq)) ||
          b.status.toLowerCase().includes(lq)
        ));
      } else {
        setBookings(combined);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load bookings');
    }
  }, [api]);

  useEffect(() => {
    if (authenticated) void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authenticated]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    void load(searchQuery);
  };

  if (!authenticated) {
    return (
      <section className="page">
        <h1>My Tickets</h1>
        <p className="muted">
          Sign in to view your bookings and tickets.
        </p>
        <Link className="btn primary" to="/login">
          Sign in
        </Link>
      </section>
    );
  }

  return (
    <section className="page">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h1>My Tickets</h1>
        <form onSubmit={handleSearch} style={{ display: 'flex', gap: '0.5rem' }}>
          <input 
            placeholder="Search by Ref or Product..." 
            value={searchQuery} 
            onChange={e => setSearchQuery(e.target.value)}
            style={{ padding: '0.4rem 0.8rem', borderRadius: '8px', border: '1px solid var(--border)' }}
          />
          <button className="btn" type="submit">Search</button>
        </form>
      </div>

      {error && <p className="error">{error}</p>}
      <div className="grid">
        {bookings.map((b) => (
          <article className="card" key={b.bookingRef}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
              <div>
                <h3 style={{ margin: 0 }}>{b.productTitle || b.bookingRef}</h3>
                <p className="muted" style={{ fontSize: '0.85rem', margin: '0.25rem 0' }}>
                  {b.providerName ? `by ${b.providerName}` : `Ref: ${b.bookingRef}`}
                </p>
              </div>
              <span className={`badge ${b.status.toLowerCase()}`}>{b.status}</span>
            </div>
            <div style={{ margin: '1rem 0', display: 'flex', gap: '1rem', fontSize: '0.9rem' }}>
              <span><strong>Seat:</strong> {b.seatNumber ?? '—'}</span>
              <span><strong>Date:</strong> {b.travelDate ?? '—'}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--border)', paddingTop: '0.75rem' }}>
              <span className="price" style={{ fontWeight: 'bold', fontSize: '1.1rem' }}>
                {b.fare != null ? `${b.fare.toFixed(2)} LKR` : '—'}
              </span>
              <button className="btn" style={{ fontSize: '0.8rem' }} onClick={() => setViewTicket(b)}>View Ticket</button>
            </div>
          </article>
        ))}
        {bookings.length === 0 && <p className="muted">No bookings found. <Link to="/">Go book something!</Link></p>}
      </div>

      {viewTicket && (
        <div className="modal-overlay" onClick={() => setViewTicket(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 520 }}>
            <div className="modal-header" style={{ background: 'var(--gradient)', color: '#fff', borderBottom: 'none' }}>
              <h3 style={{ color: '#fff', margin: 0 }}>{viewTicket.productTitle || viewTicket.bookingRef}</h3>
              <p style={{ color: 'rgba(255,255,255,0.9)', margin: '0.25rem 0 0' }}>
                {viewTicket.providerName ? `by ${viewTicket.providerName}` : `Ref: ${viewTicket.bookingRef}`} · <span className="badge" style={{ background: '#fff', color: '#4f46e5' }}>{viewTicket.status}</span>
              </p>
            </div>
            <div className="modal-body">
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '1.25rem' }}>
                <div className="field">
                  <label>Reference</label>
                  <div className="tag" style={{ fontSize: '0.9rem', padding: '0.35rem 0.6rem' }}>{viewTicket.bookingRef}</div>
                </div>
                <div className="field">
                  <label>Status</label>
                  <span className={`badge ${viewTicket.status.toLowerCase()}`}>{viewTicket.status}</span>
                </div>
                <div className="field">
                  <label>Seat</label>
                  <span>{viewTicket.seatNumber ?? '—'}</span>
                </div>
                <div className="field">
                  <label>Date</label>
                  <span>{viewTicket.travelDate ?? '—'}</span>
                </div>
                <div className="field">
                  <label>Provider</label>
                  <span>{viewTicket.providerName ?? '—'}</span>
                </div>
                <div className="field">
                  <label>Amount</label>
                  <strong>{viewTicket.fare != null ? `${viewTicket.fare.toFixed(2)} LKR` : '—'}</strong>
                </div>
              </div>

              <div style={{ background: 'var(--surface-2)', border: '1px solid var(--border)', borderRadius: 12, padding: '1.25rem', textAlign: 'center', marginBottom: '1rem' }}>
                <div style={{ width: 140, height: 140, margin: '0 auto 0.75rem', background: '#fff', border: '1px solid var(--border)', borderRadius: 10, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '2.8rem' }}>🎫</div>
                <p className="muted" style={{ fontSize: '0.8rem', margin: 0 }}>Show this at entry · QR encodes <strong>{viewTicket.bookingRef}</strong></p>
                <div style={{ marginTop: '0.6rem', fontFamily: 'monospace', fontSize: '0.85rem', letterSpacing: '0.08em', background: '#fff', display: 'inline-block', padding: '0.3rem 0.6rem', borderRadius: 6, border: '1px dashed var(--border)' }}>
                  {viewTicket.bookingRef}
                </div>
              </div>

              <p className="muted" style={{ fontSize: '0.8rem', textAlign: 'center' }}>
                Ticket issued by TicketMesh · Valid for single use · In-app support available 24/7.
              </p>
            </div>
            <div className="modal-footer">
              <button className="btn" onClick={() => { navigator.clipboard.writeText(viewTicket.bookingRef).catch(()=>{}); }}>Copy Ref</button>
              <button className="btn primary" onClick={() => setViewTicket(null)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
