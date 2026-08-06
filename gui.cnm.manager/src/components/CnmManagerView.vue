<template>
  <main class="app-shell" :class="{ embedded }" @click.capture="clearMessage">
    <div v-if="!embedded" class="app-header">
      <div>
        <p>CNM Manager</p>
        <h1>Model Imports</h1>
      </div>
      <div class="header-actions">
        <AutoRefreshControl
          storage-key="egm.cnm.import.refresh.interval"
          :disabled="activeView !== 'imports'"
          @interval-change="configureImportAutoRefresh"
          @refresh="refresh"
        />
        <button class="theme-toggle" type="button" @click="toggleTheme">
          {{ lightTheme ? 'Dark' : 'Light' }}
        </button>
      </div>
    </div>

    <Menu :items="menuItems" :active-id="activeView" @select="selectActiveView" />

    <section v-if="activeView === 'imports'" class="toolbar glass-panel">
      <Dropdown v-model="serviceType" label="Service" :options="serviceOptions" />
      <Dropdown v-model="timeFrame" label="Timeframe" :options="timeFrameOptions" />
      <label class="file-picker">
        <span>RDF models</span>
        <input ref="fileInput" type="file" multiple accept=".rdf,.xml,.zip,application/rdf+xml,application/xml,application/zip" @change="selectFiles" />
      </label>
      <label class="message-input">
        <span>Message</span>
        <input v-model="importMessage" type="text" placeholder="Import context" />
      </label>
      <Button :disabled="selectedFiles.length === 0 || busy" @click="upload()">
        Import
      </Button>
    </section>

    <section v-if="activeView === 'profiles'" class="profile-filters glass-panel">
      <label>Profile import
        <select v-model="selectedProfileImportId">
          <option v-for="option in profileImportOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>
      <label>Profile type<input v-model="profileFilters.profileType" placeholder="EQ, SV, SSH..." /></label>
      <label>TSO<input v-model="profileFilters.tso" placeholder="TSO-XYZ" /></label>
      <label>Business day<input v-model="profileFilters.businessDay" type="date" /></label>
      <label>Business time<input v-model="profileFilters.businessTime" type="time" /></label>
      <Button :disabled="busy || !selectedProfileImportId" @click="refreshProfiles">Search</Button>
    </section>

    <section v-if="activeView === 'iidm'" class="profile-filters glass-panel">
      <label>Successful import
        <select v-model="selectedIidmImportId">
          <option v-for="option in iidmImportOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>
    </section>

    <section v-if="activeView === 'import-files'" class="detail-heading glass-panel">
      <div>
        <p>Import files</p>
        <strong>{{ selectedImport?.importId }}</strong>
      </div>
      <Button :disabled="busy" @click="closeImportFiles">Back to imports</Button>
    </section>

    <section v-if="activeView === 'profile-data'" class="detail-heading glass-panel">
      <div>
        <p>Profile data</p>
        <strong>{{ profileTables?.profileType }} {{ selectedProfileFileName }}</strong>
      </div>
      <Button :disabled="busy" @click="activeView = profileReturnView">
        Back to {{ profileReturnView === 'profiles' ? 'profiles' : 'files' }}
      </Button>
    </section>

    <section v-if="activeView === 'iidm-data'" class="detail-heading glass-panel">
      <div>
        <p>IIDM data</p>
        <strong>{{ selectedIidmNetworkId }}</strong>
      </div>
      <Button :disabled="busy" @click="activeView = 'iidm'">Back to IIDM</Button>
    </section>

    <section v-if="activeView === 'iidm-data'" class="profile-filters iidm-table-filters glass-panel">
      <label>Search table
        <input v-model="iidmTableSearch" placeholder="Search selected IIDM table" @keyup.enter="searchIidmTableRows" />
      </label>
      <Button :disabled="busy || !selectedIidmTableId" @click="searchIidmTableRows">Search</Button>
      <Button :disabled="busy || !iidmTableSearch" @click="clearIidmTableSearch">Clear</Button>
    </section>

    <p v-if="message" class="status-message">{{ message }}</p>

    <DataTable
      v-if="activeView === 'imports'"
      :columns="columns"
      :rows="rows"
      :page-size="10"
      id-key="importId"
    >
      <template #cell-file="{ row }">
        <Link @click="openImportFiles(String(row.importId))">
          View {{ row.fileCount }} file{{ Number(row.fileCount) === 1 ? '' : 's' }}
        </Link>
      </template>
      <template #cell-iidmLink="{ row }">
        <Link
          v-if="row.iidmTransformationStatus === 'DONE'"
          @click="openIidmFromImport(String(row.importId))"
        >
          View transformations
        </Link>
        <span v-else class="disabled-profile-link" title="IIDM transformations are available after completion">
          Not available
        </span>
      </template>
      <template #cell-action="{ row }">
        <Button v-if="row.state === 'FAILED'" :disabled="busy" @click="chooseRetry(String(row.importId))">
          Re-upload
        </Button>
      </template>
    </DataTable>

    <DataTable
      v-if="activeView === 'import-files'"
      :columns="fileColumns"
      :rows="fileRows"
      :page-size="10"
      id-key="fileId"
    >
      <template #cell-fileName="{ row }">
        <Link
          v-if="canOpenProfileData(String(row.state), String(row.message ?? ''))"
          @click="openImportFileProfileTables(String(row.fileId), String(row.fileName))"
        >
          {{ row.fileName }}
        </Link>
        <span v-else class="disabled-profile-link" title="Profile data is available after RDF metadata is parsed">
          {{ row.fileName }}
        </span>
      </template>
    </DataTable>

    <DataTable
      v-if="activeView === 'import-files' && selectedImportSnapshots.length > 0"
      :columns="snapshotColumns"
      :rows="snapshotRows"
      :page-size="5"
      id-key="snapshotId"
    />

    <DynamicTable
      v-if="activeView === 'profile-data'"
      :tables="profileTables?.tables ?? []"
      :loading="busy"
      :error="message"
      :page-size="25"
    />

    <DataTable
      v-if="activeView === 'profiles'"
      :columns="profileColumns"
      :rows="profileRows"
      :page-size="10"
      id-key="profileId"
    >
      <template #cell-fileName="{ row }">
        <Link
          v-if="canOpenProfileData(String(row.state), String(row.message ?? ''))"
          @click="openProfileTables(String(row.importId), String(row.fileId), String(row.fileName), 'profiles')"
        >
          {{ row.fileName }}
        </Link>
        <span v-else class="disabled-profile-link" title="Profile data is available after RDF metadata is parsed">
          {{ row.fileName }}
        </span>
      </template>
    </DataTable>

    <DataTable
      v-if="activeView === 'iidm'"
      :columns="iidmColumns"
      :rows="iidmRows"
      :page-size="10"
      id-key="transformId"
    >
      <template #cell-networkId="{ row }">
        <Link
          v-if="canOpenIidmData(String(row.transformState), String(row.networkId))"
          @click="openIidmTables(String(row.networkId))"
        >
          {{ row.networkId }}
        </Link>
        <span v-else class="disabled-profile-link" title="IIDM data is available after transformation completes">
          {{ row.networkId }}
        </span>
      </template>
    </DataTable>

    <DynamicTable
      v-if="activeView === 'iidm-data'"
      :tables="iidmTables?.tables ?? []"
      :loading="busy"
      :error="message"
      :page-size="iidmTablePageSize"
      :current-page="iidmTablePage"
      server-side
      @table-selected="loadIidmTableRows($event, 0)"
      @page-change="loadIidmTableRows(selectedIidmTableId, $event)"
    />
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import {
  applyThemePreference,
  AutoRefreshControl,
  Button,
  DataTable,
  Dropdown,
  DynamicTable,
  Link,
  logClientError,
  Menu,
  toggleThemePreference
} from '@egm/gui.common/src';
import {
  getImport,
  getIidmNetworkTables,
  getIidmNetworkTableRows,
  getProfileTables,
  ImportUploadError,
  listIidmTransforms,
  listSnapshots,
  listProfiles,
  listImports,
  uploadImport,
  type CnmServiceType,
  type CnmSnapshotMetadata,
  type DynamicTableBundle,
  type IidmTransformationStatus,
  type IidmTableBundle,
  type IidmTransformSummary,
  type ImportStatus,
  type ProfileMetadata,
  type TimeFrame
} from '../services/cnmApi';

