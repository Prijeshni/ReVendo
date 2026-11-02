import { useContext } from 'react';
import AuthContext from '../context/AuthContext';

/**
 * Convenience hook — returns the AuthContext value.
 * Usage: const { currentUser, login, logout } = useAuth();
 */
export function useAuth() {
  return useContext(AuthContext);
}

export default useAuth;
