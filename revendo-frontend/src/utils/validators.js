// =====================================================
// validators.js — Client-side validation helpers
// Requirements: 2.3, 2.5, 2.6, 2.7, 4.8, 5.8
// =====================================================

/** Basic email format: local@domain.tld */
export const isValidEmail = (str) =>
  typeof str === 'string' && /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(str.trim());

/** Phone: 8–15 digits only */
export const isValidPhone = (str) =>
  typeof str === 'string' && /^\d{8,15}$/.test(str.trim());

/** Password: 8–128 characters */
export const isValidPassword = (str) =>
  typeof str === 'string' && str.length >= 8 && str.length <= 128;

/** Price range: min >= 0, min <= max */
export const isValidPriceRange = (min, max) => {
  const mn = parseFloat(min);
  const mx = parseFloat(max);
  if (isNaN(mn) || isNaN(mx)) return false;
  return mn >= 0 && mn <= mx;
};

/** Non-empty string (after trimming) */
export const isNonEmpty = (str) =>
  typeof str === 'string' && str.trim().length > 0;

/** Image file type: jpg/jpeg/png only */
export const isValidImageFile = (file) => {
  if (!file) return false;
  const name = file.name.toLowerCase();
  return name.endsWith('.jpg') || name.endsWith('.jpeg') || name.endsWith('.png');
};

/** Image file size: ≤ 5 MB */
export const isValidImageSize = (file) => {
  if (!file) return false;
  return file.size <= 5 * 1024 * 1024;
};
