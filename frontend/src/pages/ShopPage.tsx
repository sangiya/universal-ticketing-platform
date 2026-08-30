import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useApi } from '../context/ApiContext';

interface Branding {
  primaryColor: string;
  accentColor: string;
  logoUrl: string | null;
  tagline: string;
  tenantName: string;
}

export default function ShopPage() {
  const { slug } = useParams<{ slug: string }>();
  const { api } = useApi();
  const [branding, setBranding] = useState<Branding | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!slug) return;
    api
      .get<Branding>(`/tenant/${slug}/branding`)
      .then(setBranding)
      .catch((e) => setError(e instanceof Error ? e.message : 'Failed to load shop'));
  }, [api, slug]);

  const primary = branding?.primaryColor ?? '#0B3B60';

  return (
    <section className="page">
      <h2
        style={{
          color: primary,
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
        }}
      >
        {branding?.logoUrl && <img src={branding.logoUrl} alt="logo" className="logo" />}
        {branding?.tenantName ?? slug}
      </h2>
      {branding?.tagline && <p className="muted">{branding.tagline}</p>}
      {error && <p className="error">{error}</p>}
      <p>
        This shop page is rendered from the tenant's white-label configuration — colors,
        logo and images come straight from the platform with no code.
      </p>
    </section>
  );
}
