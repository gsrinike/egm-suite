<template>
  <section class="lfsa-manager">
    <header v-if="!embedded" class="lfsa-header">
      <div>
        <p class="eyebrow">CGM</p>
        <h2>Load Flow & Security Analysis</h2>
      </div>
    </header>

    <div class="tab-row">
      <button type="button" :class="{ active: activeTab === 'search' }" @click="switchTab('search')">Search</button>
      <button type="button" :class="{ active: activeTab === 'parameters' }" @click="switchTab('parameters')">LFnSA Configuration</button>
      <button type="button" :class="{ active: activeTab === 'results' }" @click="switchTab('results')">LFnSA Results</button>
    </div>

    <section v-if="activeTab === 'search'" class="panel">
      <div class="form-grid">
        <label>
          Service
          <select v-model="search.service">
            <option value="">Any service</option>
            <option value="CGM">CGM</option>
            <option value="CSA">CSA</option>
            <option value="CC">CC</option>
          </select>
        </label>
        <label>
          Timeframe
          <select v-model="search.timeFrame">
            <option value="">Any timeframe</option>
            <option value="DAY">Day Ahead</option>
            <option value="INTRA">Intra-Day</option>
            <option value="2D">Day-2</option>
          </select>
        </label>
        <label>
          Date
          <input v-model="search.date" type="date" />
        </label>
        <button type="button" @click="loadImports">Search</button>
      </div>
      <p v-if="message" class="message">{{ message }}</p>
      <DataTable :columns="importColumns" :rows="importRows" id-key="importId" :page-size="10">
        <template #cell-action="{ row }">
          <div class="run-action">
            <select v-model="selectedParameterByImport[String(row.importId)]">
              <option value="">Default LFnSA configuration</option>
              <option v-for="config in savedParameterConfigurations" :key="config.id" :value="config.id">
                {{ config.name }}
              </option>
            </select>
            <button type="button" @click="startRun(String(row.importId))">Run LFnSA</button>
          </div>
        </template>
      </DataTable>
    </section>

    <section v-else-if="activeTab === 'parameters'" class="panel">
      <div class="parameter-header">
        <div>
          <p class="eyebrow">Configuration</p>
          <h3>LFnSA Configuration</h3>
        </div>
        <button type="button" @click="resetParameterForm">Reset to default</button>
      </div>
      <div class="form-grid parameter-form">
        <label>
          Configuration name
          <input v-model="parameterName" type="text" placeholder="DateTimeStamp_SA_Conf" />
        </label>
        <label>
          Load flow strategy
          <select v-model="loadFlowStrategy">
            <option value="DC_ONLY">DC only</option>
            <option value="AC_ONLY">AC only</option>
            <option value="AC_WITH_DC_FAILOVER">AC with DC failover</option>
          </select>
        </label>
      </div>

      <div class="parameter-section">
        <p class="eyebrow">Load Flow</p>
        <div class="form-grid parameter-form">
        <label>
          Voltage init mode
          <select v-model="loadFlowForm.voltageInitMode">
            <option value="PREVIOUS_VALUES">Previous values</option>
            <option value="UNIFORM_VALUES">Uniform values</option>
            <option value="DC_VALUES">DC values</option>
          </select>
        </label>
        <label>
          Balance type
          <select v-model="loadFlowForm.balanceType">
            <option value="PROPORTIONAL_TO_GENERATION_P">Generation P</option>
            <option value="PROPORTIONAL_TO_GENERATION_P_MAX">Generation P max</option>
            <option value="PROPORTIONAL_TO_GENERATION_REMAINING_MARGIN">Generation margin</option>
            <option value="PROPORTIONAL_TO_GENERATION_PARTICIPATION_FACTOR">Participation factor</option>
            <option value="PROPORTIONAL_TO_LOAD">Load</option>
            <option value="PROPORTIONAL_TO_CONFORM_LOAD">Conform load</option>
          </select>
        </label>
        <label>
          Component mode
          <select v-model="loadFlowForm.componentMode">
            <option value="MAIN_CONNECTED">Main connected</option>
            <option value="ALL_CONNECTED">All connected</option>
            <option value="MAIN_SYNCHRONOUS">Main synchronous</option>
          </select>
        </label>
        <label>
          DC power factor
          <input v-model.number="loadFlowForm.dcPowerFactor" type="number" min="0" max="1" step="0.01" />
        </label>
      </div>
      <div class="check-grid">
        <label><input v-model="loadFlowForm.distributedSlack" type="checkbox" /> Distributed slack</label>
        <label><input v-model="loadFlowForm.useReactiveLimits" type="checkbox" /> Use reactive limits</label>
        <label><input v-model="loadFlowForm.transformerVoltageControlOn" type="checkbox" /> Transformer voltage control</label>
        <label><input v-model="loadFlowForm.phaseShifterRegulationOn" type="checkbox" /> Phase shifter regulation</label>
        <label><input v-model="loadFlowForm.shuntCompensatorVoltageControlOn" type="checkbox" /> Shunt voltage control</label>
        <label><input v-model="loadFlowForm.readSlackBus" type="checkbox" /> Read slack bus</label>
        <label><input v-model="loadFlowForm.writeSlackBus" type="checkbox" /> Write slack bus</label>
        <label><input v-model="loadFlowForm.hvdcAcEmulation" type="checkbox" /> HVDC AC emulation</label>
      </div>
      </div>

      <div class="parameter-section">
        <p class="eyebrow">Security Analysis</p>
        <div class="form-grid parameter-form">
        <label>
          Contingency element
          <select v-model="securityAnalysisForm.contingencyElementType">
            <option value="LINE">Line</option>
            <option value="BRANCH">Branch</option>
          </select>
        </label>
        <label>
          Max contingencies
          <input v-model.number="securityAnalysisForm.maxGeneratedContingencies" type="number" min="1" max="500" />
        </label>
        <label>
          Debug dir
          <input v-model="securityAnalysisForm.debugDir" type="text" placeholder="Optional" />
        </label>
      </div>
      <div class="check-grid">
        <label><input v-model="securityAnalysisForm.voltageLimitsChecked" type="checkbox" /> Voltage limits</label>
        <label><input v-model="securityAnalysisForm.currentLimitsChecked" type="checkbox" /> Current limits</label>
        <label><input v-model="securityAnalysisForm.activePowerLimitsChecked" type="checkbox" /> Active power limits</label>
        <label><input v-model="securityAnalysisForm.intermediateResultsInOperatorStrategy" type="checkbox" /> Operator-strategy intermediate results</label>
      </div>
      </div>
      <div class="form-actions">
        <button type="button" @click="saveParameters">Save configuration</button>
      </div>
      <p v-if="message" class="message">{{ message }}</p>
      <DataTable :columns="parameterColumns" :rows="parameterRows" id-key="id" :page-size="10">
        <template #cell-name="{ row }">
          <button type="button" class="link-button" @click="useParameterRow(String(row.id))">{{ row.name }}</button>
        </template>
      </DataTable>
    </section>

    <section v-else class="panel">
      <div class="form-grid">
        <label>
          Run ID
          <input v-model="runSearch.runId" type="search" placeholder="Run ID" />
        </label>
        <label>
          Run date
          <input v-model="runSearch.runDate" type="date" />
        </label>
        <label>
          Run time
          <input v-model="runSearch.runTime" type="time" />
        </label>
        <button type="button" @click="loadRuns">Search</button>
      </div>
      <DataTable :columns="runColumns" :rows="runRows" id-key="runId" :page-size="10">
        <template #cell-runId="{ row }">
          <a href="#" @click.prevent="openRun(String(row.runId))">{{ row.runId }}</a>
        </template>
      </DataTable>

      <DynamicTable
        v-if="detailTables.length > 0"
        class="detail-tables"
        :tables="detailTables"
        :page-size="25"
      />
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { DataTable, DynamicTable, logClientError } from '@egm/gui.common/src';
import {
  getDefaultSecurityAnalysisParameters,
  getRunDetail,
  listSecurityAnalysisParameters,
  searchImports,
  searchRuns,
  saveSecurityAnalysisParameters,
  startSecurityAnalysis,
  type LfSaParameterConfiguration,
  type LoadFlowParametersDto,
  type LoadFlowStrategy,
  type SecurityAnalysisParametersDto,
  type SecurityAnalysisRunDetail,
  type SecurityAnalysisRunSummary
} from '../services/lfsaApi';

