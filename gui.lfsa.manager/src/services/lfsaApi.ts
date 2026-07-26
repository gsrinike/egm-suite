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

export interface IidmNetworkOption {
  id: string;
  importId: string;
  sourceFileIds: string[];
  sourceFileNames: string[];
  businessDay: string;
  businessTime: string;
  timeFrame: string;
  tsoName: string;
  networkFormat: string;
}

export interface SensitivityAnalysisParametersDto {
  dc: boolean;
  functionType: string;
  variableType: string;
  contingencyContext: string;
  maxMonitoredBranches: number;
  maxVariables: number;
  maxGeneratedContingencies: number;
  flowFlowSensitivityValueThreshold: number;
  voltageVoltageSensitivityValueThreshold: number;
  flowVoltageSensitivityValueThreshold: number;
  angleFlowSensitivityValueThreshold: number;
  operatorStrategiesCalculationMode: string;
  debugDir: string;
}

export interface SensitivityAnalysisConfiguration {
  id: string;
  name: string;
  source: string;
  createdAt: string;
  updatedAt: string;
  parameters: SensitivityAnalysisParametersDto;
}

export interface SensitivityAnalysisRunSummary {
  runId: string;
  fileImportId: string;
  state: string;
  runDate: string;
  runTime: string;
  networkCount: number;
  factorCount: number;
  resultCount: number;
  diagnosticCount: number;
  ptdfObjectId: string;
  lodfObjectId: string;
  glskObjectId: string;
  message: string;
}

export interface SensitivityAnalysisRunDetail {
  summary: SensitivityAnalysisRunSummary;
  configuration: SensitivityAnalysisConfiguration;
  iidmNetworkIds: string[];
  inputReferences: Record<string, string>;
  factors: Record<string, unknown>[];
  matrixRows: Record<string, unknown>[];
  networkElementCounts: Record<string, number>;
  diagnostics: string[];
}

export interface SensitivityInputUploadResponse {
  kind: string;
  fileName: string;
  objectId: string;
  size: number;
}

export interface SensitivityInputTable {
  kind: string;
  objectId: string;
  rows: Record<string, unknown>[];
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

export async function listSensitivityIidmNetworks(params: {
  importId: string;
  page?: number;
  size?: number;
}, options: LfsaApiOptions = {}): Promise<Page<IidmNetworkOption>> {
  return getJson<Page<IidmNetworkOption>>(`${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/sensitivity/iidm-networks${query(params)}`);
}

export async function getDefaultSensitivityConfiguration(options: LfsaApiOptions = {}): Promise<SensitivityAnalysisConfiguration> {
  return getJson<SensitivityAnalysisConfiguration>(
    `${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/sensitivity/configurations/default`
  );
}

export async function listSensitivityConfigurations(params: {
  page?: number;
  size?: number;
}, options: LfsaApiOptions = {}): Promise<Page<SensitivityAnalysisConfiguration>> {
  return getJson<Page<SensitivityAnalysisConfiguration>>(
    `${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/sensitivity/configurations${query(params)}`
  );
}

export async function saveSensitivityConfiguration(
  name: string,
  parameters: SensitivityAnalysisParametersDto,
  options: LfsaApiOptions = {}
): Promise<SensitivityAnalysisConfiguration> {
  const response = await fetch(`${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/sensitivity/configurations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, parameters })
  });
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to save sensitivity configuration', response.url, response);
  }
  return response.json();
}

export async function uploadSensitivityInput(
  kind: 'PTDF' | 'LODF' | 'GLSK',
  file: File,
  options: LfsaApiOptions = {}
): Promise<SensitivityInputUploadResponse> {
  const formData = new FormData();
  formData.append('kind', kind);
  formData.append('file', file);
  const response = await fetch(`${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/sensitivity/inputs`, {
    method: 'POST',
    body: formData
  });
  if (!response.ok) {
    throw await HttpClientError.fromResponse(`Unable to upload ${kind} input`, response.url, response);
  }
  return response.json();
}

export async function startSensitivityAnalysis(
  fileImportId: string,
  iidmNetworkIds: string[],
  configurationId = '',
  ptdfObjectId = '',
  lodfObjectId = '',
  glskObjectId = '',
  options: LfsaApiOptions = {}
): Promise<SensitivityAnalysisRunSummary> {
  const response = await fetch(`${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/sensitivity/runs`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fileImportId, iidmNetworkIds, configurationId, ptdfObjectId, lodfObjectId, glskObjectId })
  });
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to start sensitivity analysis', response.url, response);
  }
  return response.json();
}

export async function searchSensitivityRuns(params: {
  runId?: string;
  runDate?: string;
  runTime?: string;
  page?: number;
  size?: number;
}, options: LfsaApiOptions = {}): Promise<Page<SensitivityAnalysisRunSummary>> {
  return getJson<Page<SensitivityAnalysisRunSummary>>(`${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/sensitivity/runs${query(params)}`);
}

export async function getSensitivityRunDetail(runId: string, options: LfsaApiOptions = {}): Promise<SensitivityAnalysisRunDetail> {
  return getJson<SensitivityAnalysisRunDetail>(`${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/sensitivity/runs/${encodeURIComponent(runId)}`);
}

export async function getSensitivityInputTable(
  runId: string,
  kind: 'PTDF' | 'LODF' | 'GLSK',
  options: LfsaApiOptions = {}
): Promise<SensitivityInputTable> {
  return getJson<SensitivityInputTable>(
    `${options.baseUrl ?? lfsaBaseUrl()}/api/common/lfsa/sensitivity/runs/${encodeURIComponent(runId)}/inputs/${kind}/table`
  );
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
