import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';

interface ProductOrder {
  id: number;
  orderRef: string;
  tenantId?: number;
  providerName: string;
  productTitle: string;
  productType: string;
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

export default function OrdersPage() {
  const { api, authenticated } = useApi();
  const [orders, setOrders] = useState<ProductOrder[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<unknown[]>(`/orders/mine`);
      setOrders((data as unknown as ProductOrder[]) ?? []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load orders');
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => {
    if (authenticated) void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authenticated]);

  if (!authenticated) {
    return (
      <section className="page">
        <h1>My Orders</h1>
        <p className="muted">Sign in to view your marketplace orders.</p>
        <Link className="btn primary" to="/login">
          Sign in
        </Link>
      </section>
    );
  }

  const fmt = (n: number | undefined | null) =>
    n == null ? '—' : Number(n).toFixed(2);

  return (
    <section className="page">
      <h1>My Orders</h1>
      <p className="muted">Every marketplace purchase, itemised and tracked.</p>
      {error && <p className="error">{error}</p>}
      {loading && <p className="muted">Loading your orders…</p>}

      {orders.length === 0 ? (
        <p className="muted">
          You have no orders yet.{' '}
          <Link className="btn" to="/marketplace">
            Browse the marketplace
          </Link>
        </p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Ref</th>
              <th>Product</th>
              <th>Type</th>
              <th>Qty</th>
              <th>Currency</th>
              <th>Base</th>
              <th>Tax</th>
              <th>Fee</th>
              <th>Discount</th>
              <th>Total</th>
              <th>Status</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.id}>
                <td>{o.orderRef}</td>
                <td>{o.productTitle}</td>
                <td>
                  <span className="tag">{o.productType}</span>
                </td>
                <td>{o.quantity}</td>
                <td>{o.currencyIso}</td>
                <td className="currency">{fmt(o.baseAmount)}</td>
                <td className="currency">{fmt(o.taxAmount)}</td>
                <td className="currency">{fmt(o.serviceFee)}</td>
                <td className="currency">-{fmt(o.discountAmount)}</td>
                <td className="currency">
                  <strong>{fmt(o.totalAmount)}</strong>
                </td>
                <td>
                  <span className={`badge ${o.status.toLowerCase()}`}>
                    {o.status}
                  </span>
                </td>
                <td>{new Date(o.createdAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
