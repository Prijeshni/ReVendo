import toast from 'react-hot-toast';

/**
 * Exposes a showToast(message, type) helper backed by react-hot-toast.
 * Types: 'success' | 'error' | 'info' | 'warning'
 * Auto-dismisses after 4 seconds. Requirements: 15.6
 */
export function useToast() {
  const showToast = (message, type = 'success') => {
    const options = { duration: 4000 };
    switch (type) {
      case 'error':   return toast.error(message, options);
      case 'info':    return toast(message, { ...options, icon: 'ℹ️' });
      case 'warning': return toast(message, { ...options, icon: '⚠️' });
      default:        return toast.success(message, options);
    }
  };
  return { showToast };
}

export default useToast;
