import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../context/ApiContext';
import { Link } from 'react-router-dom';
import {
  Alert,
  Currency,
  EmptyState,
  Modal,
  PageHeader,
  Skeleton,
  StatCard,
} from '../components/UI';
import { useToast } from '../components/Toast';

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

const EMPTY_BRANDING: BrandingForm = {
  logoUrl: '',
  themeColor: '',
  secondaryColor: '',
  tagline: '',
  bannerUrl: '',
};

const TABS: { key: Tab; label: string; icon: string }[] = [
  { key: 'dashboard', label: 'Overview', icon: '📊' },
  { key: 'providers', label: 'Providers', icon: '🗄️' },
  { key: 'shops', label: 'Shops', icon: '🏬' },
  { key: 'orders', label: 'Orders', icon: '🧾' },
  { key: 'users', label: 'Users', icon: '👥' },
  { key: 'tenants', label: 'Tenants', icon: '🏢' },
];

function statusVariant(s: string): string {
  const v = s.toLowerCase();
  if (v === 'active' || v === 'paid' || v === 'confirmed' || v === 'approved' || v === 'enabled' || v === 'resolved' || v === 'closed') return 'success';
  if (v === 'pending' || v === 'open' || v === 'in_progress' || v === 'low') return 'info';
  if (v === 'high' || v === 'medium' || v === 'escalated' || v === 'warning') return 'warn';
  if (v === 'suspended' || v === 'cancelled' || v === 'blocked' || v === 'rejected' || v === 'expired' || v === 'refunded' || v === 'disabled') return 'danger';
  return 'default';
}

function roleVariant(r: string): string {
  if (r === 'ADMIN') return 'role-admin';
  if (r === 'AGENT') return 'role-agent';
  if (r === 'CUSTOMER') return 'role-customer';
  return 'default';
}

