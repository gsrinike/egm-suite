<template>
  <section class="lfsa-manager">
    <header v-if="!embedded" class="lfsa-header">
      <div>
        <p class="eyebrow">CGM</p>
        <h2>Sensitivity Analysis</h2>
      </div>
    </header>

    <div class="tab-row">
      <button type="button" :class="{ active: activeTab === 'search' }" @click="switchTab('search')">Search</button>
      <button type="button" :class="{ active: activeTab === 'configuration' }" @click="switchTab('configuration')">Sensitivity Configuration</button>
      <button type="button" :class="{ active: activeTab === 'results' }" @click="switchTab('results')">Sensitivity Results</button>
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
            <option value="DAY_AHEAD">Day Ahead</option>
            <option value="ID">Intra-Day</option>
            <option value="TWO_DAYS_AHEAD">Day-2</option>
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
          <button type="button" @click="selectImport(String(row.importId))">Select</button>
        </template>
      </DataTable>

      <section v-if="selectedImportId" class="parameter-section">
        <div class="parameter-header">
          <div>
            <p class="eyebrow">IIDM Selection</p>
            <h3>{{ selectedImportId }}</h3>
          </div>
          <button type="button" @click="loadIidmNetworks">Refresh IIDM</button>
        </div>
        <div class="form-grid">
          <label>
            Configuration
            <select v-model="selectedConfigurationId">
              <option value="">Default sensitivity configuration</option>
              <option v-for="config in savedConfigurations" :key="config.id" :value="config.id">{{ config.name }}</option>
            </select>
          </label>
          <label>
            PTDF
            <input type="file" @change="uploadInputFile('PTDF', $event)" />
            <span class="hint">{{ ptdfObjectId || 'Default when blank' }}</span>
          </label>
          <label>
            LODF
            <input type="file" @change="uploadInputFile('LODF', $event)" />
            <span class="hint">{{ lodfObjectId || 'Default when blank' }}</span>
          </label>
          <label>
            GLSK
            <input type="file" @change="uploadInputFile('GLSK', $event)" />
            <span class="hint">{{ glskObjectId || 'Default when blank' }}</span>
          </label>
        </div>
        <DataTable :columns="iidmColumns" :rows="iidmRows" id-key="id" :page-size="10">
          <template #cell-selected="{ row }">
            <input
              :checked="selectedIidmNetworkIds.includes(String(row.id))"
              type="checkbox"
              @change="toggleIidmNetwork(String(row.id))"
            />
          </template>
        </DataTable>
        <div class="form-actions">
          <button type="button" :disabled="selectedIidmNetworkIds.length === 0" @click="startRun">
            Run Sensitivity Analysis
          </button>
        </div>
      </section>
    </section>

    <section v-else-if="activeTab === 'configuration'" class="panel">
      <div class="parameter-header">
        <div>
          <p class="eyebrow">Configuration</p>
          <h3>Sensitivity Configuration</h3>
        </div>
        <button type="button" @click="resetConfigurationForm">Reset to default</button>
      </div>
      <div class="form-grid parameter-form">
        <label>
          Configuration name
          <input v-model="configurationName" placeholder="DateTimeStamp_SENS_Conf" />
        </label>
        <label>
          Function type
          <select v-model="configurationForm.functionType">
            <option value="BRANCH_ACTIVE_POWER_1">Branch active power side 1</option>
            <option value="BRANCH_ACTIVE_POWER_2">Branch active power side 2</option>
            <option value="BRANCH_CURRENT_1">Branch current side 1</option>
            <option value="BUS_VOLTAGE">Bus voltage</option>
          </select>
        </label>
        <label>
          Variable type
          <select v-model="configurationForm.variableType">
            <option value="INJECTION_ACTIVE_POWER">Injection active power</option>
            <option value="INJECTION_REACTIVE_POWER">Injection reactive power</option>
            <option value="TRANSFORMER_PHASE">Transformer phase</option>
            <option value="BUS_TARGET_VOLTAGE">Bus target voltage</option>
          </select>
        </label>
        <label>
          Contingency context
          <select v-model="configurationForm.contingencyContext">
            <option value="ALL">Base and contingencies</option>
            <option value="ONLY_CONTINGENCIES">Only contingencies</option>
            <option value="NONE">Base only</option>
          </select>
        </label>
        <label>
          Max branches
          <input v-model.number="configurationForm.maxMonitoredBranches" min="1" max="500" type="number" />
        </label>
        <label>
          Max variables
          <input v-model.number="configurationForm.maxVariables" min="1" max="500" type="number" />
        </label>
        <label>
          Max contingencies
          <input v-model.number="configurationForm.maxGeneratedContingencies" min="1" max="500" type="number" />
        </label>
        <label>
          Debug dir
          <input v-model="configurationForm.debugDir" placeholder="Optional" />
        </label>
      </div>
      <div class="check-grid">
        <label><input v-model="configurationForm.dc" type="checkbox" /> DC sensitivity</label>
      </div>
      <div class="form-actions">
        <button type="button" @click="saveConfiguration">Save configuration</button>
      </div>
      <DataTable :columns="configurationColumns" :rows="configurationRows" id-key="id" :page-size="10">
        <template #cell-name="{ row }">
          <button type="button" class="link-button" @click="useConfiguration(String(row.id))">{{ row.name }}</button>
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
        <template #cell-ptdfObjectId="{ row }">
          <a v-if="row.ptdfObjectId" href="#" @click.prevent="openSensitivityInput(String(row.runId), 'PTDF')">View</a>
        </template>
        <template #cell-lodfObjectId="{ row }">
          <a v-if="row.lodfObjectId" href="#" @click.prevent="openSensitivityInput(String(row.runId), 'LODF')">View</a>
        </template>
        <template #cell-glskObjectId="{ row }">
          <a v-if="row.glskObjectId" href="#" @click.prevent="openSensitivityInput(String(row.runId), 'GLSK')">View</a>
        </template>
      </DataTable>
      <DynamicTable v-if="detailTables.length > 0" class="detail-tables" :tables="detailTables" :page-size="25" />
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { DataTable, DynamicTable, logClientError } from '@egm/gui.common/src';
import {
  getDefaultSensitivityConfiguration,
  getSensitivityInputTable,
  getSensitivityRunDetail,
  listSensitivityConfigurations,
  listSensitivityIidmNetworks,
  saveSensitivityConfiguration,
  searchImports,
  searchSensitivityRuns,
  startSensitivityAnalysis,
  uploadSensitivityInput,
  type IidmNetworkOption,
  type SensitivityAnalysisConfiguration,
  type SensitivityAnalysisParametersDto,
  type SensitivityAnalysisRunDetail,
  type SensitivityAnalysisRunSummary,
  type SensitivityInputTable
} from '../services/lfsaApi';

