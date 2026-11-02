import React from 'react';
import { Toaster } from 'react-hot-toast';

/**
 * Mount this once near the root of the app.
 * Toasts are triggered via the useToast() hook.
 * Auto-dismiss: 4 seconds. Requirements: 15.6
 */
function ToastNotification() {
  return (
    <Toaster
      position="top-right"
      gutter={8}
      toastOptions={{
        duration: 4000,
        style: {
          borderRadius: '10px',
          background: '#fff',
          color: '#1e1b4b',
          boxShadow: '0 4px 16px rgba(124,58,237,0.15)',
          fontFamily: 'Inter, sans-serif',
          fontSize: '0.9rem',
        },
        success: {
          iconTheme: { primary: '#7c3aed', secondary: '#ede9fe' },
        },
        error: {
          iconTheme: { primary: '#dc2626', secondary: '#fee2e2' },
        },
      }}
    />
  );
}

export default ToastNotification;
