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
                      <Link className="btn sm" to={`/ticket/${o.orderRef}`}>
                        View ticket
                      </Link>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
