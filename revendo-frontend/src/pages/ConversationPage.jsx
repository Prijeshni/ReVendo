
import React, { useEffect, useState, useCallback, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import LoadingSpinner from '../components/common/LoadingSpinner';
import useAuth from '../hooks/useAuth';
import useToast from '../hooks/useToast';
import messagingService from '../services/messagingService';
import { formatDate } from '../utils/formatters';

function ConversationPage() {
  const { conversationId } = useParams();
  const navigate = useNavigate();
  const { currentUser } = useAuth();
  const { showToast } = useToast();

  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [text, setText] = useState('');
  const [sending, setSending] = useState(false);

  const endRef = useRef(null);

  const loadMessages = useCallback(async () => {
    setLoading(true);

    try {
      const res = await messagingService.getMessages(conversationId);
      setMessages(res.data || []);
      await messagingService.markAsRead(conversationId);
    } catch (err) {
      setError(
        err.response?.data?.message || 'Could not load messages.'
      );
    } finally {
      setLoading(false);
    }
  }, [conversationId]);

  useEffect(() => {
    if (!currentUser) {
      navigate('/login');
      return;
    }

    loadMessages();
  }, [loadMessages, currentUser, navigate]);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async (e) => {
    e.preventDefault();

    if (!text.trim()) return;

    setSending(true);

    try {
      const res = await messagingService.sendMessage(
        conversationId,
        text.trim()
      );

      setMessages((prev) => [...prev, res.data]);
      setText('');
    } catch (err) {
      showToast(
        err.response?.data?.message || 'Could not send message.',
        'error'
      );
    } finally {
      setSending(false);
    }
  };

  if (loading) {
    return (
      <div className="container py-5">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="container py-5 text-center">
        <div className="alert alert-warning">{error}</div>
        <Link to="/messages" className="btn btn-outline-primary">
          Back to Messages
        </Link>
      </div>
    );
  }

  return (
    <div
      style={{
        background: 'var(--rv-surface)',
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Header */}
      <div
        style={{
          background: '#fff',
          borderBottom: '1px solid var(--rv-border)',
          padding: '10px 0',
        }}
      >
        <div className="container d-flex align-items-center gap-2">
          <Link
            to="/messages"
            style={{
              color: 'var(--rv-accent)',
              textDecoration: 'none',
              fontSize: '0.9rem',
            }}
          >
            ← Messages
          </Link>

          <span className="text-muted mx-2">|</span>

          <span
            style={{
              fontWeight: 700,
              color: 'var(--rv-text)',
              fontSize: '0.95rem',
            }}
          >
            💬 Conversation #{conversationId}
          </span>
        </div>
      </div>

      {/* Messages */}
      <div
        className="container py-3"
        style={{
          flex: 1,
          overflowY: 'auto',
          maxWidth: 700,
        }}
      >
        {messages.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">💬</div>
            <p>No messages yet. Say hello!</p>
          </div>
        ) : (
          messages.map((m, i) => {
            const isMe =
              String(m.senderId) === String(currentUser?.userId);

            return (
              <div
                key={i}
                style={{
                  display: 'flex',
                  justifyContent: isMe ? 'flex-end' : 'flex-start',
                  marginBottom: 8,
                }}
              >
                <div
                  style={{
                    maxWidth: '75%',
                    padding: '10px 14px',
                    borderRadius: 12,
                    background: isMe ? 'var(--rv-accent)' : '#fff',
                    color: isMe ? '#fff' : 'var(--rv-text)',
                    border: isMe
                      ? 'none'
                      : '1px solid var(--rv-border)',
                  }}
                >
                  {!isMe && (
                    <div
                      style={{
                        fontSize: '0.72rem',
                        fontWeight: 700,
                        color: 'var(--rv-accent)',
                        marginBottom: 2,
                      }}
                    >
                      {m.senderDisplayName}
                    </div>
                  )}

                  <div
                    style={{
                      fontSize: '0.875rem',
                      whiteSpace: 'pre-wrap',
                    }}
                  >
                    {m.text}
                  </div>

                  <div
                    style={{
                      fontSize: '0.68rem',
                      opacity: 0.6,
                      marginTop: 4,
                    }}
                  >
                    {formatDate(m.sentAt)}
                  </div>
                </div>
              </div>
            );
          })
        )}

        <div ref={endRef} />
      </div>

      {/* Send bar */}
      <div
        style={{
          background: '#fff',
          borderTop: '1px solid var(--rv-border)',
          padding: '12px 0',
        }}
      >
        <div className="container" style={{ maxWidth: 700 }}>
          <form className="d-flex gap-2" onSubmit={handleSend}>
            <input
              type="text"
              className="form-control"
              placeholder="Type a message…"
              maxLength={2000}
              value={text}
              onChange={(e) => setText(e.target.value)}
              disabled={sending}
            />

            <button
              type="submit"
              className="btn btn-primary"
              disabled={sending || !text.trim()}
            >
              {sending ? (
                <span className="spinner-border spinner-border-sm" />
              ) : (
                'Send'
              )}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

export default ConversationPage;
