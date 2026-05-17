import React, { useState, useCallback, useRef, useEffect } from 'react';
import './App.css';
import ConnectionPanel from './components/ConnectionPanel';
import EntityExplorer from './components/EntityExplorer';
import SchemaExplorer from './components/SchemaExplorer';
import QueryEditor from './components/QueryEditor';
import ResultsGrid from './components/ResultsGrid';
import QueryHistory from './components/QueryHistory';


function App() {
  const [sidebarWidth, setSidebarWidth] = useState(280);
  const [collapsed, setCollapsed]       = useState(false);
  const savedWidth                      = useRef(280);
  const dragging                        = useRef(false);
  const didDrag                         = useRef(false);

  useEffect(() => {
    const onMove = (e) => {
      if (!dragging.current || collapsed) return;
      const next = Math.min(600, Math.max(180, e.clientX));
      if (Math.abs(next - savedWidth.current) > 5) didDrag.current = true;
      savedWidth.current = next;
      setSidebarWidth(next);
    };
    const onUp = () => { dragging.current = false; document.body.style.cursor = ''; document.body.style.userSelect = ''; };
    document.addEventListener('mousemove', onMove);
    document.addEventListener('mouseup', onUp);
    return () => { document.removeEventListener('mousemove', onMove); document.removeEventListener('mouseup', onUp); };
  }, [collapsed]);

  const toggleCollapse = () => {
    if (collapsed) {
      setSidebarWidth(savedWidth.current);
    } else {
      savedWidth.current = sidebarWidth;
      setSidebarWidth(0);
    }
    setCollapsed(c => !c);
  };

  const [activeConnection, setActiveConnection] = useState(null);
  const [connections, setConnections]           = useState([]);
  const [queryResult, setQueryResult]           = useState(null);
  const [queryHistory, setQueryHistory]         = useState([]);
  const [sqlQuery, setSqlQuery]                 = useState('SELECT 1');
  const [hqlQuery, setHqlQuery]                 = useState('');
  const [queryType, setQueryType]               = useState('SQL');
  const currentQuery = queryType === 'SQL' ? sqlQuery : hqlQuery;
  const [sidebarTab, setSidebarTab]             = useState('entities'); // entities | tables | history
  const [isExecuting, setIsExecuting]           = useState(false);
  const [statusMsg, setStatusMsg]               = useState('Ready');
  const [theme, setTheme]                       = useState('dark');

  const handleConnectionChange = useCallback((conn, allConns) => {
    setActiveConnection(conn);
    setConnections(allConns);
    setQueryResult(null);
    if (conn) {
      setStatusMsg(`Connected: ${conn.name} (${conn.jdbcUrl})`);
    } else {
      setStatusMsg('No active connection');
    }
  }, []);

  const handleQueryExecuted = useCallback((result, query) => {
    setQueryResult(result);
    setIsExecuting(false);
    if (result.success) {
      setStatusMsg(
        `OK · ${result.rowCount ?? result.affectedRows ?? 0} rows · ${result.executionTimeMs}ms`
      );
      setQueryHistory(prev => [
        { query, result, timestamp: Date.now(), connectionName: activeConnection?.name, queryType: result.queryType || 'SQL' },
        ...prev.slice(0, 99),
      ]);
    } else {
      setStatusMsg(`Error: ${result.message || 'Query failed'}`);
    }
  }, [activeConnection]);

  const handleHistorySelect = useCallback((entry) => {
    const qt = entry.queryType || 'SQL';
    if (qt === 'HQL') {
      setHqlQuery(entry.query);
      setQueryType('HQL');
    } else {
      setSqlQuery(entry.query);
      setQueryType('SQL');
    }
  }, []);

  const handleSchemaInsert = useCallback((snippet) => {
    setSqlQuery(prev => prev + (prev.trim() ? '\n' : '') + snippet);
  }, []);

  const handleEntitySelect = useCallback((entityName) => {
    setHqlQuery(`from ${entityName}`);
    setQueryType('HQL');
  }, []);

  return (
    <div className={`app ${theme === 'light' ? 'light' : ''}`}>
      <header className="app-header">
        <div className="app-logo">
          <span className="logo-icon">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" strokeLinecap="round" strokeLinejoin="round">
              <defs>
                <clipPath id="hib-lens">
                  <circle cx="10" cy="10" r="5.8"/>
                </clipPath>
              </defs>
              {/* Lens ring */}
              <circle cx="10" cy="10" r="6.5" stroke="currentColor" strokeWidth="1.8"/>
              {/* Data rows inside lens */}
              <g clipPath="url(#hib-lens)" stroke="currentColor" strokeOpacity="0.55" strokeWidth="1.1">
                <line x1="3" y1="7.5" x2="17" y2="7.5"/>
                <line x1="3" y1="10"  x2="17" y2="10"/>
                <line x1="3" y1="12.5" x2="17" y2="12.5"/>
              </g>
              {/* Scan line */}
              <line className="logo-scan" clipPath="url(#hib-lens)" x1="3" y1="4" x2="17" y2="4" stroke="currentColor" strokeWidth="2"/>
              {/* Handle */}
              <line x1="15.1" y1="15.1" x2="20.5" y2="20.5" stroke="currentColor" strokeWidth="2.2"/>
            </svg>
          </span>
          <span className="logo-text">Hibernate IDE</span>
          <span className="logo-version">3.x Console</span>
        </div>
        <div className="header-status">
          {activeConnection
            ? <span className="status-connected">● {activeConnection.name}</span>
            : <span className="status-disconnected">○ Not connected</span>}
        </div>
        <button className="theme-toggle"
          onClick={() => setTheme(t => t === 'dark' ? 'light' : 'dark')}
          title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}>
          {theme === 'dark' ? '☀' : '☾'}
        </button>
      </header>

      <div className="app-body">
        <aside className="sidebar" style={{ width: sidebarWidth }}>
          <div className="sidebar-topbar">
            <button className="panel-toggle-btn" onClick={toggleCollapse} title="Collapse sidebar">
              <svg width="15" height="15" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round">
                <rect x="1" y="1" width="14" height="14" rx="2"/>
                <line x1="5" y1="1" x2="5" y2="15"/>
                <polyline points="8,5 4.5,8 8,11"/>
              </svg>
            </button>
          </div>
          <ConnectionPanel
            activeConnection={activeConnection}
            onConnectionChange={handleConnectionChange}
          />

          <div className="sidebar-tabs">
            <button className={`sidebar-tab ${sidebarTab === 'entities' ? 'active' : ''}`}
              onClick={() => setSidebarTab('entities')}>Entities</button>
            <button className={`sidebar-tab ${sidebarTab === 'tables' ? 'active' : ''}`}
              onClick={() => setSidebarTab('tables')}>Tables</button>
            <button className={`sidebar-tab ${sidebarTab === 'history' ? 'active' : ''}`}
              onClick={() => setSidebarTab('history')}>History</button>
          </div>

          <div className="sidebar-content">
            {sidebarTab === 'entities'
              ? <EntityExplorer activeConnection={activeConnection} onEntitySelect={handleEntitySelect} />
              : sidebarTab === 'tables'
              ? <SchemaExplorer activeConnection={activeConnection} onInsert={handleSchemaInsert} />
              : <QueryHistory history={queryHistory} onSelect={handleHistorySelect} />}
          </div>
        </aside>

        <div
          className={`resize-handle${collapsed ? ' collapsed' : ''}`}
          onMouseDown={e => { e.preventDefault(); if (!collapsed) { dragging.current = true; document.body.style.cursor = 'col-resize'; document.body.style.userSelect = 'none'; } }}
        >
          {collapsed && (
            <button className="panel-expand-btn" onClick={toggleCollapse} title="Expand sidebar">
              <svg width="15" height="15" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round">
                <rect x="1" y="1" width="14" height="14" rx="2"/>
                <line x1="5" y1="1" x2="5" y2="15"/>
                <polyline points="3,5 6.5,8 3,11"/>
              </svg>
            </button>
          )}
        </div>
        <main className="main-area">
          <QueryEditor
            activeConnection={activeConnection}
            currentQuery={currentQuery}
            onQueryChange={queryType === 'SQL' ? setSqlQuery : setHqlQuery}
            onSetSqlQuery={setSqlQuery}
            onExecuting={() => setIsExecuting(true)}
            onQueryExecuted={handleQueryExecuted}
            queryType={queryType}
            onQueryTypeChange={setQueryType}
            theme={theme}
          />
          <ResultsGrid result={queryResult} isExecuting={isExecuting} />
        </main>
      </div>

      <footer className="status-bar">
        <span className="status-text">{statusMsg}</span>
        {activeConnection && (
          <span className="status-detail">
            {activeConnection.dialect?.replace('org.hibernate.dialect.', '')}
          </span>
        )}
      </footer>
    </div>
  );
}

export default App;
