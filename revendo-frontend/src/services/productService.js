import axiosInstance from '../api/axiosInstance';

/**
 * Service layer for product / listing API calls.
 * Wraps /api/products and /api/purchase-requests endpoints.
 */
const productService = {
  getListings: (params = {}) => {
    const { page = 0, size = 12, keyword, category, condition,
            listingType, minPrice, maxPrice, sort } = params;
    const query = new URLSearchParams();
    query.set('page', page);
    query.set('size', size);
    if (keyword)     query.set('keyword', keyword);
    if (category)    query.set('category', category);
    if (condition)   query.set('condition', condition);
    if (listingType) query.set('listingType', listingType);
    if (minPrice !== undefined && minPrice !== '') query.set('minPrice', minPrice);
    if (maxPrice !== undefined && maxPrice !== '') query.set('maxPrice', maxPrice);
    if (sort)        query.set('sort', sort);
    return axiosInstance.get(`/products?${query.toString()}`);
  },

  getListingById: (id) => axiosInstance.get(`/products/${id}`),

  createListing: (formData) => axiosInstance.post('/products', formData),

  updateListing: (id, formData) => axiosInstance.put(`/products/${id}`, formData),

  deleteListing: (id) => axiosInstance.delete(`/products/${id}`),

  /** Create a purchase request (buyer → seller, PENDING status). */
  createPurchaseRequest: (listingId) =>
    axiosInstance.post('/purchase-requests', null, { params: { listingId } }),

  /** Create a rental request with a duration (buyer → seller, PENDING status). */
  createRentalRequest: (listingId, duration) =>
    axiosInstance.post('/purchase-requests/rental', { duration }, { params: { listingId } }),

  /** Get requests received by the current user as a seller. */
  getSellerRequests: () => axiosInstance.get('/purchase-requests/seller'),

  /** Get requests sent by the current user as a buyer. */
  getBuyerRequests: () => axiosInstance.get('/purchase-requests/buyer'),

  /** Seller accepts a purchase request. */
  acceptPurchaseRequest: (requestId) =>
    axiosInstance.put(`/purchase-requests/${requestId}/accept`),

  /** Seller rejects a purchase request. */
  rejectPurchaseRequest: (requestId) =>
    axiosInstance.put(`/purchase-requests/${requestId}/reject`),
};

export default productService;
