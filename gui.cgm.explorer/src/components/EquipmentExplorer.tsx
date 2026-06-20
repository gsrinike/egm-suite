import { Search } from 'lucide-react';
import { useEffect, useState } from 'react';
import { CgmesProcess, CgmesRegion, EquipmentView, ImportMetadata, normalizeTime, searchEquipment } from '../services/cgmApi';

const TYPES = ['', 'SUBSTATION', 'VOLTAGE_LEVEL', 'BUS', 'LINE', 'TRANSFORMER', 'GENERATOR', 'LOAD', 'SHUNT', 'SWITCH', 'STATE_VARIABLE'];

interface Props {
  networkId: string;
  metadata?: ImportMetadata;
}

export function EquipmentExplorer({ networkId, metadata }: Props) {
  const [query, setQuery] = useState('');
  const [type, setType] = useState('');
  const [businessDay, setBusinessDay] = useState(metadata?.businessDay ?? '');
  const [timestamp, setTimestamp] = useState(normalizeTime(metadata?.timestamp ?? ''));
  const [region, setRegion] = useState<CgmesRegion | ''>(metadata?.region ?? '');
  const [process, setProcess] = useState<CgmesProcess | ''>(metadata?.process ?? '');
  const [timeFrame, setTimeFrame] = useState(metadata?.timeFrame ?? '');
  const [tsoName, setTsoName] = useState(metadata?.tsoName ?? '');
  const [cgmesProfileType, setCgmesProfileType] = useState(metadata?.cgmesProfileType ?? '');
  const [versionNumber, setVersionNumber] = useState(metadata?.versionNumber ?? '');
  const [extension, setExtension] = useState(metadata?.extension ?? '');
  const [rows, setRows] = useState<EquipmentView[]>([]);
  const [total, setTotal] = useState(0);
  const [error, setError] = useState('');

  useEffect(() => {
    // Selecting a known import pre-fills filters with that import's business/file context.
    setBusinessDay(metadata?.businessDay ?? '');
    setTimestamp(normalizeTime(metadata?.timestamp ?? ''));
    setRegion(metadata?.region ?? '');
    setProcess(metadata?.process ?? '');
    setTimeFrame(metadata?.timeFrame ?? '');
    setTsoName(metadata?.tsoName ?? '');
    setCgmesProfileType(metadata?.cgmesProfileType ?? '');
    setVersionNumber(metadata?.versionNumber ?? '');
    setExtension(metadata?.extension ?? '');
  }, [metadata]);

  useEffect(() => {
    // Debounce keystrokes so the explorer feels responsive without flooding the API.
    const timeout = window.setTimeout(async () => {
      if (!networkId) {
        return;
      }
      try {
        const response = await searchEquipment(networkId, query, type, {
          businessDay,
          timestamp: normalizeTime(timestamp),
          region: region || undefined,
          process: process || undefined,
          timeFrame,
          tsoName,
          cgmesProfileType,
          versionNumber,
          extension
        });
        setRows(response.content);
        setTotal(response.total);
        setError('');
      } catch (exception) {
        setError(exception instanceof Error ? exception.message : 'Search failed');
      }
    }, 180);
    return () => window.clearTimeout(timeout);
  }, [networkId, query, type, businessDay, timestamp, region, process, timeFrame, tsoName, cgmesProfileType, versionNumber, extension]);

  return (
    <section className="pane">
      <div className="pane-header">
        <div>
          <h2>Network explorer</h2>
          <p>{total} indexed items</p>
        </div>
        <div className="filters">
          <label className="search-box">
            <Search size={16} aria-hidden />
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search id, name, container" />
          </label>
          <select value={type} onChange={(event) => setType(event.target.value)} aria-label="Equipment type">
            {TYPES.map((item) => <option key={item} value={item}>{item || 'All types'}</option>)}
          </select>
          <input type="date" value={businessDay} onChange={(event) => setBusinessDay(event.target.value)} aria-label="Business day filter" />
          <input type="time" step={900} value={normalizeTime(timestamp)} onChange={(event) => setTimestamp(normalizeTime(event.target.value))} aria-label="Timestamp filter" />
          <select value={region} onChange={(event) => setRegion(event.target.value as CgmesRegion | '')} aria-label="Region filter">
            <option value="">All regions</option>
            {(['CORE', 'HANSA', 'IBWT', 'SWE'] satisfies CgmesRegion[]).map((item) => <option key={item} value={item}>{item}</option>)}
          </select>
          <select value={process} onChange={(event) => setProcess(event.target.value as CgmesProcess | '')} aria-label="Process filter">
            <option value="">All processes</option>
            {(['CGM', 'CSA', 'CC', 'OPC', 'STA'] satisfies CgmesProcess[]).map((item) => <option key={item} value={item}>{item}</option>)}
          </select>
          <input value={timeFrame} onChange={(event) => setTimeFrame(event.target.value)} placeholder="Time frame" aria-label="Time frame filter" />
          <input value={tsoName} onChange={(event) => setTsoName(event.target.value)} placeholder="TSO" aria-label="TSO filter" />
          <input value={cgmesProfileType} onChange={(event) => setCgmesProfileType(event.target.value)} placeholder="Profile" aria-label="CGMES profile filter" />
          <input value={versionNumber} onChange={(event) => setVersionNumber(event.target.value)} placeholder="Version" aria-label="Version filter" />
          <input value={extension} onChange={(event) => setExtension(event.target.value)} placeholder="Ext" aria-label="Extension filter" />
        </div>
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Type</th>
              <th>Name</th>
              <th>ID</th>
              <th>Container</th>
              <th>kV</th>
              <th>Business context</th>
              <th>File context</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={`${row.networkId}-${row.id}`}>
                <td><span className="chip">{row.type}</span></td>
                <td>{row.name}</td>
                <td>{row.id}</td>
                <td>{row.containerId ?? ''}</td>
                <td>{row.nominalVoltage || ''}</td>
                <td>{row.metadata.businessDay} {row.metadata.timestamp} {row.metadata.region} {row.metadata.process}</td>
                <td>{row.metadata.timeFrame} {row.metadata.tsoName} {row.metadata.cgmesProfileType} {row.metadata.versionNumber} {row.metadata.extension}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
