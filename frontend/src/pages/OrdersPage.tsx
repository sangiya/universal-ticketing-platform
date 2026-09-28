import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';
import {
  Alert,
  Currency,
  EmptyState,
  PageHeader,
  Skeleton,
  StatCard,
} from '../components/UI';

interface ProductOrder {
  id: number;
  orderRef: string;
  productTitle: string;
  productType: string;
  providerName: string;
  quantity: number;
  unitPrice: number;
  currencyIso: string;
  baseAmount: number;
  taxAmount: number;
  serviceFee: number;
  discountAmount: number;
  totalAmount: number;
  promoCode: string | null;
  status: string;
  createdAt: string;
  paidAt: string | null;
  cancelledAt?: string | null;
  cancellationReason?: string | null;
  refundedAmount?: number | null;
  cancellationFee?: number | null;
  refundReference?: string | null;
}

interface RefundQuote {
  orderRef: string;
  refundable: boolean;
  paidAmount: number;
  refundAmount: number;
  cancellationFee: number;
  refundPercent: number;
  reason: string;
  windowLabel: string | null;
  currencyIso: string;
}

const STATUS_FILTERS = [
  { key: '', label: 'All' },
  { key: 'PENDING', label: 'Pending' },
  { key: 'PAID', label: 'Paid' },
  { key: 'ISSUED', label: 'Issued' },
  { key: 'CANCELLED', label: 'Cancelled' },
  { key: 'REFUNDED', label: 'Refunded' },
];

function statusVariant(s: string): string {
  const v = s.toLowerCase();
  if (v === 'paid' || v === 'issued' || v === 'confirmed' || v === 'active') return 'success';
  if (v === 'pending' || v === 'open' || v === 'reserved') return 'info';
  if (v === 'cancelled' || v === 'expired' || v === 'refunded' || v === 'rejected') return 'danger';
  if (v === 'escalated' || v === 'high' || v === 'medium') return 'warning';
  return 'default';
}

