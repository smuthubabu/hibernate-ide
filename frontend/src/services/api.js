import axios from 'axios';

const BASE = '/api';

const api = axios.create({
  baseURL: BASE,
  headers: { 'Content-Type': 'application/json' },
});

export const connectionApi = {
  create: (config) => api.post('/connections', config),
  test:   (config) => api.post('/connections/test', config),
  list:   ()       => api.get('/connections'),
  remove: (id)     => api.delete(`/connections/${id}`),
  addMapping:          (id, req) => api.post(`/connections/${id}/mappings`, req),
  loadMappingsFromPath:(id, req) => api.post(`/connections/${id}/mappings/path`, req),
};

export const queryApi = {
  execute: (request) => api.post('/query/execute', request),
  explain: (request) => api.post('/query/explain', request),
};

export const schemaApi = {
  getTables:     (connectionId)             => api.get(`/schema/${connectionId}/tables`),
  getTableDetail:(connectionId, tableName)  => api.get(`/schema/${connectionId}/tables/${tableName}`),
  generateHbm:   (connectionId, tableName, className) =>
    api.get(`/schema/${connectionId}/tables/${tableName}/hbm`, { params: { className } }),
  getEntities:   (connectionId)             => api.get(`/schema/${connectionId}/entities`),
};

export const profileApi = {
  list:   ()       => api.get('/profiles'),
  save:   (profile) => api.post('/profiles', profile),
  remove: (name)   => api.delete(`/profiles/${encodeURIComponent(name)}`),
};

export default api;