withDefaults(defineProps<{ embedded?: boolean }>(), {
  embedded: false
});

const emit = defineEmits<{
  viewChange: [view: string];
}>();

const activeView = ref('imports');
const imports = ref<ImportStatus[]>([]);
const profiles = ref<ProfileMetadata[]>([]);
const iidmTransforms = ref<IidmTransformSummary[]>([]);
const snapshots = ref<CnmSnapshotMetadata[]>([]);
const selectedImportSnapshots = ref<CnmSnapshotMetadata[]>([]);
const selectedFiles = ref<File[]>([]);
const serviceType = ref<CnmServiceType>('CGM');
const timeFrame = ref<TimeFrame>('DAY_AHEAD');
const importMessage = ref('');
const busy = ref(false);
const message = ref('');
const fileInput = ref<HTMLInputElement>();
const retryImportId = ref('');
const selectedImport = ref<ImportStatus>();
const profileTables = ref<DynamicTableBundle>();
const selectedProfileFileName = ref('');
const profileReturnView = ref('import-files');
const profileFilters = ref({ profileType: '', tso: '', businessDay: '', businessTime: '' });
const selectedProfileImportId = ref('');
const selectedIidmImportId = ref('');
const iidmTables = ref<IidmTableBundle>();
const selectedIidmNetworkId = ref('');
const selectedIidmTableId = ref('');
const iidmTablePage = ref(0);
const iidmTablePageSize = 100;
const iidmTableSearch = ref('');
const lightTheme = ref(false);
let importRefreshTimer: number | undefined;
let importRefreshInFlight = false;
let requestedImportRefreshInterval: number | null = null;

