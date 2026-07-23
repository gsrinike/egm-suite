import { HttpClientError } from '@egm/gui.common/src';

export type CnmServiceType = 'CGM' | 'CSA' | 'CC';
export type TimeFrame = 'ID' | 'DAY_AHEAD' | 'TWO_DAYS_AHEAD';
export type ImportState = 'INIT' | 'STORED' | 'SUCCESS' | 'FAILED';
export type ImportFileState = 'INIT' | 'STORED' | 'PARSED' | 'FAILED';
export type CnmSnapshotState = 'STARTED' | 'DONE' | 'FAILED';
export type IidmTransformationStatus = 'NOT_STARTED' | 'STARTED' | 'DONE' | 'FAILED';

export interface ImportFileStatus {
  fileId: string;
  fileName: string;
  objectId: string;
  state: ImportFileState;
  profileFamily: string;
  businessDay: string;
  businessTime: string;
  modelTimeFrame: string;
  tsoName: string;
  profileType: string;
  modelVersion: string;
  message: string;
  iidmTransformationStatus: IidmTransformationStatus;
  iidmTransformationCount: number;
  uploadedAt: string;
}

export interface ImportStatus {
  importId: string;
  serviceType: CnmServiceType;
  timeFrame: TimeFrame;
  state: ImportState;
  message: string;
  createdAt: string;
  files: ImportFileStatus[];
}

export interface ProfileMetadata {
  profileId: string;
  fileId: string;
  importId: string;
  fileName: string;
  objectId: string;
  state: ImportFileState;
  profileFamily: string;
  profileType: string;
  tsoName: string;
  businessDay: string;
  businessTime: string;
  timeFrame: string;
  version: string;
  importedAt: string;
}

export interface CnmSnapshotMetadata {
  snapshotId: string;
  importId: string;
  serviceType: CnmServiceType;
  tsoName: string;
  businessDay: string;
  businessTime: string;
  timeFrame: string;
  sourceFileIds: string[];
  staticObjectCount: number;
  relationCount: number;
  stateValueCount: number;
  diagnosticCount: number;
  payloadSectionCount: number;
  state: CnmSnapshotState;
  message: string;
  assembledAt: string;
}

export interface DynamicTableBundle {
  importId: string;
  fileId: string;
  sourceFileIds: string[];
  sourceFileNames: string[];
  profileType: string;
  profileFamily: string;
  payload: unknown;
  tables: DynamicTableDefinition[];
}

export type IidmTransformState = 'STARTED' | 'DONE' | 'FAILED';

export interface IidmTransformSummary {
  transformId: string;
  importId: string;
  fileId: string;
  sourceFileIds: string[];
  sourceFileNames: string[];
  profileType: string;
  profileFamily: string;
  transformState: IidmTransformState;
  transformMessage: string;
  diagnosticCount: number;
  networkId: string;
  startedAt: string;
  completedAt: string;
  failedAt: string;
}

export interface IidmElementCount {
  elementType: string;
  count: number;
}

export interface IidmNetworkSummary {
  networkId: string;
  importId: string;
  sourceFileIds: string[];
  businessDay: string;
  businessTime: string;
  timeFrame: string;
  tsoName: string;
  networkFormat: string;
  elementCounts: IidmElementCount[];
  createdAt: string;
  updatedAt: string;
}

export interface IidmTableBundle {
  importId: string;
  networkId: string;
  sourceFileId: string;
  tableId: string;
  page: number;
  size: number;
  tables: DynamicTableDefinition[];
}

export interface DynamicTableColumn {
  key: string;
  label: string;
  type: string;
  sortable: boolean;
  searchable: boolean;
  unit: string;
}

export interface DynamicTableRow {
  rowId: string;
  values: Record<string, unknown>;
}

export interface DynamicTableDefinition {
  tableId: string;
  label: string;
  columns: DynamicTableColumn[];
  rows: DynamicTableRow[];
  totalRows: number;
  defaultSort: string;
}

