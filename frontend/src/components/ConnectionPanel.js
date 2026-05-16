import React, { useState, useEffect, useCallback } from 'react';
import { connectionApi, profileApi } from '../services/api';
import './ConnectionPanel.css';

const PRESETS = [
  { label: 'H2 In-Memory', jdbcUrl: 'jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL', username: 'sa', password: '', driverClass: 'org.h2.Driver' },
  { label: 'MySQL',        jdbcUrl: 'jdbc:mysql://localhost:3306/mydb',                 username: 'root', password: '', driverClass: '', hidden: true },
  { label: 'PostgreSQL',   jdbcUrl: 'jdbc:postgresql://localhost:5432/mydb',            username: 'postgres', password: '', driverClass: '', hidden: true },
  { label: 'HSQLDB',       jdbcUrl: 'jdbc:hsqldb:mem:testdb',                           username: 'SA', password: '', driverClass: '', hidden: true },
  { label: 'DB2 Type 2',  jdbcUrl: 'jdbc:db2:testdb',                                  username: 'db2inst1', password: '', driverClass: 'com.ibm.db2.jcc.DB2Driver', note: 'Requires DB2 client installed on this machine (provides libdb2jcct2). The alias must also be cataloged locally. Only use this when running Hibernate IDE on the DB2 server or a machine with full DB2 client. Use Type 4 for remote connections.' },
  { label: 'DB2 Type 4',  jdbcUrl: 'jdbc:db2://localhost:50000/testdb',                username: 'db2inst1', password: '', driverClass: '' },
];

const emptyForm = { name: '', jdbcUrl: '', username: '', password: '', driverClass: '', dialect: '', showSql: true };

