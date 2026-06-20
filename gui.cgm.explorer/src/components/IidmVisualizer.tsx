import { useEffect, useMemo, useState } from 'react';
import { GitBranch, Network, RefreshCcw } from 'lucide-react';
import { convertToIidm, IidmEquipment, IidmNetwork } from '../services/cgmApi';

interface IidmVisualizerProps {
  networkId: string;
}

const TYPE_COLORS: Record<string, string> = {
  SUBSTATION: '#2a6f97',
  VOLTAGE_LEVEL: '#4f7f52',
  BUS: '#7a5c9c',
  LINE: '#be6c36',
  TWO_WINDINGS_TRANSFORMER: '#8d4b47',
  GENERATOR: '#0f766e',
  LOAD: '#6b7280',
  SHUNT_COMPENSATOR: '#916d1b',
  SWITCH: '#3f6473',
  STATE_VARIABLE: '#64748b',
  UNKNOWN: '#7b8794',
};

export function IidmVisualizer({ networkId }: IidmVisualizerProps) {
  const [network, setNetwork] = useState<IidmNetwork | null>(null);
  const [selectedId, setSelectedId] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    load();
  }, [networkId]);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const result = await convertToIidm(networkId);
      setNetwork(result);
      setSelectedId(result.equipment[0]?.id ?? '');
    } catch (exception) {
      setNetwork(null);
      setError(exception instanceof Error ? exception.message : 'IIDM conversion failed');
    } finally {
      setLoading(false);
    }
  }

  const selected = useMemo(
    () => network?.equipment.find((item) => item.id === selectedId) ?? null,
    [network, selectedId]
  );
  const groups = useMemo(() => groupByType(network?.equipment ?? []), [network]);
  const containers = useMemo(() => groupByContainer(network?.equipment ?? []), [network]);
  const nodes = useMemo(() => layoutNodes(groups), [groups]);

  return (
    <section className="pane iidm-pane">
      <div className="pane-header">
        <div>
          <h2>IIDM model</h2>
          <p>Converted network projection and PowSyBl-aligned equipment view.</p>
        </div>
        <button type="button" onClick={load} disabled={loading}>
          <RefreshCcw size={16} aria-hidden />
          Convert
        </button>
      </div>

      {error && <p className="error">{error}</p>}

      <div className="iidm-summary">
        <div className="stat">
          <span>IIDM network</span>
          <strong>{network?.id ?? networkId}</strong>
        </div>
        <div className="stat">
          <span>Equipment</span>
          <strong>{network?.equipment.length ?? 0}</strong>
        </div>
        <div className="stat">
          <span>Containers</span>
          <strong>{containers.length}</strong>
        </div>
      </div>

      <div className="iidm-layout">
        <div className="iidm-canvas" aria-label="IIDM equipment visualization">
          <svg viewBox="0 0 960 420" role="img">
            <title>IIDM equipment grouped by PowSyBl equipment type</title>
            {nodes.map((node, index) => (
              <g key={node.type}>
                {index > 0 && (
                  <line
                    x1={nodes[index - 1].x}
                    y1={nodes[index - 1].y}
                    x2={node.x}
                    y2={node.y}
                    className="iidm-link"
                  />
                )}
                <circle
                  cx={node.x}
                  cy={node.y}
                  r={Math.max(18, Math.min(46, 16 + node.count * 2))}
                  fill={TYPE_COLORS[node.type] ?? TYPE_COLORS.UNKNOWN}
                />
                <text x={node.x} y={node.y - 4} textAnchor="middle" className="iidm-node-count">{node.count}</text>
                <text x={node.x} y={node.y + 66} textAnchor="middle" className="iidm-node-label">{labelForType(node.type)}</text>
              </g>
            ))}
          </svg>
        </div>

        <aside className="iidm-side">
          <h3><Network size={16} aria-hidden /> Equipment</h3>
          <select value={selectedId} onChange={(event) => setSelectedId(event.target.value)} aria-label="Select IIDM equipment">
            {(network?.equipment ?? []).slice(0, 500).map((item) => (
              <option key={item.id} value={item.id}>{item.name || item.id}</option>
            ))}
          </select>

          {selected && (
            <div className="iidm-detail">
              <strong>{selected.name || selected.id}</strong>
              <span>{labelForType(selected.type)}</span>
              <span>{selected.containerId || 'No container'}</span>
              <span>{selected.nominalVoltage ? `${selected.nominalVoltage} kV` : 'Voltage not available'}</span>
              <span>{selected.extensions.length} IIDM extensions</span>
            </div>
          )}

          <h3><GitBranch size={16} aria-hidden /> Containers</h3>
          <div className="container-list">
            {containers.slice(0, 8).map((container) => (
              <div key={container.name}>
                <span>{container.name}</span>
                <strong>{container.count}</strong>
              </div>
            ))}
          </div>
        </aside>
      </div>
    </section>
  );
}

function groupByType(equipment: IidmEquipment[]) {
  const counts = new Map<string, number>();
  equipment.forEach((item) => counts.set(item.type, (counts.get(item.type) ?? 0) + 1));
  return Array.from(counts, ([type, count]) => ({ type, count }))
    .sort((left, right) => right.count - left.count || left.type.localeCompare(right.type));
}

function groupByContainer(equipment: IidmEquipment[]) {
  const counts = new Map<string, number>();
  equipment.forEach((item) => {
    const key = item.containerId || 'Unassigned';
    counts.set(key, (counts.get(key) ?? 0) + 1);
  });
  return Array.from(counts, ([name, count]) => ({ name, count }))
    .sort((left, right) => right.count - left.count || left.name.localeCompare(right.name));
}

function layoutNodes(groups: Array<{ type: string; count: number }>) {
  const visible = groups.slice(0, 10);
  const columns = Math.max(1, Math.ceil(visible.length / 2));
  return visible.map((group, index) => ({
    ...group,
    x: 90 + (index % columns) * (780 / Math.max(1, columns - 1 || 1)),
    y: index < columns ? 130 : 290,
  }));
}

function labelForType(type: string) {
  return type.toLowerCase().replace(/_/g, ' ');
}
