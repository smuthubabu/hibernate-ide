import React, { useState, useEffect, useCallback } from 'react';
import { schemaApi, connectionApi, profileApi } from '../services/api';
import EntityGraph from './EntityGraph';
import './EntityExplorer.css';

const ASSOC_ICON = { 'many-to-one': '→', 'one-to-one': '⇒', 'one-to-many': '↠', 'many-to-many': '⇆' };

export default function EntityExplorer({ activeConnection, onEntitySelect }) {
  const [entities, setEntities]         = useState([]);
  const [search, setSearch]             = useState('');
  const [expanded, setExpanded]         = useState({});
  const DEFAULT_MAPPING_PATH = '/Users/muthubabu/Dev/hibernate-query-console/target/classes/com/example/report/';
  const [pathModal, setPathModal]   = useState(false);
  const [hbmText, setHbmText]       = useState(DEFAULT_MAPPING_PATH);
  const [classesText, setClassesText] = useState(DEFAULT_MAPPING_PATH);
  const [loading, setLoading]           = useState(false);
  const [error, setError]               = useState('');
  const [savedPaths, setSavedPaths]     = useState({ hbmPaths: [], classesPaths: [] });
  const [showGraph, setShowGraph]       = useState(false);

  const loadEntities = useCallback(async () => {
    if (!activeConnection) { setEntities([]); return; }
    try {
      const res = await schemaApi.getEntities(activeConnection.id);
      setEntities(res.data.data || []);
    } catch {}
  }, [activeConnection]);

  useEffect(() => { loadEntities(); }, [loadEntities]);

  // Load saved paths for this connection's profile
  useEffect(() => {
    if (!activeConnection) { setSavedPaths({ hbmPaths: [], classesPaths: [] }); return; }
    profileApi.list().then(res => {
      const profile = (res.data.data || []).find(p => p.name === activeConnection.name);
      setSavedPaths({
        hbmPaths: profile?.hbmPaths || [],
        classesPaths: profile?.classesPaths || [],
      });
    }).catch(() => {});
  }, [activeConnection]);

  const splitLines = t => t.split('\n').map(l => l.trim()).filter(Boolean);

  const handleLoad = async () => {
    const hbmPaths = splitLines(hbmText);
    if (!hbmPaths.length) { setError('At least one HBM path is required'); return; }
    setLoading(true); setError('');
    try {
      const res = await connectionApi.loadMappingsFromPath(activeConnection.id, {
        hbmPaths,
        classesPaths: splitLines(classesText),
      });
      if (res.data.success) {
        setEntities(res.data.data || []);
        setSavedPaths({ hbmPaths, classesPaths: splitLines(classesText) });
        setPathModal(false); setHbmText(''); setClassesText('');
      } else { setError(res.data.message); }
    } catch (e) { setError(e.response?.data?.message || e.message); }
    finally { setLoading(false); }
  };

  const toggle = name => setExpanded(e => ({ ...e, [name]: !e[name] }));

  const filtered = entities.filter(e =>
    !search.trim() || e.entityName.toLowerCase().includes(search.toLowerCase())
  );

  if (!activeConnection) {
    return <div className="entity-explorer empty">Connect to a database first</div>;
  }

  return (
    <div className="entity-explorer">
      {/* toolbar */}
      <div className="ee-toolbar">
        <input className="ee-search" placeholder="Search entities…" value={search}
          onChange={e => setSearch(e.target.value)} />
        {entities.length > 0 && (
          <button className="ee-graph-btn" onClick={() => setShowGraph(true)} title="View relationship diagram">⬡</button>
        )}
        <button className="ee-load-btn" onClick={() => {
          // Only pre-fill when textareas are empty (don't overwrite user edits in progress)
          if (!hbmText && savedPaths.hbmPaths.length > 0) {
            setHbmText(savedPaths.hbmPaths.join('\n'));
            setClassesText(savedPaths.classesPaths.join('\n'));
          }
          setPathModal(true);
        }} title="Load mappings from path">⊕</button>
        <button className="ee-refresh-btn" onClick={loadEntities} title="Refresh">↻</button>
      </div>

      {filtered.length === 0 && (
        <div className="ee-empty">
          {entities.length === 0 ? 'No mappings loaded. Click ⊕ to load HBM files.' : 'No match.'}
        </div>
      )}

      <div className="ee-list">
        {filtered.map(entity => (
          <div key={entity.entityName} className="ee-entity">
            <div className="ee-entity-row">
              <button className="ee-arrow" onClick={() => toggle(entity.entityName)}>
                {expanded[entity.entityName] ? '▾' : '▸'}
              </button>
              <span className="ee-icon">◈</span>
              <span className="ee-name" title={entity.className || entity.entityName}
                onClick={() => onEntitySelect(entity.entityName)}>
                {entity.entityName}
              </span>
              <button className="ee-hql-btn" title="Open as HQL query"
                onClick={() => onEntitySelect(entity.entityName)}>HQL</button>
            </div>

            {expanded[entity.entityName] && (
              <div className="ee-props">
                {entity.tableName && (
                  <div className="ee-meta">
                    {entity.schemaName ? `${entity.schemaName}.` : ''}{entity.tableName}
                  </div>
                )}
                {entity.idProperty && (
                  <div className="ee-prop pk" title="Primary key">
                    <span>🔑</span>
                    <span className="ee-pname">{entity.idProperty.name}</span>
                    <span className="ee-ptype">{entity.idProperty.type}</span>
                  </div>
                )}
                {(entity.properties || []).map(p => (
                  <div key={p.name} className={`ee-prop${p.nullable ? '' : ' required'}`}>
                    <span className="ee-pdot">◦</span>
                    <span className="ee-pname">{p.name}</span>
                    <span className="ee-ptype">{p.type}</span>
                  </div>
                ))}
                {(entity.associations || []).map(a => (
                  <div key={a.name} className="ee-assoc">
                    <span className="ee-passoc">{ASSOC_ICON[a.type] || '~'}</span>
                    <span className="ee-pname">{a.name}</span>
                    <span className="ee-ptype">{a.targetEntity}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>

      {showGraph && (
        <EntityGraph entities={entities} onClose={() => setShowGraph(false)} />
      )}

      {/* Load Mappings modal */}
      {pathModal && (
        <div className="modal-overlay" onClick={() => setPathModal(false)}>
          <div className="modal load-mappings-modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <span>Load Mappings from Path</span>
              <button className="modal-close" onClick={() => setPathModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="path-section-label">HBM XML paths — one per line (file or directory)</div>
              <textarea className="path-textarea" rows={8} spellCheck={false} autoFocus
                placeholder="/path/to/ReportDefinitionBO.hbm.xml&#10;/path/to/mappings/"
                value={hbmText} onChange={e => setHbmText(e.target.value)} />
              <div className="path-section-label" style={{ marginTop: 14 }}>
                Classes / JAR paths — one per line (optional)
              </div>
              <textarea className="path-textarea" rows={5} spellCheck={false}
                placeholder="/path/to/target/classes&#10;/path/to/lib/myapp.jar"
                value={classesText} onChange={e => setClassesText(e.target.value)} />
              {error && <div className="path-error" style={{ marginTop: 8 }}>{error}</div>}
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost"
                onClick={() => { setPathModal(false); setError(''); }}>Cancel</button>
              <button className="btn btn-primary"
                onClick={handleLoad} disabled={loading || !hbmText.trim()}>
                {loading ? 'Loading…' : 'Load Mappings'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
