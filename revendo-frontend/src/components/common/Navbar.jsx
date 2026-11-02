import React, { useState, useEffect } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';
import axiosInstance from '../../api/axiosInstance';

const CATEGORIES = [
  { label: 'Electronics',      value: 'Electronics' },
  { label: 'Furniture',        value: 'Furniture' },
  { label: 'Books',            value: 'Books' },
  { label: 'Home Appliances',  value: 'Home Appliances' },
  { label: 'Household Goods',  value: 'Household Goods' },
];

/**
 * Responsive, auth-aware navigation bar.
 * - Guest: logo, search, category links, Login, Register
 * - User:  logo, search, categories, Wishlist, Messages (unread badge), avatar dropdown
 * Requirements: 1.1, 1.2, 3.8, 3.9, 9.9
 */
function Navbar() {
  const { currentUser, logout } = useAuth();
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState('');
  const [unreadCount, setUnreadCount] = useState(0);
  const [menuOpen, setMenuOpen] = useState(false);

  /* Fetch unread messages count when authenticated */
  useEffect(() => {
    if (!currentUser) { setUnreadCount(0); return; }
    let cancelled = false;
    axiosInstance.get('/messages/unread-count')
      .then((res) => { if (!cancelled) setUnreadCount(res.data.unreadCount || 0); })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [currentUser]);

  const handleSearch = (e) => {
    e.preventDefault();
    if (keyword.trim()) {
      navigate(`/products?keyword=${encodeURIComponent(keyword.trim())}`);
    } else {
      navigate('/products');
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/');
    setMenuOpen(false);
  };

  return (
    <nav className="navbar navbar-expand-lg navbar-revendo sticky-top">
      <div className="container">
        {/* Brand */}
        <Link className="navbar-brand" to="/">
          🛍️ ReVendo
        </Link>

        {/* Mobile toggle */}
        <button
          className="navbar-toggler"
          type="button"
          aria-label="Toggle navigation"
          onClick={() => setMenuOpen((o) => !o)}
        >
          <span className="navbar-toggler-icon" />
        </button>

        <div className={`collapse navbar-collapse${menuOpen ? ' show' : ''}`}>
          {/* Search bar */}
          <form
            className="navbar-search d-flex mx-auto my-2 my-lg-0"
            style={{ width: '100%', maxWidth: '420px' }}
            onSubmit={handleSearch}
          >
            <input
              type="search"
              className="form-control form-control-sm"
              placeholder="Search listings…"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              aria-label="Search listings"
            />
            <button className="btn btn-primary btn-sm" type="submit">
              🔍
            </button>
          </form>

          {/* Category links */}
          <ul className="navbar-nav me-auto ms-3">
            {CATEGORIES.map((cat) => (
              <li className="nav-item" key={cat.value}>
                <NavLink
                  className="nav-link"
                  style={{ fontSize: '0.85rem', padding: '0.4rem 0.6rem' }}
                  to={`/products?category=${encodeURIComponent(cat.value)}`}
                  onClick={() => setMenuOpen(false)}
                >
                  {cat.label}
                </NavLink>
              </li>
            ))}
          </ul>

          {/* Right-hand controls */}
          <ul className="navbar-nav ms-auto align-items-center gap-1">
            {currentUser ? (
              <>
                {/* Wishlist */}
                <li className="nav-item">
                  <NavLink
                    className="nav-link"
                    to="/wishlist"
                    title="Wishlist"
                    onClick={() => setMenuOpen(false)}
                  >
                    ❤️ <span className="d-lg-none">Wishlist</span>
                  </NavLink>
                </li>

                {/* Messages with unread badge */}
                <li className="nav-item">
                  <NavLink
                    className="nav-link position-relative"
                    to="/messages"
                    title="Messages"
                    onClick={() => setMenuOpen(false)}
                  >
                    💬 <span className="d-lg-none">Messages</span>
                    {unreadCount > 0 && (
                      <span className="unread-badge" aria-label={`${unreadCount} unread`}>
                        {unreadCount > 99 ? '99+' : unreadCount}
                      </span>
                    )}
                  </NavLink>
                </li>

                {/* Sell button */}
                <li className="nav-item">
                  <NavLink
                    className="btn btn-primary btn-sm ms-1"
                    to="/sell"
                    onClick={() => setMenuOpen(false)}
                    style={{ fontSize: '0.85rem' }}
                  >
                    + Sell
                  </NavLink>
                </li>

                {/* User dropdown */}
                <li className="nav-item dropdown">
                  <button
                    className="btn btn-link nav-link dropdown-toggle d-flex align-items-center gap-1"
                    style={{ color: 'var(--rv-text)', textDecoration: 'none' }}
                    data-bs-toggle="dropdown"
                    aria-expanded="false"
                  >
                    <span
                      style={{
                        width: 30, height: 30, borderRadius: '50%',
                        background: 'var(--rv-accent-light)',
                        color: 'var(--rv-accent-dark)',
                        display: 'inline-flex', alignItems: 'center',
                        justifyContent: 'center', fontWeight: 700, fontSize: '0.85rem',
                      }}
                    >
                      {(currentUser.displayName || '?')[0].toUpperCase()}
                    </span>
                    <span className="d-none d-lg-inline" style={{ fontSize: '0.875rem', fontWeight: 500 }}>
                      {currentUser.displayName}
                    </span>
                  </button>
                  <ul className="dropdown-menu dropdown-menu-end shadow-sm">
                    <li>
                      <Link
                        className="dropdown-item"
                        to="/dashboard"
                        onClick={() => setMenuOpen(false)}
                      >
                        📊 Dashboard
                      </Link>
                    </li>
                    {currentUser.role === 'ROLE_ADMIN' && (
                      <li>
                        <Link
                          className="dropdown-item"
                          to="/admin"
                          onClick={() => setMenuOpen(false)}
                        >
                          ⚙️ Admin Panel
                        </Link>
                      </li>
                    )}
                    <li><hr className="dropdown-divider" /></li>
                    <li>
                      <button
                        className="dropdown-item text-danger"
                        onClick={handleLogout}
                      >
                        🚪 Logout
                      </button>
                    </li>
                  </ul>
                </li>
              </>
            ) : (
              <>
                <li className="nav-item">
                  <NavLink
                    className="nav-link"
                    to="/login"
                    onClick={() => setMenuOpen(false)}
                  >
                    Login
                  </NavLink>
                </li>
                <li className="nav-item">
                  <NavLink
                    className="btn btn-primary btn-sm"
                    to="/register"
                    onClick={() => setMenuOpen(false)}
                  >
                    Register
                  </NavLink>
                </li>
              </>
            )}
          </ul>
        </div>
      </div>
    </nav>
  );
}

export default Navbar;
