import React, { useEffect, useState, useCallback, useRef } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import ProductCard from '../components/common/ProductCard';
import FilterPanel from '../components/listings/FilterPanel';
import Pagination from '../components/common/Pagination';
import LoadingSpinner from '../components/common/LoadingSpinner';
import productService from '../services/productService';

const EMPTY_FILTERS = {
  keyword:     '',
  category:    '',
  condition:   '',
  listingType: '',
  minPrice:    '',
  maxPrice:    '',
  sort:        '',
};

const SORT_LABELS = {
  '':           'Newest First',
  'PRICE_ASC':  'Price: Low to High',
  'PRICE_DESC': 'Price: High to Low',
};

/**
 * Product Browsing / Listings page.
 * - Reads ?keyword and ?category from URL so the Navbar search and
 *   homepage category cards navigate straight here with pre-applied filters.
 * - Syncs all filter/page state back to the URL so the browser back button works.
 * - Fetches from GET /api/products with all supported query params.
 */
function ProductListingPage() {
  const navigate         = useNavigate();
  const [searchParams]   = useSearchParams();

  // ── State ──────────────────────────────────────────────────────────
  const [filters, setFilters] = useState(() => ({
    ...EMPTY_FILTERS,
    keyword:     searchParams.get('keyword')  || '',
    category:    searchParams.get('category') || '',
  }));
  const [page,       setPage]       = useState(0);
  const [data,       setData]       = useState(null);   // backend Page<ListingResponse>
  const [loading,    setLoading]    = useState(true);
  const [error,      setError]      = useState(null);
  const [showFilter, setShowFilter] = useState(false);  // mobile drawer toggle

  // local search input (separate so Enter triggers the fetch, not every keypress)
  const [searchInput, setSearchInput] = useState(filters.keyword);
  const didMountRef = useRef(false);

  // ── Fetch ───────────────────────────────────────────────────────────
  const fetchListings = useCallback(async (f, p) => {
    setLoading(true);
    setError(null);
    try {
      const res = await productService.getListings({ ...f, page: p, size: 12 });
      setData(res.data);
    } catch (err) {
      const msg = err.response?.data?.message || 'Could not load listings. Is the backend running?';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch on mount + whenever filters/page change
  useEffect(() => {
    fetchListings(filters, page);
  }, [filters, page, fetchListings]);

  // When URL params change (e.g. user clicks a category from the homepage), sync filters
  useEffect(() => {
    if (!didMountRef.current) { didMountRef.current = true; return; }
    const kw  = searchParams.get('keyword')  || '';
    const cat = searchParams.get('category') || '';
    setFilters((prev) => ({ ...prev, keyword: kw, category: cat }));
    setSearchInput(kw);
    setPage(0);
  }, [searchParams]);

  // ── Handlers ────────────────────────────────────────────────────────
  const handleSearch = (e) => {
    e.preventDefault();
    setFilters((f) => ({ ...f, keyword: searchInput.trim() }));
    setPage(0);
  };

  const handleFilterChange = (updated) => {
    setFilters(updated);
    setPage(0);
  };

  const handleClear = () => {
    setFilters(EMPTY_FILTERS);
    setSearchInput('');
    setPage(0);
    navigate('/products', { replace: true });
  };

  const handleSortChange = (e) => {
    setFilters((f) => ({ ...f, sort: e.target.value }));
    setPage(0);
  };

  const handlePageChange = (p) => {
    setPage(p);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // ── Derived values ───────────────────────────────────────────────────
  const totalElements = data?.totalElements ?? 0;
  const totalPages    = data?.totalPages    ?? 0;
  const listings      = data?.content       ?? [];
  const hasFilters    = Object.values({ ...filters, sort: undefined }).some(Boolean);

  // ── Render ───────────────────────────────────────────────────────────
  return (
    <div style={{ background: 'var(--rv-surface)', minHeight: '100vh' }}>

      {/* ── Page header ── */}
      <div
        style={{
          background: 'linear-gradient(135deg, var(--rv-accent-dark) 0%, var(--rv-accent) 100%)',
          padding: '40px 0 32px',
          color: '#fff',
        }}
      >
        <div className="container">
          <p className="section-eyebrow mb-1" style={{ color: 'rgba(255,255,255,0.8)' }}>
            Marketplace
          </p>
          <h1 style={{ fontWeight: 700, fontSize: '2rem', letterSpacing: '-0.02em', marginBottom: '0.5rem' }}>
            Explore Pre-Owned Products
          </h1>
          <p style={{ opacity: 0.85, marginBottom: '1.5rem', fontSize: '1rem' }}>
            Find great deals on products you love.
          </p>

          {/* Top search bar */}
          <form
            className="d-flex"
            style={{ maxWidth: 540 }}
            onSubmit={handleSearch}
          >
            <input
              type="search"
              className="form-control form-control-lg"
              placeholder="Search by title or description…"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              style={{
                borderRadius: '12px 0 0 12px',
                border: 'none',
                background: 'rgba(255,255,255,0.95)',
                color: '#1e1b4b',
                fontSize: '0.95rem',
              }}
              aria-label="Search products"
            />
            <button
              type="submit"
              className="btn btn-lg"
              style={{
                borderRadius: '0 12px 12px 0',
                background: '#fff',
                color: 'var(--rv-accent)',
                fontWeight: 700,
                border: 'none',
                padding: '0 20px',
                whiteSpace: 'nowrap',
              }}
            >
              Search
            </button>
          </form>
        </div>
      </div>

      <div className="container py-4">
        {/* ── Toolbar row: results count + sort + mobile filter toggle ── */}
        <div className="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
          <div className="d-flex align-items-center gap-2 flex-wrap">
            <span style={{ fontWeight: 600, color: 'var(--rv-text)' }}>
              {loading
                ? 'Loading…'
                : `${totalElements.toLocaleString()} listing${totalElements !== 1 ? 's' : ''}`}
            </span>
            {filters.keyword && (
              <span
                className="badge"
                style={{ background: 'var(--rv-accent-light)', color: 'var(--rv-accent)', fontSize: '0.78rem' }}
              >
                "{filters.keyword}"
              </span>
            )}
            {filters.category && (
              <span
                className="badge"
                style={{ background: 'var(--rv-accent-light)', color: 'var(--rv-accent)', fontSize: '0.78rem' }}
              >
                {filters.category}
              </span>
            )}
            {hasFilters && (
              <button
                className="btn btn-link btn-sm p-0"
                style={{ color: 'var(--rv-danger)', fontSize: '0.8rem' }}
                onClick={handleClear}
              >
                ✕ Clear filters
              </button>
            )}
          </div>

          <div className="d-flex gap-2 align-items-center">
            {/* Sort dropdown */}
            <select
              className="form-select form-select-sm"
              value={filters.sort || ''}
              onChange={handleSortChange}
              style={{ width: 'auto', minWidth: 160, fontSize: '0.85rem' }}
              aria-label="Sort listings"
            >
              {Object.entries(SORT_LABELS).map(([v, l]) => (
                <option key={v} value={v}>{l}</option>
              ))}
            </select>

            {/* Mobile filter toggle */}
            <button
              className="btn btn-outline-primary btn-sm d-lg-none"
              onClick={() => setShowFilter((s) => !s)}
            >
              🎚 Filters
            </button>
          </div>
        </div>

        <div className="row g-3">
          {/* ── Filter sidebar ── */}
          <div className={`col-lg-3 ${showFilter ? '' : 'd-none d-lg-block'}`}>
            <div style={{ position: 'sticky', top: 80 }}>
              <FilterPanel
                filters={filters}
                onFilterChange={handleFilterChange}
                onClear={handleClear}
              />
            </div>
          </div>

          {/* ── Product grid ── */}
          <div className="col-lg-9">
            {/* Loading */}
            {loading && <LoadingSpinner />}

            {/* Error */}
            {!loading && error && (
              <div className="alert alert-warning d-flex align-items-start gap-2" role="alert">
                <span style={{ fontSize: '1.3rem' }}>⚠️</span>
                <div>
                  <strong>Unable to load listings</strong>
                  <p className="mb-1 mt-1" style={{ fontSize: '0.875rem' }}>{error}</p>
                  <button className="btn btn-sm btn-outline-warning" onClick={() => fetchListings(filters, page)}>
                    Retry
                  </button>
                </div>
              </div>
            )}

            {/* Empty state */}
            {!loading && !error && listings.length === 0 && (
              <div className="empty-state">
                <div className="empty-icon">🔍</div>
                <h5>No listings found</h5>
                <p className="text-muted mb-3">
                  {hasFilters
                    ? 'Try adjusting your filters or clearing them to see more results.'
                    : 'There are no active listings yet. Be the first to sell something!'}
                </p>
                <div className="d-flex gap-2 justify-content-center flex-wrap">
                  {hasFilters && (
                    <button className="btn btn-outline-primary btn-sm" onClick={handleClear}>
                      Clear Filters
                    </button>
                  )}
                  <Link to="/sell" className="btn btn-primary btn-sm">+ List an Item</Link>
                </div>
              </div>
            )}

            {/* Grid */}
            {!loading && !error && listings.length > 0 && (
              <>
                <div className="row g-3">
                  {listings.map((listing) => (
                    <div key={listing.id} className="col-sm-6 col-xl-4">
                      <ProductCard listing={listing} />
                    </div>
                  ))}
                </div>

                {/* Pagination */}
                <Pagination
                  currentPage={page}
                  totalPages={totalPages}
                  onPageChange={handlePageChange}
                />

                {/* Page info */}
                <p className="text-center text-muted mt-2 mb-0" style={{ fontSize: '0.8rem' }}>
                  Page {page + 1} of {totalPages} · {totalElements.toLocaleString()} total listings
                </p>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default ProductListingPage;
