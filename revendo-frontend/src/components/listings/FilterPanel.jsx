import React, { useState } from 'react';

const CATEGORIES = [
  'Electronics', 'Furniture', 'Books', 'Home Appliances', 'Household Goods',
];
const CONDITIONS  = ['New', 'Like New', 'Good', 'Fair', 'Poor'];
const SORT_OPTIONS = [
  { value: '',           label: 'Newest First (default)' },
  { value: 'PRICE_ASC',  label: 'Price: Low to High' },
  { value: 'PRICE_DESC', label: 'Price: High to Low' },
];

/**
 * Sidebar filter panel for the product listing page.
 *
 * Props:
 *   filters       {object}   current filter state
 *   onFilterChange {function} called with the updated filters object
 *   onClear       {function} called to reset all filters
 *
 * Requirements: 4.3–4.8, 4.13–4.15
 */
function FilterPanel({ filters = {}, onFilterChange, onClear }) {
  const [priceError, setPriceError] = useState('');

  const update = (key, value) => {
    onFilterChange({ ...filters, [key]: value });
  };

  const handleMinPrice = (val) => {
    const min = val === '' ? '' : parseFloat(val);
    const max = filters.maxPrice !== undefined && filters.maxPrice !== '' ? parseFloat(filters.maxPrice) : Infinity;
    if (val !== '' && !isNaN(min) && min > max && filters.maxPrice !== '') {
      setPriceError('Minimum price must not exceed maximum price.');
    } else {
      setPriceError('');
      update('minPrice', val);
    }
  };

  const handleMaxPrice = (val) => {
    const max = val === '' ? '' : parseFloat(val);
    const min = filters.minPrice !== undefined && filters.minPrice !== '' ? parseFloat(filters.minPrice) : 0;
    if (val !== '' && !isNaN(max) && max < min && filters.minPrice !== '') {
      setPriceError('Minimum price must not exceed maximum price.');
    } else {
      setPriceError('');
      update('maxPrice', val);
    }
  };

  return (
    <aside className="filter-panel">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <span className="filter-title">Filters</span>
        <button
          className="btn btn-link btn-sm p-0"
          style={{ color: 'var(--rv-accent)', fontSize: '0.8rem' }}
          onClick={onClear}
        >
          Clear all
        </button>
      </div>

      {/* Listing Type */}
      <div className="mb-3">
        <label className="form-label fw-600 mb-1" style={{ fontSize: '0.85rem', fontWeight: 600 }}>
          Type
        </label>
        <div className="d-flex gap-2 flex-wrap">
          {['', 'FOR_SALE', 'FOR_RENT'].map((v) => (
            <button
              key={v}
              className={`btn btn-sm ${filters.listingType === v ? 'btn-primary' : 'btn-outline-secondary'}`}
              style={{ fontSize: '0.8rem' }}
              onClick={() => update('listingType', v)}
            >
              {v === '' ? 'All' : v === 'FOR_SALE' ? 'For Sale' : 'For Rent'}
            </button>
          ))}
        </div>
      </div>

      {/* Category */}
      <div className="mb-3">
        <label className="form-label fw-600 mb-1" style={{ fontSize: '0.85rem', fontWeight: 600 }}>
          Category
        </label>
        <select
          className="form-select form-select-sm"
          value={filters.category || ''}
          onChange={(e) => update('category', e.target.value)}
        >
          <option value="">All Categories</option>
          {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
      </div>

      {/* Condition */}
      <div className="mb-3">
        <label className="form-label fw-600 mb-1" style={{ fontSize: '0.85rem', fontWeight: 600 }}>
          Condition
        </label>
        <select
          className="form-select form-select-sm"
          value={filters.condition || ''}
          onChange={(e) => update('condition', e.target.value)}
        >
          <option value="">All Conditions</option>
          {CONDITIONS.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
      </div>

      {/* Price range */}
      <div className="mb-3">
        <label className="form-label fw-600 mb-1" style={{ fontSize: '0.85rem', fontWeight: 600 }}>
          Price Range
        </label>
        <div className="d-flex gap-2 align-items-center">
          <input
            type="number"
            className="form-control form-control-sm"
            placeholder="Min"
            min={0}
            value={filters.minPrice ?? ''}
            onChange={(e) => handleMinPrice(e.target.value)}
            style={{ width: 80 }}
          />
          <span className="text-muted" style={{ fontSize: '0.8rem' }}>–</span>
          <input
            type="number"
            className="form-control form-control-sm"
            placeholder="Max"
            min={0}
            value={filters.maxPrice ?? ''}
            onChange={(e) => handleMaxPrice(e.target.value)}
            style={{ width: 80 }}
          />
        </div>
        {priceError && (
          <span className="field-error">{priceError}</span>
        )}
      </div>

      {/* Sort */}
      <div className="mb-1">
        <label className="form-label fw-600 mb-1" style={{ fontSize: '0.85rem', fontWeight: 600 }}>
          Sort By
        </label>
        <select
          className="form-select form-select-sm"
          value={filters.sort || ''}
          onChange={(e) => update('sort', e.target.value)}
        >
          {SORT_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </select>
      </div>
    </aside>
  );
}

export default FilterPanel;
