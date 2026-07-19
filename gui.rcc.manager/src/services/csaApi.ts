import { HttpClientError } from '@egm/gui.common/src';

export type WorkflowStatus = 'INIT' | 'RUNNING' | 'WAITING' | 'COMPLETED' | 'FAILED';

export interface NetworkCaseReference {
  caseId: string;
  networkId: string;
  businessDay: string;
  businessTime: string;
  timeFrame: 'ID' | 'DAY_AHEAD' | 'TWO_DAYS_AHEAD';
}

export interface CsaStartRequest {
  caseName: string;
  networkCase: NetworkCaseReference;
  contingencyIds: string[];
  optimizeRemedialActions: boolean;
}

export interface WorkflowTaskView {
  taskId: string;
  name: string;
  status: string;
  startedAt: string;
  completedAt: string;
  message: string;
}

export interface CsaCaseStatus {
  csaCaseId: string;
  caseName: string;
  status: WorkflowStatus;
  networkCase: NetworkCaseReference;
  processInstanceId: string;
  loadFlowResult?: { message: string; lineFlows: Array<Record<string, unknown>> };
  securityAnalysisResult?: {
    message: string;
    preContingencyViolations: Array<Record<string, unknown>>;
    postContingencyViolations: Array<Record<string, unknown>>;
  };
  raoResult?: { message: string; actions: Array<Record<string, unknown>> };
  tasks: WorkflowTaskView[];
  createdAt: string;
  updatedAt: string;
  message: string;
}

export async function startCsaCase(request: CsaStartRequest): Promise<CsaCaseStatus> {
  const baseUrl = csaBaseUrl();
  const url = `${baseUrl}/api/csa/cases`;
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  });
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to start CSA case', url, response);
  }
  return response.json();
}

export async function listCsaCases(): Promise<{ items: CsaCaseStatus[]; total: number; page: number; size: number }> {
  const baseUrl = csaBaseUrl();
  const url = `${baseUrl}/api/csa/cases?page=0&size=50`;
  const response = await fetch(url);
  if (!response.ok) {
    throw await HttpClientError.fromResponse('Unable to load CSA cases', url, response);
  }
  return response.json();
}

function csaBaseUrl() {
  return window.EGM_CONFIG?.apis?.csaBaseUrl ?? import.meta.env.VITE_CSA_API_BASE_URL ?? '';
}
