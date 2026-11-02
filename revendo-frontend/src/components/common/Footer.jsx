import React from 'react';

/** Site footer with informational links. Requirements: 1.7 */
function Footer() {
  return (
    <footer className="footer-revendo mt-auto">
      <div className="container">
        <div className="row gy-3">
          <div className="col-md-4">
            <span style={{ fontWeight: 700, fontSize: '1.2rem', color: '#fff' }}>
              ReVendo
            </span>
            <p className="mt-1 mb-0" style={{ fontSize: '0.85rem', color: '#ddd6fe' }}>
              P2P marketplace for pre-owned goods.
            </p>
          </div>
          <div className="col-md-4">
            <p className="fw-600 mb-2" style={{ color: '#fff', fontWeight: 600 }}>
              Quick Links
            </p>
            <ul className="list-unstyled mb-0" style={{ fontSize: '0.875rem' }}>
              <li><a href="/products">Browse Listings</a></li>
              <li><a href="/sell">Sell an Item</a></li>
              <li><a href="/wishlist">My Wishlist</a></li>
            </ul>
          </div>
          <div className="col-md-4">
            <p className="fw-600 mb-2" style={{ color: '#fff', fontWeight: 600 }}>
              Company
            </p>
            <ul className="list-unstyled mb-0" style={{ fontSize: '0.875rem' }}>
              <li><a href="/about">About</a></li>
              <li><a href="/contact">Contact</a></li>
              <li><a href="/terms">Terms of Service</a></li>
              <li><a href="/privacy">Privacy Policy</a></li>
            </ul>
          </div>
        </div>
        <hr style={{ borderColor: 'rgba(255,255,255,0.15)', margin: '24px 0 16px' }} />
        <p className="text-center mb-0" style={{ fontSize: '0.8rem', color: '#ddd6fe' }}>
          &copy; {new Date().getFullYear()} ReVendo. All rights reserved.
        </p>
      </div>
    </footer>
  );
}

export default Footer;
