<template>
  <section class="lfsa-manager">
    <header v-if="!embedded" class="lfsa-header">
      <div>
        <p class="eyebrow">CGM</p>
        <h2>Security Analysis</h2>
      </div>
    </header>

    <div class="tab-row">
      <button type="button" :class="{ active: activeTab === 'search' }" @click="switchTab('search')">Search</button>
      <button type="button" :class="{ active: activeTab === 'results' }" @click="switchTab('results')">Security Analysis Results</button>
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
          <button type="button" @click="startRun(String(row.importId))">Run analysis</button>
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
  getRunDetail,
  searchImports,
  searchRuns,
  startSecurityAnalysis,
  type SecurityAnalysisRunDetail,
  type SecurityAnalysisRunSummary
} from '../services/lfsaApi';

type Tab = 'search' | 'results';
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
  { key: 'runDate', label: 'Run date' },
  { key: 'runTime', label: 'Run time' },
  { key: 'networkCount', label: 'Networks' },
  { key: 'lineFlowCount', label: 'Line flows' },
  { key: 'violationCount', label: 'Violations' },
  { key: 'diagnosticCount', label: 'Diagnostics' },
  { key: 'message', label: 'Message' }
];

const detailTables = computed<DynamicTableDefinition[]>(() => {
  if (!selectedDetail.value) {
    return [];
  }
  return [
    table('summary', 'Run summary', summaryRows(selectedDetail.value)),
    table('line-flows', 'Line flows', selectedDetail.value.lineFlows),
    table('violations', 'Violations', selectedDetail.value.violations),
    table('element-counts', 'Network element counts', Object.entries(selectedDetail.value.networkElementCounts).map(([elementType, count]) => ({ elementType, count }))),
    table('diagnostics', 'Diagnostics', selectedDetail.value.diagnostics.map((diagnostic, index) => ({ index: index + 1, diagnostic })))
  ];
});

onMounted(() => {
  void loadImports();
  void loadRuns();
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
    const run = await startSecurityAnalysis(importId);
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

async function loadRuns() {
  try {
    const page = await searchRuns({ ...runSearch.value, page: 0, size: 100 });
    runRows.value = page.items.map(formatRow);
  } catch (error) {
    message.value = 'Unable to load security-analysis runs';
    logClientError('Unable to load LFSA runs', error);
  }
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
</script>