type Tab = 'search' | 'parameters' | 'results';
withDefaults(defineProps<{
  embedded?: boolean;
}>(), {
  embedded: false
});

interface DynamicTableDefinition {
  tableId: string;
  label: string;
  columns: Array<{ key: string; label: string; type: string; sortable: boolean; searchable: boolean; unit: string }>;
  rows: Array<{ rowId: string; values: Record<string, unknown> }>;
  totalRows: number;
  defaultSort: string;
}

const activeTab = ref<Tab>('search');
const message = ref('');
const search = ref({ service: 'CGM', timeFrame: '', date: '' });
const runSearch = ref({ runId: '', runDate: '', runTime: '' });
const importRows = ref<Record<string, unknown>[]>([]);
const runRows = ref<Record<string, unknown>[]>([]);
const parameterName = ref('');
const loadFlowStrategy = ref<LoadFlowStrategy>('DC_ONLY');
const loadFlowForm = ref<LoadFlowParametersDto>(emptyLoadFlowParameters());
const securityAnalysisForm = ref<SecurityAnalysisParametersDto>(emptySecurityAnalysisParameters());
const defaultParameterConfiguration = ref<LfSaParameterConfiguration | null>(null);
const savedParameterConfigurations = ref<LfSaParameterConfiguration[]>([]);
const selectedParameterByImport = ref<Record<string, string>>({});
const selectedDetail = ref<SecurityAnalysisRunDetail | null>(null);
let runPollToken = 0;

