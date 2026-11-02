import axiosInstance from '../api/axiosInstance';

/**
 * Service layer for wishlist API calls.
 * All endpoints require a valid JWT (handled by axiosInstance interceptor).
 */
const wishlistService = {
  /** GET /api/wishlist — all saved listings for the authenticated user. */
  getWishlist: () => axiosInstance.get('/wishlist'),

  /** POST /api/wishlist/:listingId — add a listing to the wishlist. */
  addToWishlist: (listingId) => axiosInstance.post(`/wishlist/${listingId}`),

  /** DELETE /api/wishlist/:listingId — remove a listing from the wishlist. */
  removeFromWishlist: (listingId) => axiosInstance.delete(`/wishlist/${listingId}`),

  /** GET /api/wishlist/:listingId/status — { wishlisted: boolean } */
  isWishlisted: (listingId) => axiosInstance.get(`/wishlist/${listingId}/status`),
};

export default wishlistService;
