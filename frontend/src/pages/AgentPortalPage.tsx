import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';

interface Shop {
  id: number;
  shopName: string;
  businessType: string;
  countryIso: string;
  currencyIso: string;
  about: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  status: string;
  tenantId: number | null;
  appliedAt: string;
  reviewedAt: string | null;
}

interface Provider {
  id: number;
  code: string;
  name: string;
  shopId: number;
  tenantId: number;
  countryIso: string;
  currencyIso: string;
  timezone: string;
  apiEndpoint: string | null;
  authMode: string;
  vertical: string;
  capabilities: string | null;
  status: string;
  logoUrl: string | null;
  themeColor: string | null;
  secondaryColor: string | null;
  tagline: string | null;
  bannerUrl: string | null;
  createdAt: string;
}

interface Product {
  id: number;
  providerId: number;
  providerName: string;
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
  createdAt: string;
}

interface ShopForm {
  shopName: string;
  businessType: string;
  countryIso: string;
  currencyIso: string;
  about: string;
  contactEmail: string;
  contactPhone: string;
}

interface ProviderForm {
  code: string;
  name: string;
  apiEndpoint: string;
  authMode: string;
  vertical: string;
  capabilities: string;
}

interface ProductForm {
  productType: string;
  title: string;
  origin: string;
  destination: string;
  eventDate: string;
  price: string;
  currencyIso: string;
  availableQuantity: string;
  description: string;
  providerCode: string;
}

const PRODUCT_TYPES = ['TICKET', 'SERVICE', 'SEAT', 'ROUTE', 'ADMISSION', 'PACKAGE'];

const AUTH_MODES = ['API_KEY', 'OAUTH2', 'BASIC'];

const VERTICALS = ['BUS', 'TRAIN', 'MOVIE', 'EVENT', 'SPORTS', 'FLIGHT', 'FERRY', 'ATTRACTION', 'OTHER'];

const EMPTY_PROVIDER: ProviderForm = {
  code: '',
  name: '',
  apiEndpoint: '',
  authMode: 'API_KEY',
  vertical: 'TRAIN',
  capabilities: '',
};

const EMPTY_PRODUCT: ProductForm = {
  productType: 'TICKET',
  title: '',
  origin: '',
  destination: '',
  eventDate: '',
  price: '',
  currencyIso: 'LKR',
  availableQuantity: '1',
  description: '',
  providerCode: '',
};

