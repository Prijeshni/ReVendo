import React, { useEffect, useState, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import LoadingSpinner from '../components/common/LoadingSpinner';
import useToast from '../hooks/useToast';
import useAuth from '../hooks/useAuth';
import productService from '../services/productService';
import axiosInstance from '../api/axiosInstance';
import { formatDate } from '../utils/formatters';

const BACKEND = 'http://localhost:8090';
const toAbs = (url) => (!url ? '' : url.startsWith('http') ? url : `${BACKEND}${url}`);

const STATUS_BADGE = {
  PENDING:  { bg: '#fef9c3', color: '#854d0e', label: '⏳ Pending' },
  ACCEPTED: { bg: '#dcfce7', color: '#166534', label: '✅ Accepted' },
  REJECTED: { bg: '#fee2e2', color: '#991b1b', label: '❌ Rejected' },
};

const INR = (n) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);

// ── Individual tabs ────────────────────────────────────────────────

function SellerRequestsTab() {
  const { showToast } = useToast();
  const [requests, setRequests] = useState([]);
  const [loading, setLoading]   = useState(true);
  const [actId, setActId]       = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await productService.getSellerRequests();
      setRequests(res.data || []);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const respond = async (id, action) => {
    setActId(id);
    try {
      if (action === 'accept') {
        await productService.acceptPurchaseRequest(id);
        showToast('Request accepted! Listing marked as Sold.');
      } else {
        await productService.rejectPurchaseRequest(id);
        showToast('Request rejected.');
      }
      await load();
    } catch (err) {
      showToast(err.response?.data?.message || 'Action failed.', 'error');
    } finally {
      setActId(null);
    }
  };

  if (loading) return <LoadingSpinner />;
  if (requests.length === 0) return (
    <div className="empty-state"><div className="empty-icon">📭</div><p className="text-muted">No purchase requests received yet.</p></div>
  );

  return (
    <div>
      {requests.map((r) => {
        const badge = STATUS_BADGE[r.status] || {};
        const isPending = r.status === 'PENDING';
        return (
          <div key={r.requestId} style={{ background: '#fff', border: '1px solid var(--rv-border)', borderRadius: 12, padding: '16px 20px', marginBottom: 12 }}>
            <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
              <div>
                <Link to={`/products/${r.listingId}`} style={{ fontWeight: 700, color: 'var(--rv-text)' }}>{r.listingTitle}</Link>
                <p style={{ margin: '4px 0 0', fontSize: '0.85rem', color: 'var(--rv-text-muted)' }}>
                  From: <strong>{r.buyerName}</strong> · {INR(r.price)}
                  {r.requestType === 'RENTAL' && r.rentalDuration ? ` · ${r.rentalDuration} period(s)` : ''}
                  {' · '} {formatDate(r.requestDate)}
                </p>
                {r.requestType === 'RENTAL' && (
                  <span className="badge mt-1" style={{ background: 'var(--rv-accent-light)', color: 'var(--rv-accent)', fontSize: '0.7rem' }}>🔄 Rental Request</span>
                )}
                {r.requestType === 'PURCHASE' && (
                  <span className="badge mt-1" style={{ background: '#f0fdf4', color: '#166534', fontSize: '0.7rem' }}>🛒 Purchase Request</span>
                )}
              </div>
              <div className="d-flex align-items-center gap-2">
                <span style={{ background: badge.bg, color: badge.color, borderRadius: 6, padding: '3px 10px', fontSize: '0.78rem', fontWeight: 700 }}>
                  {badge.label}
                </span>
                {isPending && (
                  <>
                    <button className="btn btn-sm btn-success" disabled={actId === r.requestId}
                            onClick={() => respond(r.requestId, 'accept')}>
                      {actId === r.requestId ? '…' : 'Accept'}
                    </button>
                    <button className="btn btn-sm btn-outline-danger" disabled={actId === r.requestId}
                            onClick={() => respond(r.requestId, 'reject')}>
                      Reject
                    </button>
                  </>
                )}
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

function BuyerRequestsTab() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading]   = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const res = await productService.getBuyerRequests();
        setRequests(res.data || []);
      } catch { /* ignore */ }
      finally { setLoading(false); }
    })();
  }, []);

  if (loading) return <LoadingSpinner />;
  if (requests.length === 0) return (
    <div className="empty-state"><div className="empty-icon">🛒</div><p className="text-muted">You haven't requested to buy anything yet.</p></div>
  );

  return (
    <div>
      {requests.map((r) => {
        const badge = STATUS_BADGE[r.status] || {};
        return (
          <div key={r.requestId} style={{ background: '#fff', border: '1px solid var(--rv-border)', borderRadius: 12, padding: '16px 20px', marginBottom: 12 }}>
            <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
              <div>
                <Link to={`/products/${r.listingId}`} style={{ fontWeight: 700, color: 'var(--rv-text)' }}>{r.listingTitle}</Link>
                <p style={{ margin: '4px 0 0', fontSize: '0.85rem', color: 'var(--rv-text-muted)' }}>
                  Seller: <strong>{r.sellerName}</strong> · {INR(r.price)} · {formatDate(r.requestDate)}
                </p>
                {r.status === 'ACCEPTED' && (
                  <p style={{ margin: '4px 0 0', fontSize: '0.82rem', color: '#166534', fontWeight: 600 }}>
                    ✅ Your request was accepted. Contact the seller to coordinate payment and pickup.
                  </p>
                )}
                {r.status === 'REJECTED' && (
                  <p style={{ margin: '4px 0 0', fontSize: '0.82rem', color: '#991b1b' }}>
                    The seller declined this request.
                  </p>
                )}
              </div>
              <span style={{ background: badge.bg, color: badge.color, borderRadius: 6, padding: '3px 10px', fontSize: '0.78rem', fontWeight: 700 }}>
                {badge.label}
              </span>
            </div>
          </div>
        );
      })}
    </div>
  );
}

