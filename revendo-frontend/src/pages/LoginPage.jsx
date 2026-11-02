import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import useAuth from '../hooks/useAuth';
import authService from '../services/authService';
import { isValidEmail, isNonEmpty } from '../utils/validators';

/* ── helpers ─────────────────────────────────────────────────────── */

function extractError(err) {
  if (!err.response) return 'Cannot reach the server. Please check your connection.';
  const { status, data } = err.response;
  if (status === 403) return data?.message || 'This account has been suspended.';
  if (status === 429) return data?.message || 'Account temporarily locked. Try again in 15 minutes.';
  if (status === 400) return data?.message || 'Invalid email or password.';
  return data?.message || `Unexpected error (${status}).`;
}

/* ── component ───────────────────────────────────────────────────── */

function LoginPage() {
  const navigate  = useNavigate();
  const location  = useLocation();
  const { currentUser, login } = useAuth();

  const [email,     setEmail]     = useState('');
  const [password,  setPassword]  = useState('');
  const [errors,    setErrors]    = useState({});
  const [apiError,  setApiError]  = useState('');
  const [loading,   setLoading]   = useState(false);
  const [showPwd,   setShowPwd]   = useState(false);
  const [regNotice, setRegNotice] = useState(false);

  // Already logged in → go home
  useEffect(() => {
    if (currentUser) navigate('/', { replace: true });
  }, [currentUser, navigate]);

  // Show success notice if redirected from register
  useEffect(() => {
    if (location.state?.registered) setRegNotice(true);
  }, [location.state]);

  /* ── validation ── */
  const validate = () => {
    const errs = {};
    if (!isNonEmpty(email))     errs.email    = 'Email address is required.';
    else if (!isValidEmail(email)) errs.email = 'Please enter a valid email address.';
    if (!isNonEmpty(password))  errs.password = 'Password is required.';
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
      const res = await authService.login({
        email:    email.trim(),
        password: password,
      });
      // res.data = { token, role, userId, displayName }
      login(res.data);
      navigate('/', { replace: true });
    } catch (err) {
      setApiError(extractError(err));
    } finally {
      setLoading(false);
    }
  };

  const clearFieldError = (field) => {
    if (errors[field]) setErrors((e) => ({ ...e, [field]: '' }));
    setApiError('');
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
          maxWidth: 460,
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
            Welcome back
          </h2>
          <p style={{ color: 'var(--rv-text-muted)', fontSize: '0.9rem', margin: 0 }}>
            Sign in to your account
          </p>
        </div>

        {/* Registration success notice */}
        {regNotice && (
          <div className="alert alert-success d-flex align-items-center gap-2" role="alert">
            <span>✅</span>
            <span>Account created successfully! Please sign in.</span>
          </div>
        )}

        {/* API error */}
        {apiError && (
          <div className="alert alert-danger d-flex align-items-center gap-2" role="alert">
            <span>⚠️</span>
            <span>{apiError}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          {/* Email */}
          <div className="mb-3">
            <label htmlFor="email" style={labelStyle}>Email Address</label>
            <input
              id="email"
              type="email"
              className={`form-control${errors.email ? ' is-invalid' : ''}`}
              style={inputStyle}
              placeholder="jane@example.com"
              maxLength={254}
              value={email}
              onChange={(e) => { setEmail(e.target.value); clearFieldError('email'); }}
              autoComplete="email"
              autoFocus
            />
            {errors.email && <span className="field-error">{errors.email}</span>}
          </div>

          {/* Password */}
          <div className="mb-3">
            <div className="d-flex justify-content-between align-items-center mb-1">
              <label htmlFor="password" style={labelStyle}>Password</label>
              {/* No forgot-password endpoint exists in backend — omitted */}
            </div>
            <div className="input-group">
              <input
                id="password"
                type={showPwd ? 'text' : 'password'}
                className={`form-control${errors.password ? ' is-invalid' : ''}`}
                style={{ ...inputStyle, borderRight: 'none' }}
                placeholder="Your password"
                maxLength={128}
                value={password}
                onChange={(e) => { setPassword(e.target.value); clearFieldError('password'); }}
                autoComplete="current-password"
              />
              <button
                type="button"
                className="input-group-text"
                style={{ background: 'var(--rv-accent-light)', border: '1px solid #e5e7eb', cursor: 'pointer' }}
                onClick={() => setShowPwd((v) => !v)}
                aria-label={showPwd ? 'Hide password' : 'Show password'}
              >
                {showPwd ? '🙈' : '👁️'}
              </button>
            </div>
            {errors.password && <span className="field-error">{errors.password}</span>}
          </div>

          {/* Submit */}
          <button
            type="submit"
            className="btn btn-primary w-100 mt-2"
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
                Signing in…
              </span>
            ) : 'Sign In'}
          </button>
        </form>

        {/* Divider */}
        <div className="d-flex align-items-center gap-2 my-3">
          <hr style={{ flex: 1, margin: 0, borderColor: '#e5e7eb' }} />
          <span style={{ fontSize: '0.78rem', color: 'var(--rv-text-muted)' }}>OR</span>
          <hr style={{ flex: 1, margin: 0, borderColor: '#e5e7eb' }} />
        </div>

        {/* Register CTA */}
        <Link
          to="/register"
          className="btn w-100"
          style={{
            border: '2px solid var(--rv-accent)',
            color: 'var(--rv-accent)',
            fontWeight: 700,
            borderRadius: 10,
            padding: '0.65rem',
            fontSize: '0.95rem',
            textDecoration: 'none',
            display: 'block',
            textAlign: 'center',
            transition: 'background 200ms',
          }}
          onMouseEnter={(e) => { e.currentTarget.style.background = 'var(--rv-accent-light)'; }}
          onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; }}
        >
          Create a new account
        </Link>

        <p className="text-center mt-3 mb-0" style={{ fontSize: '0.8rem', color: 'var(--rv-text-muted)' }}>
          By signing in you agree to our{' '}
          <a href="/terms" style={{ color: 'var(--rv-accent)' }}>Terms</a>
          {' '}and{' '}
          <a href="/privacy" style={{ color: 'var(--rv-accent)' }}>Privacy Policy</a>.
        </p>
      </div>
    </div>
  );
}

const labelStyle = { fontWeight: 600, fontSize: '0.875rem', marginBottom: 4, color: 'var(--rv-text)', display: 'block' };
const inputStyle  = { borderRadius: 8, border: '1px solid #e5e7eb', padding: '0.55rem 0.75rem', fontSize: '0.9rem' };

export default LoginPage;
