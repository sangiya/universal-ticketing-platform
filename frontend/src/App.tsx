import type { ReactNode } from 'react';
import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { useApi, type Role } from './context/ApiContext';
import { roleHome } from './pages/LoginPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ShopPage from './pages/ShopPage';
import SupportPage from './pages/SupportPage';
import TicketsPage from './pages/TicketsPage';
import AdminPage from './pages/AdminPage';
import MarketplacePage from './pages/MarketplacePage';
import OrdersPage from './pages/OrdersPage';
import AgentPortalPage from './pages/AgentPortalPage';
import AnalyticsPage from './pages/AnalyticsPage';
import SettingsPage from './pages/SettingsPage';
import PaymentPage from './pages/PaymentPage';

interface NavItem {
  to: string;
  label: string;
  roles?: Role[];
  guests?: boolean;
}

const NAV: NavItem[] = [
  { to: '/', label: 'Home', guests: true },
  { to: '/marketplace', label: 'Marketplace', roles: ['CUSTOMER', 'AGENT', 'ADMIN'], guests: true },
  { to: '/orders', label: 'My Orders', roles: ['CUSTOMER', 'ADMIN'] },
  { to: '/tickets', label: 'My Tickets', roles: ['CUSTOMER', 'AGENT'] },
  { to: '/agent', label: 'Seller Portal', roles: ['AGENT'] },
  { to: '/support', label: 'Support', roles: ['CUSTOMER'] },
  { to: '/analytics', label: 'Analytics', roles: ['ADMIN'] },
  { to: '/admin', label: 'Admin Dashboard', roles: ['ADMIN'] },
  { to: '/settings', label: 'Settings', roles: ['CUSTOMER', 'AGENT', 'ADMIN'] },
];

function RequireRole({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { authenticated, role } = useApi();
  if (!authenticated || !role) {
    return <Navigate to="/login" replace />;
  }
  if (!roles.includes(role)) {
    return <Navigate to={roleHome(role)} replace />;
  }
  return <>{children}</>;
}

export default function App() {
  const { authenticated, username, role, logout } = useApi();

  const visibleNav = NAV.filter((item) =>
    authenticated && role
      ? (item.roles ?? []).includes(role)
      : item.guests
  );

  return (
    <div className="app">
      <header className="topbar">
        <NavLink to="/" className="brand">
          TicketMesh
        </NavLink>
        <nav className="nav">
          {visibleNav.map((item) => (
            <NavLink key={item.to} to={item.to}>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="auth">
          {authenticated ? (
            <>
              <span className="user">{username}{role ? ` · ${role.toLowerCase()}` : ''}</span>
              <button className="link" onClick={logout}>
                Sign out
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login">Sign in</NavLink>
              <NavLink to="/register" className="btn primary">
                Sign up
              </NavLink>
            </>
          )}
        </div>
      </header>
      <main className="content">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/marketplace" element={<MarketplacePage />} />
          <Route path="/checkout/:orderRef" element={<RequireRole roles={['CUSTOMER', 'AGENT', 'ADMIN']}><PaymentPage /></RequireRole>} />
          <Route path="/orders" element={<RequireRole roles={['CUSTOMER', 'ADMIN']}><OrdersPage /></RequireRole>} />
          <Route path="/tickets" element={<RequireRole roles={['CUSTOMER', 'AGENT']}><TicketsPage /></RequireRole>} />
          <Route path="/agent" element={<RequireRole roles={['AGENT']}><AgentPortalPage /></RequireRole>} />
          <Route path="/support" element={<RequireRole roles={['CUSTOMER']}><SupportPage /></RequireRole>} />
          <Route path="/analytics" element={<RequireRole roles={['ADMIN']}><AnalyticsPage /></RequireRole>} />
          <Route path="/admin" element={<RequireRole roles={['ADMIN']}><AdminPage /></RequireRole>} />
          <Route path="/settings" element={<RequireRole roles={['CUSTOMER', 'AGENT', 'ADMIN']}><SettingsPage /></RequireRole>} />
          <Route path="/shop/:slug" element={<ShopPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <footer className="footer">
        <span>TicketMesh — All your tickets, one platform.</span>
      </footer>
    </div>
  );
}
