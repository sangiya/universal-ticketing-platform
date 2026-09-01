import type { ReactNode } from 'react';
import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { useApi, type Role } from './context/ApiContext';
import { roleHome } from './pages/LoginPage';
import HomePage from './pages/HomePage';
import DashboardPage from './pages/DashboardPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ShopPage from './pages/ShopPage';
import SupportPage from './pages/SupportPage';
import TicketsPage from './pages/TicketsPage';
import AdminPage from './pages/AdminPage';
import MarketplacePage from './pages/MarketplacePage';
import MoviesPage from './pages/MoviesPage';
import OrdersPage from './pages/OrdersPage';
import AgentPortalPage from './pages/AgentPortalPage';
import AnalyticsPage from './pages/AnalyticsPage';
import SettingsPage from './pages/SettingsPage';
import PaymentPage from './pages/PaymentPage';
import ProductDetailPage from './pages/ProductDetailPage';
import TicketDetailPage from './pages/TicketDetailPage';
import { ToastProvider } from './components/Toast';

interface NavItem {
  to: string;
  label: string;
  roles?: Role[];
  guests?: boolean;
  icon: string;
}

const NAV: NavItem[] = [
  { to: '/dashboard', label: 'Dashboard', roles: ['CUSTOMER', 'AGENT', 'ADMIN'], icon: '🏠' },
  { to: '/', label: 'Home', guests: true, icon: '🌐' },
  { to: '/marketplace', label: 'Marketplace', roles: ['CUSTOMER', 'AGENT', 'ADMIN'], guests: true, icon: '🔍' },
  { to: '/movies', label: 'Movies', guests: true, icon: '🎬' },
  { to: '/orders', label: 'My Orders', roles: ['CUSTOMER', 'AGENT', 'ADMIN'], icon: '🛒' },
  { to: '/tickets', label: 'My Tickets', roles: ['CUSTOMER', 'AGENT', 'ADMIN'], icon: '🎟️' },
  { to: '/agent', label: 'Seller Portal', roles: ['AGENT', 'ADMIN'], icon: '🏪' },
  { to: '/analytics', label: 'Analytics', roles: ['ADMIN', 'AGENT'], icon: '📊' },
  { to: '/admin', label: 'Admin', roles: ['ADMIN'], icon: '⚙️' },
  { to: '/support', label: 'Support', roles: ['CUSTOMER', 'AGENT', 'ADMIN'], icon: '💬' },
  { to: '/settings', label: 'Settings', roles: ['CUSTOMER', 'AGENT', 'ADMIN'], icon: '👤' },
];

