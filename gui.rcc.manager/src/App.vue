<template>
  <main class="rcc-shell">
    <aside class="sidebar">
      <div class="brand">
        <span>RCC Manager</span>
        <strong>Operational Services</strong>
      </div>
      <button
        v-for="item in navigation"
        :key="item.id"
        class="nav-button"
        :class="{ active: activeView === item.id }"
        type="button"
        :disabled="item.disabled"
        @click="activeView = item.id"
      >
        {{ item.label }}
      </button>
    </aside>

    <section class="workspace">
      <header class="hero">
        <div>
          <p>Coordinated Security Analysis</p>
          <h1>{{ activeTitle }}</h1>
        </div>
        <Button :disabled="busy" @click="refresh">Refresh</Button>
      </header>

      <section v-if="activeView === 'csa'" class="panel">
        <h2>CSA Case Setup</h2>
        <div class="form-grid">
          <label>Case name<input v-model="caseName" /></label>
          <label>Network ID<input v-model="networkId" /></label>
          <label>Business day<input v-model="businessDay" type="date" /></label>
          <label>Business time<input v-model="businessTime" type="time" /></label>
          <label>Timeframe
            <select v-model="timeFrame">
              <option value="ID">Intra-Day</option>
              <option value="DAY_AHEAD">Day Ahead</option>
              <option value="TWO_DAYS_AHEAD">Day-2</option>
            </select>
          </label>
          <label>Contingencies<input v-model="contingencies" placeholder="N-1-LINE-1,N-1-GEN-2" /></label>
          <label class="checkbox"><input v-model="optimizeRemedialActions" type="checkbox" /> Run RAO</label>
          <Button :disabled="busy" @click="startCase">Start CSA</Button>
        </div>
      </section>

      <p v-if="message" class="message">{{ message }}</p>

      <section v-if="activeView === 'csa' && selectedCase" class="result-grid">
        <article class="panel">
          <h2>Workflow</h2>
          <DataTable :columns="taskColumns" :rows="selectedCase.tasks" id-key="taskId" />
        </article>
        <article class="panel">
          <h2>Post-contingency Violations</h2>
          <DataTable :columns="violationColumns" :rows="selectedCase.securityAnalysisResult?.postContingencyViolations ?? []" id-key="elementId" />
        </article>
        <article class="panel">
          <h2>Remedial Actions</h2>
          <DataTable :columns="actionColumns" :rows="selectedCase.raoResult?.actions ?? []" id-key="actionId" />
        </article>
      </section>

      <section v-if="activeView === 'workflow'" class="panel">
        <h2>Workflow Monitor</h2>
        <DataTable :columns="caseColumns" :rows="caseRows" id-key="csaCaseId" />
      </section>

      <section v-if="activeView !== 'csa' && activeView !== 'workflow'" class="panel muted">
        <h2>{{ activeTitle }}</h2>
        <p>This capability is reserved for a later RCC increment.</p>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Button, DataTable } from '@egm/gui.common/src';
import { listCsaCases, startCsaCase, type CsaCaseStatus } from './services/csaApi';

const activeView = ref('csa');
const busy = ref(false);
const message = ref('');
const cases = ref<CsaCaseStatus[]>([]);
const selectedCase = ref<CsaCaseStatus>();
const caseName = ref('Day-ahead CSA');
const networkId = ref('sample-network');
const businessDay = ref(new Date().toISOString().slice(0, 10));
const businessTime = ref('10:30');
const timeFrame = ref<'ID' | 'DAY_AHEAD' | 'TWO_DAYS_AHEAD'>('DAY_AHEAD');
const contingencies = ref('N-1-LINE-1,N-1-GEN-2');
const optimizeRemedialActions = ref(true);

const navigation = [
  { id: 'csa', label: 'CSA' },
  { id: 'cc', label: 'CC', disabled: true },
  { id: 'opc', label: 'OPC', disabled: true },
  { id: 'workflow', label: 'Workflow Monitor' }
];

const titles: Record<string, string> = {
  csa: 'CSA Workspace',
  cc: 'Capacity Calculation',
  opc: 'Operational Planning Coordination',
  workflow: 'Workflow Monitor'
};

const activeTitle = computed(() => titles[activeView.value] ?? 'RCC');
const caseRows = computed(() => cases.value.map((item) => ({
  csaCaseId: item.csaCaseId,
  caseName: item.caseName,
  status: item.status,
  processInstanceId: item.processInstanceId,
  updatedAt: formatDate(item.updatedAt),
  message: item.message
})));

const caseColumns = [
  { key: 'csaCaseId', label: 'CSA Case' },
  { key: 'caseName', label: 'Name' },
  { key: 'status', label: 'Status' },
  { key: 'processInstanceId', label: 'Process' },
  { key: 'updatedAt', label: 'Updated' },
  { key: 'message', label: 'Message' }
];
const taskColumns = [
  { key: 'name', label: 'Task' },
  { key: 'status', label: 'Status' },
  { key: 'message', label: 'Message' }
];
const violationColumns = [
  { key: 'contingencyId', label: 'Contingency' },
  { key: 'elementId', label: 'Element' },
  { key: 'violationType', label: 'Type' },
  { key: 'observedValue', label: 'Observed' },
  { key: 'limitValue', label: 'Limit' },
  { key: 'severity', label: 'Severity' }
];
const actionColumns = [
  { key: 'assetId', label: 'Asset' },
  { key: 'actionType', label: 'Action' },
  { key: 'beforeValue', label: 'Before' },
  { key: 'afterValue', label: 'After' },
  { key: 'validationStatus', label: 'Validation' }
];

onMounted(refresh);

async function startCase() {
  busy.value = true;
  message.value = '';
  try {
    selectedCase.value = await startCsaCase({
      caseName: caseName.value,
      networkCase: {
        caseId: crypto.randomUUID(),
        networkId: networkId.value,
        businessDay: businessDay.value,
        businessTime: businessTime.value,
        timeFrame: timeFrame.value
      },
      contingencyIds: contingencies.value.split(',').map((item) => item.trim()).filter(Boolean),
      optimizeRemedialActions: optimizeRemedialActions.value
    });
    cases.value = [selectedCase.value, ...cases.value.filter((item) => item.csaCaseId !== selectedCase.value?.csaCaseId)];
    message.value = selectedCase.value.message;
  } catch (error) {
    message.value = error instanceof Error ? error.message : 'Unable to start CSA case';
  } finally {
    busy.value = false;
  }
}

async function refresh() {
  busy.value = true;
  message.value = '';
  try {
    cases.value = (await listCsaCases()).items;
  } catch (error) {
    message.value = error instanceof Error ? error.message : 'Unable to load CSA cases';
  } finally {
    busy.value = false;
  }
}

function formatDate(value: string) {
  return value ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '';
}
</script>
