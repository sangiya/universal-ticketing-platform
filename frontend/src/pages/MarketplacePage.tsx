import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link, useNavigate } from 'react-router-dom';

interface CheckoutOrder {
  orderRef: string;
  totalAmount: number;
  currencyIso: string;
  status: string;
}

interface Product {
  id: number;
  providerId: number;
  providerName: string;
  providerCode?: string;
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
  logoUrl?: string | null;
  themeColor?: string | null;
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

const PRODUCT_TYPES = ['TICKET', 'SERVICE', 'SEAT', 'ROUTE', 'ADMISSION', 'PACKAGE'];

export default function MarketplacePage() {
  const { api, authenticated } = useApi();
  const navigate = useNavigate();
  const [products, setProducts] = useState<Product[]>([]);
  const [orders, setOrders] = useState<ProductOrder[]>([]);
  const [loyalty, setLoyalty] = useState<Loyalty | null>(null);
  const [notifications, setNotifications] = useState<NotificationMsg[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [buyProduct, setBuyProduct] = useState<Product | null>(null);
  const [buyQty, setBuyQty] = useState(1);
  const [buyPromo, setBuyPromo] = useState('');
  const [pricing, setPricing] = useState<PricingBreakdown | null>(null);
  const [pricingLoading, setPricingLoading] = useState(false);
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
    } catch { setLoyalty(null); }
  }, [api]);

  const loadNotifications = useCallback(async () => {
    try {
      const data = await api.get<unknown[]>(`/notifications`);
      setNotifications((data as unknown as NotificationMsg[]) ?? []);
    } catch { setNotifications([]); }
  }, [api]);

  const loadOrders = useCallback(async () => {
    try {
      const data = await api.get<unknown[]>(`/orders/mine`);
      setOrders((data as unknown as ProductOrder[]) ?? []);
    } catch { setOrders([]); }
  }, [api]);

  useEffect(() => {
    setLoading(true);
    void loadCatalog().finally(() => setLoading(false));
    if (authenticated) { void loadLoyalty(); void loadNotifications(); void loadOrders(); }
  }, [authenticated]);

  const selectProduct = (product: Product) => {
    if (!authenticated) { navigate('/login'); return; }
    setBuyProduct(product);
    setBuyQty(1);
    setBuyPromo('');
    setPricing(null);
    setBuyError(null);
  };

  const previewPricing = async () => {
    if (!buyProduct) return;
    setPricingLoading(true);
    setBuyError(null);
    try {
      const promo = buyPromo.trim();
      const qs = new URLSearchParams();
      if (promo) qs.set('promoCode', promo);
      qs.set('currency', 'LKR');
      const data = await api.get<PricingBreakdown>(`/pricing/${buyProduct.id}?${qs.toString()}`);
      setPricing(data ?? null);
    } catch (e) {
      setBuyError(e instanceof Error ? e.message : 'Failed to preview pricing');
      setPricing(null);
    } finally {
      setPricingLoading(false);
    }
  };

  const placeOrder = async () => {
    if (!buyProduct) return;
    setBuyError(null);
    try {
      const promo = buyPromo.trim() || undefined;
      const order = await api.post<CheckoutOrder>('/orders/checkout', {
        tenantId: 1,
        productId: buyProduct.id,
        quantity: buyQty,
        ...(promo ? { promoCode: promo } : {}),
      });
      setBuyProduct(null);
      navigate(`/checkout/${order.orderRef}`);
    } catch (e) {
      const msg = e instanceof Error ? e.message : '';
      if (msg.includes('401') || msg.toLowerCase().includes('unauthorized')) {
        navigate('/login');
      } else {
        setBuyError(msg || 'Failed to start checkout');
      }
    }
  };

  const fmt = (n: number | undefined | null) => n == null ? '—' : Number(n).toFixed(2);

  return (
    <section className="page">
      <h1>Marketplace</h1>
      <p className="muted">Browse every product offered by connected providers and shops, preview an itemized price, and buy in seconds.</p>

      {!authenticated && (
        <p className="muted">Sign in to buy, track your orders and earn loyalty points. <Link className="btn" to="/login">Sign in</Link></p>
      )}

      {loyalty && (
        <div className="stats">
          <div className="stat card"><span className="value">{loyalty.points}</span><span className="label">Loyalty points</span></div>
          <div className="stat card"><span className="value">{loyalty.tier}</span><span className="label">Tier</span></div>
          {notifications.length > 0 && (
            <div className="stat card"><span className="value">{notifications.length}</span><span className="label">Notifications</span></div>
          )}
        </div>
      )}

      {notifications.length > 0 && (
        <div className="card" style={{ marginBottom: '1rem' }}>
          <strong>Latest:</strong> {notifications[0].subject ?? 'Notification'} — <span className="muted">{notifications[0].body}</span>
        </div>
      )}

      {error && <p className="error">{error}</p>}
      {loading && <p className="muted">Loading the marketplace...</p>}

      <div className="grid">
        {products.map((p) => {
          const color = p.themeColor || '#4f46e5';
          return (
            <article className="card product-card" key={p.id}>
              <div
                className="product-cover"
                style={{ background: `linear-gradient(135deg, ${color}, #7c3aed)` }}
              >
                <div className="cover-tint" />
                {p.logoUrl && <img src={p.logoUrl} alt="" className="logo" style={{ position: 'absolute', top: 12, left: 12, zIndex: 2, background: 'rgba(255,255,255,.25)' }} />}
                <span className="product-type">{p.productType}</span>
                <span className="price-badge">{p.currencyIso} {fmt(p.price)}</span>
              </div>
              <div className="product-body">
                <h3>{p.title}</h3>
                <p className="muted" style={{ fontSize: '0.85rem' }}>
                  by <strong style={{ color }}>{p.providerName}</strong>
                  {p.origin && p.destination ? ` · ${p.origin} → ${p.destination}` : ''}
                </p>
                {p.eventDate && <p className="muted">{new Date(p.eventDate).toLocaleString()}</p>}
                <p className="muted">{p.availableQuantity} available</p>
                <button className="btn primary"
                  onClick={() => selectProduct(p)} disabled={p.availableQuantity <= 0 || !p.enabled}>
                  {p.availableQuantity > 0 && p.enabled ? 'Buy' : 'Sold out'}
                </button>
              </div>
            </article>
          );
        })}
        {!loading && products.length === 0 && <p className="muted">No products in this marketplace yet.</p>}
      </div>

      {buyProduct && (
        <div className="modal-overlay" onClick={() => setBuyProduct(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Buy: {buyProduct.title}</h3>
              <p>{buyProduct.description || `${buyProduct.productType} from ${buyProduct.providerName}`}</p>
            </div>
            <div className="modal-body">
              <div className="stack">
                <div className="field">
                  <label>Quantity (1-5)</label>
                  <input
                    type="number"
                    min="1"
                    max="5"
                    value={buyQty}
                    onChange={(e) => setBuyQty(parseInt(e.target.value) || 1)}
                  />
                </div>
                <div className="field">
                  <label>Promo code (optional)</label>
                  <input
                    placeholder="e.g. WELCOME10"
                    value={buyPromo}
                    onChange={(e) => setBuyPromo(e.target.value)}
                  />
                </div>
                <button className="btn" onClick={previewPricing} disabled={pricingLoading}>
                  {pricingLoading ? 'Calculating…' : 'Preview price'}
                </button>

                {pricing && (
                  <div className="card" style={{ padding: '1rem', background: 'var(--surface-2)', border: '1px solid var(--border)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                      <span>Subtotal ({buyQty}x)</span>
                      <span>{pricing.currencyIso} {fmt(pricing.subtotal * buyQty)}</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                      <span>Tax</span>
                      <span>{pricing.currencyIso} {fmt(pricing.tax * buyQty)}</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                      <span>Service Fee</span>
                      <span>{pricing.currencyIso} {fmt(pricing.serviceFee * buyQty)}</span>
                    </div>
                    {pricing.discount > 0 && (
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem', color: 'var(--ok)' }}>
                        <span>Discount ({pricing.promoName})</span>
                        <span>-{pricing.currencyIso} {fmt(pricing.discount * buyQty)}</span>
                      </div>
                    )}
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 'bold', fontSize: '1.1rem', marginTop: '0.5rem', borderTop: '1px solid var(--border)', paddingTop: '0.5rem' }}>
                      <span>Total</span>
                      <span>{pricing.currencyIso} {fmt(pricing.total * buyQty)}</span>
                    </div>
                  </div>
                )}
                {buyError && <p className="error">{buyError}</p>}
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn" onClick={() => setBuyProduct(null)}>Cancel</button>
              <button
                className="btn primary"
                disabled={!pricing}
                onClick={placeOrder}
              >
                Proceed to payment
              </button>
            </div>
          </div>
        </div>
      )}

      <h2 style={{ marginTop: '2rem' }}>My orders</h2>
      {orders.length === 0 ? (
        <p className="muted">You have no orders yet.</p>
      ) : (
        <table className="table">
          <thead>
            <tr><th>Ref</th><th>Product</th><th>Type</th><th>Qty</th><th>Total</th><th>Status</th><th>Date</th></tr>
          </thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.id}>
                <td>{o.orderRef}</td>
                <td>{o.productTitle}</td>
                <td><span className="tag">{o.productType}</span></td>
                <td>{o.quantity}</td>
                <td className="currency">{o.currencyIso} {fmt(o.totalAmount)}</td>
                <td><span className={`badge ${o.status.toLowerCase()}`}>{o.status}</span></td>
                <td>{new Date(o.createdAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {authenticated && <div style={{ marginTop: '1.5rem' }}><Link className="btn" to="/orders">View all my orders</Link></div>}

      <p className="muted" style={{ marginTop: '1.5rem' }}>Product types: {PRODUCT_TYPES.join(', ')}.</p>
    </section>
  );
}