const importColumns = [
  { key: 'importId', label: 'Import ID' },
  { key: 'service', label: 'Service' },
  { key: 'timeFrame', label: 'Timeframe' },
  { key: 'businessDay', label: 'Business day' },
  { key: 'createdAt', label: 'Created' },
  { key: 'message', label: 'Message' },
  { key: 'action', label: 'Action' }
];

const runColumns = [
  { key: 'runId', label: 'Run ID' },
  { key: 'fileImportId', label: 'Import ID' },
  { key: 'state', label: 'State' },
  { key: 'loadFlowState', label: 'Load flow' },
  { key: 'securityAnalysisState', label: 'Security analysis' },
  { key: 'runDate', label: 'Run date' },
  { key: 'runTime', label: 'Run time' },
  { key: 'networkCount', label: 'Networks' },
  { key: 'lineFlowCount', label: 'Line flows' },
  { key: 'violationCount', label: 'Violations' },
  { key: 'diagnosticCount', label: 'Diagnostics' },
  { key: 'message', label: 'Message' }
];

const parameterColumns = [
  { key: 'name', label: 'Name' },
  { key: 'source', label: 'Source' },
  { key: 'createdAt', label: 'Created' },
  { key: 'updatedAt', label: 'Updated' },
  { key: 'loadFlowStrategy', label: 'LF strategy' },
  { key: 'maxGeneratedContingencies', label: 'Max contingencies' },
  { key: 'contingencyElementType', label: 'Contingency element' },
  { key: 'distributedSlack', label: 'Distributed slack' },
  { key: 'voltageLimitsChecked', label: 'Voltage limits' }
];

