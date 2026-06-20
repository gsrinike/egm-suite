export type EquipmentType =
  | 'SUBSTATION'
  | 'VOLTAGE_LEVEL'
  | 'BUS'
  | 'LINE'
  | 'TRANSFORMER'
  | 'GENERATOR'
  | 'LOAD'
  | 'SHUNT'
  | 'SWITCH'
  | 'STATE_VARIABLE'
  | 'UNKNOWN';

export type CgmesRegion = 'CORE' | 'HANSA' | 'IBWT' | 'SWE';
export type CgmesProcess = 'CGM' | 'CSA' | 'CC' | 'OPC' | 'STA';

export interface ImportMetadata {
  businessDay: string;
  timestamp: string;
  region: CgmesRegion;
  process: CgmesProcess;
  timeFrame: string;
  tsoName: string;
  cgmesProfileType: string;
  versionNumber: string;
  extension: string;
}

export interface ImportStatus {
  networkId: string;
  fileName: string;
  metadata: ImportMetadata;
  state: string;
  indexedEquipmentCount: number;
  createdAt: string;
  message: string;
}

export interface EquipmentView {
  id: string;
  networkId: string;
  metadata: ImportMetadata;
  name: string;
  type: EquipmentType;
  containerId?: string;
  nominalVoltage: number;
  attributes: Record<string, unknown>;
}

export interface PageResponse<T> {
  content: T[];
  total: number;
  page: number;
  size: number;
}

export interface NetworkDiff {
  leftNetworkId: string;
  rightNetworkId: string;
  added: EquipmentView[];
  removed: EquipmentView[];
  changed: Array<{ left: EquipmentView; right: EquipmentView; changedFields: string[] }>;
}

export type IidmEquipmentType =
  | 'SUBSTATION'
  | 'VOLTAGE_LEVEL'
  | 'BUS'
  | 'LINE'
  | 'TWO_WINDINGS_TRANSFORMER'
  | 'GENERATOR'
  | 'LOAD'
  | 'SHUNT_COMPENSATOR'
  | 'SWITCH'
  | 'STATE_VARIABLE'
  | 'UNKNOWN';

export interface IidmExtensionValue {
  type: string;
  powsyblName: string;
  values: Record<string, unknown>;
}

export interface IidmEquipment {
  id: string;
  name: string;
  type: IidmEquipmentType;
  containerId?: string;
  nominalVoltage: number;
  extensions: IidmExtensionValue[];
  attributes: Record<string, unknown>;
}

export interface IidmNetwork {
  id: string;
  name: string;
  equipment: IidmEquipment[];
}

export async function importCgmes(files: File[], context: Pick<ImportMetadata, 'region' | 'process'>): Promise<ImportStatus> {
  const body = new FormData();
  files.forEach((file) => body.append('file', file));
  // Business day, timestamp, TSO, profile, version, and extension are parsed server-side from filenames.
  body.append('region', context.region);
  body.append('process', context.process);
  const response = await fetch('/api/cgm/imports', { method: 'POST', body });
  if (!response.ok) {
    throw new Error(`Import failed with HTTP ${response.status}`);
  }
  return response.json();
}

export async function listImports(): Promise<ImportStatus[]> {
  const response = await fetch('/api/cgm/imports');
  if (!response.ok) {
    throw new Error(`Loading imports failed with HTTP ${response.status}`);
  }
  return response.json();
}

export async function searchEquipment(
  networkId: string,
  query: string,
  type: string,
  metadata?: Partial<ImportMetadata>
): Promise<PageResponse<EquipmentView>> {
  const params = new URLSearchParams({ query, page: '0', size: '100' });
  if (type) {
    params.set('type', type);
  }
  if (metadata?.businessDay) {
    params.set('businessDay', metadata.businessDay);
  }
  if (metadata?.timestamp) {
    params.set('timestamp', normalizeTime(metadata.timestamp));
  }
  if (metadata?.region) {
    params.set('region', metadata.region);
  }
  if (metadata?.process) {
    params.set('process', metadata.process);
  }
  if (metadata?.timeFrame) {
    params.set('timeFrame', metadata.timeFrame);
  }
  if (metadata?.tsoName) {
    params.set('tsoName', metadata.tsoName);
  }
  if (metadata?.cgmesProfileType) {
    params.set('cgmesProfileType', metadata.cgmesProfileType);
  }
  if (metadata?.versionNumber) {
    params.set('versionNumber', metadata.versionNumber);
  }
  if (metadata?.extension) {
    params.set('extension', metadata.extension);
  }
  // Filters are passed to the backend so Elasticsearch can search beyond the first page of indexed data.
  const response = await fetch(`/api/cgm/networks/${networkId}/equipment?${params.toString()}`);
  if (!response.ok) {
    throw new Error(`Search failed with HTTP ${response.status}`);
  }
  return response.json();
}

export function normalizeTime(value: string) {
  return value.length >= 5 ? value.slice(0, 5) : value;
}

export async function compareNetworks(leftNetworkId: string, rightNetworkId: string): Promise<NetworkDiff> {
  const response = await fetch(`/api/cgm/networks/${leftNetworkId}/compare/${rightNetworkId}`);
  if (!response.ok) {
    throw new Error(`Compare failed with HTTP ${response.status}`);
  }
  return response.json();
}

export async function convertToIidm(networkId: string): Promise<IidmNetwork> {
  const response = await fetch(`/api/cgm/networks/${networkId}/iidm`);
  if (!response.ok) {
    throw new Error(`IIDM conversion failed with HTTP ${response.status}`);
  }
  return response.json();
}
