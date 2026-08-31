import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';

interface DashboardStats {
  totalUsers: number;
  customers: number;
  agents: number;
  admins: number;
  totalTenants: number;
  activeTenants: number;
  totalProviders: number;
  activeProviders: number;
  pendingShops: number;
  approvedShops: number;
  totalProducts: number;
  enabledProducts: number;
  totalBookings: number;
}

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

interface UserSummary {
  id: number;
  username: string;
  fullName: string;
  email: string;
  role: string;
  status: string;
  tenantId: number | null;
  createdAt: string;
}

interface OrderSummary {
  id: number;
  orderRef: string;
  providerName: string;
  productTitle: string;
  productType: string;
  quantity: number;
  unitPrice: number;
  currencyIso: string;
  totalAmount: number;
  status: string;
  createdAt: string;
}

interface TenantSummary {
  id: number;
  slug: string;
  name: string;
  countryIso: string;
  currencyIso: string;
  defaultLanguage: string;
  timezone: string;
  domain: string | null;
  enabled: boolean;
  configVersion: number;
  moderationMode: string;
}

interface BrandingForm {
  logoUrl: string;
  themeColor: string;
  secondaryColor: string;
  tagline: string;
  bannerUrl: string;
}

type Tab = 'dashboard' | 'providers' | 'shops' | 'orders' | 'users' | 'tenants';

const EMPTY_BRANDING: BrandingForm = { logoUrl: '', themeColor: '', secondaryColor: '', tagline: '', bannerUrl: '' };