const menuItems = [
  { id: 'imports', label: 'Imports' },
  { id: 'profiles', label: 'Profiles' },
  { id: 'iidm', label: 'IIDM' }
];

const serviceOptions = [
  { label: 'Common Grid Model', value: 'CGM' },
  { label: 'Coordinated Security Analysis', value: 'CSA' },
  { label: 'Capacity Calculation', value: 'CC' }
];

const timeFrameOptions = [
  { label: 'Intra-Day', value: 'ID' },
  { label: 'Day Ahead', value: 'DAY_AHEAD' },
  { label: 'Day-2', value: 'TWO_DAYS_AHEAD' }
];

const columns = [
  { key: 'importId', label: 'Import ID' },
  { key: 'serviceType', label: 'Service' },
  { key: 'timeFrame', label: 'Timeframe' },
  { key: 'state', label: 'State' },
  { key: 'fileProgress', label: 'File progress' },
  { key: 'iidmTransformationStatus', label: 'IIDM status' },
  { key: 'iidmLink', label: 'IIDM' },
  { key: 'file', label: 'File' },
  { key: 'createdAt', label: 'Created' },
  { key: 'message', label: 'Message' },
  { key: 'action', label: 'Action' }
];

const fileColumns = [
  { key: 'fileName', label: 'File' },
  { key: 'state', label: 'State' },
  { key: 'profileType', label: 'Profile type' },
  { key: 'profileFamily', label: 'Profile family' },
  { key: 'tsoName', label: 'TSO' },
  { key: 'businessDay', label: 'Business day' },
  { key: 'businessTime', label: 'Business time' },
  { key: 'modelTimeFrame', label: 'Model timeframe' },
  { key: 'modelVersion', label: 'Version' },
  { key: 'uploadedAt', label: 'Imported' },
  { key: 'message', label: 'Message' }
];

const profileColumns = [
  { key: 'fileName', label: 'File' },
  { key: 'state', label: 'State' },
  { key: 'profileType', label: 'Profile type' },
  { key: 'profileFamily', label: 'Profile family' },
  { key: 'tsoName', label: 'TSO' },
  { key: 'businessDay', label: 'Business day' },
  { key: 'businessTime', label: 'Business time' },
  { key: 'timeFrame', label: 'Timeframe' },
  { key: 'version', label: 'Version' }
];