function RequireRole({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { authenticated, role } = useApi();
  if (!authenticated || !role) return <Navigate to="/login" replace />;
  if (!roles.includes(role)) return <Navigate to={roleHome(role)} replace />;
  return <>{children}</>;
}

function getInitial(name?: string | null): string {
  if (!name) return '?';
  return name.trim().charAt(0).toUpperCase();
}

export default function App() {
  const { authenticated, username, role, logout } = useApi();
  const visibleNav = NAV.filter((item) =>
    authenticated && role ? (item.roles ?? []).includes(role) : item.guests,
  );

  return (
    <ToastProvider>
      <div className="app">
        <header className="topbar">
          <NavLink to={authenticated ? '/dashboard' : '/'} className="brand" aria-label="TicketMesh home">
            <span className="brand-mark">TM</span>
            <span className="brand-text">TicketMesh</span>
          </NavLink>
          <nav className="nav" aria-label="Primary">
            {visibleNav.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) => (isActive ? 'active' : '')}
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="auth">
            {authenticated ? (
              <>
                <span className="user-chip" data-username={username ?? ''} data-initial={getInitial(username)}>
                  <span>{username}</span>
                </span>
                {role && <span className={`badge role-${role.toLowerCase()}`}>{role}</span>}
                <button className="btn ghost sm" onClick={logout}>
                  Sign out
                </button>
              </>
            ) : (
              <>
                <NavLink to="/login" className="btn ghost sm">
                  Sign in
                </NavLink>
                <NavLink to="/register" className="btn primary sm">
                  Get started
                </NavLink>
              </>
            )}
          </div>
        </header>

        <main className="content">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route
              path="/dashboard"
              element={
                <RequireRole roles={['CUSTOMER', 'AGENT', 'ADMIN']}>
                  <DashboardPage />
                </RequireRole>
              }
            />
            <Route path="/marketplace" element={<MarketplacePage />} />
            <Route path="/movies" element={<MoviesPage />} />
            <Route
              path="/checkout/:orderRef"
              element={
                <RequireRole roles={['CUSTOMER', 'AGENT', 'ADMIN']}>
                  <PaymentPage />
                </RequireRole>
              }
            />
            <Route
              path="/orders"
              element={
                <RequireRole roles={['CUSTOMER', 'AGENT', 'ADMIN']}>
                  <OrdersPage />
                </RequireRole>
              }
            />
            <Route
              path="/tickets"
              element={
                <RequireRole roles={['CUSTOMER', 'AGENT', 'ADMIN']}>
                  <TicketsPage />
                </RequireRole>
              }
            />
            <Route
              path="/agent"
              element={
                <RequireRole roles={['AGENT', 'ADMIN']}>
                  <AgentPortalPage />
                </RequireRole>
              }
            />
            <Route
              path="/support"
              element={
                <RequireRole roles={['CUSTOMER', 'AGENT', 'ADMIN']}>
                  <SupportPage />
                </RequireRole>
              }
            />
            <Route
              path="/analytics"
              element={
                <RequireRole roles={['ADMIN', 'AGENT']}>
                  <AnalyticsPage />
                </RequireRole>
              }
            />
            <Route
              path="/admin"
              element={
                <RequireRole roles={['ADMIN']}>
                  <AdminPage />
                </RequireRole>
              }
            />
            <Route
              path="/settings"
              element={
                <RequireRole roles={['CUSTOMER', 'AGENT', 'ADMIN']}>
                  <SettingsPage />
                </RequireRole>
              }
            />
            <Route path="/shop/:slug" element={<ShopPage />} />
            <Route path="/product/:id" element={<ProductDetailPage />} />
            <Route
              path="/ticket/:id"
              element={
                <RequireRole roles={['CUSTOMER', 'AGENT', 'ADMIN']}>
                  <TicketDetailPage />
                </RequireRole>
              }
            />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>

        <nav className="bottom-nav" aria-label="Mobile">
          <NavLink to={authenticated ? '/dashboard' : '/'} end>
            <span className="ico">🏠</span>{authenticated ? 'Home' : 'Home'}
          </NavLink>
          <NavLink to="/marketplace">
            <span className="ico">🔍</span>Search
          </NavLink>
          <NavLink to="/movies">
            <span className="ico">🎬</span>Movies
          </NavLink>
          <NavLink to="/tickets">
            <span className="ico">🎟️</span>Tickets
          </NavLink>
          <NavLink to="/orders">
            <span className="ico">🛒</span>Orders
          </NavLink>
          <NavLink to="/settings">
            <span className="ico">👤</span>Profile
          </NavLink>
        </nav>

        <footer className="footer">
          <div className="footer-grid">
            <div>
              <div className="footer-brand">
                <span className="brand-mark">TM</span>
                <span>TicketMesh</span>
              </div>
              <p className="tagline">
                The universal ticketing platform — bus, train, flight, movies,
                events, sports, ferries and attractions in one place. Built for
                customers, agents and operators across the world.
              </p>
            </div>
            <div>
              <h4>Platform</h4>
              <ul>
                <li><a href="/marketplace">Browse marketplace</a></li>
                <li><a href="/register">Become an agent</a></li>
                <li><a href="/admin">Admin portal</a></li>
                <li><a href="/analytics">Analytics</a></li>
              </ul>
            </div>
            <div>
              <h4>Account</h4>
              <ul>
                <li><a href="/login">Sign in</a></li>
                <li><a href="/settings">My settings</a></li>
                <li><a href="/orders">My orders</a></li>
                <li><a href="/tickets">My tickets</a></li>
              </ul>
            </div>
            <div>
              <h4>Support</h4>
              <ul>
                <li><a href="/support">Help center</a></li>
                <li><a href="/support">Open a ticket</a></li>
                <li><a href="/settings">Traveler profiles</a></li>
                <li><a href="/settings">Security &amp; privacy</a></li>
              </ul>
            </div>
          </div>
          <div className="footer-bottom">
            <span>© 2026 TicketMesh. All rights reserved.</span>
            <span>v1.0.0 · Multi-tenant · PWA-ready</span>
          </div>
        </footer>
      </div>
    </ToastProvider>
  );
}
