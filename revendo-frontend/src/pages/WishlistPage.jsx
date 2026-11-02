import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import ProductCard from '../components/common/ProductCard';
import LoadingSpinner from '../components/common/LoadingSpinner';
import useToast from '../hooks/useToast';
import wishlistService from '../services/wishlistService';
import { formatCurrency } from '../utils/formatters';

const BACKEND = 'http://localhost:8080';
const toAbs = (url) => (!url ? '' : url.startsWith('http') ? url : `${BACKEND}${url}`);

function WishlistPage() {
  const { showToast } = useToast();
  const [items, setItems]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]   = useState(null);

  const fetchWishlist = useCallback(async () => {
    setLoading(true); setError(null);
    try {
      const res = await wishlistService.getWishlist();
      setItems(res.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not load your wishlist.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchWishlist(); }, [fetchWishlist]);

  const handleRemove = async (listingId, title) => {
    try {
      await wishlistService.removeFromWishlist(listingId);
      setItems((prev) => prev.filter((i) => i.listingId !== listingId));
      showToast(`Removed "${title}" from wishlist.`);
    } catch {
      showToast('Could not remove item.', 'error');
    }
  };

  if (loading) return <div className="container py-5"><LoadingSpinner /></div>;

  if (error) {
    return (
      <div className="container py-5 text-center">
        <div className="alert alert-warning">{error}</div>
        <button className="btn btn-outline-primary" onClick={fetchWishlist}>Retry</button>
      </div>
    );
  }

  return (
    <div style={{ background: 'var(--rv-surface)', minHeight: '100vh', paddingBottom: 48 }}>
      <div style={{ background: 'linear-gradient(135deg, var(--rv-accent-dark) 0%, var(--rv-accent) 100%)', padding: '36px 0 28px', color: '#fff' }}>
        <div className="container">
          <h1 style={{ fontWeight: 700, fontSize: '1.8rem', margin: 0 }}>❤️ My Wishlist</h1>
          <p style={{ opacity: 0.85, marginTop: 6, marginBottom: 0 }}>
            {items.length} saved item{items.length !== 1 ? 's' : ''}
          </p>
        </div>
      </div>

      <div className="container py-4">
        {items.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">🤍</div>
            <h5>Your wishlist is empty</h5>
            <p className="text-muted mb-3">Browse listings and tap the heart icon to save items here.</p>
            <Link to="/products" className="btn btn-primary">Browse Listings</Link>
          </div>
        ) : (
          <div className="row g-3">
            {items.map((item) => {
              const isGone = item.listingStatus === 'Sold' || item.listingStatus === 'Rented';
              return (
                <div key={item.wishlistItemId} className="col-sm-6 col-lg-4 col-xl-3">
                  <div className="product-card position-relative">
                    {isGone && (
                      <span className="badge position-absolute" style={{ top: 10, left: 10, zIndex: 2, background: '#374151', color: '#fff', fontSize: '0.72rem' }}>
                        No Longer Available
                      </span>
                    )}
                    {item.imageUrls?.length > 0 ? (
                      <img src={toAbs(item.imageUrls[0])} alt={item.title} className="card-img-top"
                           onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'flex'; }} />
                    ) : null}
                    <div className="img-placeholder" style={{ display: item.imageUrls?.length > 0 ? 'none' : 'flex' }}>🖼️</div>
                    <div className="card-body">
                      <h6 className="card-title">{item.title}</h6>
                      <div className="d-flex gap-1 mb-2 flex-wrap">
                        {item.condition && <span className="badge bg-light text-dark border" style={{ fontSize: '0.7rem' }}>{item.condition}</span>}
                        {item.category && <span className="text-muted" style={{ fontSize: '0.75rem' }}>{item.category}</span>}
                      </div>
                      <div className="card-footer-area d-flex justify-content-between align-items-center">
                        <span className="card-price">{formatCurrency(item.price)}</span>
                        <div className="d-flex gap-1">
                          <Link to={`/products/${item.listingId}`} className="btn btn-primary btn-sm" style={{ fontSize: '0.78rem' }}>View →</Link>
                          <button className="btn btn-outline-danger btn-sm" style={{ fontSize: '0.78rem' }}
                                  onClick={() => handleRemove(item.listingId, item.title)} title="Remove from wishlist">✕</button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

export default WishlistPage;