export default function AdminPage() {
  const { api, authenticated, username } = useApi();
  const { push } = useToast();
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

  const [brandingTarget, setBrandingTarget] = useState<Provider | null>(null);
  const [brandingForm, setBrandingForm] = useState<BrandingForm>(EMPTY_BRANDING);
  const [savingBranding, setSavingBranding] = useState(false);

  const [suspendTarget, setSuspendTarget] = useState<{ id: number; name: string } | null>(null);
  const [suspendReason, setSuspendReason] = useState('');

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setStats(await api.get<DashboardStats>('/admin/dashboard'));
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authenticated, tab]);

  if (!authenticated) {
    return (
      <section className="page">
        <PageHeader
          title="Admin Portal"
          subtitle="Manage providers, shops, orders and platform settings."
        />
        <EmptyState
          icon="⚙️"
          title="Sign in with an admin account"
          description="The admin portal is restricted to platform administrators."
          action={<Link className="btn primary" to="/login">Sign in</Link>}
        />
      </section>
    );
  }

  const reviewShop = async (id: number, action: 'APPROVED' | 'REJECTED') => {
    setError(null);
    try {
      if (action === 'APPROVED') {
        await api.put<unknown>(`/admin/shops/${id}?action=APPROVED`);
        push('Shop approved', 'success');
      } else {
        const reason = window.prompt('Reason for rejection (required):', 'Does not meet marketplace policy');
        if (!reason || !reason.trim()) {
          setError('Rejection requires a reason');
          return;
        }
        await api.put<unknown>(`/admin/shops/${id}?action=SUSPENDED&reason=${encodeURIComponent(reason.trim())}`);
        push('Shop suspended', 'warn');
      }
      void loadShops();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to review shop');
    }
  };

  const confirmSuspend = async () => {
    if (!suspendTarget || !suspendReason.trim()) {
      push('Suspension reason is required', 'error');
      return;
    }
    try {
      await api.put<unknown>(
        `/admin/shops/${suspendTarget.id}?action=SUSPENDED&reason=${encodeURIComponent(suspendReason.trim())}`,
      );
      push(`Shop "${suspendTarget.name}" suspended — inventory blocked, tickets remain valid`, 'warn');
      setSuspendTarget(null);
      setSuspendReason('');
      void loadShops();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to suspend shop');
    }
  };

  const toggleProvider = async (id: number, status: 'ACTIVE' | 'SUSPENDED') => {
    setError(null);
    try {
      await api.put<unknown>(`/admin/providers/${id}/status?status=${status}`);
      push(`Provider set to ${status.toLowerCase()}`, status === 'ACTIVE' ? 'success' : 'warn');
      void loadProviders();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to update provider');
    }
  };

  const openBranding = (p: Provider) => {
    setBrandingTarget(p);
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
    setError(null);
    try {
      const body: Record<string, string> = {};
      if (brandingForm.logoUrl) body.logoUrl = brandingForm.logoUrl;
      if (brandingForm.themeColor) body.themeColor = brandingForm.themeColor;
      if (brandingForm.secondaryColor) body.secondaryColor = brandingForm.secondaryColor;
      if (brandingForm.tagline) body.tagline = brandingForm.tagline;
      if (brandingForm.bannerUrl) body.bannerUrl = brandingForm.bannerUrl;
      await api.put<unknown>(`/admin/providers/${brandingTarget.id}/branding`, body);
      push('Branding updated', 'success');
      setBrandingTarget(null);
      void loadProviders();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to save branding');
    } finally {
      setSavingBranding(false);
    }
  };

  return (
    <section className="page">
      <PageHeader
        title="Admin Portal"
        subtitle={`Platform control center · ${username ?? ''}`}
        actions={
          <button
            className="btn"
            onClick={() => {
              switch (tab) {
                case 'dashboard': void loadDashboard(); break;
                case 'providers': void loadProviders(); break;
                case 'shops': void loadShops(); break;
                case 'orders': void loadOrders(); break;
                case 'users': void loadUsers(); break;
                case 'tenants': void loadTenants(); break;
              }
            }}
            disabled={loading}
            aria-busy={loading}
          >
            {loading ? <span className="spinner" /> : '↻'} Refresh
          </button>
        }
      />

      <div className="portal">
        <aside className="portal-sidebar">
          <div className="ps-brand">
            <span className="brand-mark">TM</span>
            <div>
              Admin
              <small>Platform control center</small>
            </div>
          </div>
          <div className="portal-nav">
            {TABS.map((t) => (
              <button
                key={t.key}
                className={tab === t.key ? 'active' : ''}
                onClick={() => {
                  setTab(t.key);
                  setError(null);
                }}
              >
                <span className="pico">{t.icon}</span>
                {t.label}
                {t.key === 'shops' && pendingShops.length > 0 && (
                  <span className="badge warn" style={{ marginLeft: 'auto' }}>
                    {pendingShops.length}
                  </span>
                )}
              </button>
            ))}
          </div>
          <div className="ps-foot">
            <div style={{ color: '#94a3b8', fontSize: '0.72rem' }}>Logged in as</div>
            <strong style={{ color: '#fff' }}>{username}</strong>
          </div>
        </aside>

        <div className="portal-main">
          {error && <Alert kind="danger" title="Error">{error}</Alert>}

          {loading && !stats && tab === 'dashboard' && (
            <div className="grid">
              {Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="card skeleton-card" />
              ))}
            </div>
          )}

          {tab === 'dashboard' && stats && (
            <div className="stack loose">
              <h2 style={{ margin: 0 }}>📊 Platform overview</h2>

              <div className="stats">
                <StatCard label="Total users" value={stats.totalUsers} icon="👥" />
                <StatCard label="Customers" value={stats.customers} icon="👤" variant="info" />
                <StatCard label="Agents" value={stats.agents} icon="🏪" variant="violet" />
                <StatCard label="Admins" value={stats.admins} icon="⚙️" variant="warning" />
              </div>

              <div className="stats">
                <StatCard
                  label="Active tenants"
                  value={`${stats.activeTenants}/${stats.totalTenants}`}
                  icon="🏢"
                  variant="success"
                />
                <StatCard
                  label="Active providers"
                  value={`${stats.activeProviders}/${stats.totalProviders}`}
                  icon="🗄️"
                  variant="success"
                />
                <StatCard label="Approved shops" value={stats.approvedShops} icon="🏬" variant="success" />
                <StatCard
                  label="Pending shops"
                  value={stats.pendingShops}
                  icon="⏳"
                  variant={stats.pendingShops > 0 ? 'warning' : 'default'}
                />
              </div>

              <div className="stats">
                <StatCard
                  label="Enabled products"
                  value={`${stats.enabledProducts}/${stats.totalProducts}`}
                  icon="📦"
                />
                <StatCard label="Total bookings" value={stats.totalBookings} icon="🎫" variant="violet" />
              </div>
            </div>
          )}

          {tab === 'providers' && (
            <div className="stack loose">
              <h2 style={{ margin: 0 }}>🗄️ Providers ({providers.length})</h2>
              {loading ? (
                <Skeleton lines={6} />
              ) : providers.length === 0 ? (
                <EmptyState
                  icon="🗄️"
                  title="No providers yet"
                  description="Approved agents can register providers here."
                />
              ) : (
                <div className="table-wrap">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Provider</th>
                        <th>Code</th>
                        <th>Vertical</th>
                        <th>Status</th>
                        <th>Theme</th>
                        <th className="right">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {providers.map((p) => (
                        <tr key={p.id}>
                          <td>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                              {p.logoUrl ? (
                                <img src={p.logoUrl} alt="" className="logo" style={{ background: '#fff' }} />
                              ) : (
                                <div
                                  style={{
                                    width: 32,
                                    height: 32,
                                    borderRadius: 8,
                                    background: p.themeColor || 'var(--gradient)',
                                    color: '#fff',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    fontWeight: 800,
                                  }}
                                >
                                  {p.name.charAt(0)}
                                </div>
                              )}
                              <div>
                                <strong>{p.name}</strong>
                                {p.tagline && (
                                  <div className="muted fs-xs">{p.tagline}</div>
                                )}
                              </div>
                            </div>
                          </td>
                          <td>
                            <code className="tag">{p.code}</code>
                          </td>
                          <td>
                            <span className="tag outline">{p.vertical}</span>
                          </td>
                          <td>
                            <span className={`badge ${statusVariant(p.status)}`}>{p.status}</span>
                          </td>
                          <td>
                            {p.themeColor && (
                              <span
                                style={{
                                  display: 'inline-block',
                                  width: 20,
                                  height: 20,
                                  borderRadius: 4,
                                  background: p.themeColor,
                                  border: '1px solid var(--border)',
                                }}
                              />
                            )}
                          </td>
                          <td className="right">
                            <div className="row tight" style={{ flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                              {p.status !== 'ACTIVE' && (
                                <button className="btn success sm" onClick={() => void toggleProvider(p.id, 'ACTIVE')}>
                                  Activate
                                </button>
                              )}
                              {p.status !== 'SUSPENDED' && (
                                <button className="btn danger sm" onClick={() => void toggleProvider(p.id, 'SUSPENDED')}>
                                  Suspend
                                </button>
                              )}
                              <button className="btn sm" onClick={() => openBranding(p)}>
                                Branding
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {tab === 'shops' && (
            <div className="stack loose">
              <h2 style={{ margin: 0 }}>🏬 Shop moderation</h2>

              <div className="card">
                <div className="row between center" style={{ marginBottom: 'var(--space-3)' }}>
                  <h3 style={{ margin: 0 }}>⏳ Pending applications</h3>
                  <span className="badge warn">{pendingShops.length}</span>
                </div>
                {pendingShops.length === 0 ? (
                  <p className="muted">No pending applications. 🎉</p>
                ) : (
                  <div className="table-wrap" style={{ border: 'none', boxShadow: 'none' }}>
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Shop</th>
                          <th>Business</th>
                          <th>Country</th>
                          <th>Email</th>
                          <th>Applied</th>
                          <th className="right">Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {pendingShops.map((s) => (
                          <tr key={s.id}>
                            <td>
                              <strong>{s.shopName}</strong>
                              {s.about && <div className="muted fs-xs">{s.about}</div>}
                            </td>
                            <td>{s.businessType}</td>
                            <td>
                              <span className="tag">{s.countryIso}</span>
                            </td>
                            <td className="muted">{s.contactEmail ?? '—'}</td>
                            <td className="muted fs-sm">
                              {new Date(s.appliedAt).toLocaleDateString()}
                            </td>
                            <td className="right">
                              <div className="row tight" style={{ justifyContent: 'flex-end' }}>
                                <button
                                  className="btn success sm"
                                  onClick={() => void reviewShop(s.id, 'APPROVED')}
                                >
                                  ✓ Approve
                                </button>
                                <button
                                  className="btn danger sm"
                                  onClick={() => void reviewShop(s.id, 'REJECTED')}
                                >
                                  ✕ Reject
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              <div className="card">
                <div className="row between center" style={{ marginBottom: 'var(--space-3)' }}>
                  <h3 style={{ margin: 0 }}>✓ Approved shops</h3>
                  <span className="badge success">{approvedShops.length}</span>
                </div>
                {approvedShops.length === 0 ? (
                  <p className="muted">No approved shops yet.</p>
                ) : (
                  <div className="table-wrap" style={{ border: 'none', boxShadow: 'none' }}>
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Shop</th>
                          <th>Business</th>
                          <th>Country</th>
                          <th>Reviewed</th>
                          <th className="right">Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {approvedShops.map((s) => (
                          <tr key={s.id}>
                            <td>
                              <strong>{s.shopName}</strong>
                            </td>
                            <td>{s.businessType}</td>
                            <td>
                              <span className="tag">{s.countryIso}</span>
                            </td>
                            <td className="muted fs-sm">
                              {s.reviewedAt ? new Date(s.reviewedAt).toLocaleDateString() : '—'}
                            </td>
                            <td className="right">
                              <button
                                className="btn danger sm"
                                onClick={() => {
                                  setSuspendTarget({ id: s.id, name: s.shopName });
                                  setSuspendReason('');
                                }}
                              >
                                Suspend
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          )}

          {tab === 'orders' && (
            <div className="stack loose">
              <h2 style={{ margin: 0 }}>🧾 All platform orders</h2>
              {loading ? (
                <Skeleton lines={6} />
              ) : orders.length === 0 ? (
                <EmptyState icon="🧾" title="No orders yet" />
              ) : (
                <div className="table-wrap">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Ref</th>
                        <th>Product</th>
                        <th>Provider</th>
                        <th>Type</th>
                        <th className="right">Qty</th>
                        <th className="right">Total</th>
                        <th>Status</th>
                        <th>Date</th>
                      </tr>
                    </thead>
                    <tbody>
                      {orders.map((o) => (
                        <tr key={o.id}>
                          <td>
                            <code className="tag">{o.orderRef}</code>
                          </td>
                          <td>
                            <strong>{o.productTitle}</strong>
                          </td>
                          <td>{o.providerName}</td>
                          <td>
                            <span className="tag">{o.productType}</span>
                          </td>
                          <td className="right num">{o.quantity}</td>
                          <td className="right">
                            <Currency amount={o.totalAmount} currency={o.currencyIso} />
                          </td>
                          <td>
                            <span className={`badge ${statusVariant(o.status)}`}>{o.status}</span>
                          </td>
                          <td className="muted fs-sm">
                            {new Date(o.createdAt).toLocaleString()}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {tab === 'users' && (
            <div className="stack loose">
              <h2 style={{ margin: 0 }}>👥 Users ({users.length})</h2>
              {loading ? (
                <Skeleton lines={6} />
              ) : users.length === 0 ? (
                <EmptyState icon="👥" title="No users yet" />
              ) : (
                <div className="table-wrap">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Username</th>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Status</th>
                        <th>Joined</th>
                      </tr>
                    </thead>
                    <tbody>
                      {users.map((u) => (
                        <tr key={u.id}>
                          <td>
                            <code className="tag">{u.username}</code>
                          </td>
                          <td>
                            <strong>{u.fullName}</strong>
                          </td>
                          <td className="muted">{u.email}</td>
                          <td>
                            <span className={`badge ${roleVariant(u.role)}`}>{u.role}</span>
                          </td>
                          <td>
                            <span className={`badge ${statusVariant(u.status)}`}>{u.status}</span>
                          </td>
                          <td className="muted fs-sm">
                            {new Date(u.createdAt).toLocaleDateString()}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {tab === 'tenants' && (
            <div className="stack loose">
              <h2 style={{ margin: 0 }}>🏢 Tenants ({tenants.length})</h2>
              {loading ? (
                <Skeleton lines={6} />
              ) : tenants.length === 0 ? (
                <EmptyState icon="🏢" title="No tenants yet" />
              ) : (
                <div className="table-wrap">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Slug</th>
                        <th>Name</th>
                        <th>Country</th>
                        <th>Currency</th>
                        <th>Language</th>
                        <th>Timezone</th>
                        <th>Moderation</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {tenants.map((t) => (
                        <tr key={t.id}>
                          <td>
                            <code className="tag">{t.slug}</code>
                          </td>
                          <td>
                            <strong>{t.name}</strong>
                            {t.domain && <div className="muted fs-xs">{t.domain}</div>}
                          </td>
                          <td>
                            <span className="tag">{t.countryIso}</span>
                          </td>
                          <td>{t.currencyIso}</td>
                          <td>{t.defaultLanguage}</td>
                          <td className="muted fs-sm">{t.timezone}</td>
                          <td>
                            <span className="badge info">{t.moderationMode}</span>
                          </td>
                          <td>
                            <span className={`badge ${statusVariant(t.enabled ? 'ACTIVE' : 'SUSPENDED')}`}>
                              {t.enabled ? 'Active' : 'Disabled'}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {brandingTarget && (
        <Modal
          title={`Edit branding · ${brandingTarget.name}`}
          description="Customize how this provider appears to customers."
          size="lg"
          onClose={() => setBrandingTarget(null)}
          footer={
            <>
              <button className="btn" onClick={() => setBrandingTarget(null)}>
                Cancel
              </button>
              <button
                className="btn primary"
                type="submit"
                form="branding-form"
                disabled={savingBranding}
                aria-busy={savingBranding}
              >
                {savingBranding ? (
                  <>
                    <span className="spinner" /> Saving…
                  </>
                ) : (
                  'Save branding'
                )}
              </button>
            </>
          }
        >
          <form id="branding-form" className="form" onSubmit={(e) => void saveBranding(e)}>
            <div className="row">
              <div className="field">
                <label htmlFor="brandLogo">Logo URL</label>
                <input
                  id="brandLogo"
                  value={brandingForm.logoUrl}
                  placeholder="https://example.com/logo.png"
                  onChange={(e) => setBrandingForm({ ...brandingForm, logoUrl: e.target.value })}
                />
              </div>
              <div className="field">
                <label htmlFor="brandBanner">Banner URL</label>
                <input
                  id="brandBanner"
                  value={brandingForm.bannerUrl}
                  placeholder="https://example.com/banner.jpg"
                  onChange={(e) => setBrandingForm({ ...brandingForm, bannerUrl: e.target.value })}
                />
              </div>
            </div>
            <div className="row">
              <div className="field">
                <label htmlFor="brandTheme">Theme color</label>
                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                  <input
                    id="brandTheme"
                    type="color"
                    value={brandingForm.themeColor || '#0b3b60'}
                    onChange={(e) => setBrandingForm({ ...brandingForm, themeColor: e.target.value })}
                    style={{ width: 48, height: 38, padding: 2, cursor: 'pointer' }}
                  />
                  <input
                    value={brandingForm.themeColor}
                    style={{ flex: 1 }}
                    onChange={(e) => setBrandingForm({ ...brandingForm, themeColor: e.target.value })}
                  />
                </div>
              </div>
              <div className="field">
                <label htmlFor="brandSecondary">Secondary color</label>
                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                  <input
                    id="brandSecondary"
                    type="color"
                    value={brandingForm.secondaryColor || '#2e86de'}
                    onChange={(e) => setBrandingForm({ ...brandingForm, secondaryColor: e.target.value })}
                    style={{ width: 48, height: 38, padding: 2, cursor: 'pointer' }}
                  />
                  <input
                    value={brandingForm.secondaryColor}
                    style={{ flex: 1 }}
                    onChange={(e) => setBrandingForm({ ...brandingForm, secondaryColor: e.target.value })}
                  />
                </div>
              </div>
            </div>
            <div className="field">
              <label htmlFor="brandTagline">Tagline</label>
              <input
                id="brandTagline"
                value={brandingForm.tagline}
                placeholder="Your journey starts here"
                onChange={(e) => setBrandingForm({ ...brandingForm, tagline: e.target.value })}
              />
            </div>
            {brandingForm.themeColor && (
              <div
                className="card"
                style={{
                  background: `linear-gradient(135deg, ${brandingForm.themeColor}, ${brandingForm.secondaryColor || '#7c3aed'})`,
                  color: '#fff',
                }}
              >
                <strong>Preview</strong>
                <p style={{ margin: '0.3rem 0 0' }}>{brandingForm.tagline || 'Your tagline here'}</p>
              </div>
            )}
          </form>
        </Modal>
      )}

      {suspendTarget && (
        <Modal
          title={`Suspend ${suspendTarget.name}?`}
          description="Suspension blocks new sales. Existing tickets remain valid. This is recorded in the moderation audit trail."
          onClose={() => setSuspendTarget(null)}
          footer={
            <>
              <button className="btn" onClick={() => setSuspendTarget(null)}>
                Cancel
              </button>
              <button
                className="btn danger"
                onClick={() => void confirmSuspend()}
                disabled={!suspendReason.trim()}
              >
                Suspend shop
              </button>
            </>
          }
        >
          <div className="field">
            <label htmlFor="suspend-reason">Reason (required, audit-trail)</label>
            <textarea
              id="suspend-reason"
              value={suspendReason}
              onChange={(e) => setSuspendReason(e.target.value)}
              rows={3}
              placeholder="e.g. Policy violation — blocking new sales"
            />
          </div>
        </Modal>
      )}
    </section>
  );
}