export default function AdminPage() {
  const { api, authenticated, username } = useApi();
  const [tab, setTab] = useState<Tab>('dashboard');

  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [providers, setProviders] = useState<Provider[]>([]);
  const [pendingShops, setPendingShops] = useState<Shop[]>([]);
  const [approvedShops, setApprovedShops] = useState<Shop[]>([]);
  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [tenants, setTenants] = useState<TenantSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);

  const [brandingTarget, setBrandingTarget] = useState<number | null>(null);
  const [brandingForm, setBrandingForm] = useState<BrandingForm>(EMPTY_BRANDING);
  const [savingBranding, setSavingBranding] = useState(false);

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<DashboardStats>('/admin/dashboard');
      setStats(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load dashboard');
    } finally {
      setLoading(false);
    }
  }, [api]);

  const loadProviders = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<unknown[]>('/admin/providers');
      setProviders((data as unknown as Provider[]) ?? []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load providers');
    } finally {
      setLoading(false);
    }
  }, [api]);

  const loadShops = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [pending, approved] = await Promise.all([
        api.get<unknown[]>('/admin/shops?status=PENDING'),
        api.get<unknown[]>('/admin/shops?status=APPROVED'),
      ]);
      setPendingShops((pending as unknown as Shop[]) ?? []);
      setApprovedShops((approved as unknown as Shop[]) ?? []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load shops');
    } finally {
      setLoading(false);
    }
  }, [api]);

  const loadOrders = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<unknown[]>('/admin/orders');
      setOrders((data as unknown as OrderSummary[]) ?? []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load orders');
    } finally {
      setLoading(false);
    }
  }, [api]);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<unknown[]>('/admin/users');
      setUsers((data as unknown as UserSummary[]) ?? []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load users');
    } finally {
      setLoading(false);
    }
  }, [api]);

  const loadTenants = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<unknown[]>('/admin/tenants');
      setTenants((data as unknown as TenantSummary[]) ?? []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load tenants');
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => {
    if (!authenticated) return;
    switch (tab) {
      case 'dashboard': void loadDashboard(); break;
      case 'providers': void loadProviders(); break;
      case 'shops': void loadShops(); break;
      case 'orders': void loadOrders(); break;
      case 'users': void loadUsers(); break;
      case 'tenants': void loadTenants(); break;
    }
  }, [authenticated, tab, loadDashboard, loadProviders, loadShops, loadOrders, loadUsers, loadTenants]);

  if (!authenticated) {
    return (
      <section className="page">
        <h1>Admin Portal</h1>
        <p className="muted">Sign in with an admin account to manage the platform.</p>
        <Link className="btn primary" to="/login">Sign in</Link>
      </section>
    );
  }

  const reviewShop = async (id: number, action: 'APPROVED' | 'REJECTED') => {
    setMsg(null);
    try {
      if (action === 'APPROVED') {
        await api.put<unknown>(`/admin/shops/${id}?action=APPROVED`);
        setMsg(`Shop #${id} approved.`);
      } else {
        const reason = window.prompt('Reason for rejection / suspension (required):', 'Does not meet marketplace policy');
        if (!reason || !reason.trim()) { setError('Suspension requires a reason'); return; }
        await api.put<unknown>(`/admin/shops/${id}?action=SUSPENDED&reason=${encodeURIComponent(reason.trim())}`);
        setMsg(`Shop #${id} suspended — reason: ${reason.trim()}`);
      }
      void loadShops();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to review shop');
    }
  };

  const suspendApprovedShop = async (id: number) => {
    const reason = window.prompt('Suspension reason (required, audit trail):', 'Policy violation — blocking new sales');
    if (!reason || !reason.trim()) { setError('Suspension requires a reason'); return; }
    setMsg(null);
    try {
      await api.put<unknown>(`/admin/shops/${id}?action=SUSPENDED&reason=${encodeURIComponent(reason.trim())}`);
      setMsg(`Shop #${id} suspended — inventory blocked, tickets remain valid`);
      void loadShops();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to suspend shop');
    }
  };

  const toggleProvider = async (id: number, status: 'ACTIVE' | 'SUSPENDED') => {
    setMsg(null);
    try {
      await api.put<unknown>(`/admin/providers/${id}/status?status=${status}`);
      setMsg(`Provider #${id} set to ${status.toLowerCase()}.`);
      void loadProviders();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to update provider');
    }
  };

  const openBranding = (p: Provider) => {
    setBrandingTarget(p.id);
    setBrandingForm({
      logoUrl: p.logoUrl ?? '',
      themeColor: p.themeColor ?? '#0b3b60',
      secondaryColor: p.secondaryColor ?? '',
      tagline: p.tagline ?? '',
      bannerUrl: p.bannerUrl ?? '',
    });
  };

  const saveBranding = async (e: React.FormEvent) => {
    e.preventDefault();
    if (brandingTarget == null) return;
    setSavingBranding(true);
    setMsg(null);
    try {
      const body: Record<string, string> = {};
      if (brandingForm.logoUrl) body.logoUrl = brandingForm.logoUrl;
      if (brandingForm.themeColor) body.themeColor = brandingForm.themeColor;
      if (brandingForm.secondaryColor) body.secondaryColor = brandingForm.secondaryColor;
      if (brandingForm.tagline) body.tagline = brandingForm.tagline;
      if (brandingForm.bannerUrl) body.bannerUrl = brandingForm.bannerUrl;
      await api.put<unknown>(`/admin/providers/${brandingTarget}/branding`, body);
      setMsg('Branding updated.');
      setBrandingTarget(null);
      void loadProviders();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to save branding');
    } finally {
      setSavingBranding(false);
    }
  };

  const fmt = (n: number | undefined | null) => n == null ? '—' : Number(n).toFixed(2);

  const tabs: { key: Tab; label: string; icon: string }[] = [
    { key: 'dashboard', label: 'Overview', icon: '📊' },
    { key: 'providers', label: 'Providers', icon: '🗄️' },
    { key: 'shops', label: 'Shops', icon: '🏬' },
    { key: 'orders', label: 'Orders', icon: '🧾' },
    { key: 'users', label: 'Users', icon: '👥' },
    { key: 'tenants', label: 'Tenants', icon: '🏢' },
  ];

  return (
    <section className="page">
      <h1>Admin Portal</h1>
      <p className="muted">Manage providers, shops, orders and platform settings — {username}.</p>

      <div className="portal">
        <aside className="portal-sidebar">
          <div className="ps-brand">
            Admin
            <small>Platform control center</small>
          </div>
          <div className="portal-nav">
            {tabs.map((t) => (
              <button
                key={t.key}
                className={tab === t.key ? 'active' : ''}
                onClick={() => { setTab(t.key); setError(null); setMsg(null); }}
              >
                <span className="pico">{t.icon}</span>
                {t.label}
              </button>
            ))}
          </div>
        </aside>

        <div className="portal-main">
          {error && <p className="error">{error}</p>}
          {msg && <p className="success">{msg}</p>}
          {loading && <p className="muted">Loading...</p>}

          {!loading && tab === 'dashboard' && stats && (
            <div>
              <div className="stats">
                <div className="stat primary"><span className="value">{stats.totalUsers}</span><span className="label">Total users</span></div>
                <div className="stat"><span className="value">{stats.customers}</span><span className="label">Customers</span></div>
                <div className="stat warning"><span className="value">{stats.agents}</span><span className="label">Agents</span></div>
                <div className="stat danger"><span className="value">{stats.admins}</span><span className="label">Admins</span></div>
              </div>
              <div className="stats">
                <div className="stat success"><span className="value">{stats.activeTenants}/{stats.totalTenants}</span><span className="label">Active tenants</span></div>
                <div className="stat"><span className="value">{stats.activeProviders}/{stats.totalProviders}</span><span className="label">Active providers</span></div>
                <div className="stat success"><span className="value">{stats.approvedShops}</span><span className="label">Approved shops</span></div>
                <div className="stat warning"><span className="value">{stats.pendingShops}</span><span className="label">Pending shops</span></div>
              </div>
              <div className="stats">
                <div className="stat"><span className="value">{stats.enabledProducts}/{stats.totalProducts}</span><span className="label">Enabled products</span></div>
                <div className="stat primary"><span className="value">{stats.totalBookings}</span><span className="label">Total bookings</span></div>
              </div>
            </div>
          )}

      {!loading && tab === 'providers' && (
        <div>
          {providers.length === 0 ? (
            <p className="muted">No providers registered.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Name</th>
                  <th>Vertical</th>
                  <th>Status</th>
                  <th>Theme</th>
                  <th>Actions</th>
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
                    <td><span className={`badge ${p.status === 'ACTIVE' ? 'confirmed' : p.status === 'SUSPENDED' ? 'blocked' : 'open'}`}>{p.status}</span></td>
                    <td>
                      {p.themeColor && (
                        <span style={{ display: 'inline-block', width: 20, height: 20, borderRadius: 4, background: p.themeColor, verticalAlign: 'middle' }} />
                      )}
                    </td>
                    <td>
                      <div className="row" style={{ gap: '0.5rem', flexWrap: 'wrap' }}>
                        {p.status !== 'ACTIVE' && (
                          <button className="btn" onClick={() => void toggleProvider(p.id, 'ACTIVE')}>Activate</button>
                        )}
                        {p.status !== 'SUSPENDED' && (
                          <button className="btn" onClick={() => void toggleProvider(p.id, 'SUSPENDED')}>Suspend</button>
                        )}
                        <button className="btn primary" onClick={() => openBranding(p)}>Branding</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {brandingTarget != null && (
            <div className="card" style={{ marginTop: '1.5rem' }}>
              <h2>Edit Provider Branding</h2>
              <p className="muted">Customize how this provider appears to customers.</p>
              <form className="form" onSubmit={(e) => void saveBranding(e)}>
                <div className="row">
                  <div className="field">
                    <label htmlFor="brandLogo">Logo URL</label>
                    <input id="brandLogo" value={brandingForm.logoUrl} placeholder="https://example.com/logo.png"
                      onChange={(e) => setBrandingForm({ ...brandingForm, logoUrl: e.target.value })} />
                  </div>
                  <div className="field">
                    <label htmlFor="brandBanner">Banner URL</label>
                    <input id="brandBanner" value={brandingForm.bannerUrl} placeholder="https://example.com/banner.jpg"
                      onChange={(e) => setBrandingForm({ ...brandingForm, bannerUrl: e.target.value })} />
                  </div>
                </div>
                <div className="row">
                  <div className="field">
                    <label htmlFor="brandTheme">Theme color</label>
                    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                      <input id="brandTheme" type="color" value={brandingForm.themeColor || '#0b3b60'}
                        onChange={(e) => setBrandingForm({ ...brandingForm, themeColor: e.target.value })}
                        style={{ width: 48, height: 36, padding: 2, cursor: 'pointer' }} />
                      <input value={brandingForm.themeColor} style={{ flex: 1 }}
                        onChange={(e) => setBrandingForm({ ...brandingForm, themeColor: e.target.value })} />
                    </div>
                  </div>
                  <div className="field">
                    <label htmlFor="brandSecondary">Secondary color</label>
                    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                      <input id="brandSecondary" type="color" value={brandingForm.secondaryColor || '#2e86de'}
                        onChange={(e) => setBrandingForm({ ...brandingForm, secondaryColor: e.target.value })}
                        style={{ width: 48, height: 36, padding: 2, cursor: 'pointer' }} />
                      <input value={brandingForm.secondaryColor} style={{ flex: 1 }}
                        onChange={(e) => setBrandingForm({ ...brandingForm, secondaryColor: e.target.value })} />
                    </div>
                  </div>
                </div>
                <div className="field">
                  <label htmlFor="brandTagline">Tagline</label>
                  <input id="brandTagline" value={brandingForm.tagline} placeholder="Your journey starts here"
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
        </div>
      )}

      {!loading && tab === 'shops' && (
        <div>
          <h2>Pending shops ({pendingShops.length})</h2>
          {pendingShops.length === 0 ? (
            <p className="muted">No pending shop applications.</p>
          ) : (
            <table className="table">
              <thead>
                <tr><th>Shop</th><th>Business</th><th>Country</th><th>Email</th><th>Applied</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {pendingShops.map((s) => (
                  <tr key={s.id}>
                    <td>{s.shopName}</td>
                    <td>{s.businessType}</td>
                    <td>{s.countryIso}</td>
                    <td>{s.contactEmail ?? '—'}</td>
                    <td>{new Date(s.appliedAt).toLocaleDateString()}</td>
                    <td>
                      <div className="row" style={{ gap: '0.5rem' }}>
                        <button className="btn primary" onClick={() => void reviewShop(s.id, 'APPROVED')}>Approve</button>
                        <button className="btn" onClick={() => void reviewShop(s.id, 'REJECTED')}>Reject</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          <h2 style={{ marginTop: '1.5rem' }}>Approved shops ({approvedShops.length})</h2>
          {approvedShops.length === 0 ? (
            <p className="muted">No approved shops.</p>
          ) : (
            <table className="table">
              <thead>
                <tr><th>Shop</th><th>Business</th><th>Country</th><th>Reviewed</th><th></th></tr>
              </thead>
              <tbody>
                {approvedShops.map((s) => (
                  <tr key={s.id}>
                    <td>{s.shopName}</td>
                    <td>{s.businessType}</td>
                    <td>{s.countryIso}</td>
                    <td>{s.reviewedAt ? new Date(s.reviewedAt).toLocaleDateString() : '—'}</td>
                    <td><button className="btn" style={{ fontSize:'0.78rem'}} onClick={() => void suspendApprovedShop(s.id)}>Suspend</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {!loading && tab === 'orders' && (
        <div>
          {orders.length === 0 ? (
            <p className="muted">No orders yet.</p>
          ) : (
            <table className="table">
              <thead>
                <tr><th>Ref</th><th>Product</th><th>Type</th><th>Qty</th><th>Total</th><th>Status</th><th>Date</th></tr>
              </thead>
              <tbody>
                {orders.map((o) => (
                  <tr key={o.id}>
                    <td>{o.orderRef}</td>
                    <td>{o.productTitle}</td>
                    <td><span className="tag">{o.productType}</span></td>
                    <td>{o.quantity}</td>
                    <td className="currency">{o.currencyIso} {fmt(o.totalAmount)}</td>
                    <td><span className={`badge ${o.status.toLowerCase()}`}>{o.status}</span></td>
                    <td>{new Date(o.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {!loading && tab === 'users' && (
        <div>
          {users.length === 0 ? (
            <p className="muted">No users found.</p>
          ) : (
            <table className="table">
              <thead>
                <tr><th>Username</th><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Joined</th></tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td><span className="tag">{u.username}</span></td>
                    <td>{u.fullName}</td>
                    <td className="muted">{u.email}</td>
                    <td><span className={`badge ${u.role === 'ADMIN' ? 'high' : u.role === 'AGENT' ? 'escalated' : 'open'}`}>{u.role}</span></td>
                    <td><span className={`badge ${u.status === 'ACTIVE' ? 'confirmed' : 'blocked'}`}>{u.status}</span></td>
                    <td>{new Date(u.createdAt).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {!loading && tab === 'tenants' && (
        <div>
          {tenants.length === 0 ? (
            <p className="muted">No tenants configured.</p>
          ) : (
            <table className="table">
              <thead>
                <tr><th>Slug</th><th>Name</th><th>Country</th><th>Currency</th><th>Language</th><th>Timezone</th><th>Moderation</th><th>Enabled</th></tr>
              </thead>
              <tbody>
                {tenants.map((t) => (
                  <tr key={t.id}>
                    <td><span className="tag">{t.slug}</span></td>
                    <td>{t.name}</td>
                    <td>{t.countryIso}</td>
                    <td>{t.currencyIso}</td>
                    <td>{t.defaultLanguage}</td>
                    <td>{t.timezone}</td>
                    <td><span className="badge open">{t.moderationMode}</span></td>
                    <td><span className={`badge ${t.enabled ? 'confirmed' : 'blocked'}`}>{t.enabled ? 'Yes' : 'No'}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
        </div>
      </div>
    </section>
  );
}
