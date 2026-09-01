import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';
import {
  Alert,
  EmptyState,
  Modal,
  PageHeader,
  SectionTitle,
  Skeleton,
  StatCard,
} from '../components/UI';
import { useToast } from '../components/Toast';
import { detectLocale } from '../utils/locale';

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

const PRODUCT_TYPES = [
  { key: 'TICKET', icon: '🎫' },
  { key: 'SERVICE', icon: '🛎️' },
  { key: 'SEAT', icon: '💺' },
  { key: 'ROUTE', icon: '🚌' },
  { key: 'ADMISSION', icon: '🎟️' },
  { key: 'PACKAGE', icon: '📦' },
];

const AUTH_MODES = ['API_KEY', 'OAUTH2', 'BASIC'];

const VERTICALS = [
  { key: 'BUS', icon: '🚌' },
  { key: 'TRAIN', icon: '🚆' },
  { key: 'MOVIE', icon: '🎬' },
  { key: 'EVENT', icon: '🎤' },
  { key: 'SPORTS', icon: '⚽' },
  { key: 'FLIGHT', icon: '✈️' },
  { key: 'FERRY', icon: '⛴️' },
  { key: 'ATTRACTION', icon: '🎢' },
  { key: 'OTHER', icon: '🔖' },
];

const BUSINESS_TYPES = ['Retailer', 'Aggregator', 'Operator', 'Reseller', 'Marketplace'];

const EMPTY_PROVIDER: ProviderForm = {
  code: '',
  name: '',
  apiEndpoint: '',
  authMode: 'API_KEY',
  vertical: 'OTHER',
  capabilities: '',
};

const LOCALE_DEFAULTS = detectLocale();

const EMPTY_PRODUCT: ProductForm = {
  productType: 'TICKET',
  title: '',
  origin: '',
  destination: '',
  eventDate: '',
  price: '',
  currencyIso: LOCALE_DEFAULTS.currencyIso,
  availableQuantity: '1',
  description: '',
  providerCode: '',
};

const EMPTY_SHOP: ShopForm = {
  shopName: '',
  businessType: BUSINESS_TYPES[0],
  countryIso: LOCALE_DEFAULTS.countryIso,
  currencyIso: LOCALE_DEFAULTS.currencyIso,
  about: '',
  contactEmail: '',
  contactPhone: '',
};

function statusVariant(s: string): string {
  const v = s.toLowerCase();
  if (v === 'active' || v === 'approved' || v === 'enabled') return 'success';
  if (v === 'pending' || v === 'submitted' || v === 'in_review') return 'warn';
  if (v === 'rejected' || v === 'suspended' || v === 'disabled' || v === 'blocked') return 'danger';
  return 'info';
}

