import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import authService from '../services/authService';
import {
  isValidEmail, isValidPhone, isValidPassword, isNonEmpty,
} from '../utils/validators';

/* ── helpers ─────────────────────────────────────────────────────── */

/** Extract a human-readable error message from an Axios error. */
function extractError(err) {
  if (!err.response) return 'Cannot reach the server. Please check your connection.';
  const { status, data } = err.response;
  if (status === 409) return data?.message || 'Email address is already in use.';
  if (status === 400) {
    // Field-level errors from Spring @Valid
    if (data?.errors) return Object.values(data.errors).join(' ');
    return data?.message || 'Please check your details and try again.';
  }
  return data?.message || `Unexpected error (${status}).`;
}

const EMPTY = {
  firstName: '', lastName: '', email: '',
  phoneNumber: '', password: '', confirmPassword: '',
};

/* ── component ───────────────────────────────────────────────────── */

function RegisterPage() {
  const navigate = useNavigate();

  const [form,       setForm]       = useState(EMPTY);
  const [errors,     setErrors]     = useState({});
  const [apiError,   setApiError]   = useState('');
  const [loading,    setLoading]    = useState(false);
  const [success,    setSuccess]    = useState(false);
  const [showPwd,    setShowPwd]    = useState(false);
  const [showConfirm,setShowConfirm]= useState(false);
  const [agreed,     setAgreed]     = useState(false);

  /* ── field change ── */
  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
    // Clear field error on edit
    if (errors[name]) setErrors((e) => ({ ...e, [name]: '' }));
    setApiError('');
  };

  /* ── client-side validation ── */
  const validate = () => {
    const errs = {};
    if (!isNonEmpty(form.firstName))  errs.firstName  = 'First name is required.';
    else if (form.firstName.length > 50) errs.firstName = 'First name must not exceed 50 characters.';

    if (!isNonEmpty(form.lastName))   errs.lastName   = 'Last name is required.';
    else if (form.lastName.length > 50) errs.lastName = 'Last name must not exceed 50 characters.';

    if (!isNonEmpty(form.email))      errs.email      = 'Email address is required.';
    else if (!isValidEmail(form.email)) errs.email    = 'Please enter a valid email address.';

    if (!isNonEmpty(form.phoneNumber))  errs.phoneNumber = 'Phone number is required.';
    else if (!isValidPhone(form.phoneNumber))
      errs.phoneNumber = 'Phone number must be 8–15 digits.';

    if (!isNonEmpty(form.password))   errs.password   = 'Password is required.';
    else if (!isValidPassword(form.password))
      errs.password = 'Password must be between 8 and 128 characters.';

    if (!isNonEmpty(form.confirmPassword))
      errs.confirmPassword = 'Please confirm your password.';
    else if (form.password !== form.confirmPassword)
      errs.confirmPassword = 'Passwords do not match.';

    if (!agreed) errs.terms = 'You must accept the Terms & Conditions.';

    return errs;
  };

  /* ── submit ── */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setApiError('');
    const errs = validate();
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }

    setLoading(true);
    try {
      await authService.register({
        firstName:       form.firstName.trim(),
        lastName:        form.lastName.trim(),
        email:           form.email.trim(),
        phoneNumber:     form.phoneNumber.trim(),
        password:        form.password,
        confirmPassword: form.confirmPassword,
      });
      setSuccess(true);
      setTimeout(() => navigate('/login', { state: { registered: true } }), 2000);
    } catch (err) {
      setApiError(extractError(err));
    } finally {
      setLoading(false);
    }
  };

  /* ── render ── */
  return (
    <div
      style={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, var(--rv-accent-dark) 0%, var(--rv-accent) 60%, #a78bfa 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '32px 16px',
      }}
    >
      <div
        style={{
          background: '#fff',
          borderRadius: 20,
          boxShadow: '0 20px 60px rgba(0,0,0,0.18)',
          width: '100%',
          maxWidth: 540,
          padding: '40px 40px 32px',
        }}
      >
        {/* Logo / heading */}
        <div className="text-center mb-4">
          <Link to="/" style={{ textDecoration: 'none' }}>
            <span style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--rv-accent-dark)' }}>
              🛍️ ReVendo
            </span>
          </Link>
          <h2 style={{ fontWeight: 700, fontSize: '1.4rem', marginTop: 12, color: 'var(--rv-text)' }}>
            Create your account
          </h2>
          <p style={{ color: 'var(--rv-text-muted)', fontSize: '0.9rem', margin: 0 }}>
            Join thousands of buyers and sellers
          </p>
        </div>

        {/* Success banner */}
        {success && (
          <div className="alert alert-success d-flex align-items-center gap-2" role="alert">
            <span>✅</span>
            <span>Account created! Redirecting to login…</span>
          </div>
        )}

        {/* API error */}
        {apiError && (
          <div className="alert alert-danger d-flex align-items-center gap-2" role="alert">
            <span>⚠️</span>
            <span>{apiError}</span>
          </div>
        )}

        {!success && (
          <form onSubmit={handleSubmit} noValidate>
            {/* Name row */}
            <div className="row g-3 mb-3">
              <div className="col-6">
                <label className="form-label" style={labelStyle}>First Name *</label>
                <input
                  type="text"
                  name="firstName"
                  className={`form-control${errors.firstName ? ' is-invalid' : ''}`}
                  style={inputStyle}
                  placeholder="Jane"
                  maxLength={50}
                  value={form.firstName}
                  onChange={handleChange}
                  autoComplete="given-name"
                />
                {errors.firstName && <span className="field-error">{errors.firstName}</span>}
              </div>
              <div className="col-6">
                <label className="form-label" style={labelStyle}>Last Name *</label>
                <input
                  type="text"
                  name="lastName"
                  className={`form-control${errors.lastName ? ' is-invalid' : ''}`}
                  style={inputStyle}
                  placeholder="Doe"
                  maxLength={50}
                  value={form.lastName}
                  onChange={handleChange}
                  autoComplete="family-name"
                />
                {errors.lastName && <span className="field-error">{errors.lastName}</span>}
              </div>
            </div>

            {/* Email */}
            <div className="mb-3">
              <label className="form-label" style={labelStyle}>Email Address *</label>
              <input
                type="email"
                name="email"
                className={`form-control${errors.email ? ' is-invalid' : ''}`}
                style={inputStyle}
                placeholder="jane@example.com"
                maxLength={254}
                value={form.email}
                onChange={handleChange}
                autoComplete="email"
              />
              {errors.email && <span className="field-error">{errors.email}</span>}
            </div>

            {/* Phone */}
            <div className="mb-3">
              <label className="form-label" style={labelStyle}>Phone Number *</label>
              <input
                type="tel"
                name="phoneNumber"
                className={`form-control${errors.phoneNumber ? ' is-invalid' : ''}`}
                style={inputStyle}
                placeholder="8–15 digits"
                maxLength={15}
                value={form.phoneNumber}
                onChange={handleChange}
                autoComplete="tel"
              />
              {errors.phoneNumber && <span className="field-error">{errors.phoneNumber}</span>}
            </div>

            {/* Password */}
            <div className="mb-3">
              <label className="form-label" style={labelStyle}>Password *</label>
              <div className="input-group">
                <input
                  type={showPwd ? 'text' : 'password'}
                  name="password"
                  className={`form-control${errors.password ? ' is-invalid' : ''}`}
                  style={{ ...inputStyle, borderRight: 'none' }}
                  placeholder="At least 8 characters"
                  maxLength={128}
                  value={form.password}
                  onChange={handleChange}
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  className="input-group-text"
                  style={{ background: 'var(--rv-accent-light)', border: '1px solid #e5e7eb', cursor: 'pointer' }}
                  onClick={() => setShowPwd((v) => !v)}
                  title={showPwd ? 'Hide password' : 'Show password'}
                  aria-label={showPwd ? 'Hide password' : 'Show password'}
                >
                  {showPwd ? '🙈' : '👁️'}
                </button>
              </div>
              {errors.password && <span className="field-error">{errors.password}</span>}
            </div>

            {/* Confirm Password */}
            <div className="mb-3">
              <label className="form-label" style={labelStyle}>Confirm Password *</label>
              <div className="input-group">
                <input
                  type={showConfirm ? 'text' : 'password'}
                  name="confirmPassword"
                  className={`form-control${errors.confirmPassword ? ' is-invalid' : ''}`}
                  style={{ ...inputStyle, borderRight: 'none' }}
                  placeholder="Repeat your password"
                  maxLength={128}
                  value={form.confirmPassword}
                  onChange={handleChange}
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  className="input-group-text"
                  style={{ background: 'var(--rv-accent-light)', border: '1px solid #e5e7eb', cursor: 'pointer' }}
                  onClick={() => setShowConfirm((v) => !v)}
                  aria-label={showConfirm ? 'Hide password' : 'Show password'}
                >
                  {showConfirm ? '🙈' : '👁️'}
                </button>
              </div>
              {errors.confirmPassword && (
                <span className="field-error">{errors.confirmPassword}</span>
              )}
            </div>

            {/* Password strength hint */}
            {form.password && (
              <PasswordStrength password={form.password} />
            )}

            {/* Terms */}
            <div className="mb-4">
              <div className="d-flex align-items-start gap-2">
                <input
                  type="checkbox"
                  id="terms"
                  className="form-check-input mt-1"
                  checked={agreed}
                  onChange={(e) => { setAgreed(e.target.checked); if (errors.terms) setErrors((er) => ({ ...er, terms: '' })); }}
                  style={{ accentColor: 'var(--rv-accent)', width: 16, height: 16, cursor: 'pointer' }}
                />
                <label htmlFor="terms" style={{ fontSize: '0.85rem', color: 'var(--rv-text-muted)', cursor: 'pointer' }}>
                  I agree to the{' '}
                  <a href="/terms" target="_blank" rel="noopener noreferrer"
                     style={{ color: 'var(--rv-accent)', fontWeight: 600 }}>
                    Terms of Service
                  </a>
                  {' '}and{' '}
                  <a href="/privacy" target="_blank" rel="noopener noreferrer"
                     style={{ color: 'var(--rv-accent)', fontWeight: 600 }}>
                    Privacy Policy
                  </a>
                </label>
              </div>
              {errors.terms && <span className="field-error">{errors.terms}</span>}
            </div>

            {/* Submit */}
            <button
              type="submit"
              className="btn btn-primary w-100"
              disabled={loading}
              style={{
                padding: '0.75rem',
                fontSize: '1rem',
                fontWeight: 700,
                borderRadius: 10,
                letterSpacing: '0.01em',
              }}
            >
              {loading ? (
                <span className="d-flex align-items-center justify-content-center gap-2">
                  <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                  Creating account…
                </span>
              ) : 'Create Account'}
            </button>
          </form>
        )}

        {/* Login link */}
        <p className="text-center mt-3 mb-0" style={{ fontSize: '0.875rem', color: 'var(--rv-text-muted)' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ color: 'var(--rv-accent)', fontWeight: 600 }}>
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}

