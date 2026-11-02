import axiosInstance from '../api/axiosInstance';

/**
 * Authentication API calls.
 *
 * POST /api/auth/register  — { firstName, lastName, email, phoneNumber, password, confirmPassword }
 * POST /api/auth/login     — { email, password }
 */
const authService = {
  /**
   * Register a new user account.
   * Returns { message } on success (201).
   * Throws on 400 (validation) or 409 (duplicate email).
   */
  register: (data) => axiosInstance.post('/auth/register', data),

  /**
   * Authenticate and receive a JWT.
   * Returns { token, role, userId, displayName } on success (200).
   * Throws on 400 (bad credentials), 403 (suspended), 429 (locked).
   */
  login: (data) => axiosInstance.post('/auth/login', data),
};

export default authService;
