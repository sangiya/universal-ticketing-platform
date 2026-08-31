import { useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link, useNavigate } from 'react-router-dom';

interface Offer {
  id: number;
  title: string;
  providerName: string;
  providerCode?: string;
  price: string | number;
  currency: string;
  currencyIso?: string;
  productType?: string;
  themeColor?: string | null;
  origin?: string | null;
  destination?: string | null;
}

const HERO_TABS = ['All', 'Buses', 'Trains', 'Movies', 'Events', 'Flights', 'Attractions'];

const FEATURES = [
  { icon: '🎟️', title: 'One marketplace', text: 'Every bus, train, show, event and flight from connected providers — in a single search.' },
  { icon: '💳', title: 'Fast checkout', text: 'Preview an itemized price, apply a promo code, and pay securely with a card.' },
  { icon: '🏷️', title: 'Your currency', text: 'Prices shown in your local currency with transparent taxes and fees.' },
  { icon: '⭐', title: 'Loyalty rewards', text: 'Earn points on every booking and climb tiers as you travel more.' },
];

const TESTIMONIALS = [
  { name: 'Amaya Silva', role: 'Frequent traveller', quote: 'Everything I need in one place — I booked a rail pass, a concert and a city tour without leaving the app.' },
  { name: 'Ravindu Perera', role: 'Event organiser', quote: 'TicketMesh lets me brand my shop, price with my own currency and start selling the same day.' },
  { name: 'Dinesh Fernando', role: 'Tour operator', quote: 'The seller portal is genuinely production-grade. My providers, products and branding all sit in one dashboard.' },
];

export default function HomePage() {
  const { api, authenticated } = useApi();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [activeTab, setActiveTab] = useState('All');
  const [offers, setOffers] = useState<Offer[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [stats, setStats] = useState<{liveOffers: number, verticals: number, portals: number, availability: string} | null>(null);

  const search = async (q = query) => {
    setLoading(true);
    setError(null);
    try {
      const endpoint = `/catalog/search${q ? `?q=${encodeURIComponent(q)}` : ''}`;
      const data = await api.get<unknown[]>(endpoint);
      setOffers((data as unknown as Offer[]) ?? []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Search failed');
      setOffers([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void search('');
    api.get<any>('/public/stats').then(setStats).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const filtered =
    activeTab === 'All'
      ? offers
      : offers.filter((o) => (o.productType ?? '').toLowerCase().includes(activeTab.toLowerCase().slice(0, -1)));

  const book = () => {
    if (!authenticated) {
      navigate('/login');
    } else {
      navigate('/marketplace');
    }
  };

  const coverColor = (o: Offer) =>
    o.themeColor || '#4f46e5';

  return (
    <section className="page">
      <div className="hero">
        <span className="eyebrow">✦ Universal ticketing & marketplace</span>
        <h1>Find and book everything, anywhere.</h1>
        <p className="lead">
          Buses, trains, movies, events, flights and attractions — from every connected
          provider and shop, in one platform. One search, your currency, instant checkout.
        </p>

        <div className="hero-search">
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search buses, trains, movies, events, flights…"
          />
          <button
            className="btn"
            onClick={() => void search()}
          >
            Search
          </button>
        </div>

        <div className="hero-tabs">
          {HERO_TABS.map((t) => (
            <button
              key={t}
              className={`hero-tab ${activeTab === t ? 'active' : ''}`}
              onClick={() => setActiveTab(t)}
            >
              {t}
            </button>
          ))}
        </div>

        <div className="hero-stats">
          <div className="hero-stat"><span className="num">{stats ? stats.liveOffers : '...'}</span> <span className="lab">live offers</span></div>
          <div className="hero-stat"><span className="num">{stats ? stats.verticals : '...'}</span> <span className="lab">verticals</span></div>
          <div className="hero-stat"><span className="num">{stats ? stats.portals : '...'}</span> <span className="lab">portals</span></div>
          <div className="hero-stat"><span className="num">{stats ? stats.availability : '...'}</span> <span className="lab">availability</span></div>
        </div>
      </div>

      {error && <p className="error">{error}</p>}
      {loading && <p className="muted">Searching the marketplace…</p>}

      <div className="grid">
        {filtered.map((o) => (
          <article className="card product-card" key={o.id}>
            <div
              className="product-cover"
              style={{ background: `linear-gradient(135deg, ${coverColor(o)}, #7c3aed)` }}
            >
              <div className="cover-tint" />
              <span className="product-type">{o.productType ?? o.providerCode ?? 'Offer'}</span>
              <span className="price-badge">
                {(o.currency ?? (o as unknown as { currencyIso: string }).currencyIso ?? 'LKR')} {o.price}
              </span>
            </div>
            <div className="product-body">
              <h3>{o.title}</h3>
              <p className="muted" style={{ fontSize: '0.85rem' }}>
                by <strong style={{ color: coverColor(o) }}>{o.providerName}</strong>
                {o.origin && o.destination ? ` · ${o.origin} → ${o.destination}` : ''}
              </p>
              <button className="btn primary" onClick={() => book()}>
                {authenticated ? 'Book now' : 'Sign in to book'}
              </button>
            </div>
          </article>
        ))}
        {!loading && filtered.length === 0 && (
          <p className="muted">No results. Try a different search.</p>
        )}
      </div>

      <h2 className="section-title">Why TicketMesh</h2>
      <div className="features">
        {FEATURES.map((f) => (
          <div className="feature" key={f.title}>
            <div className="ico">{f.icon}</div>
            <h4>{f.title}</h4>
            <p>{f.text}</p>
          </div>
        ))}
      </div>

      <h2 className="section-title">Travellers & organisers love it</h2>
      <div className="testimonials">
        {TESTIMONIALS.map((t) => (
          <div className="testimonial" key={t.name}>
            <p className="quote">“{t.quote}”</p>
            <div className="who">
              <div className="avatar">{t.name.charAt(0)}</div>
              <div>
                <strong>{t.name}</strong>
                <span>{t.role}</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="banner">
        <h2>Want to sell your services?</h2>
        <p className="muted">
          Agents and shops connect via the app, upload their services and start selling.
          Your shop, your theme, your brand.
        </p>
        <Link className="btn primary" to="/register">
          Become an agent / start a shop
        </Link>
      </div>
    </section>
  );
}
