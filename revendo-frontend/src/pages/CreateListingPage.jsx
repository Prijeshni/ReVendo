import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import ImageUploader from '../components/listings/ImageUploader';
import useToast from '../hooks/useToast';
import productService from '../services/productService';

// ── Option lists must match the backend Listing enums exactly ──────
const CATEGORIES = [
  'Electronics', 'Furniture', 'Books', 'Home Appliances', 'Household Goods',
];
const CONDITIONS = ['New', 'Like New', 'Good', 'Fair', 'Poor'];
const RENTAL_UNITS = [
  { value: 'PER_DAY',   label: 'Per Day' },
  { value: 'PER_WEEK',  label: 'Per Week' },
  { value: 'PER_MONTH', label: 'Per Month' },
];
const CAT_ICONS = {
  Electronics: '💻', Furniture: '🛋️',
  Books: '📚', 'Home Appliances': '🏠', 'Household Goods': '🧺',
};

// ── Helpers ────────────────────────────────────────────────────────
function extractError(err) {
  if (!err?.response) {
    return 'Cannot reach the server. Is the Spring Boot backend running on port 8080?';
  }
  const { status, data } = err.response;
  // Try every known shape Spring Boot / GlobalExceptionHandler can return
  const serverMsg =
    data?.message ||
    data?.error   ||
    (data?.errors ? Object.values(data.errors).join(' ') : null);

  if (status === 401) return 'You must be logged in to create a listing.';
  if (status === 403) return serverMsg || 'Access denied.';
  if (status === 400) return serverMsg || 'Please check your details and try again.';
  if (status === 415) return 'The server rejected the request format. This is a bug — please report it.';
  // For any other status, always show the real server message if available
  return serverMsg || `Server error (${status}). Please try again.`;
}

const EMPTY = {
  title: '', description: '', category: '',
  condition: '', listingType: 'FOR_SALE',
  price: '', rentalPeriodUnit: '',
};

