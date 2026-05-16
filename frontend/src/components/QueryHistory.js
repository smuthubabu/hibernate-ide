import React from 'react';
import './QueryHistory.css';

function formatTime(ts) {
  const d = new Date(ts);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

export default function QueryHistory({ history, onSelect }) {
  if (!history.length) {
    return (
      <div className="query-history empty">
        <span>No queries executed yet</span>
      </div>
    );
  }

  return (
    <div className="query-history">
      {history.map((entry, i) => (
        <div key={i} className={`history-item ${entry.result?.success === false ? 'failed' : ''}`}
          onClick={() => onSelect(entry)}>
          <div className="history-meta">
            <span className="history-status">{entry.result?.success === false ? '✗' : '✓'}</span>
            <span className="history-conn">{entry.connectionName}</span>
            <span className="history-time">{formatTime(entry.timestamp)}</span>
          </div>
          <div className="history-query">{entry.query}</div>
          {entry.result?.success && (
            <div className="history-stats">
              {entry.result.rowCount > 0 ? `${entry.result.rowCount} rows` : entry.result.message || ''}
              {entry.result.executionTimeMs !== undefined ? ` · ${entry.result.executionTimeMs}ms` : ''}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
