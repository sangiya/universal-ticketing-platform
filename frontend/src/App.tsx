import { NavLink, Route, Routes } from 'react-router-dom';
import { useApi } from './context/ApiContext';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ShopPage from './pages/ShopPage';
import SupportPage from './pages/SupportPage';
import TicketsPage from './pages/TicketsPage';
import AdminPage from './pages/AdminPage';

export default function App() {
  const { authenticated, username, logout } = useApi();

  return (
    <div className="app">
      <header className="topbar">
        <NavLink to="/" className="brand">
          TicketMesh
        </NavLink>
        <nav className="nav">
          <NavLink to="/">Home</NavLink>
          <NavLink to="/tickets">My Tickets</NavLink>
          <NavLink to="/support">Support</NavLink>
          <NavLink to="/admin">Admin</NavLink>
        </nav>
        <div className="auth">
          {authenticated ? (
            <>
              <span className="user">{username}</span>
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
          <Route path="/shop/:slug" element={<ShopPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/tickets" element={<TicketsPage />} />
          <Route path="/support" element={<SupportPage />} />
          <Route path="/admin" element={<AdminPage />} />
        </Routes>
      </main>
      <footer className="footer">
        <span>TicketMesh — All your tickets, one platform.</span>
      </footer>
    </div>
  );
}
