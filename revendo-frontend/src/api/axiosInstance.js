
import axios from 'axios';

const axiosInstance = axios.create({
  baseURL: 'http://localhost:8080/api',
});

// Attach JWT to requests
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('revendo_jwt');

    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token;
    }

    // Set JSON content type only for non-FormData requests
    if (
      !config.headers['Content-Type'] &&
      !(config.data instanceof FormData)
    ) {
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
