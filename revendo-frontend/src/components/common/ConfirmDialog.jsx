import React from 'react';

/**
 * Bootstrap modal confirm dialog.
 * Props:
 *   show       {boolean}   — visibility
 *   title      {string}    — modal title
 *   message    {string}    — body text
 *   onConfirm  {function}  — called when user clicks "Confirm"
 *   onCancel   {function}  — called when user closes/cancels
 *   confirmLabel {string}  — button label (default "Confirm")
 *   variant      {string}  — Bootstrap colour (default "danger")
 * Requirements: 11.10, 11.11
 */
function ConfirmDialog({
  show,
  title = 'Confirm Action',
  message,
  onConfirm,
  onCancel,
  confirmLabel = 'Confirm',
  variant = 'danger',
}) {
  if (!show) return null;

  return (
    <>
      {/* Backdrop */}
      <div
        className="modal-backdrop fade show"
        onClick={onCancel}
        style={{ zIndex: 1040 }}
      />
      {/* Dialog */}
      <div
        className="modal fade show d-block modal-revendo"
        tabIndex="-1"
        role="dialog"
        style={{ zIndex: 1050 }}
        aria-modal="true"
      >
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header">
              <h5 className="modal-title">{title}</h5>
              <button
                type="button"
                className="btn-close"
                aria-label="Close"
                onClick={onCancel}
              />
            </div>
            <div className="modal-body">
              <p className="mb-0">{message}</p>
            </div>
            <div className="modal-footer">
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={onCancel}
              >
                Cancel
              </button>
              <button
                type="button"
                className={`btn btn-${variant}`}
                onClick={onConfirm}
              >
                {confirmLabel}
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

export default ConfirmDialog;
