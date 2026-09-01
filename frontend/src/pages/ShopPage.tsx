import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useApi } from '../context/ApiContext';
import { Alert, EmptyState, PageHeader, Skeleton } from '../components/UI';

interface Branding {
  primaryColor: string;
  accentColor: string;
  logoUrl: string | null;
  tagline: string;
  tenantName: string;
}

interface Product {
  id: number;
  title: string;
  productType: string;
  origin: string | null;
  destination: string | null;
  price: number;
  currencyIso: string;
  availableQuantity: number;
  description: string | null;
  themeColor?: string | null;
  logoUrl?: string | null;
}

export default function ShopPage() {
  const { slug } = useParams<{ slug: string }>();
  const { api } = useApi();
  const [branding, setBranding] = useState<Branding | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!slug) return;
    setLoading(true);
    setError(null);
    api
      .get<Branding>(`/tenant/${slug}/branding`)
      .then(setBranding)
      .catch((e) => setError(e instanceof Error ? e.message : 'Failed to load shop'))
      .finally(() => setLoading(false));
  }, [api, slug]);

  useEffect(() => {
    if (!slug) return;
    api
      .get<unknown[]>(`/catalog?tenantSlug=${encodeURIComponent(slug)}`)
      .then((data) => setProducts((data as unknown as Product[]) ?? []))
      .catch(() => setProducts([]));
  }, [api, slug]);

  const primary = branding?.primaryColor ?? '#0B3B60';
  const accent = branding?.accentColor ?? '#7c3aed';

  if (loading && !branding) {
    return (
      <section className="page">
        <PageHeader title="Loading shop…" />
        <div className="card">
          <Skeleton lines={4} />
        </div>
      </section>
    );
  }

  if (error || !branding) {
    return (
      <section className="page">
        <PageHeader title="Shop" />
        <EmptyState
          icon="🏪"
          title="Shop not found"
          description={error ?? 'This shop may be inactive or removed.'}
        />
      </section>
    );
  }

  return (
    <section className="page">
      <div
        className="hero"
        style={{
          background: `linear-gradient(135deg, ${primary} 0%, ${accent} 100%)`,
          marginTop: 0,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
          {branding.logoUrl ? (
            <img
              src={branding.logoUrl}
              alt={`${branding.tenantName} logo`}
              className="logo"
              style={{ width: 56, height: 56, borderRadius: 12, background: '#fff', padding: 4 }}
            />
          ) : (
            <div
              style={{
                width: 56,
                height: 56,
                borderRadius: 12,
                background: 'rgba(255,255,255,0.18)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '1.5rem',
              }}
            >
              🏪
            </div>
          )}
          <div>
            <span className="eyebrow">Shop · @{slug}</span>
            <h1 style={{ margin: 0 }}>{branding.tenantName}</h1>
            {branding.tagline && (
              <p className="lead" style={{ margin: '0.4rem 0 0' }}>
                {branding.tagline}
              </p>
            )}
          </div>
        </div>
      </div>

      <div className="features" style={{ marginTop: 'var(--space-6)' }}>
        <div className="feature">
          <div className="ico" style={{ background: 'var(--gradient-soft)', color: primary }}>
            🛍️
          </div>
          <h4>{products.length} products</h4>
          <p>Live inventory curated by this shop</p>
        </div>
        <div className="feature">
          <div className="ico" style={{ background: 'var(--gradient-soft)', color: primary }}>
            🌍
          </div>
          <h4>Multi-currency</h4>
          <p>Pay in your local currency</p>
        </div>
        <div className="feature">
          <div className="ico" style={{ background: 'var(--gradient-soft)', color: primary }}>
            🛡️
          </div>
          <h4>Trust &amp; safety</h4>
          <p>Verified by TicketMesh</p>
        </div>
      </div>

      <div className="section-title">
        <h2>Products</h2>
        <span className="muted">Live inventory</span>
      </div>

      {products.length === 0 ? (
        <EmptyState
          icon="🛍️"
          title="No products yet"
          description="This shop hasn't added any products to the marketplace yet."
        />
      ) : (
        <div className="grid">
          {products.map((p) => (
            <article className="card product-card" key={p.id}>
              <div
                className="product-cover"
                style={{
                  background: `linear-gradient(135deg, ${p.themeColor || primary}, ${accent})`,
                }}
              >
                <span className="product-type">{p.productType}</span>
                <span className="price-badge">
                  {p.currencyIso} {Number(p.price).toFixed(2)}
                </span>
              </div>
              <div className="product-body">
                <h3>{p.title}</h3>
                <p className="muted fs-sm">
                  {p.origin && p.destination ? `${p.origin} → ${p.destination}` : '—'}
                </p>
                <div className="product-meta">
                  <span className="tag outline">
                    {p.availableQuantity > 0 ? `🎟 ${p.availableQuantity} left` : 'Sold out'}
                  </span>
                </div>
                <button
                  className="btn primary block"
                  disabled={p.availableQuantity <= 0}
                >
                  {p.availableQuantity > 0 ? 'Add to cart' : 'Sold out'}
                </button>
              </div>
            </article>
          ))}
        </div>
      )}

      <Alert kind="info" title="About this shop">
        This shop is rendered from the tenant's white-label configuration — colors,
        logo and content come straight from the platform with no code changes.
      </Alert>
    </section>
  );
}
