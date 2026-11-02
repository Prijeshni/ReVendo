import React from 'react';
import { Link } from 'react-router-dom';

function NotFoundPage() {
  return (
    <div className="container text-center" style={{ padding: '80px 16px' }}>
      <h1 style={{ fontSize: '5rem', fontWeight: 700, color: 'var(--rv-accent)' }}>404</h1>
      <h2 className="mb-3">Page Not Found</h2>
      <p className="text-muted mb-4">
        The page you are looking for does not exist or has been moved.
      </p>
      <Link to="/" className="btn btn-primary">Back to Home</Link>
    </div>
  );
}

export default NotFoundPage;
