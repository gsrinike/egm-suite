import { GitCompare } from 'lucide-react';
import { FormEvent, useState } from 'react';
import { NetworkDiff, compareNetworks } from '../services/cgmApi';

interface Props {
  currentNetworkId: string;
}

export function NetworkCompare({ currentNetworkId }: Props) {
  const [left, setLeft] = useState(currentNetworkId);
  const [right, setRight] = useState('');
  const [diff, setDiff] = useState<NetworkDiff | null>(null);
  const [error, setError] = useState('');

  async function submit(event: FormEvent) {
    event.preventDefault();
    try {
      setDiff(await compareNetworks(left, right));
      setError('');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Compare failed');
    }
  }

  return (
    <section className="pane">
      <div className="pane-header">
        <div>
          <h2>State comparison</h2>
          <p>Added, removed and changed equipment</p>
        </div>
      </div>
      <form className="compare-form" onSubmit={submit}>
        <input value={left} onChange={(event) => setLeft(event.target.value)} placeholder="Base network id" />
        <input value={right} onChange={(event) => setRight(event.target.value)} placeholder="Study network id" />
        <button type="submit" disabled={!left || !right}><GitCompare size={16} aria-hidden />Compare</button>
      </form>
      {error && <p className="error" role="alert">{error}</p>}
      {diff && (
        <div className="diff-grid">
          <Stat label="Added" value={diff.added.length} />
          <Stat label="Removed" value={diff.removed.length} />
          <Stat label="Changed" value={diff.changed.length} />
        </div>
      )}
    </section>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="stat">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
