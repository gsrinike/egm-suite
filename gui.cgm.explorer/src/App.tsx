import { useEffect, useMemo, useState } from 'react';
import { Activity } from 'lucide-react';
import { EquipmentExplorer } from './components/EquipmentExplorer';
import { ImportPanel } from './components/ImportPanel';
import { IidmVisualizer } from './components/IidmVisualizer';
import { NetworkCompare } from './components/NetworkCompare';
import { ImportStatus, listImports } from './services/cgmApi';
import './styles.css';

export default function App() {
  const [latestImport, setLatestImport] = useState<ImportStatus | null>(null);
  const [imports, setImports] = useState<ImportStatus[]>([]);
  const [networkId, setNetworkId] = useState('');
  const [activeTab, setActiveTab] = useState<'explore' | 'iidm' | 'compare'>('explore');

  useEffect(() => {
    // Import history drives the network-id selector after page reloads.
    refreshImports();
  }, []);

  async function refreshImports() {
    try {
      setImports(await listImports());
    } catch {
      setImports([]);
    }
  }

  function handleImported(status: ImportStatus) {
    setLatestImport(status);
    // Keep the newest import at the top without duplicating an existing network id.
    setImports((current) => [status, ...current.filter((item) => item.networkId !== status.networkId)]);
    setNetworkId(status.networkId);
  }

  const selectedImport = useMemo(
    () => latestImport?.networkId === networkId ? latestImport : imports.find((item) => item.networkId === networkId) ?? null,
    [imports, latestImport, networkId]
  );

  return (
    <main>
      <header className="app-header">
        <div>
          <h1>CGMES grid explorer</h1>
          <p>Import, search and compare electrical network states.</p>
        </div>
        <Activity size={28} aria-hidden />
      </header>

      <ImportPanel onImported={handleImported} />

      {latestImport && (
        <div className="status">
          <strong>{latestImport.state}</strong>
          <span>{latestImport.indexedEquipmentCount} items indexed</span>
          <span>{latestImport.metadata.businessDay} {latestImport.metadata.timestamp} {latestImport.metadata.region} {latestImport.metadata.process}</span>
          <span>{latestImport.metadata.timeFrame} {latestImport.metadata.tsoName} {latestImport.metadata.cgmesProfileType} {latestImport.metadata.versionNumber}</span>
          <code>{latestImport.networkId}</code>
        </div>
      )}

      <section className="network-entry">
        <label htmlFor="network-id">Network id</label>
        <input
          id="network-id"
          list="network-id-options"
          value={networkId}
          onChange={(event) => setNetworkId(event.target.value)}
          placeholder="Select or paste an imported network id"
        />
        <datalist id="network-id-options">
          {imports.map((item) => (
            <option
              key={item.networkId}
              value={item.networkId}
              label={`${item.metadata.businessDay} ${item.metadata.timestamp} ${item.metadata.region} ${item.metadata.process} ${fileContext(item)} (${item.indexedEquipmentCount})`}
            />
          ))}
        </datalist>
      </section>

      {networkId && (
        <>
          <div className="tabs" role="tablist" aria-label="Network study views">
            <button type="button" className={activeTab === 'explore' ? 'active' : ''} onClick={() => setActiveTab('explore')}>Explore</button>
            <button type="button" className={activeTab === 'iidm' ? 'active' : ''} onClick={() => setActiveTab('iidm')}>IIDM</button>
            <button type="button" className={activeTab === 'compare' ? 'active' : ''} onClick={() => setActiveTab('compare')}>Compare</button>
          </div>
          <div className="workspace">
            {activeTab === 'explore' && <EquipmentExplorer networkId={networkId} metadata={selectedImport?.metadata} />}
            {activeTab === 'iidm' && <IidmVisualizer networkId={networkId} />}
            {activeTab === 'compare' && <NetworkCompare currentNetworkId={networkId} />}
          </div>
        </>
      )}
    </main>
  );
}

function fileContext(item: ImportStatus) {
  // Older persisted imports may not have all filename-derived fields yet.
  return [item.metadata.timeFrame, item.metadata.tsoName, item.metadata.cgmesProfileType]
    .filter(Boolean)
    .join(' ');
}
