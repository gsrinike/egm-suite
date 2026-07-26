import { HttpClientError } from '@egm/gui.common/src';

export interface Page<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

export interface SecurityAnalysisImportCandidate {
  importId: string;
  service: string;
  timeFrame: string;
  state: string;
  createdAt: string;
  businessDay: string;
  message: string;
}

export interface SecurityAnalysisRunSummary {
  runId: string;
  fileImportId: string;
  state: string;
  runDate: string;
  runTime: string;
  networkCount: number;
  lineFlowCount: number;
  violationCount: number;
  diagnosticCount: number;
  message: string;
}

export interface LineFlow {
  elementId: string;
  fromNode: string;
  toNode: string;
  activePowerMw: number;
  reactivePowerMvar: number;
  loadingPercent: number;
}

export interface ContingencyViolation {
  contingencyId: string;
  elementId: string;
  violationType: string;
  observedValue: number;
  limitValue: number;
  unit: string;
  severity: string;
}

export interface SecurityAnalysisRunDetail {
  summary: SecurityAnalysisRunSummary;
  lineFlows: LineFlow[];
  violations: ContingencyViolation[];
  networkElementCounts: Record<string, number>;
  diagnostics: string[];
}

export interface LfsaApiOptions {
  baseUrl?: string;
}

export function lfsaBaseUrl() {
  return window.EGM_CONFIG?.apis?.lfsaBaseUrl ?? '';
}

export async function searchImports(params: {
  service?: string;
  timeFrame?: string;
  date?: string;
  page?: number;
  size?: number;
}, options: LfsaApiOptions = {}): Promise<Page<SecurityAnalysisImportCandidate>> {
  return getJson<Page<SecurityAnalysisImportCandidate>>(`${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/imports${query(params)}`);
}

export async function startSecurityAnalysis(fileImportId: string, options: LfsaApiOptions = {}): Promise<SecurityAnalysisRunSummary> {
  const response = await fetch(`${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/security-analysis/runs`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fileImportId })
  });
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to start security analysis', response.url, response);
  }
  return response.json();
}

export async function searchRuns(params: {
  runId?: string;
  runDate?: string;
  runTime?: string;
  page?: number;
  size?: number;
}, options: LfsaApiOptions = {}): Promise<Page<SecurityAnalysisRunSummary>> {
  return getJson<Page<SecurityAnalysisRunSummary>>(`${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/security-analysis/runs${query(params)}`);
}

export async function getRunDetail(runId: string, options: LfsaApiOptions = {}): Promise<SecurityAnalysisRunDetail> {
  return getJson<SecurityAnalysisRunDetail>(`${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/security-analysis/runs/${encodeURIComponent(runId)}`);
}

async function getJson<T>(url: string): Promise<T> {
  const response = await fetch(url, { cache: 'no-store' });
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to load LFSA data', response.url, response);
  }
  return response.json();
}

function query(params: Record<string, unknown>) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      search.set(key, String(value));
    }
  });
  const text = search.toString();
  return text ? `?${text}` : '';
}