/* ── Password strength indicator ── */
function PasswordStrength({ password }) {
  const len     = password.length;
  const hasUpper = /[A-Z]/.test(password);
  const hasDigit = /\d/.test(password);
  const hasSpecial = /[^A-Za-z0-9]/.test(password);
  const score = [len >= 8, hasUpper, hasDigit, hasSpecial].filter(Boolean).length;

  const levels = [
    { label: 'Too short', color: '#e5e7eb', active: '#dc2626' },
    { label: 'Weak',      color: '#e5e7eb', active: '#f97316' },
    { label: 'Fair',      color: '#e5e7eb', active: '#eab308' },
    { label: 'Strong',    color: '#e5e7eb', active: '#22c55e' },
  ];
  const current = levels[score - 1] || levels[0];

  if (len === 0) return null;
  return (
    <div className="mb-3">
      <div className="d-flex gap-1 mb-1">
        {[0,1,2,3].map((i) => (
          <div key={i} style={{
            flex: 1, height: 4, borderRadius: 2,
            background: i < score ? current.active : '#e5e7eb',
            transition: 'background 200ms',
          }} />
        ))}
      </div>
      <span style={{ fontSize: '0.75rem', color: current.active, fontWeight: 600 }}>
        {current.label}
      </span>
    </div>
  );
}

/* ── shared styles ── */
const labelStyle = { fontWeight: 600, fontSize: '0.85rem', marginBottom: 4, color: 'var(--rv-text)' };
const inputStyle  = { borderRadius: 8, border: '1px solid #e5e7eb', padding: '0.55rem 0.75rem', fontSize: '0.9rem' };

export default RegisterPage;