type Tab = 'search' | 'configuration' | 'results';
type SensitivityInputKind = 'PTDF' | 'LODF' | 'GLSK';

withDefaults(defineProps<{ embedded?: boolean }>(), {
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
const selectedImportId = ref('');
const selectedIidmNetworkIds = ref<string[]>([]);
const selectedConfigurationId = ref('');
const ptdfObjectId = ref('');
const lodfObjectId = ref('');
const glskObjectId = ref('');
const importRows = ref<Record<string, unknown>[]>([]);
const iidmNetworks = ref<IidmNetworkOption[]>([]);
const runs = ref<SensitivityAnalysisRunSummary[]>([]);
const selectedDetail = ref<SensitivityAnalysisRunDetail | null>(null);
const selectedInputTable = ref<SensitivityInputTable | null>(null);
const configurationName = ref('');
const configurationForm = ref<SensitivityAnalysisParametersDto>(emptyConfiguration());
const defaultConfiguration = ref<SensitivityAnalysisConfiguration | null>(null);
const savedConfigurations = ref<SensitivityAnalysisConfiguration[]>([]);

const importColumns = [
  { key: 'importId', label: 'Import ID' },
  { key: 'service', label: 'Service' },
  { key: 'timeFrame', label: 'Timeframe' },
  { key: 'businessDay', label: 'Business day' },
  { key: 'createdAt', label: 'Created' },
  { key: 'message', label: 'Message' },
  { key: 'action', label: 'Action' }
];
const iidmColumns = [
  { key: 'selected', label: 'Use' },
  { key: 'id', label: 'Network ID' },
  { key: 'sourceFileNames', label: 'Source files' },
  { key: 'businessDay', label: 'Business day' },
  { key: 'businessTime', label: 'Business time' },
  { key: 'tsoName', label: 'TSO' },
  { key: 'networkFormat', label: 'Format' }
];
const configurationColumns = [
  { key: 'name', label: 'Name' },
  { key: 'source', label: 'Source' },
  { key: 'dc', label: 'DC' },
  { key: 'functionType', label: 'Function' },
  { key: 'variableType', label: 'Variable' },
  { key: 'contingencyContext', label: 'Context' },
  { key: 'maxMonitoredBranches', label: 'Branches' },
  { key: 'maxVariables', label: 'Variables' }
];
const runColumns = [
  { key: 'runId', label: 'Run ID' },
  { key: 'fileImportId', label: 'Import ID' },
  { key: 'state', label: 'State' },
  { key: 'runDate', label: 'Run date' },
  { key: 'runTime', label: 'Run time' },
  { key: 'networkCount', label: 'Networks' },
  { key: 'factorCount', label: 'Factors' },
  { key: 'resultCount', label: 'Results' },
  { key: 'diagnosticCount', label: 'Diagnostics' },
  { key: 'ptdfObjectId', label: 'PTDF' },
  { key: 'lodfObjectId', label: 'LODF' },
  { key: 'glskObjectId', label: 'GLSK' },
  { key: 'message', label: 'Message' }
];

const iidmRows = computed(() => iidmNetworks.value.map((row) => formatRow({
  ...row,
  sourceFileNames: row.sourceFileNames?.join(', ') ?? ''
})));
const configurationRows = computed(() => savedConfigurations.value.map((config) => formatRow({
  id: config.id,
  name: config.name,
  source: config.source,
  dc: config.parameters.dc,
  functionType: config.parameters.functionType,
  variableType: config.parameters.variableType,
  contingencyContext: config.parameters.contingencyContext,
  maxMonitoredBranches: config.parameters.maxMonitoredBranches,
  maxVariables: config.parameters.maxVariables
})));
const runRows = computed(() => runs.value.map((row) => formatRow(row as unknown as Record<string, unknown>)));
const detailTables = computed<DynamicTableDefinition[]>(() => {
  const tables: DynamicTableDefinition[] = [];
  if (selectedInputTable.value) {
    tables.push(table(
      `input-${selectedInputTable.value.kind.toLowerCase()}`,
      `${selectedInputTable.value.kind} input`,
      selectedInputTable.value.rows
    ));
  }
  if (!selectedDetail.value) {
    return tables;
  }
  tables.push(
    table('summary', 'Run summary', Object.entries(selectedDetail.value.summary).map(([field, value]) => ({ field, value }))),
    table('configuration', 'Configuration', configurationDetailRows(selectedDetail.value.configuration)),
    table('inputs', 'Input references', Object.entries(selectedDetail.value.inputReferences).map(([field, value]) => ({ field, value }))),
    table('factors', 'Factors', selectedDetail.value.factors),
    table('matrix', 'Sensitivity matrix', selectedDetail.value.matrixRows),
    table('element-counts', 'Network element counts', Object.entries(selectedDetail.value.networkElementCounts).map(([elementType, count]) => ({ elementType, count }))),
    table('diagnostics', 'Diagnostics', selectedDetail.value.diagnostics.map((diagnostic, index) => ({ index: index + 1, diagnostic })))
  );
  return tables;
});

onMounted(() => {
  void loadImports();
  void loadConfigurations();
  void loadRuns();
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
    logClientError('Unable to load sensitivity import candidates', error);
  }
}

async function selectImport(importId: string) {
  selectedImportId.value = importId;
  selectedIidmNetworkIds.value = [];
  await loadIidmNetworks();
}

async function loadIidmNetworks() {
  if (!selectedImportId.value) {
    return;
  }
  try {
    const page = await listSensitivityIidmNetworks({ importId: selectedImportId.value, page: 0, size: 100 });
    iidmNetworks.value = page.items;
    selectedIidmNetworkIds.value = page.items.map((item) => item.id);
  } catch (error) {
    message.value = 'Unable to load IIDM networks';
    logClientError('Unable to load sensitivity IIDM networks', error, { importId: selectedImportId.value });
  }
}

function toggleIidmNetwork(id: string) {
  selectedIidmNetworkIds.value = selectedIidmNetworkIds.value.includes(id)
    ? selectedIidmNetworkIds.value.filter((value) => value !== id)
    : [...selectedIidmNetworkIds.value, id];
}

async function loadConfigurations() {
  try {
    defaultConfiguration.value = await getDefaultSensitivityConfiguration();
    configurationForm.value = { ...defaultConfiguration.value.parameters };
    const page = await listSensitivityConfigurations({ page: 0, size: 100 });
    savedConfigurations.value = page.items;
  } catch (error) {
    message.value = 'Unable to load sensitivity configurations';
    logClientError('Unable to load sensitivity configurations', error);
  }
}

async function saveConfiguration() {
  try {
    const saved = await saveSensitivityConfiguration(configurationName.value, configurationForm.value);
    savedConfigurations.value = [saved, ...savedConfigurations.value.filter((item) => item.id !== saved.id)];
    configurationName.value = '';
    message.value = `Sensitivity configuration ${saved.name} saved`;
  } catch (error) {
    message.value = 'Unable to save sensitivity configuration';
    logClientError('Unable to save sensitivity configuration', error);
  }
}

function resetConfigurationForm() {
  configurationForm.value = { ...(defaultConfiguration.value?.parameters ?? emptyConfiguration()) };
  configurationName.value = '';
}

function useConfiguration(id: string) {
  const configuration = savedConfigurations.value.find((item) => item.id === id);
  if (!configuration) {
    return;
  }
  configurationForm.value = { ...configuration.parameters };
  configurationName.value = configuration.name;
}

async function uploadInputFile(kind: SensitivityInputKind, event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  try {
    const uploaded = await uploadSensitivityInput(kind, file);
    if (kind === 'PTDF') {
      ptdfObjectId.value = uploaded.objectId;
    } else if (kind === 'LODF') {
      lodfObjectId.value = uploaded.objectId;
    } else {
      glskObjectId.value = uploaded.objectId;
    }
    message.value = `${kind} input ${uploaded.fileName} uploaded`;
  } catch (error) {
    message.value = `Unable to upload ${kind} input`;
    logClientError(`Unable to upload ${kind} input`, error);
  } finally {
    input.value = '';
  }
}

async function startRun() {
  try {
    const run = await startSensitivityAnalysis(
      selectedImportId.value,
      selectedIidmNetworkIds.value,
      selectedConfigurationId.value,
      ptdfObjectId.value,
      lodfObjectId.value,
      glskObjectId.value
    );
    message.value = `Sensitivity run ${run.runId} started`;
    activeTab.value = 'results';
    runSearch.value.runId = run.runId;
    await loadRuns();
  } catch (error) {
    message.value = 'Unable to start sensitivity analysis';
    logClientError('Unable to start sensitivity analysis', error);
  }
}

async function loadRuns() {
  try {
    const page = await searchSensitivityRuns({ ...runSearch.value, page: 0, size: 100 });
    runs.value = page.items;
    selectedInputTable.value = null;
    await refreshSelectedRunDetail(page.items);
  } catch (error) {
    message.value = 'Unable to load sensitivity runs';
    logClientError('Unable to load sensitivity runs', error);
  }
}

async function refreshSelectedRunDetail(runSummaries: SensitivityAnalysisRunSummary[]) {
  const selectedRunId = selectedDetail.value?.summary.runId;
  const searchRunId = runSearch.value.runId.trim();
  const runToOpen = runSummaries.find((run) => selectedRunId && run.runId === selectedRunId)
    ?? runSummaries.find((run) => searchRunId && run.runId === searchRunId)
    ?? (runSummaries.length === 1 ? runSummaries[0] : undefined);
  if (!runToOpen) {
    if (selectedRunId && !runSummaries.some((run) => run.runId === selectedRunId)) {
      selectedDetail.value = null;
    }
    return;
  }
  await openRun(runToOpen.runId);
}

async function openRun(runId: string) {
  try {
    selectedDetail.value = await getSensitivityRunDetail(runId);
    selectedInputTable.value = null;
  } catch (error) {
    message.value = 'Unable to load sensitivity run detail';
    logClientError('Unable to load sensitivity run detail', error, { runId });
  }
}

async function openSensitivityInput(runId: string, kind: SensitivityInputKind) {
  try {
    selectedInputTable.value = await getSensitivityInputTable(runId, kind);
    selectedDetail.value = null;
    message.value = `${kind} input loaded`;
  } catch (error) {
    message.value = `Unable to load ${kind} input`;
    logClientError(`Unable to load ${kind} sensitivity input`, error, { runId });
  }
}

function configurationDetailRows(config: SensitivityAnalysisConfiguration) {
  return [
    { field: 'configurationId', value: config.id || 'default' },
    { field: 'configurationName', value: config.name },
    { field: 'source', value: config.source },
    ...Object.entries(config.parameters ?? {}).map(([field, value]) => ({ field, value }))
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

function formatRow(row: Record<string, unknown>) {
  return Object.fromEntries(Object.entries(row).map(([key, value]) => [key, formatValue(value)]));
}

function formatValue(value: unknown) {
  if (Array.isArray(value)) {
    return value.join(', ');
  }
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)) {
    return new Date(value).toLocaleString();
  }
  return value ?? '';
}

function emptyConfiguration(): SensitivityAnalysisParametersDto {
  return {
    dc: true,
    functionType: 'BRANCH_ACTIVE_POWER_1',
    variableType: 'INJECTION_ACTIVE_POWER',
    contingencyContext: 'ALL',
    maxMonitoredBranches: 25,
    maxVariables: 25,
    maxGeneratedContingencies: 25,
    flowFlowSensitivityValueThreshold: 0,
    voltageVoltageSensitivityValueThreshold: 0,
    flowVoltageSensitivityValueThreshold: 0,
    angleFlowSensitivityValueThreshold: 0,
    operatorStrategiesCalculationMode: 'NONE',
    debugDir: ''
  };
}
</script>

<style scoped>
.hint {
  color: var(--text-muted);
  display: block;
  font-size: 0.78rem;
  line-height: 1.3;
  margin-top: 0.35rem;
  overflow-wrap: anywhere;
}
</style>