const iidmColumns = [
  { key: 'importId', label: 'Import ID' },
  { key: 'sourceFiles', label: 'Source files' },
  { key: 'profileType', label: 'Profile type' },
  { key: 'profileFamily', label: 'Profile family' },
  { key: 'transformState', label: 'State' },
  { key: 'networkId', label: 'Network' },
  { key: 'diagnosticCount', label: 'Diagnostics' },
  { key: 'startedAt', label: 'Started' },
  { key: 'completedAt', label: 'Completed' },
  { key: 'failedAt', label: 'Failed' },
  { key: 'transformMessage', label: 'Message' }
];

const snapshotColumns = [
  { key: 'snapshotId', label: 'Snapshot' },
  { key: 'state', label: 'State' },
  { key: 'tsoName', label: 'TSO' },
  { key: 'businessDay', label: 'Business day' },
  { key: 'businessTime', label: 'Business time' },
  { key: 'timeFrame', label: 'Timeframe' },
  { key: 'staticObjectCount', label: 'Objects' },
  { key: 'stateValueCount', label: 'State values' },
  { key: 'payloadSectionCount', label: 'Payload sections' },
  { key: 'assembledAt', label: 'Assembled' },
  { key: 'message', label: 'Message' }
];

const rows = computed(() => imports.value.map((item) => {
  const fileSummary = importFileSummary(item);
  return {
    importId: item.importId,
    serviceType: item.serviceType,
    timeFrame: displayTimeFrame(item.timeFrame),
    state: item.state,
    fileProgress: displayFileProgress(fileSummary),
    iidmTransformationStatus: displayIidmTransformationStatus(item.iidmTransformationStatus),
    fileCount: item.files?.length ?? 0,
    createdAt: formatDateTime(item.createdAt),
    message: item.message
  };
}));
const fileRows = computed(() => (selectedImport.value?.files ?? []).map((file) => ({
  ...file,
  modelTimeFrame: displayModelTimeFrame(file.modelTimeFrame),
  uploadedAt: formatDateTime(file.uploadedAt)
})));
const profileRows = computed(() => profiles.value.map((profile) => ({ ...profile })));
const iidmRows = computed(() => iidmTransforms.value.map((transform) => ({
  ...transform,
  sourceFiles: (transform.sourceFileNames?.length ? transform.sourceFileNames : [transform.fileId]).join(', '),
  startedAt: formatDateTime(transform.startedAt),
  completedAt: formatDateTime(transform.completedAt),
  failedAt: formatDateTime(transform.failedAt)
})));
const snapshotRows = computed(() => selectedImportSnapshots.value.map((snapshot) => ({
  ...snapshot,
  timeFrame: displayModelTimeFrame(snapshot.timeFrame),
  assembledAt: formatDateTime(snapshot.assembledAt)
})));
const profileCapableImports = computed(() => imports.value.filter((item) =>
  (item.files ?? []).some((file) => canOpenProfileData(file.state, file.message))
));
const profileImportOptions = computed(() => [
  {
    label: profileCapableImports.value.length === 0 ? 'No parsed profile imports available' : 'Select profile import',
    value: ''
  },
  ...profileCapableImports.value.map((item) => ({
    label: importOptionLabel(item),
    value: item.importId
  }))
]);
const iidmReadyImports = computed(() =>
  profileCapableImports.value.filter((item) =>
    item.state === 'SUCCESS' && item.iidmTransformationStatus === 'DONE'
  )
);
const iidmImportOptions = computed(() => [
  {
    label: iidmReadyImports.value.length === 0 ? 'No successful imports available' : 'Select successful import',
    value: ''
  },
  ...iidmReadyImports.value.map((item) => ({
    label: importOptionLabel(item),
    value: item.importId
  }))
]);