export interface ImportPage {
  items: ImportStatus[];
  total: number;
  page: number;
  size: number;
}

export interface IidmPage<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

const CHUNK_SIZE = 8 * 1024 * 1024;
const MAX_FILE_SIZE = 1024 * 1024 * 1024;

export async function listImports(): Promise<ImportPage> {
  const baseUrl = cnmBaseUrl();
  const url = `${baseUrl}/api/cnm/imports?page=0&size=50`;
  const response = await fetch(url);
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to load imports', url, response);
  }
  return response.json();
}

export async function getImport(importId: string): Promise<ImportStatus> {
  const baseUrl = cnmBaseUrl();
  const url = `${baseUrl}/api/cnm/imports/${encodeURIComponent(importId)}`;
  const response = await fetch(url);
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to load import files', url, response);
  }
  return response.json();
}

export async function getProfilePayload(importId: string, fileId: string): Promise<unknown> {
  const baseUrl = cnmBaseUrl();
  const url = `${baseUrl}/api/cnm/imports/${encodeURIComponent(importId)}/files/${encodeURIComponent(fileId)}/profile/payload`;
  const response = await fetch(url);
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to load profile payload', url, response);
  }
  return response.json();
}

export async function getProfileTables(importId: string, fileId: string): Promise<DynamicTableBundle> {
  const baseUrl = cnmBaseUrl();
  const url = `${baseUrl}/api/cnm/imports/${encodeURIComponent(importId)}/files/${encodeURIComponent(fileId)}/profile/tables`;
  const response = await fetch(url);
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to load profile data', url, response);
  }
  return response.json();
}

export async function uploadImport(
  files: File[],
  serviceType: CnmServiceType,
  timeFrame: TimeFrame,
  message: string,
  importId = crypto.randomUUID()
): Promise<ImportStatus> {
  const baseUrl = cnmBaseUrl();
  try {
    for (const file of files) {
      if (file.size > MAX_FILE_SIZE) {
        throw new Error(`${file.name} exceeds the 1 GB import limit`);
      }
      const fileId = crypto.randomUUID();
      const totalChunks = Math.max(1, Math.ceil(file.size / CHUNK_SIZE));
      for (let chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
        const chunk = file.slice(chunkIndex * CHUNK_SIZE, Math.min(file.size, (chunkIndex + 1) * CHUNK_SIZE));
        const params = new URLSearchParams({
          importId,
          fileId,
          fileName: file.name,
          chunkIndex: String(chunkIndex),
          totalChunks: String(totalChunks),
          fileSize: String(file.size)
        });
        const url = `${baseUrl}/api/cnm/imports/chunks?${params}`;
        const chunkResponse = await fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/octet-stream' },
          body: chunk
        });
        if (!chunkResponse.ok) {
          throw await HttpClientError.fromResponse(`Unable to upload ${file.name} chunk ${chunkIndex + 1}`, url, chunkResponse);
        }
      }
    }
    const url = `${baseUrl}/api/cnm/imports/chunks/complete`;
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ importId, serviceType, timeFrame, message })
    });
    if (!response.ok) {
      throw await HttpClientError.fromResponse('Unable to complete model import', url, response);
    }
    return response.json();
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unable to import model: upload connection failed';
    await reportImportFailure(importId, files, serviceType, timeFrame, message);
    throw new ImportUploadError(message, importId);
  }
}

export async function listProfiles(filters: {
  importId?: string;
  profileType?: string;
  tso?: string;
  businessDay?: string;
  businessTime?: string;
}): Promise<{ items: ProfileMetadata[]; total: number; page: number; size: number }> {
  const baseUrl = cnmBaseUrl();
  const params = new URLSearchParams({ page: '0', size: '100' });
  Object.entries(filters).forEach(([key, value]) => {
    if (value) {
      params.set(key, value);
    }
  });
  const url = `${baseUrl}/api/cnm/imports/profiles?${params}`;
  const response = await fetch(url);
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to load profiles', url, response);
  }
  return response.json();
}