const parameterRows = computed(() => savedParameterConfigurations.value.map((config) => formatRow({
  id: config.id,
  name: config.name,
  source: config.source,
  createdAt: config.createdAt,
  updatedAt: config.updatedAt,
  loadFlowStrategy: config.loadFlowStrategy,
  maxGeneratedContingencies: config.securityAnalysisParameters.maxGeneratedContingencies,
  contingencyElementType: config.securityAnalysisParameters.contingencyElementType,
  distributedSlack: config.loadFlowParameters.distributedSlack,
  voltageLimitsChecked: config.securityAnalysisParameters.voltageLimitsChecked
})));

const detailTables = computed<DynamicTableDefinition[]>(() => {
  if (!selectedDetail.value) {
    return [];
  }
  return [
    table('summary', 'Run summary', summaryRows(selectedDetail.value)),
    table('parameters', 'Parameters', selectedDetail.value.parameterConfiguration ? parameterDetailRows(selectedDetail.value.parameterConfiguration) : []),
    table('load-flow-result', 'Load flow result', loadFlowRows(selectedDetail.value.loadFlowResult)),
    table('pow-sybl-result', 'PowSyBl result', computationRows(selectedDetail.value.computationResult)),
    table('line-flows', 'Line flows', selectedDetail.value.lineFlows),
    table('violations', 'Violations', selectedDetail.value.violations),
    table('element-counts', 'Network element counts', Object.entries(selectedDetail.value.networkElementCounts).map(([elementType, count]) => ({ elementType, count }))),
    table('diagnostics', 'Diagnostics', selectedDetail.value.diagnostics.map((diagnostic, index) => ({ index: index + 1, diagnostic })))
  ];
});

onMounted(() => {
  void loadImports();
  void loadRuns();
  void loadParameters();
});
onUnmounted(() => {
  runPollToken++;
});

function switchTab(tab: Tab) {
  activeTab.value = tab;
  message.value = '';
}

async function loadImports() {
  try {
    const page = await searchImports({ ...search.value, page: 0, size: 100 });
    importRows.value = page.items.map(formatRow);
  } catch (error) {
    message.value = 'Unable to load successful imports';
    logClientError('Unable to load LFSA import candidates', error);
  }
}

async function startRun(importId: string) {
  try {
    const run = await startSecurityAnalysis(importId, selectedParameterByImport.value[importId] ?? '');
    message.value = `Security analysis run ${run.runId} started`;
    activeTab.value = 'results';
    runSearch.value.runId = run.runId;
    await loadRuns();
    void pollRunUntilTerminal(run.runId);
  } catch (error) {
    message.value = 'Unable to start security analysis';
    logClientError('Unable to start LFSA security analysis', error);
  }
}

async function loadParameters() {
  try {
    defaultParameterConfiguration.value = await getDefaultSecurityAnalysisParameters();
    applyConfiguration(defaultParameterConfiguration.value);
    const page = await listSecurityAnalysisParameters({ page: 0, size: 100 });
    savedParameterConfigurations.value = page.items;
  } catch (error) {
    message.value = 'Unable to load security-analysis parameter configurations';
    logClientError('Unable to load LFSA parameter configurations', error);
  }
}

async function saveParameters() {
  try {
    const saved = await saveSecurityAnalysisParameters(
      parameterName.value,
      loadFlowStrategy.value,
      loadFlowForm.value,
      securityAnalysisForm.value
    );
    savedParameterConfigurations.value = [saved, ...savedParameterConfigurations.value.filter((config) => config.id !== saved.id)];
    parameterName.value = '';
    message.value = `LFnSA configuration ${saved.name} saved`;
  } catch (error) {
    message.value = 'Unable to save LFnSA configuration';
    logClientError('Unable to save LFSA parameter configuration', error);
  }
}

function resetParameterForm() {
  applyConfiguration(defaultParameterConfiguration.value ?? defaultLfSaConfiguration());
  parameterName.value = '';
}

