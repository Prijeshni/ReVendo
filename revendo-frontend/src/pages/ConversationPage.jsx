
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

  // Identify the logged-in user.
  const currentUserId =
    currentUser?.userId ??
    currentUser?.id ??
    currentUser?.studentId ??
    currentUser?.user?.id ??
    null;

  // Load conversation messages.
  const loadMessages = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const res = await messagingService.getMessages(conversationId);
      const data = res.data;

      const fetchedMessages = Array.isArray(data)
        ? data
        : Array.isArray(data?.content)
          ? data.content
          : Array.isArray(data?.messages)
            ? data.messages
            : [];

      setMessages(fetchedMessages);

      await messagingService.markAsRead(conversationId);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          'Could not load messages. Please try again.'
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

  // Scroll to the latest message.
  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Send a message.
  const handleSend = async (e) => {
    e.preventDefault();

    const messageText = text.trim();

    if (!messageText || sending) return;

    setSending(true);

    try {
      const res = await messagingService.sendMessage(
        conversationId,
        messageText
      );

      if (res.data) {
        setMessages((prev) => [...prev, res.data]);
      }

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

        <button
          type="button"
          className="btn btn-primary me-2"
          onClick={loadMessages}
        >
          Retry
        </button>

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
          width: '100%',
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
            const senderId =
              m.senderId ??
              m.sender?.userId ??
              m.sender?.id ??
              m.sender?.studentId ??
              m.sender?.user?.id ??
              null;

            const isMe =
              currentUserId != null &&
              senderId != null &&
              String(senderId) === String(currentUserId);

            const senderName =
              m.senderDisplayName ??
              m.senderName ??
              m.sender?.displayName ??
              m.sender?.name ??
              'User';

            return (
              <div
                key={m.id ?? m.messageId ?? i}
                style={{
                  display: 'flex',
                  justifyContent: isMe ? 'flex-end' : 'flex-start',
                  width: '100%',
                  marginBottom: 10,
                }}
              >
                <div
                  style={{
                    maxWidth: '75%',
                    minWidth: 80,
                    padding: '10px 14px',
                    borderRadius: isMe
                      ? '14px 14px 3px 14px'
                      : '14px 14px 14px 3px',
                    background: isMe ? 'var(--rv-accent)' : '#fff',
                    color: isMe ? '#fff' : 'var(--rv-text)',
                    border: isMe
                      ? 'none'
                      : '1px solid var(--rv-border)',
                    overflowWrap: 'anywhere',
                  }}
                >
                  {!isMe && (
                    <div
                      style={{
                        fontSize: '0.72rem',
                        fontWeight: 700,
                        color: 'var(--rv-accent)',
                        marginBottom: 4,
                      }}
                    >
                      {senderName}
                    </div>
                  )}

                  <div
                    style={{
                      fontSize: '0.875rem',
                      whiteSpace: 'pre-wrap',
                    }}
                  >
                    {m.text ?? m.content ?? ''}
                  </div>

                  <div
                    style={{
                      fontSize: '0.68rem',
                      opacity: 0.7,
                      marginTop: 5,
                      textAlign: 'right',
                    }}
                  >
                    {m.sentAt ? formatDate(m.sentAt) : ''}
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
              placeholder="Type a message..."
              maxLength={2000}
              value={text}
              onChange={(e) => setText(e.target.value)}
              disabled={sending}
              aria-label="Message text"
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
