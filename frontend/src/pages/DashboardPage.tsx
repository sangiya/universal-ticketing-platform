import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useApi } from '../context/ApiContext';
import { Alert, Currency, EmptyState, PageHeader, SectionTitle, StatCard } from '../components/UI';

interface Order {
  id: number;
  orderRef: string;
  productTitle: string;
  productType: string;
  providerName: string;
  quantity: number;
  totalAmount: number;
  currencyIso: string;
  status: string;
  createdAt: string;
  paidAt: string | null;
}

interface Loyalty {
  points: number;
  lifetimePoints: number;
  tier: string;
}

interface Notification {
  id: number;
  subject: string | null;
  body: string;
  channel: string;
  status: string;
  createdAt: string;
}

interface OfferSummary {
  id: number;
  title: string;
  currentPrice: number;
  originalPrice: number | null;
  discountPercent: number;
  dealTag: string | null;
  currencyIso: string;
  validUntil: string | null;
  providerName: string;
  productType: string;
}

interface VerticalCount {
  vertical: string;
  productType: string;
  count: number;
}

function statusVariant(s: string): string {
  const v = s.toLowerCase();
  if (v === 'paid' || v === 'issued' || v === 'confirmed') return 'success';
  if (v === 'pending' || v === 'open') return 'info';
  if (v === 'cancelled' || v === 'refunded' || v === 'expired') return 'danger';
  return 'default';
}

function statusIcon(s: string): string {
  const v = s.toLowerCase();
  if (v === 'paid' || v === 'issued' || v === 'confirmed') return '✅';
  if (v === 'pending' || v === 'open') return '⏳';
  if (v === 'cancelled' || v === 'refunded') return '✕';
  return '🎟';
}

function greetingFor(name: string | null | undefined): string {
  const h = new Date().getHours();
  if (h < 5) return 'Good night';
  if (h < 12) return 'Good morning';
  if (h < 18) return 'Good afternoon';
  return 'Good evening';
  void name;
}

