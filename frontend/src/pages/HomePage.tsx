import { useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';

interface Offer {
  id: number;
  title: string;
  providerName: string;
  price: string;
  currency: string;
}

export default function HomePage() {
  const { api } = useApi();
  const [query, setQuery] = useState('');
  const [offers, setOffers] = useState<Offer[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const search = async (q = query) => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<unknown[]>(
        `/catalog/search${q ? `?q=${encodeURIComponent(q)}` : ''}`
      );
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <section className="page">
      <h1>Find and book everything, anywhere.</h1>
      <p className="muted">
        Search tickets and services from every connected provider and shop — one
        platform, your country, your currency, your language.
      </p>
      <form
        className="search"
        onSubmit={(e) => {
          e.preventDefault();
          void search();
        }}
      >
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search buses, trains, movies, events, flights…"
        />
        <button className="btn primary" type="submit">
          Search
        </button>
      </form>

      {error && <p className="error">{error}</p>}
      {loading && <p className="muted">Searching the marketplace…</p>}

      <div className="grid">
        {offers.map((o) => (
          <article className="card" key={o.id}>
            <h3>{o.title}</h3>
            <p className="muted">by {o.providerName}</p>
            <p className="price">
              {o.currency} {o.price}
            </p>
            <Link className="btn" to="/tickets">
              Book
            </Link>
          </article>
        ))}
        {!loading && offers.length === 0 && (
          <p className="muted">No results. Try a different search.</p>
        )}
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
