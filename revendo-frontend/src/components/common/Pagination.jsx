import React from 'react';

/**
 * Page-number pagination controls.
 *
 * Props:
 *   currentPage  {number}   0-based current page index
 *   totalPages   {number}
 *   onPageChange {function} called with the new 0-based page index
 *
 * Requirements: 4.14, 4.15
 */
function Pagination({ currentPage, totalPages, onPageChange }) {
  if (!totalPages || totalPages <= 1) return null;

  // Show at most 5 page buttons centred on currentPage
  const getPages = () => {
    const pages = [];
    const half = 2;
    let start = Math.max(0, currentPage - half);
    let end = Math.min(totalPages - 1, start + 4);
    if (end - start < 4) start = Math.max(0, end - 4);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  };

  return (
    <nav aria-label="Page navigation" className="mt-4">
      <ul className="pagination pagination-revendo justify-content-center mb-0">
        {/* Previous */}
        <li className={`page-item${currentPage === 0 ? ' disabled' : ''}`}>
          <button
            className="page-link"
            onClick={() => onPageChange(currentPage - 1)}
            disabled={currentPage === 0}
            aria-label="Previous page"
          >
            ‹
          </button>
        </li>

        {/* First page shortcut */}
        {getPages()[0] > 0 && (
          <>
            <li className="page-item">
              <button className="page-link" onClick={() => onPageChange(0)}>1</button>
            </li>
            {getPages()[0] > 1 && (
              <li className="page-item disabled">
                <span className="page-link">…</span>
              </li>
            )}
          </>
        )}

        {/* Numbered pages */}
        {getPages().map((p) => (
          <li key={p} className={`page-item${p === currentPage ? ' active' : ''}`}>
            <button
              className="page-link"
              onClick={() => p !== currentPage && onPageChange(p)}
              aria-current={p === currentPage ? 'page' : undefined}
            >
              {p + 1}
            </button>
          </li>
        ))}

        {/* Last page shortcut */}
        {getPages()[getPages().length - 1] < totalPages - 1 && (
          <>
            {getPages()[getPages().length - 1] < totalPages - 2 && (
              <li className="page-item disabled">
                <span className="page-link">…</span>
              </li>
            )}
            <li className="page-item">
              <button className="page-link" onClick={() => onPageChange(totalPages - 1)}>
                {totalPages}
              </button>
            </li>
          </>
        )}

        {/* Next */}
        <li className={`page-item${currentPage >= totalPages - 1 ? ' disabled' : ''}`}>
          <button
            className="page-link"
            onClick={() => onPageChange(currentPage + 1)}
            disabled={currentPage >= totalPages - 1}
            aria-label="Next page"
          >
            ›
          </button>
        </li>
      </ul>
    </nav>
  );
}

export default Pagination;
