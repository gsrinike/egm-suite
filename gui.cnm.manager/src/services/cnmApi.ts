export type CnmServiceType = 'CGM' | 'CSA' | 'CC';
export type TimeFrame = 'ID' | 'DAY_AHEAD' | 'TWO_DAYS_AHEAD';
export type ImportState = 'INIT' | 'STORED' | 'FAILED';
export type ImportFileState = 'INIT' | 'STORED' | 'PARSED' | 'FAILED';

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

export interface ImportPage {
  items: ImportStatus[];
  total: number;
  page: number;
  size: number;
}

const baseUrl = import.meta.env.VITE_CNM_API_BASE_URL ?? '';
const CHUNK_SIZE = 8 * 1024 * 1024;
const MAX_FILE_SIZE = 1024 * 1024 * 1024;

export async function listImports(): Promise<ImportPage> {
  const response = await fetch(`${baseUrl}/api/cnm/imports?page=0&size=50`);
  if (!response.ok) {
    throw new Error(`Unable to load imports: ${response.status}`);
  }
  return response.json();
}

export async function getImport(importId: string): Promise<ImportStatus> {
  const response = await fetch(`${baseUrl}/api/cnm/imports/${encodeURIComponent(importId)}`);
  if (!response.ok) {
    throw new Error(`Unable to load import files: ${response.status}`);
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
        const chunkResponse = await fetch(`${baseUrl}/api/cnm/imports/chunks?${params}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/octet-stream' },
          body: chunk
        });
        if (!chunkResponse.ok) {
          throw new Error(`Unable to upload ${file.name} chunk ${chunkIndex + 1}: ${chunkResponse.status}`);
        }
      }
    }
    const response = await fetch(`${baseUrl}/api/cnm/imports/chunks/complete`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ importId, serviceType, timeFrame, message })
    });
    if (!response.ok) {
      throw new Error(`Unable to complete model import: ${response.status}`);
    }
    return response.json();
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unable to import model: upload connection failed';
    await reportImportFailure(importId, files, serviceType, timeFrame, message);
    throw new ImportUploadError(message, importId);
  }
}

export async function listProfiles(filters: {
  profileType?: string;
  tso?: string;
  businessDay?: string;
  businessTime?: string;
}): Promise<{ items: ProfileMetadata[]; total: number; page: number; size: number }> {
  const params = new URLSearchParams({ page: '0', size: '100' });
  Object.entries(filters).forEach(([key, value]) => {
    if (value) {
      params.set(key, value);
    }
  });
  const response = await fetch(`${baseUrl}/api/cnm/imports/profiles?${params}`);
  if (!response.ok) {
    throw new Error(`Unable to load profiles: ${response.status}`);
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