export default function OrdersPage() {
  const { api } = useApi();
  const [orders, setOrders] = useState<ProductOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState('');
  const [search, setSearch] = useState('');
  const [cancelTarget, setCancelTarget] = useState<ProductOrder | null>(null);
  const [quote, setQuote] = useState<RefundQuote | null>(null);
  const [quoteLoading, setQuoteLoading] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);

  const openCancel = async (order: ProductOrder) => {
    setCancelTarget(order);
    setQuote(null);
    setCancelError(null);
    setQuoteLoading(true);
    try {
      const q = await api.get<RefundQuote>(
        `/orders/${encodeURIComponent(order.orderRef)}/refund-quote`,
      );
      setQuote(q);
    } catch (e) {
      setCancelError(e instanceof Error ? e.message : 'Could not load refund details');
    } finally {
      setQuoteLoading(false);
    }
  };

  const confirmCancel = async () => {
    if (!cancelTarget) return;
    setCancelling(true);
    setCancelError(null);
    try {
      await api.post(
        `/orders/${encodeURIComponent(cancelTarget.orderRef)}/cancel`,
        { reason: 'Cancelled by customer' },
      );
      setCancelTarget(null);
      setQuote(null);
      await load();
    } catch (e) {
      setCancelError(e instanceof Error ? e.message : 'Cancellation failed');
    } finally {
      setCancelling(false);
    }
  };

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<unknown[]>('/orders/mine');
      setOrders((data as unknown as ProductOrder[]) ?? []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load orders');
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => {
    void load();
  }, [load]);

  const filtered = orders
    .filter((o) => !filter || o.status === filter)
    .filter((o) =>
      !search.trim()
        ? true
        : o.orderRef.toLowerCase().includes(search.toLowerCase()) ||
          o.productTitle.toLowerCase().includes(search.toLowerCase()) ||
          o.providerName.toLowerCase().includes(search.toLowerCase()),
    );

  const totalSpent = orders
    .filter((o) => o.status === 'PAID' || o.status === 'ISSUED')
    .reduce((s, o) => s + Number(o.totalAmount || 0), 0);
  const paidCount = orders.filter((o) => o.status === 'PAID' || o.status === 'ISSUED').length;
  const pendingCount = orders.filter((o) => o.status === 'PENDING').length;
  const lastOrder = orders[0];

  return (
    <section className="page">
      <div className="breadcrumb">
        <Link to="/dashboard">Dashboard</Link>
        <span className="sep">›</span>
        <span>My Orders</span>
      </div>

      <PageHeader
        title="My Orders"
        subtitle="Track, pay, or download every ticket you've booked. Your history is permanent and exportable."
      />

      <div className="stats">
        <StatCard
          label="Total orders"
          value={orders.length}
          icon="🧾"
        />
        <StatCard
          label="Paid"
          value={paidCount}
          icon="✓"
          variant="success"
        />
        <StatCard
          label="Pending"
          value={pendingCount}
          icon="⏳"
          variant="warning"
        />
        <StatCard
          label="Lifetime spend"
          value={lastOrder ? `${lastOrder.currencyIso} ${totalSpent.toFixed(0)}` : '—'}
          icon="💰"
          variant="violet"
        />
      </div>

      {error && <Alert kind="danger" title="Could not load orders">{error}</Alert>}

      <div className="filters">
        <div className="filter-group" style={{ flex: 1, minWidth: 220 }}>
          <input
            placeholder="🔍 Search by ref, product, or provider…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ flex: 1 }}
          />
        </div>
        <div className="chip-row">
          {STATUS_FILTERS.map((f) => {
            const count = f.key ? orders.filter((o) => o.status === f.key).length : orders.length;
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

      {loading ? (
        <div className="card">
          <Skeleton lines={6} />
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          icon="🧾"
          title={orders.length === 0 ? 'You have no orders yet' : 'No orders match your filters'}
          description={
            orders.length === 0
              ? 'Browse the marketplace to make your first booking.'
              : 'Try a different status or clear the search.'
          }
          action={
            orders.length === 0 ? (
              <Link to="/marketplace" className="btn primary">
                Browse marketplace
              </Link>
            ) : (
              <button
                className="btn"
                onClick={() => {
                  setFilter('');
                  setSearch('');
                }}
              >
                Clear filters
              </button>
            )
          }
        />
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Reference</th>
                <th>Product</th>
                <th>Provider</th>
                <th>Type</th>
                <th className="right">Qty</th>
                <th className="right">Total</th>
                <th>Status</th>
                <th>Date</th>
                <th className="right"></th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((o) => (
                <tr key={o.id}>
                  <td>
                    <code className="tag">{o.orderRef}</code>
                  </td>
                  <td>
                    <strong>{o.productTitle}</strong>
                    {o.promoCode && (
                      <div className="muted fs-xs">Promo: {o.promoCode}</div>
                    )}
                  </td>
                  <td>{o.providerName}</td>
                  <td>
                    <span className="tag">{o.productType}</span>
                  </td>
                  <td className="right num">{o.quantity}</td>
                  <td className="right">
                    <Currency amount={o.totalAmount} currency={o.currencyIso} />
                  </td>
                  <td>
                    <span className={`badge ${statusVariant(o.status)}`}>{o.status}</span>
                  </td>
                  <td className="muted fs-sm">
                    {new Date(o.createdAt).toLocaleString()}
                  </td>
                  <td className="right">
                    {o.status === 'PENDING' ? (
                      <Link className="btn sm primary" to={`/checkout/${o.orderRef}`}>
                        Pay
                      </Link>
                    ) : o.status === 'PAID' || o.status === 'ISSUED' ? (
                      <>
                        <Link className="btn sm" to={`/ticket/${o.orderRef}`}>
                          View ticket
                        </Link>{' '}
                        <button
                          className="btn sm danger"
                          onClick={() => void openCancel(o)}
                          type="button"
                        >
                          Cancel
                        </button>
                      </>
                    ) : o.status === 'REFUNDED' && o.refundedAmount ? (
                      <span className="muted fs-xs">
                        Refunded{' '}
                        <Currency amount={o.refundedAmount} currency={o.currencyIso} />
                      </span>
                    ) : o.status === 'CANCELLED' && o.cancellationFee ? (
                      <span className="muted fs-xs">
                        Fee <Currency amount={o.cancellationFee} currency={o.currencyIso} />
                      </span>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {cancelTarget && (
        <div className="modal-backdrop" onClick={() => setCancelTarget(null)}>
          <div
            className="modal"
            role="dialog"
            aria-modal="true"
            aria-label="Cancel order"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="modal-head">
              <h3>Cancel order</h3>
              <button
                className="btn-icon"
                aria-label="Close"
                onClick={() => setCancelTarget(null)}
                type="button"
              >
                ✕
              </button>
            </div>

            <div className="modal-body">
              <p className="muted">
                <code className="tag">{cancelTarget.orderRef}</code>{' '}
                {cancelTarget.productTitle}
              </p>

              {quoteLoading && <p className="muted">Checking refund policy…</p>}

              {cancelError && <Alert kind="danger" title="Cancellation failed">{cancelError}</Alert>}

              {quote && (
                <>
                  <div className="card" style={{ marginTop: 12 }}>
                    <div className="kv">
                      <span>Amount paid</span>
                      <Currency amount={quote.paidAmount} currency={quote.currencyIso} />
                    </div>
                    <div className="kv">
                      <span>Refund</span>
                      <strong>
                        <Currency amount={quote.refundAmount} currency={quote.currencyIso} />
                      </strong>
                    </div>
                    <div className="kv">
                      <span>Cancellation fee</span>
                      <Currency amount={quote.cancellationFee} currency={quote.currencyIso} />
                    </div>
                  </div>

                  <Alert
                    kind={quote.refundable ? 'info' : 'warning'}
                    title={quote.windowLabel ?? 'Cancellation policy'}
                    style={{ marginTop: 12 }}
                  >
                    {quote.reason}. Refunds are returned to your TicketMesh wallet and any
                    loyalty points from this order are reversed.
                  </Alert>
                </>
              )}
            </div>

            <div className="modal-foot">
              <button
                className="btn"
                onClick={() => setCancelTarget(null)}
                disabled={cancelling}
                type="button"
              >
                Keep order
              </button>
              <button
                className="btn danger"
                onClick={() => void confirmCancel()}
                disabled={cancelling || quoteLoading || (quote !== null && !quote.refundable)}
                type="button"
              >
                {cancelling ? 'Cancelling…' : 'Cancel order'}
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
