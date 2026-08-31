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
              <button className="btn" style={{ fontSize: '0.8rem' }}>View Ticket</button>
            </div>
          </article>
        ))}
        {bookings.length === 0 && <p className="muted">No bookings found. <Link to="/">Go book something!</Link></p>}
      </div>
    </section>
  );
}
