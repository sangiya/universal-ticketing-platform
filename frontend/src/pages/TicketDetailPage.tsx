import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useApi } from '../context/ApiContext';
import { Alert, Currency, EmptyState, PageHeader } from '../components/UI';

interface TicketDetail {
  orderRef: string;
  bookingId?: number;
  productTitle: string;
  productType: string;
  providerName: string;
  origin?: string | null;
  destination?: string | null;
  eventDate?: string | null;
  totalAmount: number;
  currencyIso: string;
  status: string;
  createdAt: string;
  paidAt: string | null;
  quantity: number;
  /** Signed QR payload issued by the backend (MarketplaceTicketResponse.qrData). */
  qrData?: string;
  ticketCode?: string;
  seatNumbers?: string[];
  holderName?: string;
  holderEmail?: string;
  attributes?: string;
  validFrom?: string;
  validUntil?: string;
  entriesRemaining?: number;
  cancellationPolicy?: {
    minHoursBeforeEvent: number;
    refundPercent: number;
    feeAmount: number | null;
    feePercent: number | null;
    label: string | null;
  }[];
}

function statusVariant(s: string): string {
  const v = s.toLowerCase();
  if (v === 'paid' || v === 'issued' || v === 'confirmed' || v === 'active') return 'success';
  if (v === 'pending' || v === 'open') return 'info';
  if (v === 'cancelled' || v === 'refunded' || v === 'expired') return 'danger';
  return 'default';
}

function statusIcon(s: string): string {
  const v = s.toLowerCase();
  if (v === 'paid' || v === 'issued' || v === 'confirmed' || v === 'active') return '✅';
  if (v === 'pending' || v === 'open') return '⏳';
  if (v === 'cancelled' || v === 'refunded' || v === 'expired') return '✕';
  return '🎟';
}