onMounted(() => {
  lightTheme.value = applyThemePreference();
  emit('viewChange', activeView.value);
  void refresh();
});
onUnmounted(() => {
  if (importRefreshTimer !== undefined) {
    window.clearInterval(importRefreshTimer);
  }
});
watch(activeView, (view) => {
  emit('viewChange', view);
  clearMessage();
  applyImportAutoRefresh();
  if (view === 'profiles') {
    void refreshProfiles();
  }
  if (view === 'iidm') {
    alignSelectedMetadataSelections();
    void refreshIidmTransforms();
  }
});
watch(selectedProfileImportId, () => {
  clearMessage();
  profiles.value = [];
  profileTables.value = undefined;
  selectedProfileFileName.value = '';
  if (activeView.value === 'profiles' && selectedProfileImportId.value) {
    void refreshProfiles();
  }
});
watch(selectedIidmImportId, () => {
  clearMessage();
  iidmTransforms.value = [];
  iidmTables.value = undefined;
  selectedIidmNetworkId.value = '';
  selectedIidmTableId.value = '';
  iidmTableSearch.value = '';
  if (activeView.value === 'iidm' && selectedIidmImportId.value) {
    void refreshIidmTransforms();
  }
});

function toggleTheme() {
  clearMessage();
  lightTheme.value = toggleThemePreference(lightTheme.value);
}

function selectActiveView(view: string) {
  clearMessage();
  activeView.value = view;
}

function selectFiles(event: Event) {
  clearMessage();
  selectedFiles.value = Array.from((event.target as HTMLInputElement).files ?? []);
  if (retryImportId.value && selectedFiles.value.length > 0) {
    void upload(retryImportId.value);
  }
}

function chooseRetry(importId: string) {
  clearMessage();
  const failedImport = imports.value.find((item) => item.importId === importId);
  if (failedImport) {
    serviceType.value = failedImport.serviceType;
    timeFrame.value = failedImport.timeFrame;
  }
  importMessage.value = '';
  retryImportId.value = importId;
  selectedFiles.value = [];
  if (fileInput.value) {
    fileInput.value.value = '';
    fileInput.value.click();
  }
}

async function openImportFiles(importId: string) {
  busy.value = true;
  clearMessage();
  try {
    selectedImport.value = await getImport(importId);
    selectedImportSnapshots.value = (await listSnapshots({ importId, page: 0, size: 50 })).items;
    activeView.value = 'import-files';
  } catch (error) {
    logClientError('openImportFiles failed', error, { importId });
    message.value = error instanceof Error ? error.message : 'Unable to load import files';
  } finally {
    busy.value = false;
  }
}

function closeImportFiles() {
  clearMessage();
  selectedImport.value = undefined;
  selectedImportSnapshots.value = [];
  profileTables.value = undefined;
  selectedProfileFileName.value = '';
  profileReturnView.value = 'import-files';
  activeView.value = 'imports';
}

function canOpenProfileData(state: string, messageText = '') {
  return state === 'PARSED'
    || (state === 'FAILED' && messageText.includes('Unable to assemble CGM network snapshot'));
}

function canOpenIidmData(state: string, networkId: string) {
  return state === 'DONE' && Boolean(networkId);
}

async function openImportFileProfileTables(fileId: string, fileName: string) {
  if (!selectedImport.value) {
    return;
  }
  await openProfileTables(selectedImport.value.importId, fileId, fileName, 'import-files');
}

async function openIidmFromImport(importId: string) {
  clearMessage();
  selectedIidmImportId.value = importId;
  activeView.value = 'iidm';
  await refreshIidmTransforms();
}

async function openProfileTables(importId: string, fileId: string, fileName: string, returnView: string) {
  busy.value = true;
  clearMessage();
  selectedProfileFileName.value = fileName;
  profileReturnView.value = returnView;
  try {
    profileTables.value = await getProfileTables(importId, fileId);
    activeView.value = 'profile-data';
  } catch (error) {
    logClientError('openProfileTables failed', error, {
      importId,
      fileId,
      fileName
    });
    message.value = error instanceof Error ? error.message : 'Unable to load profile data';
  } finally {
    busy.value = false;
  }
}