function MyListingsTab() {
  const { showToast } = useToast();
  const [listings, setListings] = useState([]);
  const [loading, setLoading]   = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      await axiosInstance.get('/users/me/purchases');
      setListings([]);
    } catch {
      setListings([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingSpinner />;
  return (
    <div className="empty-state">
      <div className="empty-icon">📦</div>
      <p className="text-muted mb-3">Your listings are visible on the marketplace.</p>
      <Link to="/products" className="btn btn-outline-primary me-2">Browse Marketplace</Link>
      <Link to="/sell" className="btn btn-primary">+ List an Item</Link>
    </div>
  );
}

// ── Main Dashboard ─────────────────────────────────────────────────

const TABS = [
  { id: 'seller',  label: '📥 Sell Requests' },
  { id: 'buyer',   label: '🛒 My Requests' },
  { id: 'listings',label: '📦 My Listings' },
];

function UserDashboardPage() {
  const { currentUser } = useAuth();
  const [tab, setTab] = useState('seller');

  return (
    <div style={{ background: 'var(--rv-surface)', minHeight: '100vh', paddingBottom: 48 }}>
      <div style={{ background: 'linear-gradient(135deg, var(--rv-accent-dark) 0%, var(--rv-accent) 100%)', padding: '36px 0 28px', color: '#fff' }}>
        <div className="container">
          <h1 style={{ fontWeight: 700, fontSize: '1.8rem', margin: 0 }}>📊 My Dashboard</h1>
          <p style={{ opacity: 0.85, marginTop: 6, marginBottom: 0 }}>
            Welcome back, {currentUser?.displayName}
          </p>
        </div>
      </div>

      <div className="container py-4">
        {/* Tab nav */}
        <div className="d-flex gap-2 flex-wrap mb-4">
          {TABS.map((t) => (
            <button key={t.id}
              className={`btn btn-sm ${tab === t.id ? 'btn-primary' : 'btn-outline-secondary'}`}
              style={{ borderRadius: 8 }}
              onClick={() => setTab(t.id)}>
              {t.label}
            </button>
          ))}
        </div>

        {tab === 'seller'   && <SellerRequestsTab />}
        {tab === 'buyer'    && <BuyerRequestsTab />}
        {tab === 'listings' && <MyListingsTab />}
      </div>
    </div>
  );
}

export default UserDashboardPage;