export default function ConnectionPanel({ activeConnection, onConnectionChange }) {
  const [connections, setConnections] = useState([]);
  const [profiles, setProfiles]       = useState([]);
  const [form, setForm]               = useState(emptyForm);
  const [expanded, setExpanded]       = useState(true);
  const [testing, setTesting]         = useState(false);
  const [connecting, setConnecting]   = useState(false);
  const [saving, setSaving]           = useState(false);
  const [testResult, setTestResult]   = useState(null);
  const [error, setError]             = useState('');
  const [saveMsg, setSaveMsg]         = useState('');

  const loadConnections = useCallback(async () => {
    try {
      const res = await connectionApi.list();
      setConnections(res.data.data || []);
    } catch {}
  }, []);

  const loadProfiles = useCallback(async () => {
    try {
      const res = await profileApi.list();
      setProfiles(res.data.data || []);
    } catch {}
  }, []);

  useEffect(() => {
    loadConnections();
    loadProfiles();
  }, [loadConnections, loadProfiles]);

  const applyPreset = (preset) => {
    setForm(f => ({ ...f, name: preset.label, jdbcUrl: preset.jdbcUrl,
      username: preset.username, password: preset.password, driverClass: preset.driverClass }));
    setTestResult(null); setError('');
  };

  const applyProfile = (profile) => {
    setForm({
      name: profile.name,
      jdbcUrl: profile.jdbcUrl || '',
      username: profile.username || '',
      password: profile.password || '',
      driverClass: profile.driverClass || '',
      dialect: profile.dialect || '',
      showSql: true,
    });
    setTestResult(null); setError('');
  };

  const handleTest = async () => {
    setTesting(true); setTestResult(null); setError('');
    try {
      const res = await connectionApi.test(form);
      if (res.data.success) {
        setTestResult({ ok: true, msg: res.data.message + ' — ' + res.data.data?.name });
      } else {
        setTestResult({ ok: false, msg: res.data.message });
      }
    } catch (e) {
      setTestResult({ ok: false, msg: e.response?.data?.message || e.message });
    } finally { setTesting(false); }
  };

  const doConnect = async (config, hbmPaths, classesPaths) => {
    const res = await connectionApi.create(config);
    if (!res.data.success) throw new Error(res.data.message);
    const newConn = res.data.data;
    const updated = [...connections.filter(c => c.id !== newConn.id), newConn];
    setConnections(updated);

    if (hbmPaths && hbmPaths.length > 0) {
      try {
        await connectionApi.loadMappingsFromPath(newConn.id, { hbmPaths, classesPaths: classesPaths || [] });
      } catch (e) {
        // non-fatal — connection still works, just without mappings
        setError('Connected but mapping paths failed: ' + (e.response?.data?.message || e.message));
      }
    }
    onConnectionChange(newConn, updated);
    return newConn;
  };

  const handleConnect = async () => {
    setConnecting(true); setError('');
    try {
      await doConnect(form, null, null);
      setForm(emptyForm); setTestResult(null);
    } catch (e) {
      setError(e.response?.data?.message || e.message);
    } finally { setConnecting(false); }
  };

  const handleConnectProfile = async (profile) => {
    setConnecting(true); setError('');
    try {
      await doConnect(
        { name: profile.name, jdbcUrl: profile.jdbcUrl, username: profile.username,
          password: profile.password, driverClass: profile.driverClass, dialect: profile.dialect, showSql: true },
        profile.hbmPaths,
        profile.classesPaths,
      );
    } catch (e) {
      setError(e.response?.data?.message || e.message);
    } finally { setConnecting(false); }
  };

  const handleSaveProfile = async () => {
    if (!form.name) { setError('Set a connection name before saving'); return; }
    setSaving(true); setSaveMsg('');
    try {
      await profileApi.save({
        name: form.name, jdbcUrl: form.jdbcUrl, username: form.username,
        password: form.password, driverClass: form.driverClass, dialect: form.dialect,
        hbmPaths: [], classesPaths: [],
      });
      await loadProfiles();
      setSaveMsg('Saved');
      setTimeout(() => setSaveMsg(''), 2000);
    } catch (e) {
      setError(e.response?.data?.message || e.message);
    } finally { setSaving(false); }
  };

  const handleDeleteProfile = async (name) => {
    try {
      await profileApi.remove(name);
      await loadProfiles();
    } catch {}
  };

  const handleActivate = (conn) => onConnectionChange(conn, connections);

  const handleDisconnect = async (conn) => {
    try {
      await connectionApi.remove(conn.id);
      const updated = connections.filter(c => c.id !== conn.id);
      setConnections(updated);
      if (activeConnection?.id === conn.id) onConnectionChange(null, updated);
    } catch (e) {
      setError(e.response?.data?.message || e.message);
    }
  };

  return (
    <div className="connection-panel">
      <div className="panel-header" onClick={() => setExpanded(e => !e)}>
        <span className="panel-title">Connections</span>
        <span className="panel-toggle">{expanded ? '▾' : '▸'}</span>
      </div>

      {expanded && (
        <div className="panel-body">

          {/* Saved profiles */}
          {profiles.length > 0 && (
            <div className="profiles-section">
              <div className="profiles-title">Saved Profiles</div>
              {profiles.map(p => (
                <div key={p.name} className="profile-item">
                  <span className="profile-name" onClick={() => applyProfile(p)} title="Fill form from this profile">
                    {p.name}
                  </span>
                  {p.hbmPaths?.length > 0 && (
                    <span className="profile-paths-badge" title={p.hbmPaths.join('\n')}>
                      {p.hbmPaths.length} path{p.hbmPaths.length > 1 ? 's' : ''}
                    </span>
                  )}
                  <button className="profile-connect-btn" disabled={connecting}
                    onClick={() => handleConnectProfile(p)} title="Connect with this profile (auto-loads mappings)">
                    ▶
                  </button>
                  <button className="profile-delete-btn" onClick={() => handleDeleteProfile(p.name)} title="Delete profile">
                    ✕
                  </button>
                </div>
              ))}
              <div className="profiles-divider" />
            </div>
          )}

          <div className="presets">
            {PRESETS.filter(p => !p.hidden).map(p => (
              <span key={p.label} className="preset-wrap">
                <button className="preset-btn" onClick={() => applyPreset(p)}>{p.label}</button>
                {p.note && <span className="preset-note-icon" data-tooltip={p.note}>ⓘ</span>}
              </span>
            ))}
          </div>

          <div className="form-group">
            <label className="form-label">Name</label>
            <input className="form-input" placeholder="My Connection" value={form.name}
              onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
          </div>
          <div className="form-group">
            <label className="form-label">JDBC URL</label>
            <input className="form-input" placeholder="jdbc:h2:mem:testdb" value={form.jdbcUrl}
              onChange={e => setForm(f => ({ ...f, jdbcUrl: e.target.value }))} />
          </div>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Username</label>
              <input className="form-input" placeholder="sa" value={form.username}
                onChange={e => setForm(f => ({ ...f, username: e.target.value }))} />
            </div>
            <div className="form-group">
              <label className="form-label">Password</label>
              <input className="form-input" type="password" placeholder="••••" value={form.password}
                onChange={e => setForm(f => ({ ...f, password: e.target.value }))} />
            </div>
          </div>
          <div className="form-group">
            <label className="form-label">Driver Class (optional)</label>
            <input className="form-input" placeholder="auto-detected" value={form.driverClass}
              onChange={e => setForm(f => ({ ...f, driverClass: e.target.value }))} />
          </div>
          <div className="form-group">
            <label className="form-label">Dialect (optional)</label>
            <select className="form-input" value={form.dialect}
              onChange={e => setForm(f => ({ ...f, dialect: e.target.value }))}>
              <option value="">Auto-detect</option>
              <option value="org.hibernate.dialect.H2Dialect">H2</option>
              <option value="org.hibernate.dialect.MySQL5Dialect" style={{ display: 'none' }}>MySQL 5</option>
              <option value="org.hibernate.dialect.PostgreSQLDialect" style={{ display: 'none' }}>PostgreSQL</option>
              <option value="org.hibernate.dialect.HSQLDialect" style={{ display: 'none' }}>HSQLDB</option>
              <option value="org.hibernate.dialect.Oracle9iDialect">Oracle 9i+</option>
              <option value="org.hibernate.dialect.SQLServerDialect">SQL Server</option>
              <option value="org.hibernate.dialect.DB2Dialect">DB2 (LUW)</option>
              <option value="org.hibernate.dialect.DB2390Dialect">DB2/390 (Mainframe)</option>
              <option value="org.hibernate.dialect.DB2400Dialect">DB2/400 (iSeries)</option>
            </select>
          </div>

          {testResult && (
            <div className={`test-result ${testResult.ok ? 'ok' : 'fail'}`}>
              {testResult.ok ? '✓ ' : '✗ '}{testResult.msg}
            </div>
          )}
          {error && <div className="error-msg">✗ {error}</div>}
          {saveMsg && <div className="save-msg">✓ {saveMsg}</div>}

          <div className="btn-row">
            <button className="btn btn-ghost" onClick={handleTest} disabled={testing || !form.jdbcUrl}>
              {testing ? 'Testing…' : 'Test'}
            </button>
            <button className="btn btn-ghost" onClick={handleSaveProfile} disabled={saving || !form.name}
              title="Save connection config as a reusable profile">
              {saving ? 'Saving…' : 'Save Profile'}
            </button>
            <button className="btn btn-primary" onClick={handleConnect} disabled={connecting || !form.jdbcUrl}>
              {connecting ? 'Connecting…' : 'Connect'}
            </button>
          </div>
        </div>
      )}

      {connections.length > 0 && (
        <div className="conn-list">
          <div className="conn-list-title">Active Sessions</div>
          {connections.map(conn => (
            <div key={conn.id} className={`conn-item ${activeConnection?.id === conn.id ? 'active' : ''}`}>
              <div className="conn-info" onClick={() => handleActivate(conn)}>
                <span className="conn-dot">●</span>
                <span className="conn-name">{conn.name}</span>
              </div>
              <button className="conn-remove" onClick={() => handleDisconnect(conn)} title="Disconnect">✕</button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