async function refresh() {
  busy.value = true;
  clearMessage();
  try {
    await applyImportSnapshot((await listImports()).items);
  } catch (error) {
    logClientError('refresh imports failed', error);
    message.value = error instanceof Error ? error.message : 'Unable to load imports';
  } finally {
    busy.value = false;
  }
}

async function refreshImportsSilently() {
  if (busy.value || importRefreshInFlight) {
    return;
  }
  importRefreshInFlight = true;
  try {
    await applyImportSnapshot((await listImports()).items);
  } catch (error) {
    logClientError('silent import refresh failed', error);
  } finally {
    importRefreshInFlight = false;
  }
}

function configureImportAutoRefresh(intervalMs: number | null) {
  requestedImportRefreshInterval = intervalMs;
  applyImportAutoRefresh();
}

function applyImportAutoRefresh() {
  if (importRefreshTimer !== undefined) {
    window.clearInterval(importRefreshTimer);
    importRefreshTimer = undefined;
  }
  if (activeView.value === 'imports' && requestedImportRefreshInterval !== null) {
    importRefreshTimer = window.setInterval(refreshImportsSilently, requestedImportRefreshInterval);
  }
}

async function applyImportSnapshot(nextImports: ImportStatus[]) {
  imports.value = nextImports;
  if (selectedImport.value) {
    const updatedImport = nextImports.find((item) => item.importId === selectedImport.value?.importId);
    if (updatedImport) {
      selectedImport.value = updatedImport;
    }
  }
  alignSelectedMetadataSelections();
}

async function refreshProfiles() {
  clearMessage();
  if (!selectedProfileImportId.value) {
    profiles.value = [];
    message.value = profileCapableImports.value.length === 0
      ? 'No parsed profile imports available'
      : 'Select a profile import';
    return;
  }
  busy.value = true;
  message.value = '';
  try {
    profiles.value = (await listProfiles({
      importId: selectedProfileImportId.value,
      ...profileFilters.value
    })).items;
  } catch (error) {
    logClientError('refreshProfiles failed', error, {
      importId: selectedProfileImportId.value,
      filters: profileFilters.value
    });
    message.value = error instanceof Error ? error.message : 'Unable to load profiles';
  } finally {
    busy.value = false;
  }
}

async function refreshIidmTransforms() {
  clearMessage();
  if (!selectedIidmImportId.value) {
    iidmTransforms.value = [];
    message.value = iidmReadyImports.value.length === 0
      ? 'No successful imports available'
      : 'Select a successful import';
    return;
  }
  busy.value = true;
  message.value = '';
  try {
    iidmTransforms.value = (await listIidmTransforms({
      importId: selectedIidmImportId.value,
      page: 0,
      size: 100
    })).items;
  } catch (error) {
    logClientError('refreshIidmTransforms failed', error, {
      importId: selectedIidmImportId.value
    });
    message.value = error instanceof Error ? error.message : 'Unable to load IIDM transforms';
  } finally {
    busy.value = false;
  }
}

async function openIidmTables(networkId: string) {
  busy.value = true;
  clearMessage();
  selectedIidmNetworkId.value = networkId;
  selectedIidmTableId.value = '';
  iidmTablePage.value = 0;
  iidmTableSearch.value = '';
  try {
    iidmTables.value = await getIidmNetworkTables(networkId);
    selectedIidmTableId.value = iidmTables.value.tables[0]?.tableId ?? '';
    activeView.value = 'iidm-data';
    if (selectedIidmTableId.value) {
      await loadIidmTableRows(selectedIidmTableId.value, 0);
    }
  } catch (error) {
    logClientError('openIidmTables failed', error, { networkId });
    message.value = error instanceof Error ? error.message : 'Unable to load IIDM data';
  } finally {
    busy.value = false;
  }
}

