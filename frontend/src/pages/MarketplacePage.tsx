import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';

interface Product {
  id: number;
  providerId: number;
  providerName: string;
  tenantId: number;
  productType: string;
  title: string;
  origin: string | null;
  destination: string | null;
  eventDate: string | null;
  price: number;
  currencyIso: string;
  availableQuantity: number;
  description: string | null;
  attributes: string | null;
  enabled: boolean;
  createdAt: string;
}

interface PricingBreakdown {
  base: number;
  tax: number;
  serviceFee: number;
  discount: number;
  subtotal: number;
  total: number;
  currencyIso: string;
  targetCurrency: string;
  appliedPromoCode: string | null;
  convertedTotal: number;
  promoName: string | null;
}

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

interface Loyalty {
  id: number;
  points: number;
  lifetimePoints: number;
  tier: string;
}

interface NotificationMsg {
  id: number;
  subject: string | null;
  body: string;
  channel: string;
  status: string;
  createdAt: string;
}

interface BuyState {
  productId: number;
  quantity: number;
  promoCode: string;
}

interface PurchaseResult {
  orderRef: string;
  total: number;
  currency: string;
}

const PRODUCT_TYPES = ['TICKET', 'SERVICE', 'SEAT', 'ROUTE', 'ADMISSION', 'PACKAGE'];

