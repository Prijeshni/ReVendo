import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';
import wishlistService from '../../services/wishlistService';

/** Format a number as ₹ Indian Rupees */
const formatINR = (amount) => {
  const num = parseFloat(amount);
  if (isNaN(num)) return '₹0';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency', currency: 'INR',
    maximumFractionDigits: 0,
  }).format(num);
};

const CONDITION_BADGE = {
  'New':      { cls: 'badge-condition-new',      label: '✨ New' },
  'Like New': { cls: 'badge-condition-like-new', label: '👍 Like New' },
  'Good':     { cls: 'badge-condition-good',     label: '👌 Good' },
  'Fair':     { cls: 'badge-condition-fair',     label: '🔧 Fair' },
  'Poor':     { cls: 'badge-condition-poor',     label: '⚠️ Poor' },
};

const TYPE_LABEL = { FOR_SALE: 'For Sale', FOR_RENT: 'For Rent' };

const CAT_ICONS = {
  'Electronics': '💻', 'Furniture': '🛋️',
  'Books': '📚', 'Home Appliances': '🏠', 'Household Goods': '🧺',
};

/**
 * Reusable product card.
 *
 * Props:
 *   listing      { id, title, price, category, condition, listingType,
 *                  status, imageUrls, sellerDisplayName }
 *   isWishlisted {boolean}  optional – controlled externally
 *   onWishlistChange {fn}   optional – called with (id, newWishlistedState)
 */
function ProductCard({ listing, isWishlisted: wishedProp, onWishlistChange }) {
  const { currentUser } = useAuth();
  const navigate = useNavigate();

  const {
    id, title, price, category, condition,
    listingType, status, imageUrls = [], sellerDisplayName,
  } = listing;

  const [wishlisted, setWishlisted] = useState(!!wishedProp);
  const [wlLoading, setWlLoading]   = useState(false);
  const cond = CONDITION_BADGE[condition] || { cls: '', label: condition };
  const isSold = status === 'Sold' || status === 'Rented';

  const handleWishlist = async (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!currentUser) { navigate('/login'); return; }
    setWlLoading(true);
    try {
      if (wishlisted) {
        await wishlistService.removeFromWishlist(id);
        setWishlisted(false);
        onWishlistChange?.(id, false);
      } else {
        await wishlistService.addToWishlist(id);
        setWishlisted(true);
        onWishlistChange?.(id, true);
      }
    } catch { /* silently ignore */ }
    finally { setWlLoading(false); }
  };

  return (
    <div className="product-card" style={{ position: 'relative' }}>
      {/* Wishlist button */}
      <button
        onClick={handleWishlist}
        disabled={wlLoading}
        title={wishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
        aria-label={wishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
        style={{
          position: 'absolute', top: 10, right: 10, zIndex: 2,
          width: 34, height: 34, borderRadius: '50%',
          background: 'rgba(255,255,255,0.92)',
          border: '1px solid #e5e7eb',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          cursor: 'pointer', fontSize: '1rem',
          transition: 'transform 150ms ease, box-shadow 150ms ease',
          boxShadow: '0 1px 4px rgba(0,0,0,0.12)',
        }}
        onMouseEnter={(e) => { e.currentTarget.style.transform = 'scale(1.15)'; }}
        onMouseLeave={(e) => { e.currentTarget.style.transform = 'scale(1)'; }}
      >
        {wlLoading ? '…' : wishlisted ? '❤️' : '🤍'}
      </button>

      {/* Image */}
      {imageUrls.length > 0 ? (
        <img
          src={imageUrls[0]}
          alt={title}
          className="card-img-top"
          onError={(e) => {
            e.target.style.display = 'none';
            e.target.nextSibling.style.display = 'flex';
          }}
        />
      ) : null}
      <div
        className="img-placeholder"
        style={{ display: imageUrls.length > 0 ? 'none' : 'flex' }}
        aria-hidden="true"
      >
        {CAT_ICONS[category] || '🖼️'}
      </div>

      {/* Sold / Rented overlay badge */}
      {isSold && (
        <span
          className={`badge position-absolute`}
          style={{
            top: 10, left: 10, zIndex: 2,
            background: status === 'Sold' ? '#374151' : '#d97706',
            color: '#fff', fontSize: '0.72rem',
          }}
        >
          {status}
        </span>
      )}

      <div className="card-body">
        {/* Category + type chips */}
        <div className="d-flex gap-1 mb-1 flex-wrap">
          {category && (
            <span style={{ fontSize: '0.72rem', color: 'var(--rv-text-muted)' }}>
              {CAT_ICONS[category]} {category}
            </span>
          )}
          <span
            className="badge ms-auto"
            style={{
              fontSize: '0.68rem',
              background: listingType === 'FOR_RENT' ? 'var(--rv-accent-light)' : '#f0fdf4',
              color: listingType === 'FOR_RENT' ? 'var(--rv-accent)' : '#166534',
            }}
          >
            {TYPE_LABEL[listingType] || listingType}
          </span>
        </div>

        <h6 className="card-title" title={title}>{title}</h6>

        {/* Condition badge */}
        <div className="mb-2">
          <span className={`badge ${cond.cls}`} style={{ fontSize: '0.7rem' }}>
            {cond.label}
          </span>
        </div>

        {/* Seller */}
        {sellerDisplayName && (
          <p style={{ fontSize: '0.75rem', color: 'var(--rv-text-muted)', margin: '0 0 8px' }}>
            👤 {sellerDisplayName}
          </p>
        )}

        <div className="card-footer-area d-flex justify-content-between align-items-center">
          <div>
            <span className="card-price">{formatINR(price)}</span>
            {listingType === 'FOR_RENT' && (
              <small className="text-muted d-block" style={{ fontSize: '0.7rem' }}>per period</small>
            )}
          </div>
          <Link
            to={`/products/${id}`}
            className="btn btn-primary btn-sm"
            style={{ fontSize: '0.78rem', padding: '0.28rem 0.7rem' }}
          >
            View →
          </Link>
        </div>
      </div>
    </div>
  );
}

export default ProductCard;