function useParameterRow(id: string) {
  const config = savedParameterConfigurations.value.find((item) => item.id === id);
  if (!config) {
    return;
  }
  applyConfiguration(config);
  parameterName.value = config.name;
}

async function loadRuns() {
  try {
    const page = await searchRuns({ ...runSearch.value, page: 0, size: 100 });
    runRows.value = page.items.map(formatRow);
    await refreshSelectedRunDetail(page.items);
  } catch (error) {
    message.value = 'Unable to load security-analysis runs';
    logClientError('Unable to load LFSA runs', error);
  }
}

async function refreshSelectedRunDetail(runs: SecurityAnalysisRunSummary[]) {
  const selectedRunId = selectedDetail.value?.summary.runId;
  const searchRunId = runSearch.value.runId.trim();
  const runToOpen = runs.find((run) => selectedRunId && run.runId === selectedRunId)
    ?? runs.find((run) => searchRunId && run.runId === searchRunId)
    ?? (runs.length === 1 ? runs[0] : undefined);
  if (!runToOpen) {
    if (selectedRunId && !runs.some((run) => run.runId === selectedRunId)) {
      selectedDetail.value = null;
    }
    return;
  }
  await openRun(runToOpen.runId);
}

async function openRun(runId: string) {
  try {
    const detail = await getRunDetail(runId);
    selectedDetail.value = detail;
    updateRunRow(detail.summary);
  } catch (error) {
    message.value = 'Unable to load run detail';
    logClientError('Unable to load LFSA run detail', error);
  }
}

async function pollRunUntilTerminal(runId: string) {
  const token = ++runPollToken;
  for (let attempt = 0; attempt < 20 && token === runPollToken; attempt++) {
    await delay(attempt === 0 ? 600 : 1500);
    try {
      const detail = await getRunDetail(runId);
      updateRunRow(detail.summary);
      if (selectedDetail.value?.summary.runId === runId) {
        selectedDetail.value = detail;
      }
      if (detail.summary.state !== 'STARTED') {
        message.value = detail.summary.message;
        return;
      }
    } catch (error) {
      logClientError('Unable to poll LFSA run state', error, { runId });
      return;
    }
  }
}

function updateRunRow(summary: SecurityAnalysisRunSummary) {
  const nextRow = formatRow(summary as unknown as Record<string, unknown>);
  const index = runRows.value.findIndex((row) => String(row.runId) === summary.runId);
  if (index >= 0) {
    runRows.value = runRows.value.map((row, rowIndex) => (rowIndex === index ? nextRow : row));
  } else {
    runRows.value = [nextRow, ...runRows.value];
  }
}

function delay(milliseconds: number) {
  return new Promise((resolve) => window.setTimeout(resolve, milliseconds));
}

function formatRow(row: Record<string, unknown>) {
  return Object.fromEntries(Object.entries(row).map(([key, value]) => [key, formatValue(value)]));
}

function formatValue(value: unknown) {
  if (typeof value !== 'string') {
    return value ?? '';
  }
  if (/^\d{4}-\d{2}-\d{2}T/.test(value)) {
    return new Date(value).toLocaleString();
  }
  return value;
}

function summaryRows(detail: SecurityAnalysisRunDetail) {
  return Object.entries(detail.summary).map(([field, value]) => ({ field, value }));
}

function parameterDetailRows(config: LfSaParameterConfiguration) {
  return [
    { field: 'configurationId', value: config.id || 'default' },
    { field: 'configurationName', value: config.name },
    { field: 'source', value: config.source },
    { field: 'loadFlowStrategy', value: config.loadFlowStrategy },
    ...Object.entries(config.loadFlowParameters ?? {}).map(([field, value]) => ({ field: `loadFlow.${field}`, value })),
    ...Object.entries(config.securityAnalysisParameters ?? {}).map(([field, value]) => ({ field: `securityAnalysis.${field}`, value }))
  ];
}

