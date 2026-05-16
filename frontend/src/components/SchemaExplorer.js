import React, { useState, useEffect, useCallback } from 'react';
import { schemaApi, connectionApi } from '../services/api';
import './SchemaExplorer.css';

export default function SchemaExplorer({ activeConnection, onInsert }) {
  const [tables, setTables]             = useState([]);
  const [expanded, setExpanded]         = useState({});
  const [tableDetails, setTableDetails] = useState({});
  const [loading, setLoading]           = useState(false);
  const [error, setError]               = useState('');
  const [hbmModal, setHbmModal]         = useState(null);
  const [hbmXml, setHbmXml]            = useState('');
  const [savingMapping, setSavingMapping] = useState(false);

  const loadTables = useCallback(async () => {
    if (!activeConnection) { setTables([]); return; }
    setLoading(true); setError('');
    try {
      const res = await schemaApi.getTables(activeConnection.id);
      setTables(res.data.data || []);
    } catch (e) { setError(e.response?.data?.message || e.message); }
    finally { setLoading(false); }
  }, [activeConnection]);

  useEffect(() => { loadTables(); }, [loadTables]);

  const toggleTable = async (tableName) => {
    const isExpanded = expanded[tableName];
    setExpanded(e => ({ ...e, [tableName]: !isExpanded }));
    if (!isExpanded && !tableDetails[tableName]) {
      try {
        const res = await schemaApi.getTableDetail(activeConnection.id, tableName);
        setTableDetails(d => ({ ...d, [tableName]: res.data.data }));
      } catch (e) { setError(e.response?.data?.message || e.message); }
    }
  };

  const generateHbm = async (tableName) => {
    try {
      const res = await schemaApi.generateHbm(activeConnection.id, tableName, null);
      setHbmXml(res.data.data);
      setHbmModal(tableName);
    } catch (e) { setError(e.response?.data?.message || e.message); }
  };

  const saveMapping = async () => {
    setSavingMapping(true);
    try {
      await connectionApi.addMapping(activeConnection.id, { connectionId: activeConnection.id, hbmXml });
      setHbmModal(null); setHbmXml('');
    } catch (e) { setError(e.response?.data?.message || e.message); }
    finally { setSavingMapping(false); }
  };

  if (!activeConnection) {
    return <div className="schema-explorer empty">Connect to a database to browse tables</div>;
  }

  return (
    <div className="schema-explorer">
      <div className="schema-toolbar">
        <span className="schema-title">Tables ({tables.length})</span>
        <button className="refresh-btn" onClick={loadTables} title="Refresh">↻</button>
      </div>

      {loading && <div className="schema-loading">Loading…</div>}
      {error   && <div className="schema-error">{error}</div>}

      <div className="table-tree">
        {tables.map(table => (
          <div key={table.tableName} className="table-node">
            <div className="table-header" onClick={() => toggleTable(table.tableName)}>
              <span className="tree-arrow">{expanded[table.tableName] ? '▾' : '▸'}</span>
              <span className="table-icon">{table.tableType === 'VIEW' ? '⊞' : '⊟'}</span>
              <span className="table-name">{table.tableName}</span>
              <div className="table-actions">
                <button className="table-action-btn" title="SELECT *"
                  onClick={e => { e.stopPropagation(); onInsert(`SELECT * FROM ${table.tableName} FETCH FIRST 100 ROWS ONLY`); }}>▶</button>
                <button className="table-action-btn hbm-btn" title="Generate HBM XML"
                  onClick={e => { e.stopPropagation(); generateHbm(table.tableName); }}>⊕</button>
              </div>
            </div>

            {expanded[table.tableName] && tableDetails[table.tableName] && (
              <div className="column-list">
                {tableDetails[table.tableName].rowCount > 0 && (
                  <div className="row-count">≈ {tableDetails[table.tableName].rowCount.toLocaleString()} rows</div>
                )}
                {tableDetails[table.tableName].columns?.map(col => (
                  <div key={col.columnName} className="column-item"
                    onClick={() => onInsert(col.columnName)}
                    title={`${col.dataType}(${col.columnSize})${col.nullable ? '' : ' NOT NULL'}`}>
                    <span className={`col-icon ${col.primaryKey ? 'pk' : col.nullable ? 'nullable' : ''}`}>
                      {col.primaryKey ? '🔑' : '◦'}
                    </span>
                    <span className="col-name">{col.columnName}</span>
                    <span className="col-type">{col.dataType}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>

      {hbmModal && (
        <div className="modal-overlay" onClick={() => setHbmModal(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <span>HBM Mapping — {hbmModal}</span>
              <button className="modal-close" onClick={() => setHbmModal(null)}>✕</button>
            </div>
            <div className="modal-body">
              <p className="modal-note">
                Review and register this mapping to enable HQL on <strong>{hbmModal}</strong>.
              </p>
              <textarea className="hbm-editor" value={hbmXml}
                onChange={e => setHbmXml(e.target.value)} rows={20} spellCheck={false} />
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setHbmModal(null)}>Cancel</button>
              <button className="btn btn-primary" onClick={saveMapping} disabled={savingMapping}>
                {savingMapping ? 'Registering…' : 'Register Mapping'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
