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
  loadFlowState: string;
  securityAnalysisState: string;
  runDate: string;
  runTime: string;
  networkCount: number;
  lineFlowCount: number;
  violationCount: number;
  diagnosticCount: number;
  message: string;
}

export interface LoadFlowComputationResult {
  succeeded: boolean;
  status: string;
  componentCount: number;
  componentStatuses: string[];
  metrics: Record<string, string>;
  logs: string;
}

export type LoadFlowStrategy = 'DC_ONLY' | 'AC_ONLY' | 'AC_WITH_DC_FAILOVER';

export interface LoadFlowParametersDto {
  distributedSlack: boolean;
  useReactiveLimits: boolean;
  transformerVoltageControlOn: boolean;
  phaseShifterRegulationOn: boolean;
  shuntCompensatorVoltageControlOn: boolean;
  readSlackBus: boolean;
  writeSlackBus: boolean;
  voltageInitMode: string;
  balanceType: string;
  componentMode: string;
  hvdcAcEmulation: boolean;
  dcPowerFactor: number;
}

export interface SecurityAnalysisParametersDto {
  voltageLimitsChecked: boolean;
  currentLimitsChecked: boolean;
  activePowerLimitsChecked: boolean;
  intermediateResultsInOperatorStrategy: boolean;
  debugDir: string;
  contingencyElementType: string;
  maxGeneratedContingencies: number;
}

export interface LfSaParameterConfiguration {
  id: string;
  name: string;
  source: string;
  createdAt: string;
  updatedAt: string;
  loadFlowStrategy: LoadFlowStrategy;
  loadFlowParameters: LoadFlowParametersDto;
  securityAnalysisParameters: SecurityAnalysisParametersDto;
}

export interface SecurityAnalysisComputationResult {
  succeeded: boolean;
  preContingencyStatus: string;
  contingencyCount: number;
  postContingencyStatuses: string[];
  preContingencyViolations: ContingencyViolation[];
  postContingencyViolations: ContingencyViolation[];
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
  parameterConfiguration: LfSaParameterConfiguration;
  loadFlowResult: LoadFlowComputationResult | null;
  computationResult: SecurityAnalysisComputationResult | null;
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

export async function startSecurityAnalysis(
  fileImportId: string,
  parameterConfigurationId = '',
  options: LfsaApiOptions = {}
): Promise<SecurityAnalysisRunSummary> {
  const response = await fetch(`${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/security-analysis/runs`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fileImportId, parameterConfigurationId })
  });
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to start security analysis', response.url, response);
  }
  return response.json();
}

export async function getDefaultSecurityAnalysisParameters(options: LfsaApiOptions = {}): Promise<LfSaParameterConfiguration> {
  return getJson<LfSaParameterConfiguration>(
    `${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/security-analysis/parameters/default`
  );
}

export async function listSecurityAnalysisParameters(params: {
  page?: number;
  size?: number;
}, options: LfsaApiOptions = {}): Promise<Page<LfSaParameterConfiguration>> {
  return getJson<Page<LfSaParameterConfiguration>>(
    `${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/security-analysis/parameters${query(params)}`
  );
}

export async function saveSecurityAnalysisParameters(
  name: string,
  loadFlowStrategy: LoadFlowStrategy,
  loadFlowParameters: LoadFlowParametersDto,
  securityAnalysisParameters: SecurityAnalysisParametersDto,
  options: LfsaApiOptions = {}
): Promise<LfSaParameterConfiguration> {
  const response = await fetch(`${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/security-analysis/parameters`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, loadFlowStrategy, loadFlowParameters, securityAnalysisParameters })
  });
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to save LFnSA configuration', response.url, response);
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
