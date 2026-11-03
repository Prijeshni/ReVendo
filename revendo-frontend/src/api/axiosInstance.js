
import axios from 'axios';

// Backend API URL
const API_BASE_URL = import.meta.env.PROD
  ? 'https://revendo-backend.onrender.com/api'
  : 'http://localhost:8080/api';

// Create Axios instance
const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    Accept: 'application/json',
  },
});

// Attach JWT and configure request headers
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('revendo_jwt');

    if (token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Let the browser set Content-Type and boundary for file uploads.
    const isFormData =
      typeof FormData !== 'undefined' && config.data instanceof FormData;

    if (!isFormData && !config.headers['Content-Type']) {
      config.headers['Content-Type'] = 'application/json';
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Handle unauthorized responses

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const message = error.response?.data?.message || '';

      // This is a business-rule error, not a login failure.
      if (message === 'You cannot start a conversation about your own listing.') {
        return Promise.reject(error);
      }

      // Preserve existing handling for other 401 responses.
      localStorage.removeItem('revendo_jwt');

      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);


export default axiosInstance;
