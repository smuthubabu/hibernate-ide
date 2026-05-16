import React, { useMemo, useCallback, useState } from 'react';
import ReactFlow, {
  Background, Controls, MiniMap,
  MarkerType, useNodesState, useEdgesState,
  Handle, Position,
} from 'reactflow';
import dagre from 'dagre';
import 'reactflow/dist/style.css';
import './EntityGraph.css';

const NODE_W       = 230;
const HEAD_H       = 48;  // collapsed height
const ROW_H        = 21;
const MAX_ROWS     = 5;

const ASSOC_COLOR = {
  'many-to-one': '#cba6f7',
  'one-to-one':  '#89dceb',
  'one-to-many': '#a6e3a1',
  'many-to-many':'#fab387',
};
const ASSOC_LABEL = {
  'many-to-one': 'N:1',
  'one-to-one':  '1:1',
  'one-to-many': '1:N',
  'many-to-many':'N:N',
};

function expandedHeight(entity) {
  const rows = (entity.idProperty ? 1 : 0) + Math.min((entity.properties || []).length, MAX_ROWS);
  return HEAD_H + 8 + rows * ROW_H + 6;
}

function layout(nodes, edges) {
  const g = new dagre.graphlib.Graph();
  g.setDefaultEdgeLabel(() => ({}));
  g.setGraph({ rankdir: 'LR', ranksep: 140, nodesep: 50 });
  nodes.forEach(n => g.setNode(n.id, { width: NODE_W, height: HEAD_H }));
  edges.forEach(e => g.setEdge(e.source, e.target));
  dagre.layout(g);
  return nodes.map(n => {
    const p = g.node(n.id);
    return { ...n, position: { x: p.x - NODE_W / 2, y: p.y - HEAD_H / 2 } };
  });
}

function EntityNode({ data }) {
  const { entity: e, collapsed, onToggle } = data;
  const extra = (e.properties || []).length - MAX_ROWS;

  return (
    <div className={`eg-node ${collapsed ? 'eg-collapsed' : ''}`}
         style={{ width: NODE_W }}>
      <Handle type="target" position={Position.Left}
        style={{ background: '#45475a', border: 'none', width: 8, height: 8 }} />
      <Handle type="source" position={Position.Right}
        style={{ background: '#45475a', border: 'none', width: 8, height: 8 }} />

      <div className="eg-node-head" onClick={() => onToggle(e.entityName)}>
        <div className="eg-node-head-row">
          <span className="eg-node-name">{e.entityName}</span>
          <span className="eg-chevron">{collapsed ? '▸' : '▾'}</span>
        </div>
        {e.tableName && (
          <span className="eg-node-table">
            {e.schemaName ? `${e.schemaName}.` : ''}{e.tableName}
          </span>
        )}
      </div>

      {!collapsed && (
        <div className="eg-node-body">
          {e.idProperty && (
            <div className="eg-row eg-pk">
              <span className="eg-icon">🔑</span>
              <span className="eg-pname">{e.idProperty.name}</span>
              <span className="eg-ptype">{e.idProperty.type}</span>
            </div>
          )}
          {(e.properties || []).slice(0, MAX_ROWS).map(p => (
            <div key={p.name} className="eg-row">
              <span className="eg-icon eg-dot">◦</span>
              <span className="eg-pname">{p.name}</span>
              <span className="eg-ptype">{p.type}</span>
            </div>
          ))}
          {extra > 0 && <div className="eg-more">+{extra} more fields</div>}
          {(e.associations || []).length > 0 && (
            <div className="eg-assoc-section">
              {(e.associations || []).map(a => (
                <div key={a.name} className="eg-row eg-assoc-row">
                  <span className="eg-icon eg-assoc-icon"
                    style={{ color: ASSOC_COLOR[a.type] || '#6c7086' }}>→</span>
                  <span className="eg-pname">{a.name}</span>
                  <span className="eg-ptype" style={{ color: ASSOC_COLOR[a.type] || '#6c7086' }}>
                    {ASSOC_LABEL[a.type]}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

const nodeTypes = { entity: EntityNode };

export default function EntityGraph({ entities, onClose }) {
  const known = useMemo(() => new Set(entities.map(e => e.entityName)), [entities]);

  const { laidOut, rawEdges } = useMemo(() => {
    const nodes = entities.map(e => ({
      id: e.entityName,
      type: 'entity',
      data: { entity: e, collapsed: true, onToggle: () => {} },
      position: { x: 0, y: 0 },
    }));

    const rawEdges = [];
    entities.forEach(e => {
      (e.associations || []).forEach((a, i) => {
        if (!a.targetEntity || !known.has(a.targetEntity)) return;
        const color = ASSOC_COLOR[a.type] || '#6c7086';
        rawEdges.push({
          id: `${e.entityName}__${a.name}__${i}`,
          source: e.entityName,
          target: a.targetEntity,
          label: `${a.name}  ${ASSOC_LABEL[a.type] || a.type}`,
          type: 'smoothstep',
          markerEnd: { type: MarkerType.ArrowClosed, color },
          style: { stroke: color, strokeWidth: 1.5 },
          labelStyle: { fill: '#cdd6f4', fontSize: 10 },
          labelBgStyle: { fill: '#1e1e2e', fillOpacity: 0.85 },
          labelBgPadding: [4, 3],
          labelBgBorderRadius: 3,
        });
      });
    });

    return { laidOut: layout(nodes, rawEdges), rawEdges };
  }, [entities, known]);

  const [rfInstance, setRfInstance] = useState(null);
  const [nodes, setNodes, onNodesChange] = useNodesState(laidOut);
  const [edges, , onEdgesChange] = useEdgesState(rawEdges);

  const onToggle = useCallback((entityName) => {
    setNodes(nds => nds.map(n => {
      if (n.id !== entityName) return n;
      const collapsed = !n.data.collapsed;
      return { ...n, data: { ...n.data, collapsed } };
    }));
    setTimeout(() => rfInstance?.fitView({ padding: 0.3, duration: 250 }), 60);
  }, [setNodes, rfInstance]);

  // Inject the real onToggle after setNodes is available
  const nodesWithToggle = useMemo(() =>
    nodes.map(n => ({ ...n, data: { ...n.data, onToggle } })),
  [nodes, onToggle]);

  return (
    <div className="eg-overlay">
      <div className="eg-header">
        <span className="eg-title">Entity Relationship Diagram</span>
        <div className="eg-legend">
          {Object.entries(ASSOC_LABEL).map(([type, lbl]) => (
            <span key={type} className="eg-legend-item">
              <span className="eg-dot-key" style={{ background: ASSOC_COLOR[type] }} />
              <span>{lbl} {type}</span>
            </span>
          ))}
        </div>
        <button className="eg-close" onClick={onClose}>✕</button>
      </div>
      <div className="eg-canvas">
        <ReactFlow
          nodes={nodesWithToggle} edges={edges}
          onNodesChange={onNodesChange} onEdgesChange={onEdgesChange}
          nodeTypes={nodeTypes}
          onInit={setRfInstance}
          fitView fitViewOptions={{ padding: 0.3 }}
          minZoom={0.2} maxZoom={3}
        >
          <Background color="#313244" gap={24} />
          <Controls />
          <MiniMap nodeColor="#313244" maskColor="rgba(30,30,46,0.75)" />
        </ReactFlow>
      </div>
    </div>
  );
}