export default function MarketplacePage() {
  const { api, authenticated } = useApi();
  const [products, setProducts] = useState<Product[]>([]);
  const [orders, setOrders] = useState<ProductOrder[]>([]);
  const [loyalty, setLoyalty] = useState<Loyalty | null>(null);
  const [notifications, setNotifications] = useState<NotificationMsg[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [buy, setBuy] = useState<BuyState>({
    productId: 0,
    quantity: 1,
    promoCode: '',
  });
  const [pricing, setPricing] = useState<PricingBreakdown | null>(null);
  const [pricingLoading, setPricingLoading] = useState(false);
  const [orderResult, setOrderResult] = useState<PurchaseResult | null>(null);
  const [buyError, setBuyError] = useState<string | null>(null);

  const loadCatalog = useCallback(async () => {
    try {
      const data = await api.get<unknown[]>(`/catalog?tenantId=1`);
      setProducts((data as unknown as Product[]) ?? []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load catalog');
    }
  }, [api]);

  const loadLoyalty = useCallback(async () => {
    try {
      const data = await api.get<Loyalty>(`/loyalty`);
      setLoyalty(data ?? null);
    } catch {
      setLoyalty(null);
    }
  }, [api]);

  const loadNotifications = useCallback(async () => {
    try {
      const data = await api.get<unknown[]>(`/notifications`);
      setNotifications((data as unknown as NotificationMsg[]) ?? []);
    } catch {
      setNotifications([]);
    }
  }, [api]);

  const loadOrders = useCallback(async () => {
    try {
      const data = await api.get<unknown[]>(`/orders/mine`);
      setOrders((data as unknown as ProductOrder[]) ?? []);
    } catch {
      setOrders([]);
    }
  }, [api]);

  const loadAll = useCallback(() => {
    void loadCatalog();
    if (authenticated) {
      void loadLoyalty();
      void loadNotifications();
      void loadOrders();
    }
  }, [authenticated, loadCatalog, loadLoyalty, loadNotifications, loadOrders]);

  useEffect(() => {
    setLoading(true);
    void loadCatalog().finally(() => setLoading(false));
    if (authenticated) {
      void loadLoyalty();
      void loadNotifications();
      void loadOrders();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authenticated]);

  const selectProduct = (productId: number) => {
    setBuy({ productId, quantity: 1, promoCode: '' });
    setPricing(null);
    setOrderResult(null);
    setBuyError(null);
  };

  const previewPricing = async () => {
    if (!buy.productId) {
      setBuyError('Choose a product first');
      return;
    }
    setPricingLoading(true);
    setBuyError(null);
    setOrderResult(null);
    try {
      const promo = buy.promoCode.trim();
      const qs = new URLSearchParams();
      if (promo) qs.set('promoCode', promo);
      qs.set('currency', 'LKR');
      const data = await api.get<PricingBreakdown>(
        `/pricing/${buy.productId}?${qs.toString()}`
      );
      setPricing(data ?? null);
    } catch (e) {
      setBuyError(e instanceof Error ? e.message : 'Failed to preview pricing');
      setPricing(null);
    } finally {
      setPricingLoading(false);
    }
  };

  const placeOrder = async () => {
    if (!buy.productId) {
      setBuyError('Choose a product first');
      return;
    }
    setBuyError(null);
    setOrderResult(null);
    try {
      const promo = buy.promoCode.trim() || undefined;
      const order = await api.post<ProductOrder>('/orders', {
        tenantId: 1,
        productId: buy.productId,
        quantity: buy.quantity,
        ...(promo ? { promoCode: promo } : {}),
      });
      setOrderResult({
        orderRef: order.orderRef,
        total: order.totalAmount,
        currency: order.currencyIso,
      });
      setPricing(null);
      setBuy({ productId: 0, quantity: 1, promoCode: '' });
      loadAll();
    } catch (e) {
      setBuyError(e instanceof Error ? e.message : 'Failed to place order');
    }
  };

  const selectedProduct = products.find((p) => p.id === buy.productId) ?? null;

  const fmt = (n: number | undefined | null) =>
    n == null ? '—' : Number(n).toFixed(2);

  return (
    <section className="page">
      <h1>Marketplace</h1>
      <p className="muted">
        Browse every product offered by connected providers and shops, preview an
        itemized price, and buy in seconds.
      </p>

      {!authenticated && (
        <p className="muted">
          Sign in to buy, track your orders and earn loyalty points.{' '}
          <Link className="btn" to="/login">
            Sign in
          </Link>
        </p>
      )}

      {loyalty && (
        <div className="stats">
          <div className="stat card">
            <span className="value">{loyalty.points}</span>
            <span className="label">Loyalty points</span>
          </div>
          <div className="stat card">
            <span className="value">{loyalty.tier}</span>
            <span className="label">Tier</span>
          </div>
          {notifications.length > 0 && (
            <div className="stat card">
              <span className="value">{notifications.length}</span>
              <span className="label">Notifications</span>
            </div>
          )}
        </div>
      )}

      {notifications.length > 0 && (
        <div className="card" style={{ marginBottom: '1rem' }}>
          <strong>Latest:</strong> {notifications[0].subject ?? 'Notification'} —{' '}
          <span className="muted">{notifications[0].body}</span>
        </div>
      )}

      {error && <p className="error">{error}</p>}
      {loading && <p className="muted">Loading the marketplace…</p>}

      <div className="grid">
        {products.map((p) => (
          <article className="card" key={p.id}>
            <span className="tag">{p.productType}</span>
            <h3>{p.title}</h3>
            <p className="muted">
              by {p.providerName}
              {p.origin && p.destination
                ? ` · ${p.origin} → ${p.destination}`
                : ''}
            </p>
            {p.eventDate && (
              <p className="muted">
                {new Date(p.eventDate).toLocaleString()}
              </p>
            )}
            <p className="price">
              {p.currencyIso} {fmt(p.price)}
            </p>
            <p className="muted">
              {p.availableQuantity} available
            </p>
            <button
              className="btn primary"
              onClick={() => selectProduct(p.id)}
              disabled={p.availableQuantity <= 0 || !p.enabled}
            >
              {p.availableQuantity > 0 && p.enabled ? 'Buy' : 'Sold out'}
            </button>
          </article>
        ))}
        {!loading && products.length === 0 && (
          <p className="muted">No products in this marketplace yet.</p>
        )}
      </div>

      {buy.productId !== 0 && selectedProduct && (
        <div className="card" style={{ marginTop: '1.5rem' }}>
          <h2>Buy: {selectedProduct.title}</h2>
          <p className="muted">{selectedProduct.description}</p>
          <form
            className="form"
            onSubmit={(e) => {
              e.preventDefault();
              void previewPricing();
            }}
          >
            <div className="field">
              <label htmlFor="qty">Quantity (1-5)</label>
              <input
                id="qty"
                type="number"
                min={1}
                max={5}
                value={buy.quantity}
                onChange={(e) =>
                  setBuy({ ...buy, quantity: Number(e.target.value) })
                }
              />
            </div>
            <div className="field">
              <label htmlFor="promo">Promo code (optional)</label>
              <input
                id="promo"
                value={buy.promoCode}
                placeholder="e.g. WELCOME10"
                onChange={(e) => setBuy({ ...buy, promoCode: e.target.value })}
              />
            </div>
            <div className="row">
              <button className="btn" type="submit" disabled={pricingLoading}>
                {pricingLoading ? 'Loading…' : 'Preview price'}
              </button>
              <button
                className="btn primary"
                type="button"
                onClick={() => void placeOrder()}
              >
                Place order
              </button>
            </div>
          </form>

          {buyError && <p className="error">{buyError}</p>}

          {pricing && (
            <div className="card" style={{ marginTop: '1rem', background: '#f8fafc' }}>
              <h3>Price breakdown ({pricing.targetCurrency})</h3>
              <table className="table">
                <tbody>
                  <tr>
                    <td>Base</td>
                    <td className="currency">{fmt(pricing.base)}</td>
                  </tr>
                  <tr>
                    <td>Tax</td>
                    <td className="currency">{fmt(pricing.tax)}</td>
                  </tr>
                  <tr>
                    <td>Service fee</td>
                    <td className="currency">{fmt(pricing.serviceFee)}</td>
                  </tr>
                  <tr>
                    <td>Discount</td>
                    <td className="currency">
                      -{fmt(pricing.discount)}{' '}
                      {pricing.promoName ? (
                        <span className="tag">{pricing.promoName}</span>
                      ) : null}
                    </td>
                  </tr>
                  <tr>
                    <td>
                      <strong>Total</strong>
                    </td>
                    <td className="currency">
                      <strong>{fmt(pricing.total)}</strong>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          )}

          {orderResult && (
            <p className="success">
              Order {orderResult.orderRef} placed for{' '}
              {orderResult.currency} {fmt(orderResult.total)}.
            </p>
          )}
        </div>
      )}

      <h2 style={{ marginTop: '2rem' }}>My orders</h2>
      {orders.length === 0 ? (
        <p className="muted">You have no orders yet.</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Ref</th>
              <th>Product</th>
              <th>Type</th>
              <th>Qty</th>
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
                <td className="currency">
                  {o.currencyIso} {fmt(o.totalAmount)}
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

      {authenticated && (
        <div style={{ marginTop: '1.5rem' }}>
          <Link className="btn" to="/orders">
            View all my orders
          </Link>
        </div>
      )}

      <p className="muted" style={{ marginTop: '1.5rem' }}>
        Product types: {PRODUCT_TYPES.join(', ')}.
      </p>
    </section>
  );
}
