import React, { createContext, useState, useCallback, useEffect } from 'react';

/**
 * AuthContext — provides authentication state to the entire app.
 *
 * Value shape:
 *   currentUser : { userId, role, displayName } | null
 *   login(tokenData)  — persist JWT, set currentUser
 *   logout()          — clear JWT, reset currentUser
 *
 * Requirements: 3.6, 3.7, 3.8, 3.9
 */
export const AuthContext = createContext(null);

const JWT_KEY = 'revendo_jwt';

/** Decode a JWT payload without verifying the signature (client-side only). */
function decodePayload(token) {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

/** Build a currentUser object from a raw JWT string. */
function userFromToken(token) {
  const payload = decodePayload(token);
  if (!payload) return null;
  return {
    userId:      parseInt(payload.sub, 10),
    role:        payload.role,
    displayName: payload.displayName || '',
  };
}

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(() => {
    // Re-hydrate from localStorage on initial load
    const token = localStorage.getItem(JWT_KEY);
    return token ? userFromToken(token) : null;
  });

  /**
   * Called after a successful login API response.
   * tokenData = { token, role, userId, displayName }
   */
  const login = useCallback((tokenData) => {
    localStorage.setItem(JWT_KEY, tokenData.token);
    setCurrentUser({
      userId:      tokenData.userId,
      role:        tokenData.role,
      displayName: tokenData.displayName,
    });
  }, []);

  /** Called on logout — clears storage and resets state. */
  const logout = useCallback(() => {
    localStorage.removeItem(JWT_KEY);
    setCurrentUser(null);
  }, []);

  const value = { currentUser, login, logout };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export default AuthContext;
