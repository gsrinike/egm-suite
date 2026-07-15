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

const baseUrl = import.meta.env.VITE_CSA_API_BASE_URL ?? '';

export async function startCsaCase(request: CsaStartRequest): Promise<CsaCaseStatus> {
  const response = await fetch(`${baseUrl}/api/csa/cases`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  });
  if (!response.ok) {
    throw new Error(`Unable to start CSA case: ${response.status}`);
  }
  return response.json();
}

export async function listCsaCases(): Promise<{ items: CsaCaseStatus[]; total: number; page: number; size: number }> {
  const response = await fetch(`${baseUrl}/api/csa/cases?page=0&size=50`);
  if (!response.ok) {
    throw new Error(`Unable to load CSA cases: ${response.status}`);
  }
  return response.json();
}
