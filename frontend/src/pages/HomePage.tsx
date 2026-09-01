import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link, useNavigate } from 'react-router-dom';
import { Currency, EmptyState, SectionTitle } from '../components/UI';

interface Offer {
  providerCode: string;
  offerId: string;
  title: string;
  origin: string | null;
  destination: string | null;
  price: number;
  currencyIso: string;
  availableSeats: number;
  vertical: string;
  epochDepartureMillis: number;
}

interface VerticalCount {
  vertical: string;
  productType: string;
  count: number;
}

interface Suggestion {
  text: string;
  type: 'origin' | 'destination' | 'route' | 'product';
  meta?: string;
}

const DEFAULT_FROM = '';
const DEFAULT_TO = '';

type TabKey = 'All' | 'Bus' | 'Train' | 'Flights' | 'Movies' | 'Events' | 'More';

interface HeroTab {
  key: TabKey;
  icon: string;
  productType?: string;
}

const HERO_TABS: HeroTab[] = [
  { key: 'All', icon: '✨' },
  { key: 'Bus', icon: '🚌', productType: 'ROUTE' },
  { key: 'Train', icon: '🚆', productType: 'ROUTE' },
  { key: 'Flights', icon: '✈️', productType: 'ROUTE' },
  { key: 'Movies', icon: '🎬', productType: 'ADMISSION' },
  { key: 'Events', icon: '🎟️', productType: 'ADMISSION' },
  { key: 'More', icon: '⋯' },
];

const POPULAR: { icon: string; label: string; sub: string; productType: string; accent: string }[] = [
  { icon: '🚌', label: 'Bus Tickets', sub: 'Intercity & local', productType: 'ROUTE', accent: 'linear-gradient(135deg, #06b6d4 0%, #3b82f6 100%)' },
  { icon: '🚆', label: 'Train Tickets', sub: 'High-speed rail', productType: 'ROUTE', accent: 'linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%)' },
  { icon: '✈️', label: 'Flight Tickets', sub: 'Domestic & international', productType: 'ROUTE', accent: 'linear-gradient(135deg, #0ea5e9 0%, #6366f1 100%)' },
  { icon: '🎬', label: 'Movie Tickets', sub: 'Cinema & IMAX', productType: 'ADMISSION', accent: 'linear-gradient(135deg, #f59e0b 0%, #ef4444 100%)' },
  { icon: '🎟️', label: 'Event Tickets', sub: 'Concerts & festivals', productType: 'ADMISSION', accent: 'linear-gradient(135deg, #ec4899 0%, #8b5cf6 100%)' },
  { icon: '🏖️', label: 'Attractions', sub: 'Tours & activities', productType: 'ADMISSION', accent: 'linear-gradient(135deg, #10b981 0%, #14b8a6 100%)' },
];

const FEATURES = [
  { icon: '🌍', title: 'Global & local', desc: 'Multi-currency, multi-language, multi-tenant' },
  { icon: '🛒', title: 'Marketplace model', desc: 'Uber/PickMe-style — agents own their shops' },
  { icon: '🤖', title: 'AI search', desc: 'Ask in natural language, get the best matches' },
  { icon: '🛡️', title: 'Secure payments', desc: 'PCI-aware flows, fraud signals, held inventory' },
  { icon: '📱', title: 'PWA + mobile', desc: 'Installable, offline-ready, bottom-nav UX' },
  { icon: '⚡', title: 'Circuit-broken', desc: 'Resilient to flaky providers, always available' },
];

function verticalIcon(vertical: string): string {
  const v = (vertical || '').toUpperCase();
  if (v.includes('TRAIN')) return '🚆';
  if (v.includes('BUS')) return '🚌';
  if (v.includes('FLIGHT')) return '✈️';
  if (v.includes('FERRY')) return '⛴️';
  if (v.includes('MOVIE')) return '🎬';
  if (v.includes('EVENT')) return '🎟️';
  if (v.includes('SPORTS')) return '⚽';
  if (v.includes('ATTRACTION')) return '🏖️';
  if (v.includes('PACKAGE')) return '📦';
  if (v.includes('SERVICE')) return '🛎️';
  if (v === 'ROUTE') return '🚆';
  if (v === 'SEAT') return '💺';
  if (v === 'TICKET') return '🎫';
  return '🎫';
}

