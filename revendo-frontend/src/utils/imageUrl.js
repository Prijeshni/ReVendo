
const BACKEND_URL = import.meta.env.PROD
  ? 'https://revendo-backend.onrender.com'
  : 'http://localhost:8080';

export const getImageUrl = (imageUrl) => {
  if (!imageUrl) return '';

  // Keep full URLs unchanged.
  if (/^https?:\/\//i.test(imageUrl)) {
    return imageUrl;
  }

  // Convert a relative image path to the backend URL.
  const path = imageUrl.startsWith('/')
    ? imageUrl
    : `/${imageUrl}`;

  return `${BACKEND_URL}${path}`;
};
