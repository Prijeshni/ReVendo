import React, { useEffect, useState, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import ProductCard from '../components/common/ProductCard';
import LoadingSpinner from '../components/common/LoadingSpinner';
import productService from '../services/productService';

// ----------------------------------------------------------------
// Category data
// ----------------------------------------------------------------
const CATEGORIES = [
  { label: 'Electronics',     icon: '💻', value: 'Electronics',     color: '#ede9fe' },
  { label: 'Furniture',       icon: '🛋️', value: 'Furniture',       color: '#fce7f3' },
  { label: 'Books',           icon: '📚', value: 'Books',           color: '#dbeafe' },
  { label: 'Home Appliances', icon: '🏠', value: 'Home Appliances', color: '#dcfce7' },
  { label: 'Household Goods', icon: '🧺', value: 'Household Goods', color: '#fef9c3' },
];

// ----------------------------------------------------------------
// How It Works steps
// ----------------------------------------------------------------
const HOW_STEPS = [
  { n: '1', icon: '📷', title: 'List Your Item', desc: 'Take a few photos, write a description, set your price and publish in minutes.' },
  { n: '2', icon: '💬', title: 'Connect with Buyers', desc: 'Chat directly with interested buyers, negotiate offers and agree on a deal.' },
  { n: '3', icon: '🤝', title: 'Complete the Sale', desc: 'Finalise the transaction securely inside the platform and get paid.' },
];

// ----------------------------------------------------------------
// Benefits
// ----------------------------------------------------------------
const BENEFITS = [
  { icon: '🔒', title: 'Secure Transactions', desc: 'Every deal is tracked and protected through our platform.' },
  { icon: '💸', title: 'Great Deals', desc: 'Buy pre-owned goods at a fraction of the retail price.' },
  { icon: '🌱', title: 'Sustainable Choice', desc: 'Give items a second life and reduce waste together.' },
  { icon: '⚡', title: 'Fast & Easy', desc: 'List or browse in seconds — no complicated setup required.' },
  { icon: '🎯', title: 'Smart Search', desc: 'Filter by category, condition, price and listing type.' },
  { icon: '📱', title: 'Works Everywhere', desc: 'Fully responsive on desktop, tablet and mobile.' },
];

// ----------------------------------------------------------------
// Component
// ----------------------------------------------------------------
function HomePage() {
  const navigate = useNavigate();
  const [featured, setFeatured]   = useState([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState(null);
  const [searchKeyword, setSearch] = useState('');

  const loadFeatured = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await productService.getListings({ size: 8, sort: '' });
      setFeatured(res.data.content || []);
    } catch (err) {
      // If the backend is not running, show an empty state without crashing
      setError('Could not load featured listings. Start the backend to see products.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadFeatured(); }, [loadFeatured]);

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchKeyword.trim()) {
      navigate(`/products?keyword=${encodeURIComponent(searchKeyword.trim())}`);
    } else {
      navigate('/products');
    }
  };

  const goToCategory = (cat) =>
    navigate(`/products?category=${encodeURIComponent(cat)}`);

  return (
    <>
      {/* ====================================================
          HERO
          ==================================================== */}
      <section className="hero-section">
        <div className="container">
          <div className="row align-items-center gy-4">
            <div className="col-lg-6">
              <span className="hero-badge">🛍️ P2P Marketplace</span>
              <h1>Give Pre-Owned Products<br />a New Life</h1>
              <p className="hero-sub mt-3 mb-4">
                Buy, sell and rent second-hand electronics, furniture, books,
                home appliances and household goods — directly between people.
              </p>

              {/* Inline search */}
              <form className="d-flex mb-4" onSubmit={handleSearch} style={{ maxWidth: 480 }}>
                <input
                  type="search"
                  className="form-control form-control-lg"
                  placeholder="Search for anything…"
                  value={searchKeyword}
                  onChange={(e) => setSearch(e.target.value)}
                  style={{
                    borderRadius: '12px 0 0 12px',
                    border: 'none',
                    background: 'rgba(255,255,255,0.95)',
                    color: '#1e1b4b',
                  }}
                  aria-label="Search products"
                />
                <button
                  className="btn btn-lg"
                  type="submit"
                  style={{
                    borderRadius: '0 12px 12px 0',
                    background: '#fff',
                    color: 'var(--rv-accent)',
                    fontWeight: 700,
                    border: 'none',
                    padding: '0 20px',
                  }}
                >
                  Search
                </button>
              </form>

              <div className="d-flex gap-3 flex-wrap">
                <Link
                  to="/products"
                  className="btn btn-lg"
                  style={{
                    background: '#fff',
                    color: 'var(--rv-accent)',
                    fontWeight: 700,
                    borderRadius: 12,
                    padding: '0.6rem 1.6rem',
                    boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                  }}
                >
                  Browse Products
                </Link>
                <Link
                  to="/sell"
                  className="btn btn-lg"
                  style={{
                    background: 'rgba(255,255,255,0.15)',
                    color: '#fff',
                    fontWeight: 700,
                    borderRadius: 12,
                    border: '2px solid rgba(255,255,255,0.6)',
                    padding: '0.6rem 1.6rem',
                  }}
                >
                  + Sell Your Item
                </Link>
              </div>

              {/* Stat strip */}
              <div className="hero-stats d-flex gap-4 mt-4 flex-wrap">
                <div><span>10K+</span><small>Active Listings</small></div>
                <div style={{ opacity: 0.3 }}>|</div>
                <div><span>5K+</span><small>Happy Users</small></div>
                <div style={{ opacity: 0.3 }}>|</div>
                <div><span>5</span><small>Categories</small></div>
              </div>
            </div>

            <div className="col-lg-6 text-center d-none d-lg-block">
              {/* Decorative illustration — CSS shapes */}
              <div style={{ position: 'relative', height: 320 }}>
                <div style={{
                  position: 'absolute', inset: 0, display: 'flex',
                  alignItems: 'center', justifyContent: 'center', gap: 16, flexWrap: 'wrap',
                }}>
                  {['💻','📱','🛋️','📚','🏠','🎮','⌚','🎸'].map((e, i) => (
                    <div
                      key={i}
                      style={{
                        width: 72, height: 72,
                        background: 'rgba(255,255,255,0.15)',
                        backdropFilter: 'blur(4px)',
                        borderRadius: 16,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontSize: '2rem',
                        border: '1px solid rgba(255,255,255,0.25)',
                        animation: `float ${2.5 + i * 0.3}s ease-in-out infinite alternate`,
                      }}
                    >
                      {e}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ====================================================
          CATEGORIES
          ==================================================== */}
      <section className="py-5" style={{ background: 'var(--rv-surface)' }}>
        <div className="container">
          <div className="text-center mb-4">
            <p className="section-eyebrow">Shop by category</p>
            <h2 className="section-title">What are you looking for?</h2>
          </div>
          <div className="row g-3 justify-content-center">
            {CATEGORIES.map((cat) => (
              <div key={cat.value} className="col-6 col-sm-4 col-md-3 col-lg-2">
                <button
                  className="category-card w-100 border-0"
                  onClick={() => goToCategory(cat.value)}
                  style={{ background: cat.color }}
                >
                  <div className="cat-icon">{cat.icon}</div>
                  <div className="cat-label">{cat.label}</div>
                </button>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ====================================================
          FEATURED PRODUCTS
          ==================================================== */}
      <section className="py-5">
        <div className="container">
          <div className="d-flex justify-content-between align-items-end mb-4 flex-wrap gap-2">
            <div>
              <p className="section-eyebrow mb-1">Fresh listings</p>
              <h2 className="section-title mb-0">Featured Products</h2>
            </div>
            <Link to="/products" className="btn btn-outline-primary btn-sm">
              View All →
            </Link>
          </div>

          {loading && <LoadingSpinner />}

          {!loading && error && (
            <div className="alert alert-warning d-flex align-items-center gap-2" role="alert">
              <span>⚠️</span>
              <span>{error}</span>
            </div>
          )}

          {!loading && !error && featured.length === 0 && (
            <div className="empty-state">
              <div className="empty-icon">📦</div>
              <h5>No listings yet</h5>
              <p className="text-muted mb-3">Be the first to list an item!</p>
              <Link to="/sell" className="btn btn-primary">Sell Something</Link>
            </div>
          )}

          {!loading && !error && featured.length > 0 && (
            <div className="row g-3">
              {featured.map((listing) => (
                <div key={listing.id} className="col-sm-6 col-md-4 col-lg-3">
                  <ProductCard listing={listing} />
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* ====================================================
          HOW IT WORKS
          ==================================================== */}
      <section className="py-5" style={{ background: 'var(--rv-accent-light)' }}>
        <div className="container">
          <div className="text-center mb-5">
            <p className="section-eyebrow">Simple steps</p>
            <h2 className="section-title">How ReVendo Works</h2>
            <p className="text-muted mt-2" style={{ maxWidth: 460, margin: '0.5rem auto 0' }}>
              Sell your unused items or find great deals in three easy steps.
            </p>
          </div>
          <div className="row g-4">
            {HOW_STEPS.map((step) => (
              <div key={step.n} className="col-md-4">
                <div className="step-card">
                  <div className="step-number">{step.n}</div>
                  <div style={{ fontSize: '2rem', marginBottom: '0.75rem' }}>{step.icon}</div>
                  <h5>{step.title}</h5>
                  <p className="text-muted mb-0" style={{ fontSize: '0.9rem' }}>{step.desc}</p>
                </div>
              </div>
            ))}
          </div>

          <div className="text-center mt-5">
            <Link to="/register" className="btn btn-primary btn-lg px-5">
              Get Started — It's Free
            </Link>
          </div>
        </div>
      </section>

      {/* ====================================================
          BENEFITS
          ==================================================== */}
      <section className="py-5">
        <div className="container">
          <div className="text-center mb-5">
            <p className="section-eyebrow">Why ReVendo?</p>
            <h2 className="section-title">Built for Smart Shoppers</h2>
          </div>
          <div className="row g-4">
            {BENEFITS.map((b) => (
              <div key={b.title} className="col-md-6 col-lg-4">
                <div className="benefit-card">
                  <div className="benefit-icon">{b.icon}</div>
                  <div>
                    <h6 style={{ fontWeight: 700, marginBottom: '0.25rem' }}>{b.title}</h6>
                    <p className="text-muted mb-0" style={{ fontSize: '0.875rem' }}>{b.desc}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ====================================================
          CTA BANNER
          ==================================================== */}
      <section
        style={{
          background: 'linear-gradient(135deg, var(--rv-accent-dark) 0%, var(--rv-accent) 100%)',
          padding: '64px 0',
          color: '#fff',
          textAlign: 'center',
        }}
      >
        <div className="container">
          <h2 style={{ fontWeight: 700, fontSize: '2rem', marginBottom: '0.75rem' }}>
            Ready to declutter? Start selling today.
          </h2>
          <p style={{ opacity: 0.85, marginBottom: '2rem', fontSize: '1.05rem' }}>
            List your first item in under 2 minutes. No fees to get started.
          </p>
          <div className="d-flex gap-3 justify-content-center flex-wrap">
            <Link
              to="/sell"
              className="btn btn-lg"
              style={{
                background: '#fff', color: 'var(--rv-accent)',
                fontWeight: 700, borderRadius: 12, padding: '0.65rem 2rem',
              }}
            >
              + List an Item
            </Link>
            <Link
              to="/products"
              className="btn btn-lg"
              style={{
                background: 'rgba(255,255,255,0.15)', color: '#fff',
                border: '2px solid rgba(255,255,255,0.5)',
                fontWeight: 600, borderRadius: 12, padding: '0.65rem 2rem',
              }}
            >
              Browse Listings
            </Link>
          </div>
        </div>
      </section>

      {/* Floating keyframe for hero illustrations */}
      <style>{`
        @keyframes float {
          from { transform: translateY(0px); }
          to   { transform: translateY(-10px); }
        }
      `}</style>
    </>
  );
}

export default HomePage;
