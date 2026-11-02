import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';

/**
 * Redirects unauthenticated users to /login.
 * Wrap any route element that requires authentication.
 * Requirements: 5.2
 */
function ProtectedRoute() {
  const { currentUser } = useAuth();
  if (!currentUser) {
    return <Navigate to="/login" replace />;
  }
  return <Outlet />;
}

export default ProtectedRoute;