export async function listSnapshots(filters: {
  importId?: string;
  state?: CnmSnapshotState;
  page?: number;
  size?: number;
}): Promise<{ items: CnmSnapshotMetadata[]; total: number; page: number; size: number }> {
  const baseUrl = cnmBaseUrl();
  const params = new URLSearchParams({
    page: String(filters.page ?? 0),
    size: String(filters.size ?? 100)
  });
  if (filters.importId) {
    params.set('importId', filters.importId);
  }
  if (filters.state) {
    params.set('state', filters.state);
  }
  const url = `${baseUrl}/api/cnm/imports/snapshots?${params}`;
  const response = await fetch(url);
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to load CNM snapshots', url, response);
  }
  return response.json();
}

export async function listIidmTransforms(filters: {
  importId?: string;
  search?: string;
  page?: number;
  size?: number;
}): Promise<IidmPage<IidmTransformSummary>> {
  const baseUrl = iidmBaseUrl();
  const params = new URLSearchParams({
    page: String(filters.page ?? 0),
    size: String(filters.size ?? 100)
  });
  if (filters.importId) {
    params.set('importId', filters.importId);
  }
  if (filters.search) {
    params.set('search', filters.search);
  }
  const url = `${baseUrl}/api/iidm/transforms?${params}`;
  const response = await fetch(url);
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to load IIDM transforms', url, response);
  }
  return response.json();
}

export async function listIidmNetworks(filters: {
  importId?: string;
  page?: number;
  size?: number;
}): Promise<IidmPage<IidmNetworkSummary>> {
  const baseUrl = iidmBaseUrl();
  const params = new URLSearchParams({
    page: String(filters.page ?? 0),
    size: String(filters.size ?? 100)
  });
  if (filters.importId) {
    params.set('importId', filters.importId);
  }
  const url = `${baseUrl}/api/iidm/networks?${params}`;
  const response = await fetch(url);
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to load IIDM networks', url, response);
  }
  return response.json();
}

export async function getIidmNetworkTables(networkId: string): Promise<IidmTableBundle> {
  const baseUrl = iidmBaseUrl();
  const url = `${baseUrl}/api/iidm/networks/${encodeURIComponent(networkId)}/tables`;
  const response = await fetch(url);
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to load IIDM table metadata', url, response);
  }
  return response.json();
}

export async function getIidmNetworkTableRows(
  networkId: string,
  tableId: string,
  page: number,
  size: number,
  search = ''
): Promise<IidmTableBundle> {
  const baseUrl = iidmBaseUrl();
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (search) {
    params.set('search', search);
  }
  const url = `${baseUrl}/api/iidm/networks/${encodeURIComponent(networkId)}/tables/${encodeURIComponent(tableId)}?${params}`;
  const response = await fetch(url);
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to load IIDM table rows', url, response);
  }
  return response.json();
}

export async function reportImportFailure(
  importId: string,
  files: File[],
  serviceType: CnmServiceType,
  timeFrame: TimeFrame,
  message: string
): Promise<ImportStatus | undefined> {
  const baseUrl = cnmBaseUrl();
  try {
    const response = await fetch(`${baseUrl}/api/cnm/imports/failures`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        importId,
        serviceType,
        timeFrame,
        fileNames: files.map((file) => file.name),
        message
      })
    });
    return response.ok ? response.json() : undefined;
  } catch {
    return undefined;
  }
}

export class ImportUploadError extends Error {
  constructor(message: string, readonly importId: string) {
    super(message);
  }
}

function cnmBaseUrl() {
  return window.EGM_CONFIG?.apis?.cnmBaseUrl ?? import.meta.env.VITE_CNM_API_BASE_URL ?? '';
}

function iidmBaseUrl() {
  return window.EGM_CONFIG?.apis?.iidmBaseUrl ?? import.meta.env.VITE_IIDM_API_BASE_URL ?? '';
}
