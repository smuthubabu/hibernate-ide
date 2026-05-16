import React, { useState } from 'react';
import './ResultsGrid.css';

const PAGE_SIZE = 50;

export default function ResultsGrid({ result, isExecuting }) {
  const [page, setPage] = useState(0);

  if (isExecuting) {
    return (
      <div className="results-grid">
        <div className="results-executing">
          <div className="spinner"></div>
          <span>Executing query…</span>
        </div>
      </div>
    );
  }

  if (!result) {
    return (
      <div className="results-grid results-empty">
        <span>Results will appear here</span>
      </div>
    );
  }

  if (!result.success) {
    return (
      <div className="results-grid">
        <div className="results-error">
          <div className="error-icon">✗</div>
          <div className="error-body">
            <div className="error-title">Query Error</div>
            <pre className="error-detail">{result.message}</pre>
          </div>
        </div>
      </div>
    );
  }

  const { columns = [], rows = [], rowCount = 0, executionTimeMs, affectedRows } = result;

  if (!columns.length) {
    return (
      <div className="results-grid">
        <div className="results-info-bar">
          <span className="results-ok">✓ {result.message || `${affectedRows ?? 0} row(s) affected`}</span>
          <span className="results-time">{executionTimeMs}ms</span>
        </div>
      </div>
    );
  }

  const totalPages = Math.ceil(rows.length / PAGE_SIZE);
  const pageRows   = rows.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  return (
    <div className="results-grid">
      <div className="results-info-bar">
        <span className="results-count">{rowCount} row{rowCount !== 1 ? 's' : ''}</span>
        {executionTimeMs !== undefined && <span className="results-time">{executionTimeMs}ms</span>}
        {totalPages > 1 && (
          <div className="pagination">
            <button onClick={() => setPage(0)}          disabled={page === 0}               className="page-btn">«</button>
            <button onClick={() => setPage(p => p - 1)} disabled={page === 0}               className="page-btn">‹</button>
            <span className="page-info">{page + 1} / {totalPages}</span>
            <button onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}   className="page-btn">›</button>
            <button onClick={() => setPage(totalPages - 1)} disabled={page >= totalPages - 1} className="page-btn">»</button>
          </div>
        )}
      </div>

      <div className="table-wrap">
        <table className="results-table">
          <thead>
            <tr>
              <th className="row-num-header">#</th>
              {columns.map((col, i) => (
                <th key={i} title={col}>{col}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {pageRows.map((row, ri) => (
              <tr key={ri} className={ri % 2 === 0 ? 'even' : 'odd'}>
                <td className="row-num">{page * PAGE_SIZE + ri + 1}</td>
                {row.map((cell, ci) => (
                  <td key={ci} title={cell ?? ''} className={cell === null ? 'null-cell' : ''}>
                    {cell === null ? <span className="null-label">NULL</span> : String(cell)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