async function loadIidmTableRows(tableId: string, page: number) {
  if (!selectedIidmNetworkId.value || !tableId) {
    return;
  }
  busy.value = true;
  clearMessage();
  selectedIidmTableId.value = tableId;
  iidmTablePage.value = page;
  try {
    iidmTables.value = await getIidmNetworkTableRows(
      selectedIidmNetworkId.value,
      tableId,
      page,
      iidmTablePageSize,
      iidmTableSearch.value
    );
  } catch (error) {
    logClientError('loadIidmTableRows failed', error, {
      networkId: selectedIidmNetworkId.value,
      tableId,
      page,
      search: iidmTableSearch.value
    });
    message.value = error instanceof Error ? error.message : 'Unable to load IIDM table rows';
  } finally {
    busy.value = false;
  }
}

async function searchIidmTableRows() {
  await loadIidmTableRows(selectedIidmTableId.value, 0);
}

async function clearIidmTableSearch() {
  if (!iidmTableSearch.value) {
    return;
  }
  iidmTableSearch.value = '';
  await loadIidmTableRows(selectedIidmTableId.value, 0);
}

async function upload(importId?: string) {
  if (selectedFiles.value.length === 0) {
    return;
  }
  busy.value = true;
  clearMessage();
  try {
    const imported = await uploadImport(
      selectedFiles.value,
      serviceType.value,
      timeFrame.value,
      importMessage.value,
      importId
    );
    await applyImportSnapshot([imported, ...imports.value.filter((item) => item.importId !== imported.importId)]);
    message.value = imported.state === 'FAILED'
      ? imported.message
      : `Import created with ${imported.files.length} model file${imported.files.length === 1 ? '' : 's'}`;
    importMessage.value = '';
  } catch (error) {
    logClientError('upload import failed', error, {
      importId,
      fileNames: selectedFiles.value.map((file) => file.name)
    });
    const errorMessage = error instanceof Error ? error.message : 'Unable to import model';
    if (error instanceof ImportUploadError) {
      await refresh();
    }
    message.value = errorMessage;
  } finally {
    retryImportId.value = '';
    busy.value = false;
  }
}

function displayTimeFrame(value: TimeFrame) {
  if (value === 'DAY_AHEAD') {
    return 'DAY AHEAD';
  }
  if (value === 'TWO_DAYS_AHEAD') {
    return 'DAY-2';
  }
  return 'INTRA-DAY';
}

function importOptionLabel(item: ImportStatus) {
  return `${formatDateTime(item.createdAt)}_${item.serviceType}_${displayTimeFrame(item.timeFrame)}(${item.importId})`;
}

function alignSelectedMetadataSelections() {
  if (selectedProfileImportId.value && !profileCapableImports.value.some((item) => item.importId === selectedProfileImportId.value)) {
    selectedProfileImportId.value = '';
    profiles.value = [];
  }
  if (selectedIidmImportId.value && !iidmReadyImports.value.some((item) => item.importId === selectedIidmImportId.value)) {
    selectedIidmImportId.value = '';
    iidmTransforms.value = [];
  }
  if (!selectedIidmImportId.value && iidmReadyImports.value.length === 1) {
    selectedIidmImportId.value = iidmReadyImports.value[0].importId;
  }
}

function displayModelTimeFrame(value: string) {
  return value === '1D' ? 'DAY AHEAD' : value;
}

function displayIidmTransformationStatus(value: string) {
  return value === 'NOT_STARTED' ? 'NOT STARTED' : value;
}

function importFileSummary(importStatus: ImportStatus) {
  const files = importStatus.files ?? [];
  return {
    count: files.length,
    parsedCount: files.filter((file) => file.state === 'PARSED').length,
    failedCount: files.filter((file) => file.state === 'FAILED').length
  };
}

function displayFileProgress(summary: { count: number; parsedCount: number; failedCount: number }) {
  if (summary.count === 0) {
    return '0/0 parsed';
  }
  const failed = summary.failedCount > 0 ? `, ${summary.failedCount} failed` : '';
  return `${summary.parsedCount}/${summary.count} parsed${failed}`;
}

function clearMessage() {
  message.value = '';
}

function formatDateTime(value: string) {
  if (!value) {
    return '';
  }
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value));
}

defineExpose({
  configureImportAutoRefresh,
  refresh
});
</script>
