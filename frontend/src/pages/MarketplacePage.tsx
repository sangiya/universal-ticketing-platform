import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import {
  Alert,
  Currency,
  EmptyState,
  Modal,
  PageHeader,
  SectionTitle,
  StatCard,
} from '../components/UI';
import SeatSelection, {
  type Seat,
  type SeatLayout,
  type MealSelection,
} from '../components/SeatSelection';

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

function productIcon(type: string): string {
  switch (type) {
    case 'ROUTE': return '🚌';
    case 'ADMISSION': return '🎟️';
    case 'SERVICE': return '🛎️';
    case 'SEAT': return '💺';
    case 'PACKAGE': return '📦';
    default: return '🎫';
  }
}

export default function MarketplacePage() {
  const { api, authenticated, tenantId } = useApi();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [products, setProducts] = useState<Product[]>([]);
  const [orders, setOrders] = useState<ProductOrder[]>([]);
  const [loyalty, setLoyalty] = useState<Loyalty | null>(null);
  const [notifications, setNotifications] = useState<NotificationMsg[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>('');
  const [sortBy, setSortBy] = useState<'price-asc' | 'price-desc' | 'name'>('price-asc');

  const [buyProduct, setBuyProduct] = useState<Product | null>(null);
  const [buyQty, setBuyQty] = useState(1);
  const [buyPromo, setBuyPromo] = useState('');
  const [pricing, setPricing] = useState<PricingBreakdown | null>(null);
  const [pricingLoading, setPricingLoading] = useState(false);
  const [buyError, setBuyError] = useState<string | null>(null);
  const [selectedSeats, setSelectedSeats] = useState<Seat[]>([]);
  const [seatLayout, setSeatLayout] = useState<SeatLayout>('2-2');
  const [mealSelections, setMealSelections] = useState<MealSelection[]>([]);

  const needsSeatSelection = (type: string) =>
    type === 'ROUTE' || type === 'SEAT' || type === 'ADMISSION';
  // Trains, flights and long-distance buses offer food; short routes and
  // admission tickets typically don't.
  const supportsMeals = (type: string) =>
    type === 'ROUTE' || type === 'SEAT';

  const loadCatalog = useCallback(async () => {
    try {
      const qs = tenantId ? `?tenantId=${tenantId}` : '';
      const data = await api.get<unknown[]>(`/catalog${qs}`);
      setProducts((data as unknown as Product[]) ?? []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load catalog');
    }
  }, [api, tenantId]);

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

  // Auto-open buy modal if a `?productId=ID` deep-link is present (used by
  // MoviesPage Book button, dashboard offers, etc).
  useEffect(() => {
    if (loading) return;
    const pid = searchParams.get('productId');
    if (!pid) return;
    const target = products.find((p) => p.id === parseInt(pid, 10));
    if (!target) return;
    if (!authenticated) {
      navigate('/login');
      return;
    }
    selectProduct(target);
    // Clear the query so a refresh doesn't re-trigger
    const next = new URLSearchParams(searchParams);
    next.delete('productId');
    setSearchParams(next, { replace: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading, products, authenticated, searchParams]);

  // Apply deep-link filters (from dashboard search, vertical click, etc)
  useEffect(() => {
    const q = searchParams.get('search');
    if (q) setSearch(q);
    const t = searchParams.get('type');
    if (t) setTypeFilter(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  const selectProduct = (product: Product) => {
    if (!authenticated) {
      navigate('/login');
      return;
    }
    setBuyProduct(product);
    setBuyQty(1);
    setBuyPromo('');
    setPricing(null);
    setBuyError(null);
    setSelectedSeats([]);
    // Pick seat layout from product attributes / type / quantity
    const attrs = (product.attributes || '').toLowerCase();
    if (attrs.includes('2-3') || attrs.includes('sleeper')) setSeatLayout('2-3');
    else if (attrs.includes('3-2')) setSeatLayout('3-2');
    else if (attrs.includes('1-1') || attrs.includes('open')) setSeatLayout('1-1');
    else if (attrs.includes('open') || product.productType === 'ADMISSION') setSeatLayout('2-2');
    else setSeatLayout('2-2');
  };

  const onSeatsContinue = (seats: Seat[], _totalPrice: number, meals?: MealSelection[]) => {
    setSelectedSeats(seats);
    setMealSelections(meals ?? []);
    setBuyQty(seats.length);
    // Trigger pricing preview with seat count
    void previewPricingWithSeats(seats.length);
  };

  const previewPricingWithSeats = async (_qty: number) => {
    if (!buyProduct) return;
    setPricingLoading(true);
    setBuyError(null);
    try {
      const promo = buyPromo.trim();
      const qs = new URLSearchParams();
      if (promo) qs.set('promoCode', promo);
      if (buyProduct.currencyIso) qs.set('currency', buyProduct.currencyIso);
      const data = await api.get<PricingBreakdown>(
        `/pricing/${buyProduct.id}?${qs.toString()}`,
      );
      setPricing(data ?? null);
    } catch (e) {
      setBuyError(e instanceof Error ? e.message : 'Failed to preview pricing');
      setPricing(null);
    } finally {
      setPricingLoading(false);
    }
  };

  const previewPricing = async () => {
    if (!buyProduct) return;
    setPricingLoading(true);
    setBuyError(null);
    try {
      const promo = buyPromo.trim();
      const qs = new URLSearchParams();
      if (promo) qs.set('promoCode', promo);
      if (buyProduct.currencyIso) qs.set('currency', buyProduct.currencyIso);
      const data = await api.get<PricingBreakdown>(
        `/pricing/${buyProduct.id}?${qs.toString()}`,
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
    if (!buyProduct) return;
    setBuyError(null);
    try {
      const promo = buyPromo.trim() || undefined;
      const order = await api.post<CheckoutOrder>('/orders/checkout', {
        productId: buyProduct.id,
        quantity: Math.max(buyQty, selectedSeats.length || 1),
        ...(promo ? { promoCode: promo } : {}),
        ...(selectedSeats.length > 0 ? { seatIds: selectedSeats.map((s) => s.id) } : {}),
        ...(mealSelections.length > 0
          ? {
              meals: mealSelections.map((m) => ({
                mealId: m.mealId,
                mealName: m.mealName,
                quantity: m.quantity,
                unitPrice: m.mealPrice,
              })),
            }
          : {}),
      });
      setBuyProduct(null);
      setSelectedSeats([]);
      setMealSelections([]);
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

  const filtered = products
    .filter((p) =>
      !typeFilter ? true : p.productType === typeFilter,
    )
    .filter((p) =>
      !search.trim()
        ? true
        : p.title.toLowerCase().includes(search.toLowerCase()) ||
          p.providerName.toLowerCase().includes(search.toLowerCase()) ||
          (p.origin || '').toLowerCase().includes(search.toLowerCase()) ||
          (p.destination || '').toLowerCase().includes(search.toLowerCase()),
    )
    .sort((a, b) => {
      if (sortBy === 'price-asc') return a.price - b.price;
      if (sortBy === 'price-desc') return b.price - a.price;
      return a.title.localeCompare(b.title);
    });

  const fmt = (n: number | undefined | null) => (n == null ? '—' : Number(n).toFixed(2));

  return (
    <section className="page">
      <div className="breadcrumb">
        {authenticated ? (
          <>
            <Link to="/dashboard">Dashboard</Link>
            <span className="sep">›</span>
            <span>Marketplace</span>
          </>
        ) : (
          <span>Marketplace</span>
        )}
      </div>

      <PageHeader
        title="Marketplace"
        subtitle="Browse every product offered by connected providers and shops. Preview pricing, redeem promo codes, and buy in seconds."
        badge={
          <span className="badge role-agent no-dot">{products.length} live</span>
        }
      />

      {authenticated && loyalty && (
        <div className="stats">
          <StatCard
            label="Loyalty points"
            value={loyalty.points.toLocaleString()}
            icon="⭐"
            variant="violet"
          />
          <StatCard
            label="Tier"
            value={loyalty.tier}
            icon="🏆"
            variant="warning"
          />
          <StatCard
            label="Lifetime"
            value={loyalty.lifetimePoints.toLocaleString()}
            icon="📈"
            variant="info"
          />
          {notifications.length > 0 && (
            <StatCard
              label="Notifications"
              value={notifications.length}
              icon="🔔"
              variant="success"
            />
          )}
        </div>
      )}

      {!authenticated && (
        <Alert kind="info" title="Sign in to unlock the full marketplace">
          <Link to="/login">Sign in</Link> or{' '}
          <Link to="/register">create an account</Link> to buy, track your
          orders, and earn loyalty points. You can still browse the catalog.
        </Alert>
      )}

      {error && <Alert kind="danger" title="Could not load catalog">{error}</Alert>}

      <div className="filters">
        <div className="filter-group" style={{ flex: 1, minWidth: 220 }}>
          <input
            placeholder="🔍 Search products, providers, routes…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ flex: 1 }}
          />
        </div>
        <div className="filter-group">
          <span className="filter-label">Type</span>
          <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
            <option value="">All types</option>
            {PRODUCT_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </div>
        <div className="filter-group">
          <span className="filter-label">Sort</span>
          <select value={sortBy} onChange={(e) => setSortBy(e.target.value as never)}>
            <option value="price-asc">Price · Low → High</option>
            <option value="price-desc">Price · High → Low</option>
            <option value="name">Name (A–Z)</option>
          </select>
        </div>
      </div>

      <div className="results-count" style={{ margin: '0 0 var(--space-3)' }}>
        <strong>{filtered.length}</strong> {filtered.length === 1 ? 'product' : 'products'}
        {typeFilter ? ` in ${typeFilter}` : ''}
        {search ? ` matching "${search}"` : ''}
      </div>

      {loading && (
        <div className="grid">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="card skeleton-card" />
          ))}
        </div>
      )}

      {!loading && filtered.length === 0 && (
        <EmptyState
          icon="🛍️"
          title="No products match your filters"
          description="Try clearing your filters, switching tabs, or searching with different terms."
          action={
            <button
              className="btn"
              onClick={() => {
                setSearch('');
                setTypeFilter('');
              }}
            >
              Clear filters
            </button>
          }
        />
      )}

      {!loading && filtered.length > 0 && (
        <div className="grid">
          {filtered.map((p) => {
            const color = p.themeColor || '#4f46e5';
            const soldOut = p.availableQuantity <= 0 || !p.enabled;
            return (
              <article
                className="card product-card"
                key={p.id}
                aria-label={`${p.title} by ${p.providerName}`}
              >
                <div
                  className="product-cover"
                  style={{ background: `linear-gradient(135deg, ${color}, #7c3aed)` }}
                >
                  <span className="product-type">
                    {productIcon(p.productType)} {p.productType}
                  </span>
                  <span className="price-badge">
                    {p.currencyIso} {fmt(p.price)}
                  </span>
                </div>
                <div className="product-body">
                  <h3>
                    <Link to={`/product/${p.id}`} style={{ color: 'inherit', textDecoration: 'none' }}>
                      {p.title}
                    </Link>
                  </h3>
                  <p className="muted fs-sm">
                    by{' '}
                    <strong style={{ color }}>{p.providerName}</strong>
                    {p.origin && p.destination ? ` · ${p.origin} → ${p.destination}` : ''}
                  </p>
                  {p.eventDate && (
                    <p className="muted fs-xs">
                      📅 {new Date(p.eventDate).toLocaleString()}
                    </p>
                  )}
                  <div className="product-meta">
                    <span className="tag outline">
                      {soldOut ? '✕ Sold out' : `🎟 ${p.availableQuantity} left`}
                    </span>
                    {p.attributes && (
                      <span className="tag" title={p.attributes}>
                        Refundable
                      </span>
                    )}
                  </div>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <Link to={`/product/${p.id}`} className="btn block" style={{ flex: 1 }}>
                      View details
                    </Link>
                    <button
                      className="btn primary block"
                      style={{ flex: 1 }}
                      onClick={() => selectProduct(p)}
                      disabled={soldOut}
                    >
                      {soldOut ? 'Sold out' : authenticated ? 'Buy' : 'Sign in'}
                    </button>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}

      {buyProduct && (
        <Modal
          title={needsSeatSelection(buyProduct.productType) ? 'Select your seats' : `Buy: ${buyProduct.title}`}
          description={buyProduct.description || `${buyProduct.productType} from ${buyProduct.providerName}`}
          onClose={() => setBuyProduct(null)}
          size="lg"
          footer={
            needsSeatSelection(buyProduct.productType) ? (
              <>
                <button className="btn" onClick={() => setBuyProduct(null)}>
                  Cancel
                </button>
                <button
                  className="btn primary"
                  disabled={selectedSeats.length === 0 || !pricing}
                  onClick={placeOrder}
                >
                  Continue to payment ({selectedSeats.length} {selectedSeats.length === 1 ? 'seat' : 'seats'}) →
                </button>
              </>
            ) : (
              <>
                <button className="btn" onClick={() => setBuyProduct(null)}>
                  Cancel
                </button>
                <button
                  className="btn primary"
                  disabled={!pricing}
                  onClick={placeOrder}
                >
                  Proceed to payment →
                </button>
              </>
            )
          }
        >
          {needsSeatSelection(buyProduct.productType) ? (
            <div className="stack">
              <SeatSelection
                totalSeats={Math.min(40, buyProduct.availableQuantity || 40)}
                layout={seatLayout}
                seatPrice={buyProduct.price}
                currencyIso={buyProduct.currencyIso || 'USD'}
                productTitle={buyProduct.title}
                productType={buyProduct.productType}
                enableMeals={supportsMeals(buyProduct.productType)}
                onContinue={onSeatsContinue}
                onCancel={() => setBuyProduct(null)}
              />
              {selectedSeats.length > 0 && (
                <div className="field">
                  <label htmlFor="buy-promo-seat">Promo code (optional)</label>
                  <input
                    id="buy-promo-seat"
                    placeholder="e.g. WELCOME10"
                    value={buyPromo}
                    onChange={(e) => setBuyPromo(e.target.value.toUpperCase())}
                    onBlur={() => previewPricingWithSeats(selectedSeats.length)}
                  />
                </div>
              )}
              {pricing && (
                <div
                  className="card"
                  style={{ background: 'var(--surface-2)', border: '1px solid var(--border)' }}
                >
                  <div className="summary-row">
                    <span className="muted">Subtotal ({selectedSeats.length}×)</span>
                    <Currency amount={pricing.subtotal * selectedSeats.length} currency={pricing.currencyIso} />
                  </div>
                  <div className="summary-row">
                    <span className="muted">Tax</span>
                    <Currency amount={pricing.tax * selectedSeats.length} currency={pricing.currencyIso} />
                  </div>
                  <div className="summary-row">
                    <span className="muted">Service fee</span>
                    <Currency amount={pricing.serviceFee * selectedSeats.length} currency={pricing.currencyIso} />
                  </div>
                  {pricing.discount > 0 && (
                    <div className="summary-row text-success">
                      <span>Discount {pricing.promoName ? `· ${pricing.promoName}` : ''}</span>
                      <span>
                        −<Currency amount={pricing.discount * selectedSeats.length} currency={pricing.currencyIso} />
                      </span>
                    </div>
                  )}
                  {mealSelections.length > 0 && (
                    <>
                      {mealSelections.map((m) => (
                        <div className="summary-row" key={m.mealId}>
                          <span className="muted">🍱 {m.mealName} × {m.quantity}</span>
                          <Currency amount={m.mealPrice * m.quantity} currency={pricing.currencyIso} />
                        </div>
                      ))}
                    </>
                  )}
                  <div className="summary-row total">
                    <span>Total</span>
                    <Currency
                      amount={pricing.total * selectedSeats.length + mealSelections.reduce((s, m) => s + m.mealPrice * m.quantity, 0)}
                      currency={pricing.currencyIso}
                    />
                  </div>
                </div>
              )}
              {buyError && (
                <Alert kind="danger" title="Could not start checkout">
                  {buyError}
                </Alert>
              )}
            </div>
          ) : (
            <div className="stack">
              <div className="field">
                <label htmlFor="buy-qty">Quantity</label>
                <input
                  id="buy-qty"
                  type="number"
                  min={1}
                  max={Math.min(5, buyProduct.availableQuantity)}
                  value={buyQty}
                  onChange={(e) => setBuyQty(parseInt(e.target.value) || 1)}
                />
                <span className="hint">{buyProduct.availableQuantity} tickets available</span>
              </div>
              <div className="field">
                <label htmlFor="buy-promo">Promo code (optional)</label>
                <input
                  id="buy-promo"
                  placeholder="e.g. WELCOME10"
                  value={buyPromo}
                  onChange={(e) => setBuyPromo(e.target.value.toUpperCase())}
                />
              </div>
              <button
                className="btn"
                onClick={previewPricing}
                disabled={pricingLoading}
              >
                {pricingLoading ? (
                  <>
                    <span className="spinner" />
                    Calculating…
                  </>
                ) : (
                  '💰 Preview price'
                )}
              </button>

              {pricing && (
                <div
                  className="card"
                  style={{ background: 'var(--surface-2)', border: '1px solid var(--border)' }}
                >
                  <div className="summary-row">
                    <span className="muted">Subtotal ({buyQty}×)</span>
                    <Currency amount={pricing.subtotal * buyQty} currency={pricing.currencyIso} />
                  </div>
                  <div className="summary-row">
                    <span className="muted">Tax</span>
                    <Currency amount={pricing.tax * buyQty} currency={pricing.currencyIso} />
                  </div>
                  <div className="summary-row">
                    <span className="muted">Service fee</span>
                    <Currency amount={pricing.serviceFee * buyQty} currency={pricing.currencyIso} />
                  </div>
                  {pricing.discount > 0 && (
                    <div className="summary-row text-success">
                      <span>Discount {pricing.promoName ? `· ${pricing.promoName}` : ''}</span>
                      <span>
                        −<Currency amount={pricing.discount * buyQty} currency={pricing.currencyIso} />
                      </span>
                    </div>
                  )}
                  <div className="summary-row total">
                    <span>Total</span>
                    <Currency amount={pricing.total * buyQty} currency={pricing.currencyIso} />
                  </div>
                </div>
              )}

              {buyError && (
                <Alert kind="danger" title="Could not start checkout">
                  {buyError}
                </Alert>
              )}
            </div>
          )}
        </Modal>
      )}

      {authenticated && (
        <>
          <SectionTitle
            title="My recent orders"
            right={
              <Link to="/orders" className="muted">
                View all →
              </Link>
            }
          />
          {orders.length === 0 ? (
            <p className="muted">You have no orders yet. Start browsing above!</p>
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Reference</th>
                    <th>Product</th>
                    <th>Type</th>
                    <th className="right">Qty</th>
                    <th className="right">Total</th>
                    <th>Status</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.slice(0, 5).map((o) => (
                    <tr key={o.id}>
                      <td>
                        <code className="tag">{o.orderRef}</code>
                      </td>
                      <td>{o.productTitle}</td>
                      <td>
                        <span className="tag">{o.productType}</span>
                      </td>
                      <td className="right num">{o.quantity}</td>
                      <td className="right">
                        <Currency amount={o.totalAmount} currency={o.currencyIso} />
                      </td>
                      <td>
                        <span className={`badge ${o.status.toLowerCase()}`}>{o.status}</span>
                      </td>
                      <td className="muted fs-sm">
                        {new Date(o.createdAt).toLocaleDateString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </section>
  );
}
