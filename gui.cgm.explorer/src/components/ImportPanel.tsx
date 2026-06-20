import { Upload } from 'lucide-react';
import { FormEvent, useState } from 'react';
import { CgmesProcess, CgmesRegion, ImportStatus, importCgmes } from '../services/cgmApi';

interface Props {
  onImported: (status: ImportStatus) => void;
}

export function ImportPanel({ onImported }: Props) {
  const [files, setFiles] = useState<File[]>([]);
  const [context, setContext] = useState<{ region: CgmesRegion; process: CgmesProcess }>(() => ({
    region: 'CORE',
    process: 'CGM'
  }));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (files.length === 0) {
      return;
    }
    setBusy(true);
    setError('');
    try {
      onImported(await importCgmes(files, context));
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Import failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="toolbar" onSubmit={submit}>
      <label>
        <span>Region</span>
        <select value={context.region} onChange={(event) => setContext({ ...context, region: event.target.value as CgmesRegion })}>
          {(['CORE', 'HANSA', 'IBWT', 'SWE'] satisfies CgmesRegion[]).map((region) => <option key={region} value={region}>{region}</option>)}
        </select>
      </label>
      <label>
        <span>Process</span>
        <select value={context.process} onChange={(event) => setContext({ ...context, process: event.target.value as CgmesProcess })}>
          {(['CGM', 'CSA', 'CC', 'OPC', 'STA'] satisfies CgmesProcess[]).map((process) => <option key={process} value={process}>{process}</option>)}
        </select>
      </label>
      <label className="file-picker">
        <Upload size={18} aria-hidden />
        <span>{files.length > 0 ? fileLabel(files) : 'Choose CGMES files'}</span>
        <input
          type="file"
          accept=".xml,.rdf,.zip,.uct,.iidm"
          multiple
          onChange={(event) => setFiles(Array.from(event.target.files ?? []))}
        />
      </label>
      <button type="submit" disabled={files.length === 0 || busy}>{busy ? 'Importing' : 'Import'}</button>
      {error && <span className="error" role="alert">{error}</span>}
    </form>
  );
}

function fileLabel(files: File[]) {
  if (files.length === 1) {
    return files[0].name;
  }
  return `${files.length} CGMES files selected`;
}