export default function AgentPortalPage() {
  const { api, authenticated } = useApi();
  const [shop, setShop] = useState<Shop | null>(null);
  const [shopLoading, setShopLoading] = useState(true);
  const [shopError, setShopError] = useState<string | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [productsError, setProductsError] = useState<string | null>(null);
  const [providers, setProviders] = useState<Provider[]>([]);
  const [providersLoading, setProvidersLoading] = useState(true);
  const [providersError, setProvidersError] = useState<string | null>(null);

  const [providerForm, setProviderForm] = useState<ProviderForm>(EMPTY_PROVIDER);
  const [connecting, setConnecting] = useState(false);
  const [providerMessage, setProviderMessage] = useState<string | null>(null);

  const [shopForm, setShopForm] = useState<ShopForm>({
    shopName: '',
    businessType: 'Retailer',
    countryIso: 'LK',
    currencyIso: 'LKR',
    about: '',
    contactEmail: '',
    contactPhone: '',
  });
  const [applying, setApplying] = useState(false);
  const [applyMessage, setApplyMessage] = useState<string | null>(null);

  const [productForm, setProductForm] = useState<ProductForm>(EMPTY_PRODUCT);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [productMessage, setProductMessage] = useState<string | null>(null);
  const [productError, setProductError] = useState<string | null>(null);

  const [brandingTarget, setBrandingTarget] = useState<string | null>(null);
  const [brandingForm, setBrandingForm] = useState({
    logoUrl: '', themeColor: '#0b3b60', secondaryColor: '', tagline: '', bannerUrl: '',
  });
  const [savingBranding, setSavingBranding] = useState(false);
  const [brandingMsg, setBrandingMsg] = useState<string | null>(null);

  const loadShop = useCallback(async () => {
    setShopLoading(true);
    try {
      const data = await api.get<Shop>('/agent/shops/me');
      setShop(data ?? null);
      setShopError(null);
    } catch (e) {
      setShop(null);
      setShopError(e instanceof Error ? e.message : 'You have no shop yet');
    } finally {
      setShopLoading(false);
    }
  }, [api]);

  const loadProducts = useCallback(async () => {
    try {
      const data = await api.get<unknown[]>('/agent/products');
      setProducts((data as unknown as Product[]) ?? []);
    } catch (e) {
      setProductsError(e instanceof Error ? e.message : 'Failed to load products');
    }
  }, [api]);

  const loadProviders = useCallback(async () => {
    setProvidersLoading(true);
    try {
      const data = await api.get<unknown[]>('/agent/providers');
      const list = (data as unknown as Provider[]) ?? [];
      setProviders(list);
      setProvidersError(null);
      setProductForm((prev) =>
        prev.providerCode === '' && list.length > 0
          ? { ...prev, providerCode: list[0].code }
          : prev
      );
    } catch (e) {
      setProvidersError(e instanceof Error ? e.message : 'Failed to load providers');
    } finally {
      setProvidersLoading(false);
    }
  }, [api]);

  useEffect(() => {
    if (!authenticated) return;
    void loadShop();
    void loadProducts();
    void loadProviders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authenticated]);

  const applyShop = async (e: React.FormEvent) => {
    e.preventDefault();
    setApplying(true);
    setApplyMessage(null);
    try {
      const created = await api.post<Shop>('/agent/shops?tenant=tickets', shopForm);
      setShop(created ?? null);
      setApplyMessage('Shop application submitted.');
      void loadProducts();
    } catch (err) {
      setApplyMessage(
        err instanceof Error ? err.message : 'Failed to submit shop application'
      );
    } finally {
      setApplying(false);
    }
  };

  const connectProvider = async (e: React.FormEvent) => {
    e.preventDefault();
    setConnecting(true);
    setProviderMessage(null);
    try {
      const body = {
        code: providerForm.code,
        name: providerForm.name,
        apiEndpoint: providerForm.apiEndpoint || undefined,
        authMode: providerForm.authMode,
        vertical: providerForm.vertical,
        capabilities: providerForm.capabilities || undefined,
      };
      await api.post<Provider>('/agent/providers', body);
      setProviderMessage(
        `Provider "${providerForm.code}" connected successfully.`
      );
      setProviderForm(EMPTY_PROVIDER);
      void loadProviders();
    } catch (err) {
      setProviderMessage(
        err instanceof Error ? err.message : 'Failed to connect provider'
      );
    } finally {
      setConnecting(false);
    }
  };

  const resetProductForm = () => {
    setProductForm(EMPTY_PRODUCT);
    setEditingId(null);
    setProductMessage(null);
    setProductError(null);
  };

  const openBrandingEditor = (p: Provider) => {
    setBrandingTarget(p.code);
    setBrandingForm({
      logoUrl: p.logoUrl ?? '',
      themeColor: p.themeColor ?? '#0b3b60',
      secondaryColor: p.secondaryColor ?? '',
      tagline: p.tagline ?? '',
      bannerUrl: p.bannerUrl ?? '',
    });
    setBrandingMsg(null);
  };

  const saveBranding = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!brandingTarget) return;
    setSavingBranding(true);
    setBrandingMsg(null);
    try {
      const body: Record<string, string> = {};
      if (brandingForm.logoUrl) body.logoUrl = brandingForm.logoUrl;
      if (brandingForm.themeColor) body.themeColor = brandingForm.themeColor;
      if (brandingForm.secondaryColor) body.secondaryColor = brandingForm.secondaryColor;
      if (brandingForm.tagline) body.tagline = brandingForm.tagline;
      if (brandingForm.bannerUrl) body.bannerUrl = brandingForm.bannerUrl;
      await api.put<unknown>(`/agent/providers/${brandingTarget}/branding`, body);
      setBrandingMsg('Branding saved.');
      setBrandingTarget(null);
      void loadProviders();
    } catch (err) {
      setBrandingMsg(err instanceof Error ? err.message : 'Failed to save branding');
    } finally {
      setSavingBranding(false);
    }
  };

  const toggleProduct = async (id: number, enabled: boolean) => {
    setProductError(null);
    try {
      await api.put<unknown>(`/agent/products/${id}/status?enabled=${enabled}`);
      setProducts((prev) =>
        prev.map((p) => (p.id === id ? { ...p, enabled } : p))
      );
    } catch (e) {
      setProductError(
        e instanceof Error ? e.message : 'Failed to update product'
      );
    }
  };

  const startEdit = (p: Product) => {
    setEditingId(p.id);
    setProductForm({
      productType: p.productType,
      title: p.title,
      origin: p.origin ?? '',
      destination: p.destination ?? '',
      eventDate: p.eventDate ? toLocalInput(p.eventDate) : '',
      price: String(p.price),
      currencyIso: p.currencyIso,
      availableQuantity: String(p.availableQuantity),
      description: p.description ?? '',
      providerCode: productForm.providerCode,
    });
    setProductMessage(null);
    setProductError(null);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const saveProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setProductMessage(null);
    setProductError(null);
    const body = {
      productType: productForm.productType,
      title: productForm.title,
      origin: productForm.origin || null,
      destination: productForm.destination || null,
      eventDate: productForm.eventDate ? productForm.eventDate : null,
      price: Number(productForm.price),
      currencyIso: productForm.currencyIso,
      availableQuantity: Number(productForm.availableQuantity),
      description: productForm.description || null,
      attributes: null,
    };
    try {
      if (editingId) {
        await api.put<unknown>(`/agent/products/${editingId}`, body);
        setProductMessage('Product updated.');
      } else {
        await api.post<unknown>(
          `/agent/providers/${productForm.providerCode}/products`,
          body
        );
        setProductMessage('Product published.');
      }
      resetProductForm();
      void loadProducts();
    } catch (err) {
      setProductError(
        err instanceof Error ? err.message : 'Failed to save product'
      );
    } finally {
      setSaving(false);
    }
  };

  if (!authenticated) {
    return (
      <section className="page">
        <h1>Agent Portal</h1>
        <p className="muted">Sign in as an agent to run your shop.</p>
        <Link className="btn primary" to="/login">
          Sign in
        </Link>
      </section>
    );
  }

  const fmt = (n: number | undefined | null) =>
    n == null ? '—' : Number(n).toFixed(2);

  return (
    <section className="page">
      <h1>Agent Portal</h1>
      <p className="muted">
        Self-service shop management: apply to open a shop, publish products and
        control availability.
      </p>

      {shopLoading && <p className="muted">Loading your shop…</p>}

      {!shop && !shopLoading && (
        <>
          <h2>Apply to open a shop</h2>
          {shopError && <p className="error">{shopError}</p>}
          <form className="form" onSubmit={(e) => void applyShop(e)}>
            <div className="field">
              <label htmlFor="shopName">Shop name</label>
              <input
                id="shopName"
                required
                value={shopForm.shopName}
                onChange={(e) =>
                  setShopForm({ ...shopForm, shopName: e.target.value })
                }
              />
            </div>
            <div className="field">
              <label htmlFor="businessType">Business type</label>
              <input
                id="businessType"
                required
                value={shopForm.businessType}
                onChange={(e) =>
                  setShopForm({ ...shopForm, businessType: e.target.value })
                }
              />
            </div>
            <div className="row">
              <div className="field">
                <label htmlFor="countryIso">Country (ISO-2)</label>
                <input
                  id="countryIso"
                  required
                  maxLength={2}
                  value={shopForm.countryIso}
                  onChange={(e) =>
                    setShopForm({ ...shopForm, countryIso: e.target.value })
                  }
                />
              </div>
              <div className="field">
                <label htmlFor="currencyIso">Currency (ISO-3)</label>
                <input
                  id="currencyIso"
                  required
                  maxLength={3}
                  value={shopForm.currencyIso}
                  onChange={(e) =>
                    setShopForm({ ...shopForm, currencyIso: e.target.value })
                  }
                />
              </div>
            </div>
            <div className="field">
              <label htmlFor="contactEmail">Contact email</label>
              <input
                id="contactEmail"
                type="email"
                value={shopForm.contactEmail}
                onChange={(e) =>
                  setShopForm({ ...shopForm, contactEmail: e.target.value })
                }
              />
            </div>
            <div className="field">
              <label htmlFor="contactPhone">Contact phone</label>
              <input
                id="contactPhone"
                value={shopForm.contactPhone}
                onChange={(e) =>
                  setShopForm({ ...shopForm, contactPhone: e.target.value })
                }
              />
            </div>
            <div className="field">
              <label htmlFor="about">About</label>
              <textarea
                id="about"
                rows={3}
                value={shopForm.about}
                onChange={(e) =>
                  setShopForm({ ...shopForm, about: e.target.value })
                }
              />
            </div>
            <button className="btn primary" type="submit" disabled={applying}>
              {applying ? 'Submitting…' : 'Apply to open shop'}
            </button>
          </form>
          {applyMessage && <p className="muted">{applyMessage}</p>}
        </>
      )}

      {shop && (
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <h2>{shop.shopName}</h2>
          <p className="muted">
            {shop.businessType} · {shop.countryIso} · {shop.currencyIso}
          </p>
          {shop.about && <p>{shop.about}</p>}
          <p>
            Status: <span className={`badge ${shop.status.toLowerCase()}`}>{shop.status}</span>
            {shop.contactEmail && <span> · {shop.contactEmail}</span>}
            {shop.contactPhone && <span> · {shop.contactPhone}</span>}
          </p>
        </div>
      )}

      <h2 style={{ marginTop: '2rem' }}>Providers</h2>
      {providersLoading && <p className="muted">Loading providers…</p>}
      {!providersLoading && providersError && <p className="error">{providersError}</p>}

      {!providersLoading && providers.length === 0 && (
        <div className="card">
          <h3>Connect your first provider</h3>
          <p className="muted">
            A provider (e.g. a railway, bus or movie operator) must be connected
            before you can publish products. Pick a concrete vertical — there is
            no mixed type.
          </p>
          {providerMessage && <p className="error">{providerMessage}</p>}
          <form className="form" onSubmit={(e) => void connectProvider(e)}>
            <div className="row">
              <div className="field">
                <label htmlFor="newProviderCode">Provider code</label>
                <input
                  id="newProviderCode"
                  required
                  value={providerForm.code}
                  onChange={(e) =>
                    setProviderForm({ ...providerForm, code: e.target.value })
                  }
                />
              </div>
              <div className="field" style={{ flex: 2 }}>
                <label htmlFor="providerName">Provider name</label>
                <input
                  id="providerName"
                  required
                  value={providerForm.name}
                  onChange={(e) =>
                    setProviderForm({ ...providerForm, name: e.target.value })
                  }
                />
              </div>
            </div>
            <div className="field">
              <label htmlFor="apiEndpoint">API endpoint (optional)</label>
              <input
                id="apiEndpoint"
                value={providerForm.apiEndpoint}
                onChange={(e) =>
                  setProviderForm({
                    ...providerForm,
                    apiEndpoint: e.target.value,
                  })
                }
              />
            </div>
            <div className="row">
              <div className="field">
                <label htmlFor="authMode">Auth mode</label>
                <select
                  id="authMode"
                  value={providerForm.authMode}
                  onChange={(e) =>
                    setProviderForm({ ...providerForm, authMode: e.target.value })
                  }
                >
                  {AUTH_MODES.map((m) => (
                    <option key={m} value={m}>
                      {m}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label htmlFor="vertical">Vertical</label>
                <select
                  id="vertical"
                  value={providerForm.vertical}
                  onChange={(e) =>
                    setProviderForm({ ...providerForm, vertical: e.target.value })
                  }
                >
                  {VERTICALS.map((v) => (
                    <option key={v} value={v}>
                      {v}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="field">
              <label htmlFor="capabilities">Capabilities (optional)</label>
              <input
                id="capabilities"
                value={providerForm.capabilities}
                onChange={(e) =>
                  setProviderForm({
                    ...providerForm,
                    capabilities: e.target.value,
                  })
                }
              />
            </div>
            <button
              className="btn primary"
              type="submit"
              disabled={connecting}
            >
              {connecting ? 'Connecting…' : 'Connect provider'}
            </button>
          </form>
        </div>
      )}

      {!providersLoading && providers.length > 0 && (
        <>
          {providerMessage && <p className="success">{providerMessage}</p>}
          <table className="table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Vertical</th>
                <th>Branding</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {providers.map((p) => (
                <tr key={p.id}>
                  <td><span className="tag">{p.code}</span></td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      {p.logoUrl && <img src={p.logoUrl} alt="" className="logo" />}
                      <div>
                        <strong>{p.name}</strong>
                        {p.tagline && <div className="muted" style={{ fontSize: '0.8rem' }}>{p.tagline}</div>}
                      </div>
                    </div>
                  </td>
                  <td>{p.vertical}</td>
                  <td>
                    {p.themeColor && (
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
                        <span style={{ display: 'inline-block', width: 16, height: 16, borderRadius: 4, background: p.themeColor }} />
                        <span className="muted" style={{ fontSize: '0.8rem' }}>{p.themeColor}</span>
                      </span>
                    )}
                    {!p.themeColor && <span className="muted">Not set</span>}
                  </td>
                  <td>
                    <span className={`badge ${p.status === 'ACTIVE' ? 'confirmed' : 'pending'}`}>{p.status}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {brandingMsg && <p className={brandingMsg.includes('saved') ? 'success' : 'error'}>{brandingMsg}</p>}

          <h3 style={{ marginTop: '1.5rem' }}>Customize provider branding</h3>
          <p className="muted">Set logo, theme color and tagline for each of your providers. This is how customers see you in the marketplace.</p>
          <div className="row" style={{ flexWrap: 'wrap', gap: '0.75rem', marginBottom: '1rem' }}>
            {providers.map((p) => (
              <button key={p.code} className={`btn ${brandingTarget === p.code ? 'primary' : ''}`}
                onClick={() => openBrandingEditor(p)}>
                {p.logoUrl && <img src={p.logoUrl} alt="" className="logo" style={{ marginRight: 4 }} />}
                {p.name}
              </button>
            ))}
          </div>

          {brandingTarget && (
            <div className="card">
              <h3>Edit branding: {brandingTarget}</h3>
              <form className="form" onSubmit={(e) => void saveBranding(e)}>
                <div className="row">
                  <div className="field">
                    <label htmlFor="agLogo">Logo URL</label>
                    <input id="agLogo" value={brandingForm.logoUrl} placeholder="https://example.com/logo.png"
                      onChange={(e) => setBrandingForm({ ...brandingForm, logoUrl: e.target.value })} />
                  </div>
                  <div className="field">
                    <label htmlFor="agBanner">Banner URL</label>
                    <input id="agBanner" value={brandingForm.bannerUrl} placeholder="https://example.com/banner.jpg"
                      onChange={(e) => setBrandingForm({ ...brandingForm, bannerUrl: e.target.value })} />
                  </div>
                </div>
                <div className="row">
                  <div className="field">
                    <label htmlFor="agTheme">Theme color</label>
                    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                      <input id="agTheme" type="color" value={brandingForm.themeColor}
                        onChange={(e) => setBrandingForm({ ...brandingForm, themeColor: e.target.value })}
                        style={{ width: 48, height: 36, padding: 2, cursor: 'pointer' }} />
                      <input value={brandingForm.themeColor} style={{ flex: 1 }}
                        onChange={(e) => setBrandingForm({ ...brandingForm, themeColor: e.target.value })} />
                    </div>
                  </div>
                  <div className="field">
                    <label htmlFor="agSecondary">Secondary color</label>
                    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                      <input id="agSecondary" type="color" value={brandingForm.secondaryColor || '#2e86de'}
                        onChange={(e) => setBrandingForm({ ...brandingForm, secondaryColor: e.target.value })}
                        style={{ width: 48, height: 36, padding: 2, cursor: 'pointer' }} />
                      <input value={brandingForm.secondaryColor} style={{ flex: 1 }}
                        onChange={(e) => setBrandingForm({ ...brandingForm, secondaryColor: e.target.value })} />
                    </div>
                  </div>
                </div>
                <div className="field">
                  <label htmlFor="agTagline">Tagline</label>
                  <input id="agTagline" value={brandingForm.tagline} placeholder="Your journey starts here"
                    onChange={(e) => setBrandingForm({ ...brandingForm, tagline: e.target.value })} />
                </div>
                <div className="row">
                  <button className="btn primary" type="submit" disabled={savingBranding}>
                    {savingBranding ? 'Saving...' : 'Save branding'}
                  </button>
                  <button className="btn" type="button" onClick={() => setBrandingTarget(null)}>Cancel</button>
                </div>
              </form>
            </div>
          )}
        </>
      )}

      <h2 style={{ marginTop: '2rem' }}>
        {editingId ? `Edit product #${editingId}` : 'Publish a product'}
      </h2>
      {!providersLoading && providers.length === 0 && (
        <p className="muted">
          Connect a provider first to enable product publishing.
        </p>
      )}
      <form className="form" onSubmit={(e) => void saveProduct(e)}>
        <div className="row">
          <div className="field" style={{ flex: 0.6 }}>
            <label htmlFor="productType">Product type</label>
            <select
              id="productType"
              value={productForm.productType}
              onChange={(e) =>
                setProductForm({ ...productForm, productType: e.target.value })
              }
            >
              {PRODUCT_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </div>
          <div className="field" style={{ flex: 1.4 }}>
            <label htmlFor="title">Title</label>
            <input
              id="title"
              required
              value={productForm.title}
              onChange={(e) =>
                setProductForm({ ...productForm, title: e.target.value })
              }
            />
          </div>
        </div>
        <div className="row">
          <div className="field">
            <label htmlFor="origin">Origin</label>
            <input
              id="origin"
              value={productForm.origin}
              onChange={(e) =>
                setProductForm({ ...productForm, origin: e.target.value })
              }
            />
          </div>
          <div className="field">
            <label htmlFor="destination">Destination</label>
            <input
              id="destination"
              value={productForm.destination}
              onChange={(e) =>
                setProductForm({ ...productForm, destination: e.target.value })
              }
            />
          </div>
        </div>
        <div className="field">
          <label htmlFor="eventDate">Event date / time</label>
          <input
            id="eventDate"
            type="datetime-local"
            value={productForm.eventDate}
            onChange={(e) =>
              setProductForm({ ...productForm, eventDate: e.target.value })
            }
          />
        </div>
        <div className="row">
          <div className="field">
            <label htmlFor="price">Price</label>
            <input
              id="price"
              type="number"
              min={0}
              step="0.01"
              required
              value={productForm.price}
              onChange={(e) =>
                setProductForm({ ...productForm, price: e.target.value })
              }
            />
          </div>
          <div className="field">
            <label htmlFor="prodCurrencyIso">Currency (ISO-3)</label>
            <input
              id="prodCurrencyIso"
              maxLength={3}
              required
              value={productForm.currencyIso}
              onChange={(e) =>
                setProductForm({ ...productForm, currencyIso: e.target.value })
              }
            />
          </div>
          <div className="field">
            <label htmlFor="availableQuantity">Available quantity</label>
            <input
              id="availableQuantity"
              type="number"
              min={0}
              required
              value={productForm.availableQuantity}
              onChange={(e) =>
                setProductForm({
                  ...productForm,
                  availableQuantity: e.target.value,
                })
              }
            />
          </div>
        </div>
        <div className="field">
          <label htmlFor="description">Description</label>
          <textarea
            id="description"
            rows={2}
            value={productForm.description}
            onChange={(e) =>
              setProductForm({ ...productForm, description: e.target.value })
            }
          />
        </div>
        {!editingId && (
          <div className="field">
            <label htmlFor="providerCode">Provider code</label>
            <input
              id="providerCode"
              required
              value={productForm.providerCode}
              onChange={(e) =>
                setProductForm({ ...productForm, providerCode: e.target.value })
              }
            />
          </div>
        )}
        <div className="row">
          <button
            className="btn primary"
            type="submit"
            disabled={saving || (!editingId && providers.length === 0)}
            title={
              !editingId && providers.length === 0
                ? 'Connect a provider first'
                : undefined
            }
          >
            {saving
              ? 'Saving…'
              : editingId
                ? 'Update product'
                : 'Publish product'}
          </button>
          {editingId && (
            <button className="btn" type="button" onClick={resetProductForm}>
              Cancel edit
            </button>
          )}
        </div>
      </form>
      {productMessage && <p className="success">{productMessage}</p>}
      {productError && <p className="error">{productError}</p>}

      <h2 style={{ marginTop: '2rem' }}>My products</h2>
      {productsError && <p className="error">{productsError}</p>}
      {products.length === 0 ? (
        <p className="muted">No products published yet.</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Title</th>
              <th>Type</th>
              <th>Price</th>
              <th>Qty</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {products.map((p) => (
              <tr key={p.id}>
                <td>{p.title}</td>
                <td>
                  <span className="tag">{p.productType}</span>
                </td>
                <td className="currency">
                  {p.currencyIso} {fmt(p.price)}
                </td>
                <td>{p.availableQuantity}</td>
                <td>
                  <span className={`badge ${p.enabled ? 'confirmed' : 'cancelled'}`}>
                    {p.enabled ? 'Enabled' : 'Disabled'}
                  </span>
                </td>
                <td>
                  <div className="row" style={{ gap: '0.5rem' }}>
                    <button
                      className="btn"
                      onClick={() => void toggleProduct(p.id, !p.enabled)}
                    >
                      {p.enabled ? 'Disable' : 'Enable'}
                    </button>
                    <button className="btn" onClick={() => startEdit(p)}>
                      Edit
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

function toLocalInput(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(
    d.getHours()
  )}:${pad(d.getMinutes())}`;
}