// ── Component ──────────────────────────────────────────────────────
function CreateListingPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [form,     setForm]    = useState(EMPTY);
  const [images,   setImages]  = useState([]);   // File[]
  const [errors,   setErrors]  = useState({});
  const [apiError, setApiError]= useState('');
  const [loading,  setLoading] = useState(false);

  // ── Field change ────────────────────────────────────────────────
  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => {
      const updated = { ...f, [name]: value };
      // Clear rentalPeriodUnit when switching to FOR_SALE
      if (name === 'listingType' && value === 'FOR_SALE') {
        updated.rentalPeriodUnit = '';
      }
      return updated;
    });
    if (errors[name]) setErrors((er) => ({ ...er, [name]: '' }));
    setApiError('');
  };

  // ── Validation ──────────────────────────────────────────────────
  const validate = () => {
    const errs = {};

    if (!form.title.trim())
      errs.title = 'Product title is required.';
    else if (form.title.trim().length > 150)
      errs.title = 'Title must not exceed 150 characters.';

    if (!form.description.trim())
      errs.description = 'Description is required.';
    else if (form.description.trim().length > 2000)
      errs.description = 'Description must not exceed 2000 characters.';

    if (!form.category)
      errs.category = 'Please select a category.';

    if (!form.condition)
      errs.condition = 'Please select the item condition.';

    if (!form.price)
      errs.price = 'Price is required.';
    else if (isNaN(parseFloat(form.price)) || parseFloat(form.price) < 0.01)
      errs.price = 'Price must be at least ₹0.01.';
    else if (parseFloat(form.price) > 999999999.99)
      errs.price = 'Price exceeds the maximum allowed value.';

    if (form.listingType === 'FOR_RENT' && !form.rentalPeriodUnit)
      errs.rentalPeriodUnit = 'Please select a rental period for rental listings.';

    if (images.length === 0)
      errs.images = 'At least one product image is required.';

    return errs;
  };

  // ── Submit ──────────────────────────────────────────────────────
  const handleSubmit = async (e) => {
    e.preventDefault();
    setApiError('');

    const errs = validate();
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      // Scroll to first error
      const el = document.querySelector('.field-error');
      el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }

    setLoading(true);
    try {
      // Build multipart/form-data payload.
      // The backend uses @ModelAttribute for text fields + @RequestParam("images")
      // for files, all inside a single multipart request.
      const formData = new FormData();
      formData.append('title',       form.title.trim());
      formData.append('description', form.description.trim());
      formData.append('category', form.category.replace(/ /g, '_'));
      formData.append('condition', form.condition.replace(/ /g, '_'));
      formData.append('listingType', form.listingType);
      formData.append('price',       form.price);
      if (form.listingType === 'FOR_RENT' && form.rentalPeriodUnit) {
        formData.append('rentalPeriodUnit', form.rentalPeriodUnit);
      }
      images.forEach((file) => formData.append('images', file));

      const res = await productService.createListing(formData);
      const newId = res.data?.id;

      showToast('Listing created successfully! 🎉');

      if (newId) {
        navigate(`/products/${newId}`);
      } else {
        navigate('/products');
      }
    } catch (err) {
      const msg = extractError(err);
      setApiError(msg);
      showToast(msg, 'error');
    } finally {
      setLoading(false);
    }
  };

  const isRental = form.listingType === 'FOR_RENT';

  // ── Render ───────────────────────────────────────────────────────
  return (
    <div style={{ background: 'var(--rv-surface)', minHeight: '100vh', paddingBottom: 48 }}>

      {/* Page header */}
      <div
        style={{
          background: 'linear-gradient(135deg, var(--rv-accent-dark) 0%, var(--rv-accent) 100%)',
          padding: '36px 0 28px',
          color: '#fff',
        }}
      >
        <div className="container">
          <nav aria-label="breadcrumb" style={{ marginBottom: 8 }}>
            <ol className="breadcrumb mb-0" style={{ fontSize: '0.82rem', opacity: 0.85 }}>
              <li className="breadcrumb-item">
                <Link to="/" style={{ color: 'rgba(255,255,255,0.8)', textDecoration: 'none' }}>Home</Link>
              </li>
              <li className="breadcrumb-item">
                <Link to="/dashboard" style={{ color: 'rgba(255,255,255,0.8)', textDecoration: 'none' }}>Dashboard</Link>
              </li>
              <li className="breadcrumb-item active" style={{ color: '#fff' }}>New Listing</li>
            </ol>
          </nav>
          <h1 style={{ fontWeight: 700, fontSize: '1.8rem', margin: 0 }}>
            📦 Sell Your Item
          </h1>
          <p style={{ opacity: 0.88, marginTop: 6, marginBottom: 0, fontSize: '0.95rem' }}>
            Fill in the details below and reach thousands of buyers.
          </p>
        </div>
      </div>

      <div className="container py-4">
        <div className="row justify-content-center">
          <div className="col-lg-8">

            {/* API error banner */}
            {apiError && (
              <div className="alert alert-danger d-flex align-items-start gap-2 mb-4" role="alert">
                <span style={{ fontSize: '1.2rem' }}>⚠️</span>
                <span>{apiError}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} noValidate encType="multipart/form-data">

              {/* ── Card 1: Listing basics ─────────────────────── */}
              <FormCard title="📋 Listing Details">

                {/* Title */}
                <div className="mb-3">
                  <label style={labelStyle}>
                    Product Title <Required />
                  </label>
                  <input
                    type="text"
                    name="title"
                    className={`form-control${errors.title ? ' is-invalid' : ''}`}
                    style={inputStyle}
                    placeholder="e.g. iPhone 13, Office Chair, Harry Potter Series…"
                    maxLength={150}
                    value={form.title}
                    onChange={handleChange}
                  />
                  <div className="d-flex justify-content-between">
                    {errors.title
                      ? <span className="field-error">{errors.title}</span>
                      : <span />}
                    <span style={{ fontSize: '0.75rem', color: 'var(--rv-text-muted)' }}>
                      {form.title.length}/150
                    </span>
                  </div>
                </div>

                {/* Category */}
                <div className="mb-3">
                  <label style={labelStyle}>Category <Required /></label>
                  <div className="row g-2">
                    {CATEGORIES.map((cat) => (
                      <div key={cat} className="col-6 col-sm-4">
                        <button
                          type="button"
                          onClick={() => {
                            setForm((f) => ({ ...f, category: cat }));
                            setErrors((er) => ({ ...er, category: '' }));
                          }}
                          style={{
                            width: '100%',
                            padding: '10px 8px',
                            borderRadius: 10,
                            border: `2px solid ${form.category === cat ? 'var(--rv-accent)' : '#e5e7eb'}`,
                            background: form.category === cat ? 'var(--rv-accent-light)' : '#fff',
                            color: form.category === cat ? 'var(--rv-accent-dark)' : 'var(--rv-text)',
                            cursor: 'pointer',
                            fontWeight: form.category === cat ? 700 : 400,
                            fontSize: '0.85rem',
                            transition: 'all 150ms ease',
                            textAlign: 'center',
                          }}
                        >
                          <div style={{ fontSize: '1.3rem', marginBottom: 2 }}>{CAT_ICONS[cat]}</div>
                          {cat}
                        </button>
                      </div>
                    ))}
                  </div>
                  {errors.category && <span className="field-error mt-1 d-block">{errors.category}</span>}
                </div>

                {/* Description */}
                <div className="mb-3">
                  <label style={labelStyle}>Description <Required /></label>
                  <textarea
                    name="description"
                    className={`form-control${errors.description ? ' is-invalid' : ''}`}
                    style={{ ...inputStyle, resize: 'vertical', minHeight: 110 }}
                    placeholder="Describe the item — include brand, model, age, any defects or damage, reason for selling…"
                    maxLength={2000}
                    value={form.description}
                    onChange={handleChange}
                    rows={4}
                  />
                  <div className="d-flex justify-content-between">
                    {errors.description
                      ? <span className="field-error">{errors.description}</span>
                      : <span />}
                    <span style={{ fontSize: '0.75rem', color: 'var(--rv-text-muted)' }}>
                      {form.description.length}/2000
                    </span>
                  </div>
                </div>
              </FormCard>

              {/* ── Card 2: Pricing & type ─────────────────────── */}
              <FormCard title="💰 Pricing & Type">

                {/* Listing type toggle */}
                <div className="mb-3">
                  <label style={labelStyle}>Listing Type <Required /></label>
                  <div className="d-flex gap-2">
                    {[
                      { value: 'FOR_SALE', icon: '🏷️', label: 'For Sale' },
                      { value: 'FOR_RENT', icon: '🔄', label: 'For Rent' },
                    ].map((opt) => (
                      <button
                        key={opt.value}
                        type="button"
                        onClick={() => {
                          setForm((f) => ({
                            ...f,
                            listingType: opt.value,
                            rentalPeriodUnit: opt.value === 'FOR_SALE' ? '' : f.rentalPeriodUnit,
                          }));
                        }}
                        style={{
                          flex: 1,
                          padding: '12px 8px',
                          borderRadius: 10,
                          border: `2px solid ${form.listingType === opt.value ? 'var(--rv-accent)' : '#e5e7eb'}`,
                          background: form.listingType === opt.value ? 'var(--rv-accent-light)' : '#fff',
                          color: form.listingType === opt.value ? 'var(--rv-accent-dark)' : 'var(--rv-text)',
                          fontWeight: form.listingType === opt.value ? 700 : 400,
                          cursor: 'pointer',
                          transition: 'all 150ms',
                          fontSize: '0.9rem',
                        }}
                      >
                        <span style={{ fontSize: '1.4rem', display: 'block', marginBottom: 4 }}>
                          {opt.icon}
                        </span>
                        {opt.label}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Price */}
                <div className={`mb-3 ${isRental ? 'row g-3' : ''}`}>
                  <div className={isRental ? 'col-sm-6' : ''}>
                    <label style={labelStyle}>
                      Price (₹) <Required />
                    </label>
                    <div className="input-group">
                      <span
                        className="input-group-text"
                        style={{ background: 'var(--rv-accent-light)', border: '1px solid #e5e7eb', fontWeight: 700, color: 'var(--rv-accent-dark)' }}
                      >
                        ₹
                      </span>
                      <input
                        type="number"
                        name="price"
                        className={`form-control${errors.price ? ' is-invalid' : ''}`}
                        style={{ ...inputStyle, borderLeft: 'none' }}
                        placeholder="0.00"
                        min={0.01}
                        max={999999999.99}
                        step={0.01}
                        value={form.price}
                        onChange={handleChange}
                      />
                    </div>
                    {errors.price && <span className="field-error">{errors.price}</span>}
                  </div>

                  {/* Rental period — only shown for FOR_RENT */}
                  {isRental && (
                    <div className="col-sm-6">
                      <label style={labelStyle}>Rental Period <Required /></label>
                      <select
                        name="rentalPeriodUnit"
                        className={`form-select${errors.rentalPeriodUnit ? ' is-invalid' : ''}`}
                        style={inputStyle}
                        value={form.rentalPeriodUnit}
                        onChange={handleChange}
                      >
                        <option value="">Select period…</option>
                        {RENTAL_UNITS.map((u) => (
                          <option key={u.value} value={u.value}>{u.label}</option>
                        ))}
                      </select>
                      {errors.rentalPeriodUnit && (
                        <span className="field-error">{errors.rentalPeriodUnit}</span>
                      )}
                    </div>
                  )}
                </div>
              </FormCard>

              {/* ── Card 3: Condition ─────────────────────────── */}
              <FormCard title="🔍 Item Condition">
                <div className="mb-1">
                  <label style={labelStyle}>Condition <Required /></label>
                  <div className="row g-2">
                    {CONDITIONS.map((cond) => {
                      const meta = {
                        'New':      { icon: '✨', tip: 'Brand new, never used' },
                        'Like New': { icon: '👍', tip: 'Used once or twice, no signs of wear' },
                        'Good':     { icon: '👌', tip: 'Minor signs of use, fully functional' },
                        'Fair':     { icon: '🔧', tip: 'Visible wear but works correctly' },
                        'Poor':     { icon: '⚠️', tip: 'Heavy wear, may need repairs' },
                      }[cond] || { icon: '•', tip: '' };
                      const selected = form.condition === cond;
                      return (
                        <div key={cond} className="col-6 col-sm-4">
                          <button
                            type="button"
                            title={meta.tip}
                            onClick={() => {
                              setForm((f) => ({ ...f, condition: cond }));
                              setErrors((er) => ({ ...er, condition: '' }));
                            }}
                            style={{
                              width: '100%',
                              padding: '10px 6px',
                              borderRadius: 10,
                              border: `2px solid ${selected ? 'var(--rv-accent)' : '#e5e7eb'}`,
                              background: selected ? 'var(--rv-accent-light)' : '#fff',
                              color: selected ? 'var(--rv-accent-dark)' : 'var(--rv-text)',
                              fontWeight: selected ? 700 : 400,
                              cursor: 'pointer',
                              fontSize: '0.82rem',
                              transition: 'all 150ms',
                              textAlign: 'center',
                            }}
                          >
                            <div style={{ fontSize: '1.2rem', marginBottom: 2 }}>{meta.icon}</div>
                            {cond}
                            <div style={{ fontSize: '0.68rem', color: selected ? 'var(--rv-accent)' : 'var(--rv-text-muted)', marginTop: 2 }}>
                              {meta.tip}
                            </div>
                          </button>
                        </div>
                      );
                    })}
                  </div>
                  {errors.condition && <span className="field-error mt-1 d-block">{errors.condition}</span>}
                </div>
              </FormCard>

              {/* ── Card 4: Images ────────────────────────────── */}
              <FormCard title="📸 Product Images">
                <p style={{ fontSize: '0.85rem', color: 'var(--rv-text-muted)', marginBottom: '0.75rem' }}>
                  Upload 1–5 clear photos (JPG, JPEG or PNG, max 5 MB each).
                  The first image will be shown as the main product photo.
                </p>
                <ImageUploader
                  images={images}
                  onChange={(files) => {
                    setImages(files);
                    if (files.length > 0) setErrors((er) => ({ ...er, images: '' }));
                  }}
                />
                {errors.images && (
                  <span className="field-error mt-2 d-block">{errors.images}</span>
                )}
              </FormCard>

              {/* ── Summary preview ───────────────────────────── */}
              {(form.title || form.price) && (
                <div
                  style={{
                    background: 'var(--rv-accent-light)',
                    border: '1px solid var(--rv-accent-muted)',
                    borderRadius: 12,
                    padding: '16px 20px',
                    marginBottom: '1.5rem',
                  }}
                >
                  <p style={{ fontWeight: 700, color: 'var(--rv-accent-dark)', marginBottom: 6, fontSize: '0.85rem' }}>
                    📝 Listing Preview
                  </p>
                  <div style={{ fontSize: '0.875rem', color: 'var(--rv-text)' }}>
                    <strong>{form.title || '—'}</strong>
                    {form.category && <span className="ms-2 text-muted">· {form.category}</span>}
                    {form.condition && <span className="ms-2 text-muted">· {form.condition}</span>}
                    {form.price && (
                      <span className="ms-2" style={{ color: 'var(--rv-accent)', fontWeight: 700 }}>
                        · ₹{parseFloat(form.price).toLocaleString('en-IN')}
                        {isRental && form.rentalPeriodUnit && ` / ${form.rentalPeriodUnit.replace('PER_', '').toLowerCase()}`}
                      </span>
                    )}
                  </div>
                </div>
              )}

              {/* ── Submit button ─────────────────────────────── */}
              <button
                type="submit"
                className="btn btn-primary w-100"
                disabled={loading}
                style={{
                  padding: '0.85rem',
                  fontSize: '1.05rem',
                  fontWeight: 700,
                  borderRadius: 12,
                  letterSpacing: '0.01em',
                }}
              >
                {loading ? (
                  <span className="d-flex align-items-center justify-content-center gap-2">
                    <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                    Uploading &amp; saving…
                  </span>
                ) : '🚀 Publish Listing'}
              </button>

              <p className="text-center mt-3 mb-0" style={{ fontSize: '0.8rem', color: 'var(--rv-text-muted)' }}>
                Your listing will be set to <strong>Active</strong> and immediately visible on the marketplace.
              </p>
            </form>
          </div>

          {/* ── Tips sidebar ─── */}
          <div className="col-lg-3 d-none d-lg-block">
            <div style={{ position: 'sticky', top: 80 }}>
              <TipCard />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ── Sub-components ───────────────────────────────────────────────── */

function FormCard({ title, children }) {
  return (
    <div
      style={{
        background: '#fff',
        border: '1px solid var(--rv-border)',
        borderRadius: 14,
        padding: '24px',
        marginBottom: '1.5rem',
        boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
      }}
    >
      <h6
        style={{
          fontWeight: 700,
          fontSize: '0.95rem',
          color: 'var(--rv-accent-dark)',
          marginBottom: '1.25rem',
          paddingBottom: '0.75rem',
          borderBottom: '2px solid var(--rv-accent-light)',
        }}
      >
        {title}
      </h6>
      {children}
    </div>
  );
}

function Required() {
  return <span style={{ color: 'var(--rv-danger)', marginLeft: 2 }}>*</span>;
}

function TipCard() {
  const tips = [
    { icon: '📷', tip: 'Use good lighting and multiple angles for photos.' },
    { icon: '✏️', tip: 'Write an honest, detailed description for faster sales.' },
    { icon: '💡', tip: 'Research similar listings to price competitively.' },
    { icon: '🔍', tip: 'Choose the most accurate category and condition.' },
    { icon: '⚡', tip: 'Listings with images get 5× more views.' },
  ];
  return (
    <div
      style={{
        background: 'var(--rv-accent-light)',
        border: '1px solid var(--rv-accent-muted)',
        borderRadius: 14,
        padding: '20px',
      }}
    >
      <p style={{ fontWeight: 700, color: 'var(--rv-accent-dark)', marginBottom: '1rem', fontSize: '0.9rem' }}>
        💬 Listing Tips
      </p>
      <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {tips.map((t, i) => (
          <li key={i} style={{ display: 'flex', gap: 10, marginBottom: 12, fontSize: '0.83rem', color: 'var(--rv-text)' }}>
            <span style={{ fontSize: '1.1rem', flexShrink: 0 }}>{t.icon}</span>
            <span>{t.tip}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

const labelStyle = {
  fontWeight: 600, fontSize: '0.875rem',
  marginBottom: 6, color: 'var(--rv-text)',
  display: 'block',
};
const inputStyle = {
  borderRadius: 8,
  border: '1px solid #e5e7eb',
  padding: '0.55rem 0.75rem',
  fontSize: '0.9rem',
};

export default CreateListingPage;
