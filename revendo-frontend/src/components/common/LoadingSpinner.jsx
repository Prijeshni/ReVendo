import React from 'react';

/** Centred Bootstrap spinner. Requirements: 15.5 */
function LoadingSpinner({ size = 'md', text = 'Loading…' }) {
  const sizeCls = size === 'sm' ? 'spinner-border-sm' : '';
  return (
    <div className="spinner-wrapper">
      <div className={`spinner-border ${sizeCls}`} role="status" aria-live="polite">
        <span className="visually-hidden">{text}</span>
      </div>
    </div>
  );
}

export default LoadingSpinner;