function computationRows(result: SecurityAnalysisRunDetail['computationResult']) {
  if (!result) {
    return [];
  }
  return [
    { field: 'succeeded', value: result.succeeded },
    { field: 'preContingencyStatus', value: result.preContingencyStatus },
    { field: 'contingencyCount', value: result.contingencyCount },
    { field: 'postStatusCount', value: result.postContingencyStatuses.length },
    { field: 'preViolationCount', value: result.preContingencyViolations.length },
    { field: 'postViolationCount', value: result.postContingencyViolations.length }
  ];
}

function loadFlowRows(result: SecurityAnalysisRunDetail['loadFlowResult']) {
  if (!result) {
    return [];
  }
  return [
    { field: 'succeeded', value: result.succeeded },
    { field: 'status', value: result.status },
    { field: 'componentCount', value: result.componentCount },
    { field: 'componentStatuses', value: result.componentStatuses.join('; ') },
    { field: 'metrics', value: Object.entries(result.metrics).map(([key, value]) => `${key}=${value}`).join(', ') },
    { field: 'logs', value: result.logs }
  ];
}

function table(tableId: string, label: string, rows: Record<string, unknown>[]): DynamicTableDefinition {
  const keys = [...new Set(rows.flatMap((row) => Object.keys(row)))];
  return {
    tableId,
    label,
    columns: keys.map((key) => ({ key, label: title(key), type: 'text', sortable: true, searchable: true, unit: '' })),
    rows: rows.map((row, index) => ({ rowId: `${tableId}-${index}`, values: formatRow(row) })),
    totalRows: rows.length,
    defaultSort: keys[0] ?? ''
  };
}

function title(value: string) {
  return value.replace(/([A-Z])/g, ' $1').replace(/^./, (letter) => letter.toUpperCase());
}

function applyConfiguration(config: LfSaParameterConfiguration) {
  loadFlowStrategy.value = config.loadFlowStrategy ?? 'DC_ONLY';
  loadFlowForm.value = cloneLoadFlowParameters(config.loadFlowParameters ?? emptyLoadFlowParameters());
  securityAnalysisForm.value = cloneSecurityAnalysisParameters(config.securityAnalysisParameters ?? emptySecurityAnalysisParameters());
}

function defaultLfSaConfiguration(): LfSaParameterConfiguration {
  return {
    id: '',
    name: 'Default LFnSA',
    source: 'DEFAULT',
    createdAt: '',
    updatedAt: '',
    loadFlowStrategy: 'DC_ONLY',
    loadFlowParameters: emptyLoadFlowParameters(),
    securityAnalysisParameters: emptySecurityAnalysisParameters()
  };
}

function cloneLoadFlowParameters(parameters: LoadFlowParametersDto): LoadFlowParametersDto {
  return { ...parameters };
}

function cloneSecurityAnalysisParameters(parameters: SecurityAnalysisParametersDto): SecurityAnalysisParametersDto {
  return { ...parameters };
}

function emptyLoadFlowParameters(): LoadFlowParametersDto {
  return {
    distributedSlack: true,
    useReactiveLimits: true,
    transformerVoltageControlOn: true,
    phaseShifterRegulationOn: true,
    shuntCompensatorVoltageControlOn: true,
    readSlackBus: false,
    writeSlackBus: false,
    voltageInitMode: 'PREVIOUS_VALUES',
    balanceType: 'PROPORTIONAL_TO_GENERATION_P',
    componentMode: 'MAIN_CONNECTED',
    hvdcAcEmulation: true,
    dcPowerFactor: 1.0
  };
}

function emptySecurityAnalysisParameters(): SecurityAnalysisParametersDto {
  return {
    voltageLimitsChecked: true,
    currentLimitsChecked: true,
    activePowerLimitsChecked: true,
    intermediateResultsInOperatorStrategy: false,
    debugDir: '',
    contingencyElementType: 'LINE',
    maxGeneratedContingencies: 25
  };
}
</script>
