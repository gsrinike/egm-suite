import { useEffect, useMemo, useState } from 'react';
import { Activity, Play, RefreshCw } from 'lucide-react';
import { EquipmentExplorer } from './components/EquipmentExplorer';
import { ImportPanel } from './components/ImportPanel';
import { IidmVisualizer } from './components/IidmVisualizer';
import { NetworkCompare } from './components/NetworkCompare';
import { ImportStatus, getProcessHistory, listImports, startCgmImport } from './services/cgmApi';
import './styles.css';

export default function App() {
  const [latestImport, setLatestImport] = useState<ImportStatus | null>(null);
  const [imports, setImports] = useState<ImportStatus[]>([]);
  const [networkId, setNetworkId] = useState('');
  const [activeTab, setActiveTab] = useState<'explore' | 'iidm' | 'compare'>('explore');
  const [processBusy, setProcessBusy] = useState(false);
  const [processError, setProcessError] = useState('');

  useEffect(() => {
    // Import history drives the network-id selector after page reloads.
    refreshImports();
  }, []);

  useEffect(() => {
    if (!imports.some((item) => item.state === 'In Progress')) {
      return;
    }
    const timer = window.setInterval(refreshImports, 5000);
    return () => window.clearInterval(timer);
  }, [imports]);

  async function refreshImports() {
    try {
      const loaded = sortImports(await listImports());
      setImports(loaded);
      setLatestImport((current) => current ? loaded.find((item) => item.networkId === current.networkId) ?? current : loaded[0] ?? null);
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

  async function startSelectedImport(status: ImportStatus) {
    setProcessBusy(true);
    setProcessError('');
    try {
      const updated = await startCgmImport(status);
      handleImported(updated);
    } catch (exception) {
      setProcessError(exception instanceof Error ? exception.message : 'Starting BPM import failed');
    } finally {
      setProcessBusy(false);
    }
  }

  async function refreshSelectedHistory() {
    if (!networkId) {
      return;
    }
    setProcessBusy(true);
    setProcessError('');
    try {
      handleImported(await getProcessHistory(networkId));
    } catch (exception) {
      setProcessError(exception instanceof Error ? exception.message : 'Loading process history failed');
    } finally {
      setProcessBusy(false);
    }
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
          {latestImport.processInstanceId && <span>Process <code>{latestImport.processInstanceId}</code></span>}
          <code>{latestImport.networkId}</code>
        </div>
      )}

      {imports.length > 0 && (
        <section className="import-history">
          <div className="pane-header">
            <div>
              <h2>Import status</h2>
              <p>{imports.length} uploaded network batch{imports.length === 1 ? '' : 'es'}</p>
            </div>
          </div>
          <div className="import-list">
            {imports.map((item) => (
              <button
                type="button"
                key={item.networkId}
                className={networkId === item.networkId ? 'import-row active' : 'import-row'}
                onClick={() => setNetworkId(item.networkId)}
              >
                <span className={`import-state ${stateClass(item.state)}`}>{item.state}</span>
                <span>{formatDate(item.createdAt)}</span>
                <span>{item.metadata.businessDay} {item.metadata.timestamp}</span>
                <span>{item.metadata.region} {item.metadata.process}</span>
                <span>{fileContext(item) || item.fileName}</span>
                <span>{item.indexedEquipmentCount} items</span>
              </button>
            ))}
          </div>
        </section>
      )}

      {selectedImport && (
        <section className="process-history">
          <div className="pane-header">
            <div>
              <h2>Process history</h2>
              <p>{selectedImport.files?.length ?? 0} file status entr{selectedImport.files?.length === 1 ? 'y' : 'ies'}</p>
            </div>
            <div className="history-actions">
              <button type="button" onClick={() => startSelectedImport(selectedImport)} disabled={selectedImport.state !== 'Init' || processBusy}>
                <Play size={16} aria-hidden />
                Start
              </button>
              <button type="button" onClick={refreshSelectedHistory} disabled={processBusy}>
                <RefreshCw size={16} aria-hidden />
                Refresh
              </button>
            </div>
          </div>
          {processError && <p className="error" role="alert">{processError}</p>}
          <div className="file-status-list">
            {(selectedImport.files ?? []).map((file) => (
              <div className="file-status-row" key={file.objectId}>
                <span className={`import-state ${stateClass(file.status)}`}>{file.status}</span>
                <span>{file.fileName}</span>
                <span>IIDM {file.iidmTransformStatus}</span>
                <span>{file.documentIds.length} docs</span>
                <span>{file.message}</span>
              </div>
            ))}
          </div>
        </section>
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

function sortImports(imports: ImportStatus[]) {
  return [...imports].sort((left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt));
}

function formatDate(value: string) {
  return new Date(value).toLocaleString();
}

function stateClass(state: string) {
  return state.toLowerCase().replace(/\s+/g, '-');
}
