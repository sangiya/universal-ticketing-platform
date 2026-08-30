import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';

interface Booking {
  bookingRef: string;
  status: string;
  seatNumber?: number;
  fare?: number;
  travelDate?: string;
}

export default function TicketsPage() {
  const { api, authenticated } = useApi();
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const data = await api.get<unknown[]>(`/bookings`);
      setBookings((data as unknown as Booking[]) ?? []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load bookings');
    }
  }, [api]);

  useEffect(() => {
    if (authenticated) void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authenticated]);

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
      <h1>My Tickets</h1>
      {error && <p className="error">{error}</p>}
      <div className="grid">
        {bookings.map((b) => (
          <article className="card" key={b.bookingRef}>
            <h3>{b.bookingRef}</h3>
            <p>
              Seat {b.seatNumber ?? '—'} · {b.travelDate ?? '—'}
            </p>
            <p className="price">
              {b.fare != null ? `${b.fare.toFixed(2)} LKR` : '—'}
            </p>
            <span className={`badge ${b.status.toLowerCase()}`}>{b.status}</span>
          </article>
        ))}
        {bookings.length === 0 && <p className="muted">No bookings yet. Go book something!</p>}
      </div>
      <Link className="btn" to="/">
        Search tickets
      </Link>
    </section>
  );
}
