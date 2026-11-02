import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import ImageCarousel from '../components/listings/ImageCarousel';
import LoadingSpinner from '../components/common/LoadingSpinner';
import useAuth from '../hooks/useAuth';
import useToast from '../hooks/useToast';
import productService from '../services/productService';
import wishlistService from '../services/wishlistService';
import messagingService from '../services/messagingService';
import { formatDate } from '../utils/formatters';

// Backend base URL for images served at /images/**
const BACKEND = 'http://localhost:8080';

/** Prefix relative image paths (/images/…) with the backend URL. */
const toAbsoluteUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http')) return url;
  return `${BACKEND}${url}`;
};

const CONDITION_STYLE = {
  'New':      { bg: '#dcfce7', color: '#166534', icon: '✨' },
  'Like New': { bg: '#d1fae5', color: '#065f46', icon: '👍' },
  'Good':     { bg: '#dbeafe', color: '#1e40af', icon: '👌' },
  'Fair':     { bg: '#fef9c3', color: '#854d0e', icon: '🔧' },
  'Poor':     { bg: '#fee2e2', color: '#991b1b', icon: '⚠️' },
};

const CAT_ICONS = {
  Electronics: '💻', Furniture: '🛋️',
  Books: '📚', 'Home Appliances': '🏠', 'Household Goods': '🧺',
};

const INR = (n) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);