const STORAGE_KEY = 'tm_recent';

export default function HomePage() {
  const { api, authenticated } = useApi();
  const navigate = useNavigate();

  // Search state
  const [query, setQuery] = useState('');
  const [from, setFrom] = useState(DEFAULT_FROM);
  const [to, setTo] = useState(DEFAULT_TO);
  const [date, setDate] = useState('');
  const [passengers, setPassengers] = useState(1);

  // Catalog state — single source of truth
  const [activeTab, setActiveTab] = useState<TabKey>('All');
  const [allOffers, setAllOffers] = useState<Offer[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [verticals, setVerticals] = useState<VerticalCount[]>([]);
  const [verticalsLoaded, setVerticalsLoaded] = useState(false);
  const [catalogLoaded, setCatalogLoaded] = useState(false);

  // Auto-suggest state
  const [fromSuggestions, setFromSuggestions] = useState<Suggestion[]>([]);
  const [toSuggestions, setToSuggestions] = useState<Suggestion[]>([]);
  const [showFromSugg, setShowFromSugg] = useState(false);
  const [showToSugg, setShowToSugg] = useState(false);
  const fromDebounce = useRef<ReturnType<typeof setTimeout> | null>(null);
  const toDebounce = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Recent searches
  const [recent, setRecent] = useState<string[]>(() => {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
    } catch {
      return [];
    }
  });

  // AI search state
  const [aiAnswer, setAiAnswer] = useState<string | null>(null);
  const [aiLoading, setAiLoading] = useState(false);

  /**
   * Load verticals + full catalog ONCE on mount. After that, all tab
   * filtering is client-side — no repeated API calls.
   */
  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setLoading(true);
      try {
        const [vertData, searchData] = await Promise.all([
          api.get<VerticalCount[]>('/search/verticals'),
          api.get<Offer[]>('/search'),
        ]);
        if (cancelled) return;
        setVerticals((vertData as unknown as VerticalCount[]) ?? []);
        setVerticalsLoaded(true);
        setAllOffers((searchData as unknown as Offer[]) ?? []);
        setCatalogLoaded(true);
        setError(null);
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Failed to load marketplace');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    void load();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Redirect authenticated users to their personalized dashboard
  useEffect(() => {
    if (authenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [authenticated, navigate]);

  // Auto-suggest with 3-letter debounce
  useEffect(() => {
    if (fromDebounce.current) clearTimeout(fromDebounce.current);
    if (!from || from.length < 3) {
      setFromSuggestions([]);
      return;
    }
    fromDebounce.current = setTimeout(async () => {
      try {
        const data = await api.get<{ offers: Offer[] } | Offer[]>(
          `/search?q=${encodeURIComponent(from)}`,
        );
        const offers = Array.isArray(data) ? data : (data as { offers: Offer[] }).offers ?? [];
        const set = new Set<string>();
        const sugg: Suggestion[] = [];
        for (const o of offers) {
          if (o.origin && o.origin.toLowerCase().includes(from.toLowerCase())) {
            if (!set.has(o.origin)) {
              set.add(o.origin);
              sugg.push({ text: o.origin, type: 'origin' });
            }
          }
          if (o.title && o.title.toLowerCase().includes(from.toLowerCase())) {
            if (!set.has(o.title)) {
              set.add(o.title);
              sugg.push({ text: o.title, type: 'product', meta: o.providerCode });
            }
          }
        }
        setFromSuggestions(sugg.slice(0, 6));
      } catch {
        setFromSuggestions([]);
      }
    }, 250);
    return () => {
      if (fromDebounce.current) clearTimeout(fromDebounce.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [from]);

  useEffect(() => {
    if (toDebounce.current) clearTimeout(toDebounce.current);
    if (!to || to.length < 3) {
      setToSuggestions([]);
      return;
    }
    toDebounce.current = setTimeout(async () => {
      try {
        const data = await api.get<{ offers: Offer[] } | Offer[]>(
          `/search?q=${encodeURIComponent(to)}`,
        );
        const offers = Array.isArray(data) ? data : (data as { offers: Offer[] }).offers ?? [];
        const set = new Set<string>();
        const sugg: Suggestion[] = [];
        for (const o of offers) {
          if (o.destination && o.destination.toLowerCase().includes(to.toLowerCase())) {
            if (!set.has(o.destination)) {
              set.add(o.destination);
              sugg.push({ text: o.destination, type: 'destination' });
            }
          }
        }
        setToSuggestions(sugg.slice(0, 6));
      } catch {
        setToSuggestions([]);
      }
    }, 250);
    return () => {
      if (toDebounce.current) clearTimeout(toDebounce.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [to]);

  /**
   * Tab switching is now 100% client-side — uses the cached `allOffers`.
   * No repeated API calls when switching tabs.
   */
  const handleTabClick = useCallback((tab: TabKey) => {
    setActiveTab(tab);
  }, []);

  const saveRecent = useCallback((text: string) => {
    if (!text.trim()) return;
    const next = [text, ...recent.filter((x) => x !== text)].slice(0, 6);
    setRecent(next);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  }, [recent]);

  const runSearch = useCallback(() => {
    const parts = [from, to, activeTab !== 'All' ? activeTab : ''].filter(Boolean);
    const q = parts.join(' ').trim();
    if (q) saveRecent(q);
  }, [from, to, activeTab, saveRecent]);

  // Client-side filtering — instant tab switching
  const filtered = useMemo(() => {
    if (activeTab === 'All' || activeTab === 'More') return allOffers;
    const tab = HERO_TABS.find((t) => t.key === activeTab);
    if (!tab?.productType) return allOffers;
    return allOffers.filter((o) => (o.vertical || '').toUpperCase() === tab.productType);
  }, [allOffers, activeTab]);

  // Free-text search across currently filtered offers
  const visible = useMemo(() => {
    if (!query.trim()) return filtered;
    const lq = query.toLowerCase();
    return filtered.filter((o) => {
      return (
        o.title.toLowerCase().includes(lq) ||
        (o.origin ?? '').toLowerCase().includes(lq) ||
        (o.destination ?? '').toLowerCase().includes(lq) ||
        o.providerCode.toLowerCase().includes(lq)
      );
    });
  }, [filtered, query]);

  // Ask AI — uses the analytics endpoint that actually works
  const handleAI = useCallback(async () => {
    if (!query.trim()) return;
    setAiLoading(true);
    setAiAnswer(null);
    try {
      const res = await api.get<{ offers: Offer[] }>(`/ai/search/offers?q=${encodeURIComponent(query)}`);
      const offers = res?.offers ?? [];
      if (offers.length) {
        setAllOffers(offers);
        setAiAnswer(`Found ${offers.length} matching offer${offers.length === 1 ? '' : 's'} for "${query}".`);
      } else {
        setAiAnswer(`No AI offers for "${query}". Try a different query or browse categories.`);
      }
      saveRecent(query);
    } catch (e) {
      setAiAnswer(e instanceof Error ? e.message : 'AI search failed');
    } finally {
      setAiLoading(false);
    }
  }, [query, api, saveRecent]);

  const handleConsoleSearch = useCallback(() => {
    const q = [from, to].filter(Boolean).join(' ').trim();
    if (q) {
      setQuery(q);
      saveRecent(q);
    }
    runSearch();
  }, [from, to, saveRecent, runSearch]);

  const today = new Date().toISOString().split('T')[0];
  const totalLiveOffers = verticals.reduce((sum, v) => sum + v.count, 0);

  return (
    <section className="page">
      <div className="hero">
        <span className="eyebrow">✨ One platform · All ticket services · Anywhere</span>
        <h1>
          Book any ticket,<br />
          anywhere in the world
        </h1>
        <p className="lead">
          Bus, train, flight, movie, events, attractions. Pay in your currency.
          Search in your language.
        </p>

        <div className="search-console" role="search">
          <div className="field" style={{ position: 'relative' }}>
            <label htmlFor="from">📍 From</label>
            <input
              id="from"
              value={from}
              onChange={(e) => {
                setFrom(e.target.value);
                setShowFromSugg(true);
              }}
              onFocus={() => setShowFromSugg(true)}
              onBlur={() => setTimeout(() => setShowFromSugg(false), 200)}
              placeholder="City or station (3+ letters for suggestions)"
              autoComplete="off"
            />
            {showFromSugg && fromSuggestions.length > 0 && (
              <ul
                className="autocomplete-list"
                role="listbox"
                style={{
                  position: 'absolute',
                  top: '100%',
                  left: 0,
                  right: 0,
                  background: 'var(--surface-1, #fff)',
                  border: '1px solid var(--surface-3, #e5e7eb)',
                  borderRadius: 8,
                  boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
                  listStyle: 'none',
                  margin: '4px 0 0',
                  padding: 4,
                  zIndex: 50,
                  maxHeight: 240,
                  overflowY: 'auto',
                }}
              >
                {fromSuggestions.map((s) => (
                  <li
                    key={s.text}
                    role="option"
                    aria-selected="false"
                    onMouseDown={() => {
                      setFrom(s.text);
                      setShowFromSugg(false);
                    }}
                    style={{
                      padding: '8px 10px',
                      cursor: 'pointer',
                      borderRadius: 6,
                      fontSize: '0.9rem',
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.background = 'var(--surface-2, #f3f4f6)')}
                    onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
                  >
                    <span style={{ marginRight: 6 }}>📍</span>
                    {s.text}
                    {s.meta && <span className="muted fs-xs" style={{ marginLeft: 6 }}>· {s.meta}</span>}
                  </li>
                ))}
              </ul>
            )}
          </div>
          <div className="field" style={{ position: 'relative' }}>
            <label htmlFor="to">🏁 To</label>
            <input
              id="to"
              value={to}
              onChange={(e) => {
                setTo(e.target.value);
                setShowToSugg(true);
              }}
              onFocus={() => setShowToSugg(true)}
              onBlur={() => setTimeout(() => setShowToSugg(false), 200)}
              placeholder="City or station (3+ letters for suggestions)"
              autoComplete="off"
            />
            {showToSugg && toSuggestions.length > 0 && (
              <ul
                className="autocomplete-list"
                role="listbox"
                style={{
                  position: 'absolute',
                  top: '100%',
                  left: 0,
                  right: 0,
                  background: 'var(--surface-1, #fff)',
                  border: '1px solid var(--surface-3, #e5e7eb)',
                  borderRadius: 8,
                  boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
                  listStyle: 'none',
                  margin: '4px 0 0',
                  padding: 4,
                  zIndex: 50,
                  maxHeight: 240,
                  overflowY: 'auto',
                }}
              >
                {toSuggestions.map((s) => (
                  <li
                    key={s.text}
                    role="option"
                    aria-selected="false"
                    onMouseDown={() => {
                      setTo(s.text);
                      setShowToSugg(false);
                    }}
                    style={{
                      padding: '8px 10px',
                      cursor: 'pointer',
                      borderRadius: 6,
                      fontSize: '0.9rem',
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.background = 'var(--surface-2, #f3f4f6)')}
                    onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
                  >
                    <span style={{ marginRight: 6 }}>🏁</span>
                    {s.text}
                  </li>
                ))}
              </ul>
            )}
          </div>
          <div className="field">
            <label htmlFor="date">📅 Date</label>
            <input
              id="date"
              type="date"
              value={date}
              min={today}
              onChange={(e) => setDate(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="passengers">👥 Passengers</label>
            <select
              id="passengers"
              value={passengers}
              onChange={(e) => setPassengers(parseInt(e.target.value) || 1)}
            >
              {[1, 2, 3, 4, 5, 6, 7, 8].map((n) => (
                <option key={n} value={n}>
                  {n} {n === 1 ? 'passenger' : 'passengers'}
                </option>
              ))}
            </select>
          </div>
          <button className="btn primary" onClick={handleConsoleSearch}>
            Search
          </button>
        </div>

        <div className="ai-bar">
          <span aria-hidden style={{ color: '#fff', opacity: 0.85 }}>🤖</span>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Try: 'Bus tickets from Sydney to Melbourne tomorrow for 2'"
            onKeyDown={(e) => e.key === 'Enter' && handleAI()}
            aria-label="Natural language search"
          />
          <button className="btn" onClick={handleAI} disabled={!query.trim() || aiLoading}>
            {aiLoading ? <span className="spinner" /> : 'Ask AI'}
          </button>
        </div>

        {aiAnswer && (
          <div
            className="card"
            style={{
              background: 'var(--gradient-soft, #eef2ff)',
              borderColor: 'transparent',
              marginTop: 'var(--space-3)',
              padding: 'var(--space-3)',
            }}
          >
            <strong>🤖 AI:</strong> {aiAnswer}
          </div>
        )}

        <div className="hero-tabs" role="tablist" aria-label="Browse by category">
          {HERO_TABS.map((t) => {
            const count = t.productType
              ? verticals.find((v) => v.productType === t.productType)?.count ?? 0
              : totalLiveOffers;
            return (
              <button
                key={t.key}
                role="tab"
                aria-selected={activeTab === t.key}
                className={`hero-tab ${activeTab === t.key ? 'active' : ''}`}
                onClick={() => handleTabClick(t.key)}
              >
                <span style={{ marginRight: 4 }}>{t.icon}</span>
                {t.key}
                {t.key !== 'All' && t.key !== 'More' && count > 0 && (
                  <span className="hero-tab-count">{count}</span>
                )}
              </button>
            );
          })}
        </div>

        <div className="trust-strip">
          <span>✦ Best price guarantee</span>
          <span>•</span>
          <span>24/7 customer support</span>
          <span>•</span>
          <span>Secure payments</span>
          <span>•</span>
          <span>Instant confirmation</span>
        </div>

        <div className="hero-stats">
          <div className="hero-stat">
            <span className="num">{verticalsLoaded ? totalLiveOffers : '—'}</span>
            <span className="lab">Live offers</span>
          </div>
          <div className="hero-stat">
            <span className="num">{verticalsLoaded ? verticals.length : '—'}</span>
            <span className="lab">Verticals</span>
          </div>
          <div className="hero-stat">
            <span className="num">{catalogLoaded ? allOffers.length : '—'}</span>
            <span className="lab">Tickets</span>
          </div>
          <div className="hero-stat">
            <span className="num">99.9%</span>
            <span className="lab">Uptime</span>
          </div>
        </div>

        {/* ── Results preview: appears right after tabs, inside the hero ── */}
        {!loading && (visible.length > 0 || error) && (
          <div style={{ marginTop: 'var(--space-4)' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 'var(--space-2)' }}>
              <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 700 }}>
                {query ? `Results for "${query}"` : activeTab === 'All' ? 'All offers' : `${activeTab} offers`}
                <span style={{ fontWeight: 400, color: 'var(--muted)', marginLeft: 8, fontSize: '0.85rem' }}>
                  {visible.length} found
                </span>
              </h3>
              {visible.length > 4 && (
                <button
                  className="btn sm"
                  onClick={() => document.getElementById('all-results')?.scrollIntoView({ behavior: 'smooth' })}
                >
                  View all →
                </button>
              )}
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 'var(--space-2)', marginBottom: 'var(--space-2)' }}>
              {(visible.length > 0 ? visible.slice(0, 4) : []).map((o) => (
                <div
                  key={`${o.providerCode}-${o.offerId}`}
                  style={{
                    background: 'rgba(255,255,255,0.08)',
                    border: '1px solid rgba(255,255,255,0.15)',
                    borderRadius: 'var(--radius)',
                    padding: 'var(--space-3)',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 'var(--space-1)',
                    cursor: 'pointer',
                    transition: 'all 0.15s',
                  }}
                  onClick={() => (authenticated ? navigate('/marketplace') : navigate('/login'))}
                  onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(255,255,255,0.14)')}
                  onMouseLeave={(e) => (e.currentTarget.style.background = 'rgba(255,255,255,0.08)')}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => e.key === 'Enter' && (authenticated ? navigate('/marketplace') : navigate('/login'))}
                >
                  <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <span style={{ fontSize: '1.1rem' }}>{verticalIcon(o.vertical)}</span>
                      <span style={{ fontSize: '0.72rem', color: 'rgba(255,255,255,0.6)', background: 'rgba(255,255,255,0.1)', padding: '1px 6px', borderRadius: 4 }}>
                        {o.vertical}
                      </span>
                    </div>
                    <div style={{ fontWeight: 800, fontSize: '1.1rem', color: '#fff' }}>
                      <Currency amount={o.price} currency={o.currencyIso} />
                    </div>
                  </div>
                  <div style={{ fontWeight: 600, fontSize: '0.9rem', color: '#fff', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {o.title}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'rgba(255,255,255,0.6)' }}>
                    {o.origin && o.destination ? `${o.origin} → ${o.destination}` : o.providerCode}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 'auto', paddingTop: 'var(--space-1)' }}>
                    <span style={{ fontSize: '0.75rem', color: 'rgba(255,255,255,0.5)' }}>🎫 {o.availableSeats} left</span>
                    <button className="btn sm primary" style={{ padding: '3px 10px', fontSize: '0.75rem' }}>
                      Book
                    </button>
                  </div>
                </div>
              ))}
            </div>
            {error && (
              <div className="card" style={{ borderLeft: '3px solid var(--danger)', fontSize: '0.85rem' }}>
                <strong>Could not load offers</strong>
                <p className="muted" style={{ margin: '0.25rem 0 0' }}>{error}</p>
              </div>
            )}
            {!error && visible.length === 0 && (
              <div className="card" style={{ textAlign: 'center', color: 'rgba(255,255,255,0.5)', fontSize: '0.85rem', padding: 'var(--space-4)' }}>
                No results for this filter — try a different category above.
              </div>
            )}
          </div>
        )}
        {loading && (
          <div style={{ marginTop: 'var(--space-4)', display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 'var(--space-2)' }}>
            {[0,1,2,3].map(i => (
              <div key={i} style={{ height: 110, borderRadius: 'var(--radius)', background: 'rgba(255,255,255,0.07)' }} />
            ))}
          </div>
        )}
      </div>

      {recent.length > 0 && (
        <div
          className="card"
          style={{ marginBottom: 'var(--space-4)', display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}
        >
          <span className="muted fs-sm" style={{ fontWeight: 600 }}>Recent searches:</span>
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', flex: 1 }}>
            {recent.map((r) => (
              <span
                key={r}
                className="tag"
                style={{ cursor: 'pointer' }}
                role="button"
                tabIndex={0}
                onClick={() => {
                  setQuery(r);
                  runSearch();
                }}
                onKeyDown={(e) => e.key === 'Enter' && (setQuery(r), runSearch())}
              >
                {r}
              </span>
            ))}
          </div>
          <button
            className="link fs-sm"
            onClick={() => {
              setRecent([]);
              localStorage.removeItem(STORAGE_KEY);
            }}
          >
            Clear
          </button>
        </div>
      )}

      {/* ── Browse by category (quick-select chips) ── */}
      <SectionTitle
        title="Browse by category"
        right={<Link to="/marketplace" className="muted">View all →</Link>}
      />
      <div className="shelf">
        {POPULAR.map((p) => {
          const live = verticals.find((v) => v.productType === p.productType)?.count ?? 0;
          return (
            <div
              key={p.label}
              className="shelf-card"
              onClick={() => {
                setActiveTab(p.label.includes('Bus') ? 'Bus' : p.label.includes('Train') ? 'Train' : p.label.includes('Flight') ? 'Flights' : p.label.includes('Movie') ? 'Movies' : 'Events');
                setQuery('');
              }}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  setActiveTab(p.label.includes('Bus') ? 'Bus' : p.label.includes('Train') ? 'Train' : p.label.includes('Flight') ? 'Flights' : p.label.includes('Movie') ? 'Movies' : 'Events');
                  setQuery('');
                }
              }}
            >
              <div className="ico" style={{ background: p.accent, color: '#fff' }}>{p.icon}</div>
              <strong style={{ fontSize: '0.95rem' }}>{p.label}</strong>
              <div className="muted" style={{ fontSize: '0.78rem', marginTop: 2 }}>{p.sub}</div>
              <div className="count">{live} live</div>
            </div>
          );
        })}
      </div>

      {/* ── Full results — professional ticket list ── */}
      <div id="all-results" className="results-header" style={{ marginTop: 'var(--space-5)' }}>
        <h2 className="section-title" style={{ margin: 0 }}>
          {query ? `Results for "${query}"` : activeTab === 'All' ? 'All offers' : `${activeTab} offers`}
        </h2>
        <div className="results-count">
          <strong>{visible.length}</strong> result{visible.length === 1 ? '' : 's'}
        </div>
      </div>

      {error && (
        <div className="card" style={{ borderLeft: '4px solid var(--danger)' }}>
          <strong>Could not load offers</strong>
          <p className="muted fs-sm" style={{ margin: '0.3rem 0 0' }}>{error}</p>
        </div>
      )}
      {loading && (
        <div className="ticket-list">
          {[0,1,2,3].map(i => (
            <div key={i} className="card skeleton-card" style={{ height: 96 }} />
          ))}
        </div>
      )}
      {!loading && visible.length === 0 && !error && (
        <EmptyState
          icon="🔍"
          title="No offers match your search"
          description="Try a different query, clear filters, or browse by category above."
        />
      )}
      {!loading && visible.length > 0 && (
        <div className="ticket-list">
          {visible.map((o, idx) => {
            const deptTime = o.epochDepartureMillis
              ? new Date(o.epochDepartureMillis).toLocaleString(undefined, {
                  dateStyle: 'medium',
                  timeStyle: 'short',
                })
              : null;
            return (
              <div key={`${o.providerCode}-${o.offerId}`} className="ticket-row">
                <div className="ticket-left">
                  <div className="ticket-icon">{verticalIcon(o.vertical)}</div>
                  <div className="ticket-info">
                    <div className="ticket-title">{o.title}</div>
                    <div className="ticket-route">
                      {o.origin && o.destination
                        ? <><span>{o.origin}</span><span className="arrow">→</span><span>{o.destination}</span></>
                        : <span className="muted fs-xs">{o.providerCode}</span>}
                    </div>
                    {deptTime && (
                      <div className="ticket-time muted fs-xs">🕐 {deptTime}</div>
                    )}
                  </div>
                </div>
                <div className="ticket-right">
                  <div className="ticket-seats">
                    <span className="seat-count">{o.availableSeats}</span>
                    <span className="muted fs-xs">seats left</span>
                  </div>
                  <div className="ticket-price">
                    <Currency amount={o.price} currency={o.currencyIso} />
                  </div>
                  <button
                    className="btn primary sm"
                    onClick={() => (authenticated ? navigate('/marketplace') : navigate('/login'))}
                  >
                    Book
                  </button>
                </div>
                <span className="ticket-number">{idx + 1}</span>
              </div>
            );
          })}
        </div>
      )}

      <SectionTitle title="Why TicketMesh" />
      <div className="features">
        {FEATURES.map((f) => (
          <div key={f.title} className="feature">
            <div className="ico">{f.icon}</div>
            <h4>{f.title}</h4>
            <p>{f.desc}</p>
          </div>
        ))}
      </div>

      <div className="testimonials">
        <div className="testimonial">
          <p className="quote">
            Everything I need in one place — booked a rail pass, a concert and
            a city tour without leaving the app.
          </p>
          <div className="who">
            <div className="avatar">A</div>
            <div>
              <strong>Amaya Silva</strong>
              <span>Frequent traveller</span>
            </div>
          </div>
        </div>
        <div className="testimonial">
          <p className="quote">
            The seller portal is genuinely production-grade. My providers,
            products and branding all sit in one dashboard.
          </p>
          <div className="who">
            <div className="avatar">D</div>
            <div>
              <strong>Dinesh Fernando</strong>
              <span>Tour operator</span>
            </div>
          </div>
        </div>
        <div className="testimonial">
          <p className="quote">
            Multi-currency and multi-language out of the box. We launched in
            three countries in a single sprint.
          </p>
          <div className="who">
            <div className="avatar">P</div>
            <div>
              <strong>Priya Nair</strong>
              <span>Product lead</span>
            </div>
          </div>
        </div>
      </div>

      <div className="banner">
        <div>
          <h2>Want to sell on TicketMesh?</h2>
          <p>Agents and shops connect via the app, upload their services and start selling. Your shop, your theme, your brand.</p>
        </div>
        <Link className="btn primary" to="/register">
          Become an agent →
        </Link>
      </div>
    </section>
  );
}
