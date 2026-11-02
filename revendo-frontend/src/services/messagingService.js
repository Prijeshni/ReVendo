import axiosInstance from '../api/axiosInstance';

/**
 * Service layer for messaging API calls.
 * All endpoints require JWT authentication.
 */
const messagingService = {
  /**
   * POST /api/messages/conversations
   * Body: { listingId }
   * Finds or creates the conversation for this buyer+seller+listing triple.
   * Returns ConversationResponse: { conversationId, listingId, ... }
   */
  getOrCreateConversation: (listingId) =>
    axiosInstance.post('/messages/conversations', { listingId }),

  /** GET /api/messages/conversations — all conversations for the current user */
  getConversations: () => axiosInstance.get('/messages/conversations'),

  /**
   * GET /api/messages/conversations/:id — messages in a conversation
   * Only participants can call this.
   */
  getMessages: (conversationId) =>
    axiosInstance.get(`/messages/conversations/${conversationId}`),

  /**
   * POST /api/messages/conversations/:id — send a message
   * Body: { text }
   */
  sendMessage: (conversationId, text) =>
    axiosInstance.post(`/messages/conversations/${conversationId}`, { text }),

  /** PUT /api/messages/conversations/:id/read — mark messages as read */
  markAsRead: (conversationId) =>
    axiosInstance.put(`/messages/conversations/${conversationId}/read`),

  /** GET /api/messages/unread-count — { unreadCount: N } */
  getUnreadCount: () => axiosInstance.get('/messages/unread-count'),
};

export default messagingService;
