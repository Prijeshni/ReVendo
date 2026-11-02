
import axios from 'axios';

const API_BASE_URL = import.meta.env.PROD
  ? 'https://revendo-backend.onrender.com/api'
  : 'http://localhost:8090/api';

const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    Accept: 'application/json',
  },
});

// Attach JWT to requests
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('revendo_jwt');

    if (token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Set JSON content type only for non-FormData requests
    // Let the browser set the boundary for FormData uploads.
    const isFormData =
      typeof FormData !== 'undefined' && config.data instanceof FormData;

    if (!isFormData && !config.headers['Content-Type']) {
      config.headers['Content-Type'] = 'application/json';
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Handle unauthorized requests
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('revendo_jwt');

      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;