// ── RentModal ────────────────────────────────────────────────────────
function RentModal({ listing, onClose, onConfirm, loading }) {
  const [duration, setDuration] = useState('');
  const unitLabel = {
    PER_DAY: 'day(s)', PER_WEEK: 'week(s)', PER_MONTH: 'month(s)',
  }[listing?.rentalPeriodUnit] || 'period(s)';

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 2000,
      background: 'rgba(0,0,0,0.5)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      padding: 16,
    }}>
      <div style={{ background: '#fff', borderRadius: 16, padding: '28px 28px 24px', maxWidth: 420, width: '100%' }}>
        <h5 style={{ fontWeight: 700, marginBottom: 12 }}>🔄 Rent This Item</h5>
        <p style={{ color: 'var(--rv-text-muted)', fontSize: '0.875rem', marginBottom: 16 }}>
          <strong>{listing?.title}</strong> — {INR(listing?.price)} per {unitLabel.replace('(s)', '')}
        </p>
        <label style={{ fontWeight: 600, fontSize: '0.85rem', display: 'block', marginBottom: 6 }}>
          Duration (1–90 {unitLabel})
        </label>
        <input
          type="number" min={1} max={90}
          className="form-control mb-3"
          value={duration}
          onChange={(e) => setDuration(e.target.value)}
          placeholder="e.g. 7"
        />
        {duration && !isNaN(Number(duration)) && Number(duration) >= 1 && (
          <p style={{ fontSize: '0.85rem', color: 'var(--rv-accent)', fontWeight: 600, marginBottom: 16 }}>
            Total: {INR(parseFloat(listing?.price || 0) * Number(duration))}
          </p>
        )}
        <div className="d-flex gap-2">
          <button className="btn btn-outline-secondary flex-fill" onClick={onClose} disabled={loading}>
            Cancel
          </button>
          <button
            className="btn btn-primary flex-fill"
            onClick={() => onConfirm(Number(duration))}
            disabled={loading || !duration || isNaN(Number(duration)) || Number(duration) < 1 || Number(duration) > 90}
          >
            {loading ? <><span className="spinner-border spinner-border-sm me-2" />Confirming…</> : 'Confirm Rental'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Main component ───────────────────────────────────────────────────
function ProductDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { currentUser } = useAuth();
  const { showToast } = useToast();

  const [listing,     setListing]    = useState(null);
  const [loading,     setLoading]    = useState(true);
  const [error,       setError]      = useState(null);
  const [wishlisted,  setWishlisted] = useState(false);
  const [wlLoading,   setWlLoading]  = useState(false);
  const [txLoading,   setTxLoading]  = useState(false);
  const [msgLoading,  setMsgLoading] = useState(false);
  const [showRentModal, setShowRentModal] = useState(false);

  // ── Fetch listing ─────────────────────────────────────────────────
  const fetchListing = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await productService.getListingById(id);
      setListing(res.data);
    } catch (err) {
      const status = err.response?.status;
      if (status === 404) {
        setError('not_found');
      } else {
        setError(err.response?.data?.message || 'Failed to load listing.');
      }
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { fetchListing(); }, [fetchListing]);

  // ── Fetch wishlist status once listing + user are loaded ──────────
  useEffect(() => {
    if (!currentUser || !listing) return;
    wishlistService.isWishlisted(listing.id)
      .then((res) => setWishlisted(res.data?.wishlisted ?? false))
      .catch(() => {});
  }, [currentUser, listing]);

  // ── Derived ───────────────────────────────────────────────────────
  const isOwner     = currentUser && listing && String(listing.sellerId) === String(currentUser.userId);
  const isActive    = listing?.status === 'Active';
  const isSale      = listing?.listingType === 'FOR_SALE';
  const isRent      = listing?.listingType === 'FOR_RENT';
  const isSold      = listing?.status === 'Sold';
  const isRented    = listing?.status === 'Rented';
  const imageUrls   = (listing?.imageUrls || []).map(toAbsoluteUrl);
  const condStyle   = CONDITION_STYLE[listing?.condition] || {};

  // ── Wishlist toggle ───────────────────────────────────────────────
  const handleWishlist = async () => {
    if (!currentUser) { navigate('/login'); return; }
    setWlLoading(true);
    try {
      if (wishlisted) {
        await wishlistService.removeFromWishlist(listing.id);
        setWishlisted(false);
        showToast('Removed from wishlist.');
      } else {
        await wishlistService.addToWishlist(listing.id);
        setWishlisted(true);
        showToast('Added to wishlist! ❤️');
      }
    } catch {
      showToast('Could not update wishlist.', 'error');
    } finally {
      setWlLoading(false);
    }
  };

  // ── Contact Seller ────────────────────────────────────────────────
  const handleContactSeller = async () => {
    if (!currentUser) { navigate('/login'); return; }
    setMsgLoading(true);
    try {
      const res = await messagingService.getOrCreateConversation(listing.id);
      const convId = res.data?.conversationId;
      navigate(`/messages/${convId}`);
    } catch (err) {
      showToast(err.response?.data?.message || 'Could not start conversation.', 'error');
    } finally {
      setMsgLoading(false);
    }
  };

  // ── Buy Now → Create Purchase Request ─────────────────────────────
  const handleBuyNow = async () => {
    if (!currentUser) { navigate('/login'); return; }
    setTxLoading(true);
    try {
      await productService.createPurchaseRequest(listing.id);
      showToast('Purchase request sent! The seller will review it shortly.');
    } catch (err) {
      const msg = err.response?.data?.message || 'Could not send request.';
      showToast(msg, 'error');
    } finally {
      setTxLoading(false);
    }
  };

  // ── Rent Now → Create Rental Request (PENDING) ───────────────────
  const handleRentConfirm = async (duration) => {
    if (!currentUser) { navigate('/login'); return; }
    setTxLoading(true);
    try {
      await productService.createRentalRequest(listing.id, duration);
      showToast('Rental request sent! The seller will review it shortly.');
      setShowRentModal(false);
    } catch (err) {
      showToast(err.response?.data?.message || 'Could not send rental request.', 'error');
    } finally {
      setTxLoading(false);
    }
  };

  // ── Render states ─────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="container py-5">
        <LoadingSpinner text="Loading product details…" />
      </div>
    );
  }

  if (error === 'not_found') {
    return (
      <div className="container py-5 text-center">
        <div style={{ fontSize: '4rem', marginBottom: 16 }}>🔍</div>
        <h3>Product Not Found</h3>
        <p className="text-muted mb-4">
          This listing may have been removed or the URL is incorrect.
        </p>
        <Link to="/products" className="btn btn-primary">Browse All Listings</Link>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container py-5 text-center">
        <div className="alert alert-danger d-inline-flex align-items-center gap-2">
          <span>⚠️</span><span>{error}</span>
        </div>
        <div className="mt-3">
          <button className="btn btn-outline-primary me-2" onClick={fetchListing}>Retry</button>
          <Link to="/products" className="btn btn-outline-secondary">Browse Listings</Link>
        </div>
      </div>
    );
  }

  // ── Full page ────────────────────────────────────────────────────
  return (
    <div style={{ background: 'var(--rv-surface)', minHeight: '100vh', paddingBottom: 48 }}>

      {showRentModal && (
        <RentModal
          listing={listing}
          onClose={() => setShowRentModal(false)}
          onConfirm={handleRentConfirm}
          loading={txLoading}
        />
      )}

      {/* Breadcrumb bar */}
      <div style={{ background: '#fff', borderBottom: '1px solid var(--rv-border)', padding: '10px 0' }}>
        <div className="container">
          <nav aria-label="breadcrumb">
            <ol className="breadcrumb mb-0" style={{ fontSize: '0.82rem' }}>
              <li className="breadcrumb-item"><Link to="/">Home</Link></li>
              <li className="breadcrumb-item"><Link to="/products">Listings</Link></li>
              <li className="breadcrumb-item active" style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {listing.title}
              </li>
            </ol>
          </nav>
        </div>
      </div>

      <div className="container py-4">
        <div className="row g-4">

          {/* ── Left: Images ─────────────────────────────────────── */}
          <div className="col-lg-7">
            <ImageCarousel images={imageUrls} alt={listing.title} />

            {/* Description (desktop: below images) */}
            <div
              className="mt-4 d-none d-lg-block"
              style={{ background: '#fff', borderRadius: 14, padding: '24px', border: '1px solid var(--rv-border)' }}
            >
              <h5 style={{ fontWeight: 700, marginBottom: 12, color: 'var(--rv-accent-dark)' }}>
                📝 Description
              </h5>
              <p style={{ whiteSpace: 'pre-wrap', fontSize: '0.9rem', lineHeight: 1.7, color: 'var(--rv-text)', margin: 0 }}>
                {listing.description || 'No description provided.'}
              </p>
            </div>
          </div>

          {/* ── Right: Details & Actions ─────────────────────────── */}
          <div className="col-lg-5">
            <div style={{ position: 'sticky', top: 80 }}>

              {/* Status badge */}
              {(isSold || isRented) && (
                <span
                  className="badge mb-3 d-inline-block"
                  style={{
                    background: isSold ? '#374151' : '#d97706',
                    color: '#fff', fontSize: '0.85rem', padding: '6px 14px', borderRadius: 8,
                  }}
                >
                  {listing.status}
                </span>
              )}

              {/* Title */}
              <h1 style={{ fontWeight: 700, fontSize: '1.5rem', lineHeight: 1.3, marginBottom: 8, color: 'var(--rv-text)' }}>
                {listing.title}
              </h1>

              {/* Meta chips */}
              <div className="d-flex flex-wrap gap-2 mb-3">
                {listing.category && (
                  <span style={{ background: 'var(--rv-accent-light)', color: 'var(--rv-accent-dark)', borderRadius: 6, padding: '3px 10px', fontSize: '0.78rem', fontWeight: 600 }}>
                    {CAT_ICONS[listing.category]} {listing.category}
                  </span>
                )}
                {listing.condition && (
                  <span style={{ background: condStyle.bg || '#f3f4f6', color: condStyle.color || '#374151', borderRadius: 6, padding: '3px 10px', fontSize: '0.78rem', fontWeight: 600 }}>
                    {condStyle.icon} {listing.condition}
                  </span>
                )}
                <span style={{ background: isSale ? '#f0fdf4' : 'var(--rv-accent-light)', color: isSale ? '#166534' : 'var(--rv-accent)', borderRadius: 6, padding: '3px 10px', fontSize: '0.78rem', fontWeight: 600 }}>
                  {isSale ? '🏷️ For Sale' : '🔄 For Rent'}
                </span>
              </div>

              {/* Price */}
              <div style={{ marginBottom: 20 }}>
                <span style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--rv-accent)' }}>
                  {INR(listing.price)}
                </span>
                {isRent && listing.rentalPeriodUnit && (
                  <span style={{ fontSize: '0.9rem', color: 'var(--rv-text-muted)', marginLeft: 6 }}>
                    / {listing.rentalPeriodUnit.replace('PER_', '').toLowerCase()}
                  </span>
                )}
              </div>

              {/* ── Action buttons ───────────────────────────────── */}
              <div className="d-flex flex-column gap-2 mb-4">
                {isOwner ? (
                  /* Owner controls */
                  <div className="d-flex gap-2">
                    <Link
                      to={`/listings/${listing.id}/edit`}
                      className="btn btn-outline-primary flex-fill"
                      style={{ borderRadius: 10, fontWeight: 600 }}
                    >
                      ✏️ Edit Listing
                    </Link>
                  </div>
                ) : isActive ? (
                  <>
                    {/* Buy / Rent */}
                    {isSale && (
                      <button
                        className="btn btn-primary"
                        style={{ borderRadius: 10, padding: '0.7rem', fontWeight: 700, fontSize: '1rem' }}
                        onClick={handleBuyNow}
                        disabled={txLoading}
                      >
                        {txLoading ? <><span className="spinner-border spinner-border-sm me-2" />Processing…</> : '🛒 Request to Buy'}
                      </button>
                    )}
                    {isRent && (
                      <button
                        className="btn btn-primary"
                        style={{ borderRadius: 10, padding: '0.7rem', fontWeight: 700, fontSize: '1rem' }}
                        onClick={() => {
                          if (!currentUser) { navigate('/login'); return; }
                          setShowRentModal(true);
                        }}
                        disabled={txLoading}
                      >
                        🔄 Rent Now
                      </button>
                    )}

                    {/* Contact Seller */}
                    <button
                      className="btn btn-outline-primary"
                      style={{ borderRadius: 10, padding: '0.65rem', fontWeight: 600 }}
                      onClick={handleContactSeller}
                      disabled={msgLoading}
                    >
                      {msgLoading
                        ? <><span className="spinner-border spinner-border-sm me-2" />Opening chat…</>
                        : '💬 Contact Seller'}
                    </button>

                    {/* Wishlist */}
                    <button
                      className="btn btn-outline-secondary"
                      style={{ borderRadius: 10, padding: '0.65rem', fontWeight: 600 }}
                      onClick={handleWishlist}
                      disabled={wlLoading}
                    >
                      {wlLoading
                        ? <><span className="spinner-border spinner-border-sm me-2" />…</>
                        : wishlisted ? '❤️ Saved to Wishlist' : '🤍 Add to Wishlist'}
                    </button>
                  </>
                ) : (
                  /* Sold/Rented/Unavailable */
                  <div className="alert alert-secondary mb-0" style={{ borderRadius: 10 }}>
                    <strong>This listing is no longer available.</strong>
                    <div style={{ fontSize: '0.85rem', marginTop: 4 }}>Status: <strong>{listing.status}</strong></div>
                  </div>
                )}
              </div>

              {/* ── Seller info card ─────────────────────────────── */}
              {listing.sellerDisplayName && (
                <div
                  style={{
                    background: '#fff',
                    border: '1px solid var(--rv-border)',
                    borderRadius: 12,
                    padding: '16px 18px',
                    marginBottom: 16,
                  }}
                >
                  <p style={{ fontWeight: 700, marginBottom: 8, color: 'var(--rv-accent-dark)', fontSize: '0.85rem' }}>
                    👤 Seller
                  </p>
                  <div className="d-flex align-items-center gap-3">
                    <div
                      style={{
                        width: 44, height: 44, borderRadius: '50%',
                        background: 'var(--rv-accent-light)',
                        color: 'var(--rv-accent-dark)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontWeight: 800, fontSize: '1.1rem', flexShrink: 0,
                      }}
                    >
                      {listing.sellerDisplayName[0].toUpperCase()}
                    </div>
                    <div>
                      <div style={{ fontWeight: 600, color: 'var(--rv-text)' }}>
                        {listing.sellerDisplayName}
                      </div>
                      {listing.sellerJoinDate && (
                        <div style={{ fontSize: '0.78rem', color: 'var(--rv-text-muted)' }}>
                          Member since {formatDate(listing.sellerJoinDate)}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )}

              {/* ── Listing meta ─────────────────────────────────── */}
              <div
                style={{
                  background: '#fff',
                  border: '1px solid var(--rv-border)',
                  borderRadius: 12,
                  padding: '14px 18px',
                  fontSize: '0.82rem',
                  color: 'var(--rv-text-muted)',
                }}
              >
                <div className="d-flex justify-content-between mb-1">
                  <span>Listed on</span>
                  <span style={{ color: 'var(--rv-text)', fontWeight: 500 }}>
                    {formatDate(listing.createdAt)}
                  </span>
                </div>
                <div className="d-flex justify-content-between">
                  <span>Listing ID</span>
                  <span style={{ fontFamily: 'monospace', color: 'var(--rv-text)' }}>
                    #{listing.id}
                  </span>
                </div>
              </div>

            </div>
          </div>

          {/* ── Description (mobile: below the fold) ─────────────── */}
          <div className="col-12 d-lg-none">
            <div style={{ background: '#fff', borderRadius: 14, padding: '24px', border: '1px solid var(--rv-border)' }}>
              <h5 style={{ fontWeight: 700, marginBottom: 12, color: 'var(--rv-accent-dark)' }}>
                📝 Description
              </h5>
              <p style={{ whiteSpace: 'pre-wrap', fontSize: '0.9rem', lineHeight: 1.7, margin: 0 }}>
                {listing.description || 'No description provided.'}
              </p>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}

export default ProductDetailsPage;
