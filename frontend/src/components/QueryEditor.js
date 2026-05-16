import React, { useState, useCallback, useMemo, useEffect, useRef } from 'react';
import CodeMirror from '@uiw/react-codemirror';
import { sql, StandardSQL } from '@codemirror/lang-sql';
import { oneDark } from '@codemirror/theme-one-dark';
import { EditorView } from '@codemirror/view';
import { autocompletion } from '@codemirror/autocomplete';
import { queryApi, schemaApi } from '../services/api';
import './QueryEditor.css';

export default function QueryEditor({
  activeConnection, currentQuery, onQueryChange, onSetSqlQuery,
  onExecuting, onQueryExecuted,
  queryType, onQueryTypeChange, theme,
}) {
  const [maxResults, setMaxResults] = useState(500);
  const [sqlModal, setSqlModal]     = useState(null);   // generated SQL string
  const [sqlLoading, setSqlLoading] = useState(false);
  const [sqlError, setSqlError]     = useState('');
  const [copied, setCopied]         = useState(false);
  const [sqlSchema, setSqlSchema]   = useState({});
  const [hqlSchema, setHqlSchema]   = useState({});
  const cancelRef                   = useRef(false);

  useEffect(() => {
    cancelRef.current = false;
    if (!activeConnection) { setSqlSchema({}); setHqlSchema({}); return; }

    (async () => {
      try {
        // ── SQL schema: tables then columns ──
        const tablesRes = await schemaApi.getTables(activeConnection.id);
        const tables = (tablesRes.data.data || []).slice(0, 120);
        if (cancelRef.current) return;

        const sqlSch = {};
        tables.forEach(t => { sqlSch[t.tableName] = []; });
        setSqlSchema({ ...sqlSch });

        const details = await Promise.allSettled(
          tables.map(t => schemaApi.getTableDetail(activeConnection.id, t.tableName))
        );
        if (cancelRef.current) return;
        details.forEach((r, i) => {
          if (r.status === 'fulfilled')
            sqlSch[tables[i].tableName] =
              (r.value.data.data?.columns || []).map(c => c.columnName);
        });
        setSqlSchema({ ...sqlSch });
      } catch (_) {}

      try {
        // ── HQL schema: entity names + properties/associations ──
        const entRes = await schemaApi.getEntities(activeConnection.id);
        if (cancelRef.current) return;
        const entities = entRes.data.data || [];
        const hqlSch = {};
        entities.forEach(e => {
          const props = [];
          if (e.idProperty) props.push(e.idProperty.name);
          (e.properties   || []).forEach(p => props.push(p.name));
          (e.associations || []).forEach(a => props.push(a.name));
          hqlSch[e.entityName] = props;
        });
        setHqlSchema(hqlSch);
      } catch (_) {}
    })();

    return () => { cancelRef.current = true; };
  }, [activeConnection]);

  const execute = useCallback(async () => {
    if (!activeConnection) return;
    const q = currentQuery.trim();
    if (!q) return;
    onExecuting();
    try {
      const res = await queryApi.execute({ connectionId: activeConnection.id, query: q, queryType, maxResults });
      const payload = res.data;
      if (payload.success) {
        onQueryExecuted({ ...payload.data, success: true }, q);
      } else {
        onQueryExecuted({ success: false, message: payload.message }, q);
      }
    } catch (e) {
      onQueryExecuted({ success: false, message: e.response?.data?.message || e.message }, currentQuery);
    }
  }, [activeConnection, currentQuery, queryType, maxResults, onExecuting, onQueryExecuted]);

  const showSql = useCallback(async () => {
    if (!activeConnection || !currentQuery.trim()) return;
    setSqlLoading(true); setSqlError(''); setSqlModal(null);
    try {
      const res = await queryApi.explain({ connectionId: activeConnection.id, query: currentQuery.trim() });
      if (res.data.success) {
        setSqlModal(res.data.data);
      } else {
        setSqlError(res.data.message);
        setSqlModal('');
      }
    } catch (e) {
      setSqlError(e.response?.data?.message || e.message);
      setSqlModal('');
    } finally { setSqlLoading(false); }
  }, [activeConnection, currentQuery]);

  const handleKeyDown = useCallback((e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); execute(); }
  }, [execute]);

  const extensions = useMemo(() => [
    sql({ dialect: StandardSQL, schema: queryType === 'HQL' ? hqlSchema : sqlSchema, upperCaseKeywords: false }),
    EditorView.lineWrapping,
    autocompletion({ activateOnTyping: true, maxRenderedOptions: 30 }),
  ], [sqlSchema, hqlSchema, queryType]);

  return (
    <div className="query-editor" onKeyDown={handleKeyDown}>
      <div className="editor-toolbar">
        <div className="query-type-tabs">
          {['SQL', 'HQL'].map(t => (
            <button key={t} className={`type-tab ${queryType === t ? 'active' : ''}`}
              onClick={() => onQueryTypeChange(t)}>{t}</button>
          ))}
        </div>

        <div className="editor-actions">
          {queryType === 'HQL' && (
            <button className="show-sql-btn" onClick={showSql}
              disabled={sqlLoading || !activeConnection || !currentQuery.trim()}
              title="Show equivalent SQL generated by Hibernate">
              {sqlLoading ? 'Translating…' : '</> Show SQL'}
            </button>
          )}
          <label className="limit-label">
            Limit
            <input type="number" className="limit-input" value={maxResults} min={1} max={5000}
              onChange={e => setMaxResults(parseInt(e.target.value, 10) || 500)} />
          </label>
          <button className="run-btn" onClick={execute} disabled={!activeConnection} title="Run (Ctrl+Enter)">
            ▶ Run
          </button>
        </div>
      </div>

      <div className="editor-area">
        <CodeMirror
          value={currentQuery}
          height="100%"
          theme={theme === 'light' ? 'light' : oneDark}
          extensions={extensions}
          onChange={onQueryChange}
          basicSetup={{
            lineNumbers: true,
            foldGutter: false,
            highlightActiveLine: true,
            autocompletion: false,
            bracketMatching: true,
            closeBrackets: true,
          }}
        />
      </div>

      {!activeConnection && (
        <div className="editor-overlay">
          <span>Connect to a database to start querying</span>
        </div>
      )}

      {/* Show SQL modal */}
      {sqlModal !== null && (
        <div className="modal-overlay" onClick={() => setSqlModal(null)}>
          <div className="modal sql-modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <span>Generated SQL</span>
              <button className="modal-close" onClick={() => setSqlModal(null)}>✕</button>
            </div>
            <div className="modal-body">
              {sqlError
                ? <div className="path-error">{sqlError}</div>
                : <pre className="generated-sql">{sqlModal}</pre>}
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setSqlModal(null)}>Close</button>
              {!sqlError && (<>
                <button className="btn btn-ghost copy-sql-btn" onClick={() => {
                  navigator.clipboard.writeText(sqlModal).then(() => {
                    setCopied(true);
                    setTimeout(() => setCopied(false), 1800);
                  });
                }}>
                  {copied ? '✓ Copied' : 'Copy SQL'}
                </button>
                <button className="btn btn-primary" onClick={() => { (onSetSqlQuery || onQueryChange)(sqlModal); onQueryTypeChange('SQL'); setSqlModal(null); }}>
                  Use as SQL
                </button>
              </>)}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
