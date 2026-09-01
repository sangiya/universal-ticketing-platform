import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useApi } from '../context/ApiContext';
import { Alert, Currency, EmptyState, Modal } from '../components/UI';
import SeatSelection, {
  type Seat,
  type SeatLayout,
  type MealSelection,
} from '../components/SeatSelection';

interface ProductDetail {
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
  // offer fields
  isOffer?: boolean;
  originalPrice?: number | null;
  discountPercent?: number;
  dealTag?: string | null;
  // movie fields
  language?: string | null;
  genre?: string | null;
  format?: string | null;
  durationMinutes?: number | null;
  ratingStars?: number | null;
  castList?: string | null;
  director?: string | null;
  releaseDate?: string | null;
  posterUrl?: string | null;
  bannerUrl?: string | null;
  isPremiere?: boolean;
  isNowShowing?: boolean;
  tagline?: string | null;
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

function formatDuration(min?: number | null): string {
  if (!min) return '—';
  const h = Math.floor(min / 60);
  const m = min % 60;
  if (h === 0) return `${m}m`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}m`;
}

const FALLBACK_POSTER =
  'https://placehold.co/400x600/6366f1/ffffff?text=No+Poster&font=raleway';

export default function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { api, authenticated } = useApi();
  const navigate = useNavigate();

  const [product, setProduct] = useState<ProductDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [buyQty, setBuyQty] = useState(1);
  const [buyPromo, setBuyPromo] = useState('');
  const [pricing, setPricing] = useState<PricingBreakdown | null>(null);
  const [pricingLoading, setPricingLoading] = useState(false);
  const [buyError, setBuyError] = useState<string | null>(null);
  const [selectedSeats, setSelectedSeats] = useState<Seat[]>([]);
  const [seatLayout, setSeatLayout] = useState<SeatLayout>('2-2');
  const [mealSelections, setMealSelections] = useState<MealSelection[]>([]);
  const [showBuyModal, setShowBuyModal] = useState(false);

  const needsSeats = (type: string) =>
    type === 'ROUTE' || type === 'SEAT' || type === 'ADMISSION';
  const supportsMeals = (type: string) =>
    type === 'ROUTE' || type === 'SEAT';

  const load = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<ProductDetail>(`/catalog/${id}`);
      setProduct(data as unknown as ProductDetail);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load product');
    } finally {
      setLoading(false);
    }
  }, [api, id]);

  useEffect(() => { void load(); }, [load]);

  const openBuy = () => {
    if (!authenticated) {
      navigate('/login');
      return;
    }
    setShowBuyModal(true);
    setBuyQty(1);
    setBuyPromo('');
    setPricing(null);
    setBuyError(null);
    setSelectedSeats([]);
    if (product) {
      const attrs = (product.attributes || '').toLowerCase();
      if (attrs.includes('2-3') || attrs.includes('sleeper')) setSeatLayout('2-3');
      else if (attrs.includes('3-2')) setSeatLayout('3-2');
      else if (attrs.includes('1-1') || attrs.includes('open')) setSeatLayout('1-1');
      else setSeatLayout('2-2');
    }
  };

  const closeBuy = () => {
    setShowBuyModal(false);
    setSelectedSeats([]);
    setMealSelections([]);
  };

  const onSeatsContinue = (seats: Seat[], _totalPrice: number, meals?: MealSelection[]) => {
    setSelectedSeats(seats);
    setMealSelections(meals ?? []);
    setBuyQty(seats.length);
    void previewPricingWithSeats(seats.length);
  };

  const previewPricingWithSeats = async (_qty: number) => {
    if (!product) return;
    setPricingLoading(true);
    setBuyError(null);
    try {
      const promo = buyPromo.trim();
      const qs = new URLSearchParams();
      if (promo) qs.set('promoCode', promo);
      if (product.currencyIso) qs.set('currency', product.currencyIso);
      const data = await api.get<PricingBreakdown>(
        `/pricing/${product.id}?${qs.toString()}`,
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
    if (!product) return;
    setPricingLoading(true);
    setBuyError(null);
    try {
      const promo = buyPromo.trim();
      const qs = new URLSearchParams();
      if (promo) qs.set('promoCode', promo);
      if (product.currencyIso) qs.set('currency', product.currencyIso);
      const data = await api.get<PricingBreakdown>(
        `/pricing/${product.id}?${qs.toString()}`,
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
    if (!product) return;
    setBuyError(null);
    try {
      const promo = buyPromo.trim() || undefined;
      const order = await api.post<{ orderRef: string }>('/orders/checkout', {
        productId: product.id,
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
      closeBuy();
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

  if (loading) {
    return (
      <section className="page">
        <div className="breadcrumb"><Link to="/marketplace">← Back to marketplace</Link></div>
        <div className="card skeleton" style={{ height: 420 }} />
      </section>
    );
  }

  if (error || !product) {
    return (
      <section className="page">
        <div className="breadcrumb"><Link to="/marketplace">← Back to marketplace</Link></div>
        <Alert kind="danger" title="Product not found">
          {error ?? 'This product does not exist or is no longer available.'}
          {' '}<Link to="/marketplace">Browse marketplace →</Link>
        </Alert>
      </section>
    );
  }

  const soldOut = product.availableQuantity <= 0 || !product.enabled;
  const color = product.themeColor || '#4f46e5';
  const isMovie = product.productType === 'ADMISSION' && !!product.posterUrl;
  const poster = product.posterUrl || FALLBACK_POSTER;

  return (
    <section className="page">
      {/* Breadcrumb */}
      <div className="breadcrumb">
        <Link to="/">Home</Link>
        <span className="sep">›</span>
        <Link to="/marketplace">Marketplace</Link>
        <span className="sep">›</span>
        <Link to={`/marketplace?type=${product.productType}`}>{product.productType}</Link>
        <span className="sep">›</span>
        <span>{product.title}</span>
      </div>

      {isMovie ? (
        /* ── Movie layout ── */
        <div className="product-detail-movie">
          <div className="product-detail-poster">
            <img src={poster} alt={product.title} />
          </div>
          <div className="product-detail-body">
            {product.isPremiere && (
              <div className="movie-featured-badge">✨ Premiere</div>
            )}
            <h1>{product.title}</h1>
            {product.tagline && <p className="product-tagline">{product.tagline}</p>}

            <div className="product-chips">
              {product.ratingStars != null && (
                <span className="movie-chip rating">⭐ {Number(product.ratingStars).toFixed(1)}</span>
              )}
              {product.language && <span className="movie-chip">🗣 {product.language}</span>}
              {product.format && <span className="movie-chip">🎞 {product.format}</span>}
              {product.durationMinutes != null && (
                <span className="movie-chip">⏱ {formatDuration(product.durationMinutes)}</span>
              )}
              {(product.genre || '').split(',').slice(0, 3).map((g) => (
                <span key={g.trim()} className="movie-chip outline">{g.trim()}</span>
              ))}
            </div>

            {product.castList && (
              <div className="product-cast">
                <strong>Cast:</strong> {product.castList}
              </div>
            )}
            {product.director && (
              <div className="product-director">
                <strong>Director:</strong> {product.director}
              </div>
            )}
            {product.releaseDate && (
              <div className="muted fs-sm">Released: {new Date(product.releaseDate).toLocaleDateString()}</div>
            )}

            {product.description && (
              <p className="product-description">{product.description}</p>
            )}

            <div className="product-price-row">
              {product.isOffer && product.originalPrice && product.originalPrice > product.price && (
                <span className="product-price-original">
                  <Currency amount={product.originalPrice} currency={product.currencyIso} />
                </span>
              )}
              <span className="product-price">
                <Currency amount={product.price} currency={product.currencyIso} />
              </span>
              {product.isOffer && product.discountPercent != null && (
                <span className="badge danger">{product.discountPercent}% OFF</span>
              )}
              {product.dealTag && (
                <span className="badge success">{product.dealTag}</span>
              )}
            </div>

            <button
              className="btn primary lg block"
              onClick={openBuy}
              disabled={soldOut}
            >
              {soldOut ? '🎫 Sold out' : '🎟 Book tickets'}
            </button>

            {!authenticated && !soldOut && (
              <p className="muted fs-sm">
                <Link to="/login">Sign in</Link> to purchase tickets
              </p>
            )}
          </div>
        </div>
      ) : (
        /* ── Standard product layout ── */
        <div className="product-detail-grid">
          <div className="product-detail-media">
            <div
              className="product-detail-cover"
              style={{ background: `linear-gradient(135deg, ${color}, #7c3aed)` }}
            >
              <span className="product-type-badge">
                {productIcon(product.productType)} {product.productType}
              </span>
            </div>
          </div>

          <div className="product-detail-info">
            <div className="product-provider">
              by <strong style={{ color }}>{product.providerName}</strong>
            </div>
            <h1>{product.title}</h1>

            {product.origin && product.destination && (
              <div className="product-route">
                <span className="product-route-from">{product.origin}</span>
                <span className="product-route-arrow">→</span>
                <span className="product-route-to">{product.destination}</span>
              </div>
            )}

            {product.eventDate && (
              <div className="product-event">
                📅 {new Date(product.eventDate).toLocaleString()}
              </div>
            )}

            <div className="product-chips">
              <span className="tag outline">
                {soldOut ? '✕ Sold out' : `🎟 ${product.availableQuantity} available`}
              </span>
            </div>

            {product.description && (
              <p className="product-description">{product.description}</p>
            )}

            {product.attributes && (
              <div className="product-attrs">
                <strong>Details:</strong> {product.attributes}
              </div>
            )}

            {/* ── Price block ── */}
            <div className="product-price-block card">
              <div className="product-price-row">
                {product.isOffer && product.originalPrice && product.originalPrice > product.price && (
                  <span className="product-price-original">
                    <Currency amount={product.originalPrice} currency={product.currencyIso} />
                  </span>
                )}
                <span className="product-price">
                  <Currency amount={product.price} currency={product.currencyIso} />
                </span>
                {product.isOffer && product.discountPercent != null && (
                  <span className="badge danger">{product.discountPercent}% OFF</span>
                )}
                {product.dealTag && (
                  <span className="badge success">{product.dealTag}</span>
                )}
              </div>

              <button
                className="btn primary lg block"
                onClick={openBuy}
                disabled={soldOut}
                style={{ marginTop: '1rem' }}
              >
                {soldOut ? 'Sold out' : authenticated ? 'Buy now →' : 'Sign in to buy'}
              </button>

              {!authenticated && !soldOut && (
                <p className="muted fs-sm" style={{ textAlign: 'center', marginTop: 8 }}>
                  <Link to="/login">Sign in</Link> or <Link to="/register">create an account</Link>
                </p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ── Buy modal ── */}
      {showBuyModal && (
        <Modal
          title={needsSeats(product.productType) ? 'Select your seats' : `Buy: ${product.title}`}
          description={product.description || `${product.productType} from ${product.providerName}`}
          onClose={closeBuy}
          size="lg"
          footer={
            needsSeats(product.productType) ? (
              <>
                <button className="btn" onClick={closeBuy}>Cancel</button>
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
                <button className="btn" onClick={closeBuy}>Cancel</button>
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
          {needsSeats(product.productType) ? (
            <div className="stack">
              <SeatSelection
                totalSeats={Math.min(40, product.availableQuantity || 40)}
                layout={seatLayout}
                seatPrice={product.price}
                currencyIso={product.currencyIso || 'USD'}
                productTitle={product.title}
                productType={product.productType}
                enableMeals={supportsMeals(product.productType)}
                onContinue={onSeatsContinue}
                onCancel={closeBuy}
              />
              {selectedSeats.length > 0 && (
                <div className="field">
                  <label>Promo code (optional)</label>
                  <input
                    placeholder="e.g. WELCOME10"
                    value={buyPromo}
                    onChange={(e) => setBuyPromo(e.target.value.toUpperCase())}
                    onBlur={() => previewPricingWithSeats(selectedSeats.length)}
                  />
                </div>
              )}
              {pricing && (
                <PricingSummary
                  pricing={pricing}
                  qty={selectedSeats.length}
                  meals={mealSelections}
                />
              )}
              {buyError && <Alert kind="danger">{buyError}</Alert>}
            </div>
          ) : (
            <div className="stack">
              <div className="field">
                <label>Quantity</label>
                <input
                  type="number"
                  min={1}
                  max={Math.min(5, product.availableQuantity)}
                  value={buyQty}
                  onChange={(e) => setBuyQty(parseInt(e.target.value) || 1)}
                />
                <span className="hint">{product.availableQuantity} available</span>
              </div>
              <div className="field">
                <label>Promo code (optional)</label>
                <input
                  placeholder="e.g. WELCOME10"
                  value={buyPromo}
                  onChange={(e) => setBuyPromo(e.target.value.toUpperCase())}
                />
              </div>
              <button className="btn" onClick={previewPricing} disabled={pricingLoading}>
                {pricingLoading ? '⏳ Calculating…' : '💰 Preview price'}
              </button>
              {pricing && (
                <PricingSummary pricing={pricing} qty={buyQty} meals={[]} />
              )}
              {buyError && <Alert kind="danger">{buyError}</Alert>}
            </div>
          )}
        </Modal>
      )}
    </section>
  );
}

function PricingSummary({
  pricing,
  qty,
  meals,
}: {
  pricing: PricingBreakdown;
  qty: number;
  meals: MealSelection[];
}) {
  const mealTotal = meals.reduce((s, m) => s + m.mealPrice * m.quantity, 0);
  return (
    <div className="card" style={{ background: 'var(--surface-2)', border: '1px solid var(--border)' }}>
      <div className="summary-row">
        <span className="muted">Subtotal ({qty}×)</span>
        <Currency amount={pricing.subtotal * qty} currency={pricing.currencyIso} />
      </div>
      <div className="summary-row">
        <span className="muted">Tax</span>
        <Currency amount={pricing.tax * qty} currency={pricing.currencyIso} />
      </div>
      <div className="summary-row">
        <span className="muted">Service fee</span>
        <Currency amount={pricing.serviceFee * qty} currency={pricing.currencyIso} />
      </div>
      {pricing.discount > 0 && (
        <div className="summary-row text-success">
          <span>Discount {pricing.promoName ? `· ${pricing.promoName}` : ''}</span>
          <span>
            −<Currency amount={pricing.discount * qty} currency={pricing.currencyIso} />
          </span>
        </div>
      )}
      {meals.map((m) => (
        <div className="summary-row" key={m.mealId}>
          <span className="muted">🍱 {m.mealName} × {m.quantity}</span>
          <Currency amount={m.mealPrice * m.quantity} currency={pricing.currencyIso} />
        </div>
      ))}
      <div className="summary-row total">
        <span>Total</span>
        <Currency
          amount={pricing.total * qty + mealTotal}
          currency={pricing.currencyIso}
        />
      </div>
    </div>
  );
}