function toLocalInput(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(
    d.getHours()
  )}:${pad(d.getMinutes())}`;
}

export default function AgentPortalPage() {
  const { api, authenticated } = useApi();
  const { push } = useToast();

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

  const [shopForm, setShopForm] = useState<ShopForm>(EMPTY_SHOP);
  const [applying, setApplying] = useState(false);
  const [applyMessage, setApplyMessage] = useState<string | null>(null);

  const [productForm, setProductForm] = useState<ProductForm>(EMPTY_PRODUCT);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [productMessage, setProductMessage] = useState<string | null>(null);
  const [productError, setProductError] = useState<string | null>(null);

  const [brandingTarget, setBrandingTarget] = useState<Provider | null>(null);
  const [brandingForm, setBrandingForm] = useState({
    logoUrl: '',
    themeColor: '#0b3b60',
    secondaryColor: '',
    tagline: '',
    bannerUrl: '',
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
      setProductsError(null);
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
      const created = await api.post<Shop>('/agent/shops', shopForm);
      setShop(created ?? null);
      setApplyMessage('Shop application submitted for review.');
      push('Shop application submitted', 'success');
      setShopForm(EMPTY_SHOP);
      void loadProducts();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to submit shop application';
      setApplyMessage(msg);
      push(msg, 'error');
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
      setProviderMessage(`Provider "${providerForm.code}" connected.`);
      push(`Provider ${providerForm.code} connected`, 'success');
      setProviderForm(EMPTY_PROVIDER);
      void loadProviders();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to connect provider';
      setProviderMessage(msg);
      push(msg, 'error');
    } finally {
      setConnecting(false);
    }
  };

  const resetProductForm = () => {
    setProductForm({ ...EMPTY_PRODUCT, providerCode: productForm.providerCode });
    setEditingId(null);
    setProductMessage(null);
    setProductError(null);
  };

  const openBrandingEditor = (p: Provider) => {
    setBrandingTarget(p);
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
      await api.put<unknown>(`/agent/providers/${brandingTarget.code}/branding`, body);
      setBrandingMsg('Branding saved.');
      push('Branding saved', 'success');
      setBrandingTarget(null);
      void loadProviders();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to save branding';
      setBrandingMsg(msg);
      push(msg, 'error');
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
      push(`Product ${enabled ? 'enabled' : 'disabled'}`, 'success');
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Failed to update product';
      setProductError(msg);
      push(msg, 'error');
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
      providerCode:
        providers.find((pr) => pr.id === p.providerId)?.code ??
        productForm.providerCode,
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
        push('Product updated', 'success');
      } else {
        await api.post<unknown>(
          `/agent/providers/${productForm.providerCode}/products`,
          body
        );
        setProductMessage('Product published.');
        push('Product published', 'success');
      }
      resetProductForm();
      void loadProducts();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to save product';
      setProductError(msg);
      push(msg, 'error');
    } finally {
      setSaving(false);
    }
  };

  if (!authenticated) {
    return (
      <section className="page">
        <PageHeader
          title="Agent Portal"
          subtitle="Self-service shop management for agents and operators."
        />
        <EmptyState
          icon="🏪"
          title="Sign in to access your portal"
          description="Manage your shop, providers, products and branding from one place."
          action={
            <Link to="/login" className="btn primary">
              Sign in
            </Link>
          }
        />
      </section>
    );
  }

  const enabledProducts = products.filter((p) => p.enabled).length;
  const disabledProducts = products.length - enabledProducts;
  const activeProviders = providers.filter((p) => p.status === 'ACTIVE').length;

  return (
    <section className="page">
      <PageHeader
        title="🏪 Agent Portal"
        subtitle="Run your shop: apply to open one, connect providers, publish products, and customize branding."
        actions={
          <button
            className="btn"
            onClick={() => {
              void loadShop();
              void loadProviders();
              void loadProducts();
            }}
            disabled={shopLoading || providersLoading}
            aria-busy={shopLoading || providersLoading}
          >
            {shopLoading || providersLoading ? <span className="spinner" /> : '↻'} Refresh
          </button>
        }
      />

      <div className="stats">
        <StatCard
          label="My shop"
          value={shop ? shop.shopName : 'None'}
          icon="🏪"
          variant={shop ? 'success' : 'default'}
        />
        <StatCard
          label="Providers"
          value={providers.length}
          icon="🔌"
          variant="info"
        />
        <StatCard
          label="Active providers"
          value={activeProviders}
          icon="✓"
          variant="violet"
        />
        <StatCard
          label="Products"
          value={products.length}
          icon="📦"
          variant="warning"
        />
        <StatCard
          label="Enabled"
          value={enabledProducts}
          icon="🟢"
          variant="success"
        />
        <StatCard
          label="Disabled"
          value={disabledProducts}
          icon="⏸"
          variant="default"
        />
      </div>

      {shopError && !shop && !shopLoading && (
        <Alert kind="info" title="No shop yet">
          {shopError}. Apply below to get started.
        </Alert>
      )}

      {shopLoading && (
        <div className="card">
          <Skeleton lines={4} />
        </div>
      )}

      {!shop && !shopLoading && (
        <div className="card" style={{ marginBottom: 'var(--space-4)' }}>
          <SectionTitle
            title="Apply to open a shop"
            subtitle="Tell us about your business. We'll review your application and respond within 1–2 business days."
          />
          {applyMessage && (
            <Alert
              kind={applyMessage.includes('Failed') || applyMessage.includes('error') ? 'danger' : 'success'}
              title={applyMessage.includes('Failed') ? 'Application error' : 'Status'}
            >
              {applyMessage}
            </Alert>
          )}
          <form className="form" onSubmit={(e) => void applyShop(e)}>
            <div className="field">
              <label htmlFor="shopName">
                Shop name <span className="req">*</span>
              </label>
              <input
                id="shopName"
                required
                value={shopForm.shopName}
                onChange={(e) => setShopForm({ ...shopForm, shopName: e.target.value })}
                placeholder="e.g. Ceylon Express"
              />
            </div>
            <div className="row">
              <div className="field">
                <label htmlFor="businessType">Business type</label>
                <select
                  id="businessType"
                  value={shopForm.businessType}
                  onChange={(e) => setShopForm({ ...shopForm, businessType: e.target.value })}
                >
                  {BUSINESS_TYPES.map((b) => (
                    <option key={b} value={b}>
                      {b}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label htmlFor="countryIso">
                  Country (ISO-2) <span className="req">*</span>
                </label>
                <input
                  id="countryIso"
                  required
                  maxLength={2}
                  value={shopForm.countryIso}
                  onChange={(e) =>
                    setShopForm({ ...shopForm, countryIso: e.target.value.toUpperCase() })
                  }
                  placeholder={LOCALE_DEFAULTS.countryIso || 'XX'}
                />
              </div>
              <div className="field">
                <label htmlFor="currencyIso">
                  Currency (ISO-3) <span className="req">*</span>
                </label>
                <input
                  id="currencyIso"
                  required
                  maxLength={3}
                  value={shopForm.currencyIso}
                  onChange={(e) =>
                    setShopForm({ ...shopForm, currencyIso: e.target.value.toUpperCase() })
                  }
                  placeholder={LOCALE_DEFAULTS.currencyIso}
                />
              </div>
            </div>
            <div className="row">
              <div className="field">
                <label htmlFor="contactEmail">Contact email</label>
                <input
                  id="contactEmail"
                  type="email"
                  value={shopForm.contactEmail}
                  onChange={(e) => setShopForm({ ...shopForm, contactEmail: e.target.value })}
                  placeholder="hello@shop.com"
                />
              </div>
              <div className="field">
                <label htmlFor="contactPhone">Contact phone</label>
                <input
                  id="contactPhone"
                  value={shopForm.contactPhone}
                  onChange={(e) => setShopForm({ ...shopForm, contactPhone: e.target.value })}
                  placeholder="+94 11 234 5678"
                />
              </div>
            </div>
            <div className="field">
              <label htmlFor="about">About your business</label>
              <textarea
                id="about"
                rows={3}
                value={shopForm.about}
                onChange={(e) => setShopForm({ ...shopForm, about: e.target.value })}
                placeholder="What do you sell? Where do you operate? Any special partnerships?"
              />
            </div>
            <button className="btn primary block lg" type="submit" disabled={applying}>
              {applying ? (
                <>
                  <span className="spinner" />
                  Submitting application…
                </>
              ) : (
                '📨 Submit shop application'
              )}
            </button>
          </form>
        </div>
      )}

      {shop && (
        <div className="card" style={{ marginBottom: 'var(--space-4)' }}>
          <div className="row between center" style={{ marginBottom: 'var(--space-3)' }}>
            <div>
              <span className="eyebrow">My shop</span>
              <h2 style={{ margin: '0.25rem 0 0' }}>{shop.shopName}</h2>
            </div>
            <span className={`badge ${statusVariant(shop.status)}`}>{shop.status}</span>
          </div>
          <p className="muted fs-sm" style={{ marginTop: 0 }}>
            {shop.businessType} · {shop.countryIso} · {shop.currencyIso}
            {shop.contactEmail && ` · ${shop.contactEmail}`}
            {shop.contactPhone && ` · ${shop.contactPhone}`}
          </p>
          {shop.about && <p style={{ margin: 'var(--space-2) 0 0' }}>{shop.about}</p>}
          <p className="muted fs-xs" style={{ marginTop: 'var(--space-3)' }}>
            Applied {new Date(shop.appliedAt).toLocaleString()}
            {shop.reviewedAt && ` · Reviewed ${new Date(shop.reviewedAt).toLocaleString()}`}
          </p>
        </div>
      )}

      {providersError && <Alert kind="danger">{providersError}</Alert>}

      {providersLoading && !providers.length ? (
        <div className="card">
          <Skeleton lines={4} />
        </div>
      ) : !providersLoading && providers.length === 0 ? (
        <div className="card" style={{ marginBottom: 'var(--space-4)' }}>
          <SectionTitle
            title="🔌 Connect your first provider"
            subtitle="A provider (e.g. a railway, bus or movie operator) must be connected before you can publish products."
          />
          {providerMessage && (
            <Alert
              kind={providerMessage.includes('Failed') ? 'danger' : 'info'}
              title="Status"
            >
              {providerMessage}
            </Alert>
          )}
          <form className="form" onSubmit={(e) => void connectProvider(e)}>
            <div className="row">
              <div className="field">
                <label htmlFor="newProviderCode">
                  Provider code <span className="req">*</span>
                </label>
                <input
                  id="newProviderCode"
                  required
                  value={providerForm.code}
                  onChange={(e) => setProviderForm({ ...providerForm, code: e.target.value })}
                  placeholder="SL-RAIL"
                />
              </div>
              <div className="field" style={{ flex: 2 }}>
                <label htmlFor="providerName">
                  Provider name <span className="req">*</span>
                </label>
                <input
                  id="providerName"
                  required
                  value={providerForm.name}
                  onChange={(e) => setProviderForm({ ...providerForm, name: e.target.value })}
                  placeholder="Sri Lanka Railways"
                />
              </div>
            </div>
            <div className="field">
              <label htmlFor="apiEndpoint">API endpoint (optional)</label>
              <input
                id="apiEndpoint"
                value={providerForm.apiEndpoint}
                onChange={(e) => setProviderForm({ ...providerForm, apiEndpoint: e.target.value })}
                placeholder="https://api.railway.lk/v1"
              />
            </div>
            <div className="row">
              <div className="field">
                <label htmlFor="authMode">Auth mode</label>
                <select
                  id="authMode"
                  value={providerForm.authMode}
                  onChange={(e) => setProviderForm({ ...providerForm, authMode: e.target.value })}
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
                  onChange={(e) => setProviderForm({ ...providerForm, vertical: e.target.value })}
                >
                  {VERTICALS.map((v) => (
                    <option key={v.key} value={v.key}>
                      {v.icon} {v.key}
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
                onChange={(e) => setProviderForm({ ...providerForm, capabilities: e.target.value })}
                placeholder="search,book,cancel"
              />
            </div>
            <button className="btn primary" type="submit" disabled={connecting}>
              {connecting ? (
                <>
                  <span className="spinner" />
                  Connecting…
                </>
              ) : (
                '🔌 Connect provider'
              )}
            </button>
          </form>
        </div>
      ) : (
        providers.length > 0 && (
          <>
            {providerMessage && (
              <Alert
                kind={providerMessage.includes('Failed') ? 'danger' : 'success'}
                title="Provider status"
              >
                {providerMessage}
              </Alert>
            )}
            <SectionTitle
              title="🔌 Providers"
              subtitle="Connected transport operators and inventory sources. Customize each provider's branding below."
            />
            <div className="grid" style={{ marginBottom: 'var(--space-4)' }}>
              {providers.map((p) => (
                <article className="card" key={p.id}>
                  <div className="row between center" style={{ marginBottom: 'var(--space-2)' }}>
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 'var(--space-2)',
                      }}
                    >
                      {p.logoUrl ? (
                        <img
                          src={p.logoUrl}
                          alt=""
                          style={{
                            width: 40,
                            height: 40,
                            borderRadius: 8,
                            background: 'var(--surface-2)',
                            padding: 4,
                          }}
                        />
                      ) : (
                        <div
                          style={{
                            width: 40,
                            height: 40,
                            borderRadius: 8,
                            background: 'var(--gradient-soft)',
                            color: p.themeColor ?? 'var(--primary-700)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontSize: '1.2rem',
                          }}
                        >
                          {VERTICALS.find((v) => v.key === p.vertical)?.icon ?? '🔌'}
                        </div>
                      )}
                      <div>
                        <strong>{p.name}</strong>
                        <div className="muted fs-xs">
                          {VERTICALS.find((v) => v.key === p.vertical)?.icon} {p.vertical}
                        </div>
                      </div>
                    </div>
                    <span className={`badge ${statusVariant(p.status)}`}>{p.status}</span>
                  </div>
                  <p className="muted fs-sm" style={{ margin: 0 }}>
                    <code className="tag">{p.code}</code>
                    {p.themeColor && (
                      <>
                        {' · '}
                        <span
                          style={{
                            display: 'inline-block',
                            width: 12,
                            height: 12,
                            borderRadius: 3,
                            background: p.themeColor,
                            verticalAlign: 'middle',
                            marginRight: 4,
                          }}
                        />
                        {p.themeColor}
                      </>
                    )}
                    {p.tagline && ` · "${p.tagline}"`}
                  </p>
                  <div className="row tight" style={{ marginTop: 'var(--space-3)' }}>
                    <button
                      className="btn"
                      onClick={() => openBrandingEditor(p)}
                      style={{ flex: 1 }}
                    >
                      🎨 Branding
                    </button>
                  </div>
                </article>
              ))}
            </div>
          </>
        )
      )}

      <SectionTitle
        title={editingId ? `✏️ Edit product #${editingId}` : '📦 Publish a product'}
        subtitle={
          providers.length === 0
            ? 'Connect a provider first to enable product publishing.'
            : 'Add an inventory item to your catalog. Customers can purchase it through the marketplace.'
        }
      />

      {productMessage && <Alert kind="success">{productMessage}</Alert>}
      {productError && <Alert kind="danger">{productError}</Alert>}

      <div className="card" style={{ marginBottom: 'var(--space-4)' }}>
        <form className="form" onSubmit={(e) => void saveProduct(e)}>
          <div className="row">
            <div className="field" style={{ flex: '0 0 200px' }}>
              <label htmlFor="productType">Product type</label>
              <select
                id="productType"
                value={productForm.productType}
                onChange={(e) => setProductForm({ ...productForm, productType: e.target.value })}
              >
                {PRODUCT_TYPES.map((t) => (
                  <option key={t.key} value={t.key}>
                    {t.icon} {t.key}
                  </option>
                ))}
              </select>
            </div>
            <div className="field" style={{ flex: 1 }}>
              <label htmlFor="title">
                Title <span className="req">*</span>
              </label>
              <input
                id="title"
                required
                value={productForm.title}
                onChange={(e) => setProductForm({ ...productForm, title: e.target.value })}
                placeholder="Colombo → Kandy Express"
              />
            </div>
          </div>
          <div className="row">
            <div className="field">
              <label htmlFor="origin">Origin</label>
              <input
                id="origin"
                value={productForm.origin}
                onChange={(e) => setProductForm({ ...productForm, origin: e.target.value })}
                placeholder="Colombo Fort"
              />
            </div>
            <div className="field">
              <label htmlFor="destination">Destination</label>
              <input
                id="destination"
                value={productForm.destination}
                onChange={(e) => setProductForm({ ...productForm, destination: e.target.value })}
                placeholder="Kandy"
              />
            </div>
          </div>
          <div className="field">
            <label htmlFor="eventDate">Event date / time</label>
            <input
              id="eventDate"
              type="datetime-local"
              value={productForm.eventDate}
              onChange={(e) => setProductForm({ ...productForm, eventDate: e.target.value })}
            />
          </div>
          <div className="row">
            <div className="field">
              <label htmlFor="price">
                Price <span className="req">*</span>
              </label>
              <input
                id="price"
                type="number"
                min={0}
                step="0.01"
                required
                value={productForm.price}
                onChange={(e) => setProductForm({ ...productForm, price: e.target.value })}
                placeholder="0.00"
              />
            </div>
            <div className="field">
              <label htmlFor="prodCurrencyIso">Currency</label>
              <input
                id="prodCurrencyIso"
                maxLength={3}
                required
                value={productForm.currencyIso}
                onChange={(e) =>
                  setProductForm({
                    ...productForm,
                    currencyIso: e.target.value.toUpperCase(),
                  })
                }
                placeholder={LOCALE_DEFAULTS.currencyIso}
              />
            </div>
            <div className="field">
              <label htmlFor="availableQuantity">Quantity</label>
              <input
                id="availableQuantity"
                type="number"
                min={0}
                required
                value={productForm.availableQuantity}
                onChange={(e) =>
                  setProductForm({ ...productForm, availableQuantity: e.target.value })
                }
                placeholder="50"
              />
            </div>
          </div>
          <div className="field">
            <label htmlFor="description">Description</label>
            <textarea
              id="description"
              rows={2}
              value={productForm.description}
              onChange={(e) => setProductForm({ ...productForm, description: e.target.value })}
              placeholder="What's included? Any highlights? Duration, class, amenities…"
            />
          </div>
          {!editingId && (
            <div className="field">
              <label htmlFor="providerCode">
                Provider code <span className="req">*</span>
              </label>
              <select
                id="providerCode"
                required
                value={productForm.providerCode}
                onChange={(e) => setProductForm({ ...productForm, providerCode: e.target.value })}
              >
                {providers.map((p) => (
                  <option key={p.code} value={p.code}>
                    {p.code} — {p.name}
                  </option>
                ))}
              </select>
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
              {saving ? (
                <>
                  <span className="spinner" />
                  Saving…
                </>
              ) : editingId ? (
                '💾 Update product'
              ) : (
                '🚀 Publish product'
              )}
            </button>
            {editingId && (
              <button className="btn" type="button" onClick={resetProductForm}>
                Cancel edit
              </button>
            )}
          </div>
        </form>
      </div>

      <SectionTitle
        title="📋 My products"
        subtitle="Toggle availability to control which products appear in the marketplace."
      />

      {productsError && <Alert kind="danger">{productsError}</Alert>}

      {products.length === 0 ? (
        <EmptyState
          icon="📦"
          title="No products yet"
          description="Publish your first product above to start selling on the marketplace."
        />
      ) : (
        <div className="grid">
          {products.map((p) => {
            const typeIcon = PRODUCT_TYPES.find((t) => t.key === p.productType)?.icon ?? '📦';
            return (
              <article className="card" key={p.id}>
                <div className="row between center" style={{ marginBottom: 'var(--space-2)' }}>
                  <strong>
                    {typeIcon} {p.title}
                  </strong>
                  <span className={`badge ${p.enabled ? 'success' : 'default'}`}>
                    {p.enabled ? 'Enabled' : 'Disabled'}
                  </span>
                </div>
                <p className="muted fs-sm" style={{ margin: '0 0 var(--space-3)' }}>
                  {p.origin && p.destination ? `${p.origin} → ${p.destination}` : '—'}
                </p>
                <div className="row tight" style={{ marginBottom: 'var(--space-3)' }}>
                  <span className="tag outline">
                    {p.currencyIso} {Number(p.price).toFixed(2)}
                  </span>
                  <span className="tag outline">📦 {p.availableQuantity} left</span>
                </div>
                <div className="row tight">
                  <button
                    className="btn"
                    onClick={() => void toggleProduct(p.id, !p.enabled)}
                    style={{ flex: 1 }}
                  >
                    {p.enabled ? '⏸ Disable' : '▶ Enable'}
                  </button>
                  <button className="btn" onClick={() => startEdit(p)}>
                    ✏️ Edit
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}

      <Modal
        open={brandingTarget !== null}
        onClose={() => setBrandingTarget(null)}
        title={
          brandingTarget
            ? `🎨 Customize branding — ${brandingTarget.name}`
            : 'Customize branding'
        }
        subtitle="This is how customers see you in the marketplace and shop page."
      >
        {brandingMsg && (
          <Alert
            kind={brandingMsg.includes('Failed') || brandingMsg.includes('Could not') ? 'danger' : 'success'}
          >
            {brandingMsg}
          </Alert>
        )}
        <form className="form" onSubmit={(e) => void saveBranding(e)}>
          <div className="row">
            <div className="field">
              <label htmlFor="agLogo">Logo URL</label>
              <input
                id="agLogo"
                value={brandingForm.logoUrl}
                placeholder="https://example.com/logo.png"
                onChange={(e) => setBrandingForm({ ...brandingForm, logoUrl: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="agBanner">Banner URL</label>
              <input
                id="agBanner"
                value={brandingForm.bannerUrl}
                placeholder="https://example.com/banner.jpg"
                onChange={(e) => setBrandingForm({ ...brandingForm, bannerUrl: e.target.value })}
              />
            </div>
          </div>
          <div className="row">
            <div className="field">
              <label htmlFor="agTheme">Theme color</label>
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <input
                  id="agTheme"
                  type="color"
                  value={brandingForm.themeColor}
                  onChange={(e) =>
                    setBrandingForm({ ...brandingForm, themeColor: e.target.value })
                  }
                  style={{ width: 48, height: 36, padding: 2, cursor: 'pointer' }}
                />
                <input
                  value={brandingForm.themeColor}
                  style={{ flex: 1, fontFamily: 'monospace' }}
                  onChange={(e) =>
                    setBrandingForm({ ...brandingForm, themeColor: e.target.value })
                  }
                />
              </div>
            </div>
            <div className="field">
              <label htmlFor="agSecondary">Secondary color</label>
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <input
                  id="agSecondary"
                  type="color"
                  value={brandingForm.secondaryColor || '#2e86de'}
                  onChange={(e) =>
                    setBrandingForm({ ...brandingForm, secondaryColor: e.target.value })
                  }
                  style={{ width: 48, height: 36, padding: 2, cursor: 'pointer' }}
                />
                <input
                  value={brandingForm.secondaryColor}
                  placeholder="#2e86de"
                  style={{ flex: 1, fontFamily: 'monospace' }}
                  onChange={(e) =>
                    setBrandingForm({ ...brandingForm, secondaryColor: e.target.value })
                  }
                />
              </div>
            </div>
          </div>
          <div className="field">
            <label htmlFor="agTagline">Tagline</label>
            <input
              id="agTagline"
              value={brandingForm.tagline}
              placeholder="Your journey starts here"
              onChange={(e) => setBrandingForm({ ...brandingForm, tagline: e.target.value })}
            />
          </div>

          <div
            style={{
              padding: 'var(--space-3)',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border)',
              background: `linear-gradient(135deg, ${brandingForm.themeColor} 0%, ${
                brandingForm.secondaryColor || '#7c3aed'
              } 100%)`,
              color: '#fff',
              minHeight: 96,
              display: 'flex',
              alignItems: 'center',
              gap: 'var(--space-3)',
            }}
          >
            {brandingForm.logoUrl ? (
              <img
                src={brandingForm.logoUrl}
                alt=""
                style={{
                  width: 56,
                  height: 56,
                  borderRadius: 12,
                  background: 'rgba(255,255,255,0.95)',
                  padding: 4,
                }}
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
              <span style={{ opacity: 0.85, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: 0.5 }}>
                Live preview
              </span>
              <div style={{ fontSize: '1.2rem', fontWeight: 700, marginTop: 2 }}>
                {brandingTarget?.name ?? 'Provider'}
              </div>
              {brandingForm.tagline && (
                <div style={{ fontSize: '0.85rem', opacity: 0.9, marginTop: 2 }}>
                  "{brandingForm.tagline}"
                </div>
              )}
            </div>
          </div>

          <div className="row">
            <button className="btn primary" type="submit" disabled={savingBranding}>
              {savingBranding ? (
                <>
                  <span className="spinner" />
                  Saving…
                </>
              ) : (
                '💾 Save branding'
              )}
            </button>
            <button className="btn" type="button" onClick={() => setBrandingTarget(null)}>
              Cancel
            </button>
          </div>
        </form>
      </Modal>
    </section>
  );
}
