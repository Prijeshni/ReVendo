import React, { useState } from 'react';

/**
 * Image carousel for product details.
 * Uses Bootstrap Carousel markup.
 * Falls back to a placeholder when no images are provided.
 *
 * Props:
 *   images  {string[]}  Array of image URLs
 *   alt     {string}    Alt text prefix
 *
 * Requirements: 6.2, 15.7
 */
function ImageCarousel({ images = [], alt = 'Listing image' }) {
  const [activeIndex, setActiveIndex] = useState(0);

  if (!images || images.length === 0) {
    return (
      <div
        className="img-placeholder rounded"
        style={{ height: 320, borderRadius: 12 }}
        aria-label="No image available"
      >
        🖼️ No Image
      </div>
    );
  }

  const prev = () => setActiveIndex((i) => (i - 1 + images.length) % images.length);
  const next = () => setActiveIndex((i) => (i + 1) % images.length);

  return (
    <div
      className="carousel-revendo rounded overflow-hidden"
      style={{ background: '#f3f4f6', borderRadius: 12 }}
    >
      {/* Main image */}
      <div style={{ position: 'relative' }}>
        <img
          src={images[activeIndex]}
          alt={`${alt} ${activeIndex + 1}`}
          style={{ width: '100%', height: 360, objectFit: 'cover' }}
          onError={(e) => {
            e.target.src = '';
            e.target.style.display = 'none';
            e.target.parentNode.querySelector('.carousel-fallback').style.display = 'flex';
          }}
        />
        <div
          className="carousel-fallback img-placeholder"
          style={{ height: 360, display: 'none' }}
        >
          🖼️
        </div>

        {/* Controls */}
        {images.length > 1 && (
          <>
            <button
              onClick={prev}
              aria-label="Previous image"
              style={{
                position: 'absolute', top: '50%', left: 12,
                transform: 'translateY(-50%)',
                background: 'rgba(0,0,0,0.45)', color: '#fff',
                border: 'none', borderRadius: '50%',
                width: 36, height: 36, cursor: 'pointer', fontSize: '1.1rem',
              }}
            >
              ‹
            </button>
            <button
              onClick={next}
              aria-label="Next image"
              style={{
                position: 'absolute', top: '50%', right: 12,
                transform: 'translateY(-50%)',
                background: 'rgba(0,0,0,0.45)', color: '#fff',
                border: 'none', borderRadius: '50%',
                width: 36, height: 36, cursor: 'pointer', fontSize: '1.1rem',
              }}
            >
              ›
            </button>

            {/* Counter */}
            <span
              style={{
                position: 'absolute', bottom: 10, right: 14,
                background: 'rgba(0,0,0,0.5)', color: '#fff',
                borderRadius: 20, padding: '2px 10px', fontSize: '0.8rem',
              }}
            >
              {activeIndex + 1} / {images.length}
            </span>
          </>
        )}
      </div>

      {/* Thumbnail strip */}
      {images.length > 1 && (
        <div className="d-flex gap-2 p-2" style={{ overflowX: 'auto' }}>
          {images.map((url, idx) => (
            <button
              key={idx}
              onClick={() => setActiveIndex(idx)}
              style={{
                padding: 0, border: 'none', borderRadius: 6,
                outline: idx === activeIndex ? '2px solid #7c3aed' : '2px solid transparent',
                cursor: 'pointer', flexShrink: 0,
              }}
              aria-label={`Show image ${idx + 1}`}
            >
              <img
                src={url}
                alt={`${alt} thumbnail ${idx + 1}`}
                style={{ width: 60, height: 60, objectFit: 'cover', borderRadius: 6, display: 'block' }}
              />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default ImageCarousel;
