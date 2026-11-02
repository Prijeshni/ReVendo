// =====================================================
// formatters.js — Display formatting helpers
// Requirements: 4.8, 9.5, 15.4
// =====================================================

/**
 * Format a numeric amount as currency (USD by default).
 * @param {number|string} amount
 * @returns {string}  e.g. "$1,234.56"
 */
export const formatCurrency = (amount) => {
  const num = parseFloat(amount);
  if (isNaN(num)) return '$0.00';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(num);
};

/**
 * Format an ISO date string (or Date) into a human-readable date.
 * @param {string|Date} isoString
 * @returns {string}  e.g. "Jan 15, 2024"
 */
export const formatDate = (isoString) => {
  if (!isoString) return '';
  const d = new Date(isoString);
  if (isNaN(d.getTime())) return '';
  return d.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

/**
 * Truncate text to maxLength characters, appending "…" if truncated.
 * @param {string} text
 * @param {number} maxLength
 * @returns {string}
 */
export const truncateText = (text, maxLength) => {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength).trimEnd() + '…';
};
