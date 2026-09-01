import { useCallback, useEffect, useMemo, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { useNavigate } from 'react-router-dom';
import { Alert, Currency, EmptyState, PageHeader, SectionTitle } from '../components/UI';

/**
 * Movie ticket browsing page — BookMyShow style.
 *
 *  - Hero banner with the active premiere / featured film
 *  - Section tabs:  Now Showing · Coming Soon · Premieres
 *  - Horizontal shelves by language
 *  - Filter row:  Language, Genre, Format
 *  - Movie cards: poster, title, rating, language, format, genre, price, "Book" button
 *  - Click on a card opens the standard marketplace buy modal
 */

interface Movie {
  id: number;
  title: string;
  productType: string;
  description: string | null;
  price: number;
  currencyIso: string;
  availableQuantity: number;
  eventDate: string | null;
  language: string | null;
  genre: string | null;
  format: string | null;
  durationMinutes: number | null;
  ratingStars: number | null;
  castList: string | null;
  director: string | null;
  releaseDate: string | null;
  posterUrl: string | null;
  bannerUrl: string | null;
  isPremiere: boolean;
  isNowShowing: boolean;
  tagline: string | null;
  providerName: string;
}

type Section = 'now_showing' | 'coming_soon' | 'premieres';

const FALLBACK_POSTER =
  'https://placehold.co/400x600/6366f1/ffffff?text=No+Poster&font=raleway';

function moviePoster(m: Movie): string {
  return m.posterUrl || FALLBACK_POSTER;
}

function formatDuration(min?: number | null): string {
  if (!min) return '—';
  const h = Math.floor(min / 60);
  const m = min % 60;
  if (h === 0) return `${m}m`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}m`;
}

function formatEventDate(d?: string | null): string {
  if (!d) return '';
  try {
    const dt = new Date(d);
    return dt.toLocaleDateString(undefined, {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      hour: 'numeric',
      minute: '2-digit',
    });
  } catch {
    return '';
  }
}

export default function MoviesPage() {
  const { api, authenticated, tenantId } = useApi();
  const navigate = useNavigate();
  const [movies, setMovies] = useState<Movie[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [section, setSection] = useState<Section>('now_showing');
  const [language, setLanguage] = useState<string>('');
  const [genre, setGenre] = useState<string>('');
  const [format, setFormat] = useState<string>('');
  const [search, setSearch] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (tenantId) params.set('tenantId', String(tenantId));
      if (language) params.set('language', language);
      if (genre) params.set('genre', genre);
      if (format) params.set('format', format);
      params.set('section', section);
      const data = await api.get<Movie[]>(`/movies?${params.toString()}`);
      setMovies((data as unknown as Movie[]) ?? []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load movies');
    } finally {
      setLoading(false);
    }
  }, [api, tenantId, language, genre, format, section]);

  useEffect(() => {
    void load();
  }, [load]);

  // Build filter option lists from the current full set
  const allLanguages = useMemo(() => {
    const set = new Set<string>();
    movies.forEach((m) => m.language && set.add(m.language));
    return Array.from(set).sort();
  }, [movies]);

  const allGenres = useMemo(() => {
    const set = new Set<string>();
    movies.forEach((m) => {
      m.genre?.split(',').forEach((g) => {
        const trimmed = g.trim();
        if (trimmed) set.add(trimmed);
      });
    });
    return Array.from(set).sort();
  }, [movies]);

  const allFormats = useMemo(() => {
    const set = new Set<string>();
    movies.forEach((m) => m.format && set.add(m.format));
    return Array.from(set).sort();
  }, [movies]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return movies;
    return movies.filter((m) => {
      return (
        m.title.toLowerCase().includes(q) ||
        (m.language || '').toLowerCase().includes(q) ||
        (m.genre || '').toLowerCase().includes(q) ||
        (m.director || '').toLowerCase().includes(q) ||
        (m.castList || '').toLowerCase().includes(q)
      );
    });
  }, [movies, search]);

  // Featured banner — picks a premiere or the first movie
  const featured = useMemo(
    () => filtered.find((m) => m.isPremiere) || filtered[0] || null,
    [filtered],
  );

  // Group movies by language for the horizontal shelves
  const byLanguage = useMemo(() => {
    const map = new Map<string, Movie[]>();
    filtered.forEach((m) => {
      const key = m.language || 'Other';
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(m);
    });
    return Array.from(map.entries());
  }, [filtered]);

  const handleBook = (m: Movie) => {
    if (!authenticated) {
      navigate('/login');
      return;
    }
    navigate(`/product/${m.id}`);
  };

  return (
    <section className="page">
      <div className="breadcrumb">
        {authenticated ? (
          <>
            <Link to="/dashboard">Dashboard</Link>
            <span className="sep">›</span>
            <span>Movies</span>
          </>
        ) : (
          <span>Movies</span>
        )}
      </div>

      <PageHeader
        title="🎬 Movies"
        subtitle="Now showing in your city · Pick a language, pick a show, book in seconds."
        badge={
          <span className="badge role-agent no-dot">
            {loading ? '…' : filtered.length} titles
          </span>
        }
      />

      {error && <Alert kind="danger" title="Could not load movies">{error}</Alert>}

      {/* ── Section tabs (Now Showing / Coming Soon / Premieres) ── */}
      <div className="movie-section-tabs" role="tablist">
        {([
          { key: 'now_showing', label: '🎟 Now Showing' },
          { key: 'coming_soon', label: '⏳ Coming Soon' },
          { key: 'premieres', label: '✨ Premieres' },
        ] as { key: Section; label: string }[]).map((t) => (
          <button
            key={t.key}
            role="tab"
            aria-selected={section === t.key}
            className={`movie-section-tab ${section === t.key ? 'active' : ''}`}
            onClick={() => setSection(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* ── Featured banner ── */}
      {!loading && featured && (
        <div
          className="movie-featured"
          style={
            featured.bannerUrl
              ? {
                  backgroundImage: `linear-gradient(180deg, rgba(15,23,42,0.55) 0%, rgba(15,23,42,0.85) 100%), url(${featured.bannerUrl})`,
                  backgroundSize: 'cover',
                  backgroundPosition: 'center',
                }
              : {
                  backgroundImage:
                    'linear-gradient(135deg, #6366f1 0%, #ec4899 100%)',
                }
          }
        >
          <div className="movie-featured-poster">
            <img src={moviePoster(featured)} alt={featured.title} loading="lazy" />
          </div>
          <div className="movie-featured-body">
            {featured.isPremiere && (
              <div className="movie-featured-badge">✨ Premiere</div>
            )}
            <h1>{featured.title}</h1>
            <p className="movie-featured-tag">{featured.tagline}</p>
            <div className="movie-featured-meta">
              {featured.ratingStars != null && (
                <span className="movie-chip rating">
                  ⭐ {Number(featured.ratingStars).toFixed(1)}
                </span>
              )}
              {featured.language && <span className="movie-chip">🗣 {featured.language}</span>}
              {featured.format && <span className="movie-chip">🎞 {featured.format}</span>}
              {featured.durationMinutes != null && (
                <span className="movie-chip">⏱ {formatDuration(featured.durationMinutes)}</span>
              )}
              {(featured.genre || '').split(',').slice(0, 3).map((g) => (
                <span key={g.trim()} className="movie-chip outline">
                  {g.trim()}
                </span>
              ))}
            </div>
            {featured.castList && (
              <div className="movie-featured-cast">
                <strong>Cast:</strong> {featured.castList}
              </div>
            )}
            {featured.director && (
              <div className="movie-featured-cast fs-sm">
                <strong>Director:</strong> {featured.director}
              </div>
            )}
            <div className="movie-featured-price">
              <span className="muted fs-sm">From</span>{' '}
              <Currency amount={featured.price} currency={featured.currencyIso} className="text-primary fw-800" />
            </div>
            <button className="btn primary lg" onClick={() => handleBook(featured)}>
              🎟 Book tickets
            </button>
          </div>
        </div>
      )}

      {/* ── Filters bar ── */}
      <div className="movie-filters">
        <div className="movie-filter-search">
          <input
            placeholder="🔍 Search movie, cast, director…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <select value={language} onChange={(e) => setLanguage(e.target.value)} aria-label="Language">
          <option value="">All languages</option>
          {allLanguages.map((l) => (
            <option key={l} value={l}>{l}</option>
          ))}
        </select>
        <select value={genre} onChange={(e) => setGenre(e.target.value)} aria-label="Genre">
          <option value="">All genres</option>
          {allGenres.map((g) => (
            <option key={g} value={g}>{g}</option>
          ))}
        </select>
        <select value={format} onChange={(e) => setFormat(e.target.value)} aria-label="Format">
          <option value="">All formats</option>
          {allFormats.map((f) => (
            <option key={f} value={f}>{f}</option>
          ))}
        </select>
        {(language || genre || format || search) && (
          <button
            type="button"
            className="btn sm"
            onClick={() => {
              setLanguage('');
              setGenre('');
              setFormat('');
              setSearch('');
            }}
          >
            Clear
          </button>
        )}
      </div>

      {/* ── Movies by language shelf ── */}
      {loading ? (
        <div className="movie-grid">
          {[0, 1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="movie-card skeleton" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          icon="🎬"
          title="No movies match your filters"
          description="Try changing the language, genre, or format."
        />
      ) : (
        byLanguage.map(([lang, list]) => (
          <div key={lang}>
            <SectionTitle
              title={`🗣 ${lang}`}
              right={<span className="muted fs-sm">{list.length} title{list.length === 1 ? '' : 's'}</span>}
            />
            <div className="movie-grid">
              {list.map((m) => (
                <MovieCard key={m.id} movie={m} onBook={() => handleBook(m)} />
              ))}
            </div>
          </div>
        ))
      )}
    </section>
  );
}

function MovieCard({ movie, onBook }: { movie: Movie; onBook: () => void }) {
  const soldOut = movie.availableQuantity <= 0;
  return (
    <article className="movie-card interactive">
      <div className="movie-poster-wrap">
        <img src={moviePoster(movie)} alt={movie.title} loading="lazy" />
        {movie.isPremiere && <span className="movie-poster-badge premiere">✨ Premiere</span>}
        {soldOut && <span className="movie-poster-badge soldout">Sold out</span>}
        {movie.format && <span className="movie-poster-format">{movie.format}</span>}
      </div>
      <div className="movie-card-body">
        <div className="movie-card-title">{movie.title}</div>
        <div className="movie-card-meta">
          {movie.ratingStars != null && (
            <span className="movie-chip mini rating">⭐ {Number(movie.ratingStars).toFixed(1)}</span>
          )}
          {movie.language && <span className="movie-chip mini">{movie.language}</span>}
        </div>
        <div className="movie-card-genre muted fs-xs">
          {(movie.genre || '').split(',').slice(0, 3).map((g) => g.trim()).filter(Boolean).join(' · ')}
        </div>
        {movie.eventDate && (
          <div className="movie-card-when muted fs-xs">📅 {formatEventDate(movie.eventDate)}</div>
        )}
        <div className="movie-card-foot">
          <Currency amount={movie.price} currency={movie.currencyIso} className="text-primary fw-700" />
          <button
            className="btn primary sm"
            onClick={onBook}
            disabled={soldOut}
          >
            {soldOut ? 'Sold out' : 'Book'}
          </button>
        </div>
      </div>
    </article>
  );
}
