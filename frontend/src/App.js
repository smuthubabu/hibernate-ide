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

  useEffect(() => {
    const onMove = (e) => {
      if (!dragging.current || collapsed) return;
      const next = Math.min(600, Math.max(180, e.clientX));
      setSidebarWidth(next);
      savedWidth.current = next;
    };
    const onUp = () => { dragging.current = false; document.body.style.cursor = ''; };
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
          <span className="logo-icon">⬡</span>
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
          <div className="sidebar-collapse-bar">
            <button className="sidebar-collapse-btn" onClick={toggleCollapse} title="Collapse panel">◀</button>
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

        <div className={`resize-handle ${collapsed ? 'collapsed' : ''}`}
          onMouseDown={() => { if (!collapsed) { dragging.current = true; document.body.style.cursor = 'col-resize'; } }}
          onClick={() => { if (collapsed) toggleCollapse(); }}
          title={collapsed ? 'Expand panel' : undefined}
        >
          {collapsed && <span className="expand-arrow">▶</span>}
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
