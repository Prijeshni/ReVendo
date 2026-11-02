import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import LoadingSpinner from '../components/common/LoadingSpinner';
import useAuth from '../hooks/useAuth';
import messagingService from '../services/messagingService';
import { truncateText, formatDate } from '../utils/formatters';

function MessagesPage() {
  const { currentUser } = useAuth();
  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try {
      const res = await messagingService.getConversations();
      setConversations(res.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not load conversations.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  if (loading) return <div className="container py-5"><LoadingSpinner /></div>;

  if (error) {
    return (
      <div className="container py-5 text-center">
        <div className="alert alert-warning">{error}</div>
        <button className="btn btn-outline-primary" onClick={load}>Retry</button>
      </div>
    );
  }

  return (
    <div style={{ background: 'var(--rv-surface)', minHeight: '100vh', paddingBottom: 48 }}>
      <div style={{ background: 'linear-gradient(135deg, var(--rv-accent-dark) 0%, var(--rv-accent) 100%)', padding: '36px 0 28px', color: '#fff' }}>
        <div className="container">
          <h1 style={{ fontWeight: 700, fontSize: '1.8rem', margin: 0 }}>💬 Messages</h1>
          <p style={{ opacity: 0.85, marginTop: 6, marginBottom: 0 }}>
            {conversations.length} conversation{conversations.length !== 1 ? 's' : ''}
          </p>
        </div>
      </div>

      <div className="container py-4" style={{ maxWidth: 700 }}>
        {conversations.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">💬</div>
            <h5>No conversations yet</h5>
            <p className="text-muted mb-3">Contact a seller from a product details page to start chatting.</p>
            <Link to="/products" className="btn btn-primary">Browse Listings</Link>
          </div>
        ) : (
          conversations.map((c) => {
            const isBuyer = String(c.buyerId) === String(currentUser?.userId);
            const otherName = isBuyer ? c.sellerDisplayName : c.buyerDisplayName;
            return (
              <Link key={c.conversationId} to={`/messages/${c.conversationId}`}
                style={{ textDecoration: 'none' }}>
                <div style={{ background: '#fff', border: '1px solid var(--rv-border)', borderRadius: 12, padding: '14px 18px', marginBottom: 10, cursor: 'pointer', transition: 'box-shadow 200ms' }}
                     onMouseEnter={(e) => e.currentTarget.style.boxShadow = 'var(--rv-shadow-md)'}
                     onMouseLeave={(e) => e.currentTarget.style.boxShadow = 'none'}>
                  <div className="d-flex justify-content-between align-items-start">
                    <div>
                      <span style={{ fontWeight: 700, color: 'var(--rv-text)' }}>{otherName}</span>
                      <span className="text-muted ms-2" style={{ fontSize: '0.8rem' }}>· {c.listingTitle}</span>
                    </div>
                    <span style={{ fontSize: '0.75rem', color: 'var(--rv-text-muted)' }}>
                      {c.lastMessageAt ? formatDate(c.lastMessageAt) : formatDate(c.createdAt)}
                    </span>
                  </div>
                  {c.lastMessagePreview && (
                    <p style={{ margin: '6px 0 0', fontSize: '0.82rem', color: 'var(--rv-text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {truncateText(c.lastMessagePreview, 100)}
                    </p>
                  )}
                </div>
              </Link>
            );
          })
        )}
      </div>
    </div>
  );
}

export default MessagesPage;
