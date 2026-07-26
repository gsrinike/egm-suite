<template>
  <main class="rcc-shell">
    <aside class="sidebar">
      <div class="brand">
        <span>RCC Manager</span>
        <strong>Operational Services</strong>
      </div>
      <nav class="navigation" aria-label="RCC capabilities">
        <div v-for="group in navigation" :key="group.id" class="nav-group">
          <button
            class="nav-button"
            :class="{ active: activeView === group.id }"
            type="button"
            :disabled="group.disabled || Boolean(group.children?.length)"
            @click="selectView(group.id)"
          >
            {{ group.label }}
          </button>
          <button
            v-for="child in group.children ?? []"
            :key="child.id"
            class="nav-button sub-nav-button"
            :class="{ active: activeView === child.id }"
            type="button"
            :disabled="child.disabled"
            @click="selectView(child.id)"
          >
            {{ child.label }}
          </button>
        </div>
      </nav>
    </aside>

    <section class="workspace">
      <header class="hero">
        <div>
          <p>{{ activeCapability }}</p>
          <h1>{{ activeTitle }}</h1>
        </div>
        <div class="hero-actions">
          <AutoRefreshControl
            storage-key="egm.rcc.header.refresh.interval"
            :disabled="activeView !== 'cgm-import' || cnmActiveView !== 'imports'"
            @interval-change="configureHeaderAutoRefresh"
            @refresh="refreshActiveView"
          />
          <button class="theme-toggle" type="button" @click="toggleTheme">
            {{ lightTheme ? 'Dark' : 'Light' }}
          </button>
          <Button v-if="activeView !== 'cgm-import'" :disabled="busy" @click="refresh">Refresh</Button>
        </div>
      </header>

      <section v-if="activeView === 'cgm-import'" class="cnm-embed">
        <CnmManagerView ref="cnmManager" embedded @view-change="cnmActiveView = $event" />
      </section>

      <section v-if="activeView === 'cgm-security-analysis'" class="lfsa-embed">
        <LfsaManagerView embedded />
      </section>

      <section v-if="activeView === 'cgm-sensitivity-analysis'" class="lfsa-embed">
        <SensitivityManagerView embedded />
      </section>

      <p v-if="message" class="message">{{ message }}</p>

      <section v-if="['csa', 'workflow'].includes(activeView)" class="panel muted">
        <h2>{{ activeTitle }}</h2>
        <p>This capability is reserved for a later RCC increment.</p>
      </section>

      <section v-if="!['cgm-import', 'cgm-security-analysis', 'cgm-sensitivity-analysis', 'csa', 'workflow'].includes(activeView)" class="panel muted">
        <h2>{{ activeTitle }}</h2>
        <p>This capability is reserved for a later RCC increment.</p>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { applyThemePreference, AutoRefreshControl, Button, toggleThemePreference } from '@egm/gui.common/src';
import CnmManagerView from '@egm/gui.cnm.manager/src/components/CnmManagerView.vue';
import { LfsaManagerView, SensitivityManagerView } from '@egm/gui.lfsa.manager';

const activeView = ref('cgm-import');
const busy = ref(false);
const message = ref('');
const lightTheme = ref(false);
const cnmManager = ref<CnmManagerHandle>();
const cnmActiveView = ref('imports');
const headerRefreshInterval = ref<number | null>(null);
let headerRefreshTimer: number | undefined;

const navigation: NavigationItem[] = [
  { id: 'cgm', label: 'CGM', children: [
    { id: 'cgm-import', label: 'Import Manager' },
    { id: 'cgm-security-analysis', label: 'Load Flow & Security Analysis' },
    { id: 'cgm-sensitivity-analysis', label: 'Sensitivity Analysis' }
  ] },
  { id: 'csa', label: 'CSA' },
  { id: 'cc', label: 'CC', disabled: true },
  { id: 'opc', label: 'OPC', disabled: true },
  { id: 'workflow', label: 'Workflow Monitor' }
];

const titles: Record<string, string> = {
  'cgm-import': 'Import Manager',
  'cgm-security-analysis': 'Load Flow & Security Analysis',
  'cgm-sensitivity-analysis': 'Sensitivity Analysis',
  csa: 'CSA Workspace',
  cc: 'Capacity Calculation',
  opc: 'Operational Planning Coordination',
  workflow: 'Workflow Monitor'
};
const capabilities: Record<string, string> = {
  'cgm-import': 'Common Grid Model',
  'cgm-security-analysis': 'Common Grid Model',
  'cgm-sensitivity-analysis': 'Common Grid Model',
  csa: 'Coordinated Security Analysis',
  cc: 'Capacity Calculation',
  opc: 'Operational Planning Coordination',
  workflow: 'Workflow Monitor'
};

const activeTitle = computed(() => titles[activeView.value] ?? 'RCC');
const activeCapability = computed(() => capabilities[activeView.value] ?? 'RCC');

onMounted(() => {
  lightTheme.value = applyThemePreference();
});
onUnmounted(() => {
  clearHeaderRefreshTimer();
  cnmManager.value?.configureImportAutoRefresh(null);
});
watch(activeView, () => {
  void applyHeaderAutoRefresh();
});

interface NavigationItem {
  id: string;
  label: string;
  disabled?: boolean;
  children?: NavigationItem[];
}

interface CnmManagerHandle {
  configureImportAutoRefresh: (intervalMs: number | null) => void;
  refresh: () => Promise<void>;
}

function selectView(view: string) {
  activeView.value = view;
}

function toggleTheme() {
  lightTheme.value = toggleThemePreference(lightTheme.value);
}

async function configureHeaderAutoRefresh(intervalMs: number | null) {
  headerRefreshInterval.value = intervalMs;
  await applyHeaderAutoRefresh();
}

async function applyHeaderAutoRefresh() {
  clearHeaderRefreshTimer();
  await nextTick();
  if (activeView.value === 'cgm-import') {
    cnmManager.value?.configureImportAutoRefresh(headerRefreshInterval.value);
    return;
  }
  cnmManager.value?.configureImportAutoRefresh(null);
  if (headerRefreshInterval.value !== null) {
    headerRefreshTimer = window.setInterval(refresh, headerRefreshInterval.value);
  }
}

function clearHeaderRefreshTimer() {
  if (headerRefreshTimer !== undefined) {
    window.clearInterval(headerRefreshTimer);
    headerRefreshTimer = undefined;
  }
}

async function refreshActiveView() {
  if (activeView.value === 'cgm-import') {
    await cnmManager.value?.refresh();
    return;
  }
  await refresh();
}

async function refresh() {
  message.value = '';
}
</script>