export default function DashboardPage() {
  const { api, authenticated, username, role } = useApi();
  const navigate = useNavigate();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loyalty, setLoyalty] = useState<Loyalty | null>(null);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [offers, setOffers] = useState<OfferSummary[]>([]);
  const [verticals, setVerticals] = useState<VerticalCount[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [o, l, n, off, v] = await Promise.allSettled([
        api.get<Order[]>('/orders/mine'),
        api.get<Loyalty>('/loyalty').catch(() => null),
        api.get<Notification[]>('/notifications').catch(() => []),
        api.get<unknown[]>('/offers').catch(() => []),
        api.get<unknown[]>('/search/verticals').catch(() => []),
      ]);
      if (o.status === 'fulfilled') setOrders((o.value as unknown as Order[]) ?? []);
      if (l.status === 'fulfilled' && l.value) setLoyalty(l.value as Loyalty);
      if (n.status === 'fulfilled') setNotifications((n.value as unknown as Notification[]) ?? []);
      if (off.status === 'fulfilled') {
        const arr = (off.value as unknown as OfferSummary[]) ?? [];
        setOffers(arr.slice(0, 6));
      }
      if (v.status === 'fulfilled') {
        setVerticals((v.value as unknown as VerticalCount[]) ?? []);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load dashboard');
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => {
    if (!authenticated) {
      navigate('/login', { replace: true });
      return;
    }
    void load();
  }, [authenticated, load, navigate]);

  // ── Derived data ──
  const paidOrders = orders.filter((o) => o.status === 'PAID' || o.status === 'ISSUED');
  const pendingOrders = orders.filter((o) => o.status === 'PENDING');
  const totalSpent = paidOrders.reduce((s, o) => s + Number(o.totalAmount || 0), 0);
  const upcomingTrips = paidOrders
    .slice()
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 4);
  const lifetimeSpendCurrency = paidOrders[0]?.currencyIso || 'USD';

  return (
    <section className="page">
      <PageHeader
        title={`${greetingFor(username)}, ${username ?? 'there'} 👋`}
        subtitle="Your bookings, offers, and quick actions — all in one place."
        badge={
          role ? <span className={`badge role-${role.toLowerCase()}`}>{role}</span> : null
        }
      />

      {error && <Alert kind="danger">{error}</Alert>}

      {/* ── Quick search bar ── */}
      <div className="dashboard-search-bar">
        <div className="search-console" role="search">
          <div className="field" style={{ flex: 2 }}>
            <label htmlFor="dash-search">🔍 Search</label>
            <input
              id="dash-search"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  if (searchQuery.trim()) {
                    navigate(`/marketplace?search=${encodeURIComponent(searchQuery.trim())}`);
                  } else {
                    navigate('/marketplace');
                  }
                }
              }}
              placeholder="Search any bus, train, flight, movie, event…"
            />
          </div>
          <button
            className="btn primary"
            onClick={() => {
              if (searchQuery.trim()) {
                navigate(`/marketplace?search=${encodeURIComponent(searchQuery.trim())}`);
              } else {
                navigate('/marketplace');
              }
            }}
          >
            Search
          </button>
        </div>
      </div>

      {/* ── Vertical shortcuts ── */}
      <div className="shelf" style={{ marginBottom: 'var(--space-4)' }}>
        {verticals.map((v) => (
          <Link
            key={v.productType}
            to={`/marketplace?type=${v.productType}`}
            className="shelf-card"
          >
            <div className="ico">{v.vertical.includes('TRAIN') ? '🚆' : v.vertical.includes('BUS') ? '🚌' : v.vertical.includes('FLIGHT') ? '✈️' : v.vertical.includes('MOVIE') ? '🎬' : v.vertical.includes('EVENT') ? '🎟️' : '🎫'}</div>
            <strong style={{ fontSize: '0.85rem' }}>{v.vertical}</strong>
            <div className="count">{v.count} live</div>
          </Link>
        ))}
        <Link to="/movies" className="shelf-card">
          <div className="ico">🎬</div>
          <strong style={{ fontSize: '0.85rem' }}>Movies</strong>
          <div className="count">Browse</div>
        </Link>
        <Link to="/marketplace" className="shelf-card">
          <div className="ico">🌐</div>
          <strong style={{ fontSize: '0.85rem' }}>All</strong>
          <div className="count">Browse all</div>
        </Link>
      </div>

      {/* ── Stats row ── */}
      <div className="stats">
        <StatCard label="Total orders" value={orders.length} icon="🧾" />
        <StatCard label="Active" value={paidOrders.length} icon="✅" variant="success" />
        <StatCard label="Pending" value={pendingOrders.length} icon="⏳" variant="warning" />
        <StatCard
          label="Loyalty points"
          value={loyalty ? loyalty.points.toLocaleString() : '—'}
          icon="⭐"
          variant="violet"
        />
      </div>

      {/* ── Quick actions ── */}
      <div className="dashboard-quick-actions">
        <Link to="/marketplace" className="quick-action">
          <span className="quick-action-icon">🚌</span>
          <span className="quick-action-label">Browse tickets</span>
          <span className="quick-action-sub">Bus, train, flight, ferry</span>
        </Link>
        <Link to="/movies" className="quick-action">
          <span className="quick-action-icon">🎬</span>
          <span className="quick-action-label">Movies</span>
          <span className="quick-action-sub">Now showing, premieres</span>
        </Link>
        <Link to="/orders" className="quick-action">
          <span className="quick-action-icon">🧾</span>
          <span className="quick-action-label">My orders</span>
          <span className="quick-action-sub">{pendingOrders.length} pending</span>
        </Link>
        <Link to="/tickets" className="quick-action">
          <span className="quick-action-icon">🎟️</span>
          <span className="quick-action-label">My tickets</span>
          <span className="quick-action-sub">{paidOrders.length} active</span>
        </Link>
      </div>

      {/* ── Main two-column layout ── */}
      <div className="dashboard-grid">
        {/* ── Upcoming trips ── */}
        <div className="dashboard-col">
          <SectionTitle
            title="🧳 Upcoming trips"
            right={<Link to="/orders" className="muted">All orders →</Link>}
          />
          {loading ? (
            <div className="card skeleton" style={{ height: 120 }} />
          ) : upcomingTrips.length === 0 ? (
            <EmptyState
              icon="✈️"
              title="No trips yet"
              description="Book your first ticket to see it here."
              action={
                <Link to="/marketplace" className="btn primary">Browse marketplace</Link>
              }
            />
          ) : (
            <div className="dashboard-trip-list">
              {upcomingTrips.map((o) => (
                <Link
                  to={`/ticket/${o.orderRef}`}
                  key={o.id}
                  className="dashboard-trip"
                >
                  <div className="dashboard-trip-icon">{statusIcon(o.status)}</div>
                  <div className="dashboard-trip-body">
                    <div className="dashboard-trip-title">{o.productTitle}</div>
                    <div className="dashboard-trip-meta muted fs-xs">
                      {o.providerName} · {o.productType} · {o.quantity} ticket{o.quantity === 1 ? '' : 's'}
                    </div>
                    <div className="muted fs-xs">
                      Booked {new Date(o.createdAt).toLocaleDateString()}
                    </div>
                  </div>
                  <div className="dashboard-trip-right">
                    <Currency amount={o.totalAmount} currency={o.currencyIso} className="text-primary fw-700" />
                    <span className={`badge ${statusVariant(o.status)}`}>{o.status}</span>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>

        {/* ── Right column: offers + loyalty + notifications ── */}
        <div className="dashboard-col">
          {/* Loyalty card */}
          {loyalty && (
            <div className="card dashboard-loyalty">
              <div className="dashboard-loyalty-head">
                <div>
                  <div className="muted fs-xs">Loyalty tier</div>
                  <div className="dashboard-loyalty-tier">{loyalty.tier}</div>
                </div>
                <div className="dashboard-loyalty-points">
                  <div className="num">{loyalty.points.toLocaleString()}</div>
                  <div className="lab">points</div>
                </div>
              </div>
              <div className="muted fs-xs">
                Lifetime: {loyalty.lifetimePoints.toLocaleString()} points ·{' '}
                <Link to="/loyalty">View rewards →</Link>
              </div>
            </div>
          )}

          {/* Spending summary */}
          <div className="card dashboard-spend">
            <div className="muted fs-xs">Total spend (paid orders)</div>
            <div className="dashboard-spend-amount">
              <Currency amount={totalSpent} currency={lifetimeSpendCurrency} className="fw-800 text-primary" />
            </div>
            <div className="muted fs-xs">{paidOrders.length} booking{paidOrders.length === 1 ? '' : 's'}</div>
          </div>

          {/* Notifications */}
          {notifications.length > 0 && (
            <div className="dashboard-notifs">
              <div className="muted fs-xs" style={{ marginBottom: 6, fontWeight: 600 }}>📬 Recent notifications</div>
              {notifications.slice(0, 3).map((n) => (
                <div key={n.id} className="dashboard-notif">
                  <div className="dashboard-notif-subj">
                    {n.subject || (n.channel === 'EMAIL' ? 'Email' : 'Notification')}
                  </div>
                  <div className="muted fs-xs">{n.body}</div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* ── Offers strip ── */}
      {offers.length > 0 && (
        <>
          <SectionTitle
            title="🔥 Hot offers for you"
            right={<Link to="/marketplace" className="muted">See all →</Link>}
          />
          <div className="dashboard-offers">
            {offers.map((o) => (
              <Link
                to={`/product/${o.id}`}
                key={o.id}
                className="dashboard-offer"
              >
                {o.dealTag && <div className="dashboard-offer-tag">{o.dealTag}</div>}
                <div className="dashboard-offer-body">
                  <div className="dashboard-offer-title">{o.title}</div>
                  <div className="muted fs-xs">{o.providerName} · {o.productType}</div>
                  <div className="dashboard-offer-prices">
                    <Currency amount={o.currentPrice} currency={o.currencyIso} className="text-primary fw-800" />
                    {o.originalPrice != null && o.originalPrice > o.currentPrice && (
                      <span className="dashboard-offer-original">
                        <Currency amount={o.originalPrice} currency={o.currencyIso} />
                      </span>
                    )}
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </>
      )}

      {/* ── Pending orders banner ── */}
      {pendingOrders.length > 0 && (
        <Alert
          kind="warning"
          title={`You have ${pendingOrders.length} pending order${pendingOrders.length === 1 ? '' : 's'}`}
        >
          <div style={{ marginTop: 6 }}>
            Complete payment to receive your tickets.{' '}
            <Link to="/orders" className="btn sm">Pay now →</Link>
          </div>
        </Alert>
      )}
    </section>
  );
}
