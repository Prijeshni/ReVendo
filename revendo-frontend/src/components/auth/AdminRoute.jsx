import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';

/**
 * Allows only ROLE_ADMIN users to access child routes.
 * Redirects non-admin (including unauthenticated) users to the home page.
 * Requirements: 13.1, 13.2
 */
function AdminRoute() {
  const { currentUser } = useAuth();
  if (!currentUser || currentUser.role !== 'ROLE_ADMIN') {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}

export default AdminRoute;