export default function TicketDetailPage() {
  const { id } = useParams<{ id: string }>(); // orderRef
  const { api } = useApi();
  const [ticket, setTicket] = useState<TicketDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [verifyResult, setVerifyResult] = useState<{ valid: boolean; message: string } | null>(null);
  const [verifyLoading, setVerifyLoading] = useState(false);

  // Bookings expose a short ticket code; marketplace orders carry the signed
  // qrData payload. Either one is accepted by /api/tickets/verify?data=...
  const ticketCode = ticket?.qrData ?? ticket?.ticketCode;

  const load = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<TicketDetail>(`/tickets/order/${id}`);
      setTicket(data as unknown as TicketDetail);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Ticket not found');
    } finally {
      setLoading(false);
    }
  }, [api, id]);

  useEffect(() => { void load(); }, [load]);

  const verifyTicket = async () => {
    if (!ticketCode) return;
    setVerifyLoading(true);
    setVerifyResult(null);
    try {
      const result = await api.get<{ valid: boolean; message: string }>(
        `/tickets/verify?data=${encodeURIComponent(ticketCode)}`,
      );
      setVerifyResult(result as unknown as { valid: boolean; message: string });
    } catch (e) {
      setVerifyResult({
        valid: false,
        message: e instanceof Error ? e.message : 'Verification failed',
      });
    } finally {
      setVerifyLoading(false);
    }
  };

  if (loading) {
    return (
      <section className="page">
        <div className="breadcrumb">
          <Link to="/tickets">← My tickets</Link>
        </div>
        <div className="card skeleton" style={{ height: 360 }} />
      </section>
    );
  }

  if (error || !ticket) {
    return (
      <section className="page">
        <div className="breadcrumb">
          <Link to="/tickets">← My tickets</Link>
        </div>
        <EmptyState
          icon="🎫"
          title="Ticket not found"
          description={error ?? 'We could not find a ticket for this reference. It may have been refunded or expired.'}
          action={
            <>
              <Link to="/tickets" className="btn primary">View all tickets</Link>{' '}
              <Link to="/marketplace" className="btn">Browse marketplace</Link>
            </>
          }
        />
      </section>
    );
  }

  const isOpen = ticket.status === 'PAID' || ticket.status === 'ISSUED' || ticket.status === 'ACTIVE';

  return (
    <section className="page">
      <div className="breadcrumb">
        <Link to="/tickets">My tickets</Link>
        <span className="sep">›</span>
        <span>{ticket.orderRef}</span>
      </div>

      <PageHeader
        title={`🎟️ ${ticket.productTitle}`}
        subtitle={`Reference ${ticket.orderRef} · Booked ${new Date(ticket.createdAt).toLocaleString()}`}
        badge={
          <span className={`badge ${statusVariant(ticket.status)}`}>
            {statusIcon(ticket.status)} {ticket.status}
          </span>
        }
        actions={
          <Link to="/dashboard" className="btn">← Dashboard</Link>
        }
      />

      <div className="ticket-detail-grid">
        {/* ── Main ticket card ── */}
        <article className="card ticket-card">
          <div className="ticket-card-head">
            <div>
              <div className="muted fs-xs">Provider</div>
              <div className="ticket-provider">{ticket.providerName}</div>
            </div>
            <div>
              <div className="muted fs-xs">Type</div>
              <div className="ticket-type">{ticket.productType}</div>
            </div>
          </div>

          <div className="ticket-card-perforation" />

          <div className="ticket-card-body">
            {ticket.origin && ticket.destination && (
              <div className="ticket-route">
                <div>
                  <div className="muted fs-xs">From</div>
                  <div className="ticket-route-city">{ticket.origin}</div>
                </div>
                <div className="ticket-route-arrow">→</div>
                <div>
                  <div className="muted fs-xs">To</div>
                  <div className="ticket-route-city">{ticket.destination}</div>
                </div>
              </div>
            )}

            <div className="ticket-card-meta">
              {ticket.eventDate && (
                <div>
                  <div className="muted fs-xs">Event date</div>
                  <div>{new Date(ticket.eventDate).toLocaleString()}</div>
                </div>
              )}
              <div>
                <div className="muted fs-xs">Quantity</div>
                <div>{ticket.quantity} ticket{ticket.quantity === 1 ? '' : 's'}</div>
              </div>
              {ticket.holderName && (
                <div>
                  <div className="muted fs-xs">Holder</div>
                  <div>{ticket.holderName}</div>
                </div>
              )}
              {ticket.paidAt && (
                <div>
                  <div className="muted fs-xs">Paid</div>
                  <div>{new Date(ticket.paidAt).toLocaleString()}</div>
                </div>
              )}
            </div>

            {ticket.seatNumbers && ticket.seatNumbers.length > 0 && (
              <div style={{ marginTop: '1rem' }}>
                <div className="muted fs-xs">Seats</div>
                <div className="ticket-seats">
                  {ticket.seatNumbers.map((s) => (
                    <span key={s} className="ticket-seat-chip">{s}</span>
                  ))}
                </div>
              </div>
            )}

            {ticket.attributes && (
              <div style={{ marginTop: '1rem' }} className="muted fs-sm">
                {ticket.attributes}
              </div>
            )}
          </div>

          <div className="ticket-card-perforation" />

          <div className="ticket-card-foot">
            <div>
              <div className="muted fs-xs">Total paid</div>
              <div className="ticket-total">
                <Currency amount={ticket.totalAmount} currency={ticket.currencyIso} className="text-primary fw-800" />
              </div>
            </div>
            {ticket.ticketCode && (
              <div className="ticket-code-block">
                <div className="muted fs-xs">Code</div>
                <code className="ticket-code">{ticket.ticketCode}</code>
              </div>
            )}
          </div>

          {(id || ticket.bookingId) && (
            <div className="ticket-qr-wrap">
              <div className="ticket-qr">
                <img
                  src={
                    ticket.bookingId
                      ? `/api/tickets/booking/${ticket.bookingId}/qr`
                      : `/api/tickets/order/${encodeURIComponent(id ?? '')}/qr`
                  }
                  alt="Ticket QR code"
                />
              </div>
              <div className="muted fs-xs" style={{ textAlign: 'center' }}>
                Scan at entry
              </div>
            </div>
          )}
        </article>

        {/* ── Right column: actions + policies ── */}
        <aside className="ticket-aside">
          <div className="card">
            <h3 style={{ marginTop: 0 }}>Actions</h3>
            {isOpen && ticketCode && (
              <button
                className="btn block"
                onClick={verifyTicket}
                disabled={verifyLoading}
                style={{ marginBottom: 8 }}
              >
                {verifyLoading ? '⏳ Verifying…' : '🔍 Verify ticket'}
              </button>
            )}
            <Link to="/marketplace" className="btn block" style={{ marginBottom: 8 }}>
              🛍️ Buy another
            </Link>
            <Link to="/orders" className="btn block">
              🧾 View all orders
            </Link>

            {verifyResult && (
              <Alert
                kind={verifyResult.valid ? 'success' : 'danger'}
                title={verifyResult.valid ? '✓ Valid ticket' : '✕ Not valid'}
                style={{ marginTop: 12 }}
              >
                {verifyResult.message}
              </Alert>
            )}
          </div>

          {ticket.cancellationPolicy && ticket.cancellationPolicy.length > 0 && (
            <div className="card">
              <h3 style={{ marginTop: 0 }}>Cancellation policy</h3>
              <table className="policy-table">
                <thead>
                  <tr>
                    <th>Cancellation window</th>
                    <th>Refund</th>
                  </tr>
                </thead>
                <tbody>
                  {ticket.cancellationPolicy.map((w, i) => (
                    <tr key={i}>
                      <td>{w.label ?? `${w.minHoursBeforeEvent}h before`}</td>
                      <td>
                        {Number(w.refundPercent) === 0 ? (
                          <span className="badge danger">Not allowed</span>
                        ) : (
                          <span className="badge success">
                            {Number(w.refundPercent).toLocaleString()}% refund
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {ticket.entriesRemaining != null && (
            <div className="card">
              <h3 style={{ marginTop: 0 }}>Entries</h3>
              <div className="stat success" style={{ marginBottom: 0 }}>
                <span className="label">Remaining</span>
                <span className="value">{ticket.entriesRemaining}</span>
              </div>
            </div>
          )}
        </aside>
      </div>
    </section>
  );
}
