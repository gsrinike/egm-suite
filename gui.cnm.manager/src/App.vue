<template>
  <main class="app-shell">
    <Menu :items="menuItems" :active-id="activeView" @select="activeView = $event" />

    <section v-if="activeView === 'imports'" class="toolbar">
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
      <Button :disabled="busy" @click="refresh">
        Refresh
      </Button>
    </section>

    <section v-if="activeView === 'profiles'" class="profile-filters">
      <label>Profile type<input v-model="profileFilters.profileType" placeholder="EQ, SV, SSH..." /></label>
      <label>TSO<input v-model="profileFilters.tso" placeholder="TSCNET-EU" /></label>
      <label>Business day<input v-model="profileFilters.businessDay" type="date" /></label>
      <label>Business time<input v-model="profileFilters.businessTime" type="time" /></label>
      <Button :disabled="busy" @click="refreshProfiles">Search</Button>
    </section>

    <section v-if="activeView === 'import-files'" class="detail-heading">
      <div>
        <p>Import files</p>
        <strong>{{ selectedImport?.importId }}</strong>
      </div>
      <Button :disabled="busy" @click="closeImportFiles">Back to imports</Button>
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
    />

    <DataTable
      v-if="activeView === 'profiles'"
      :columns="profileColumns"
      :rows="profileRows"
      :page-size="10"
      id-key="profileId"
    />
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { Button, DataTable, Dropdown, Link, Menu } from '@egm/gui.common/src';
import {
  getImport,
  ImportUploadError,
  listProfiles,
  listImports,
  uploadImport,
  type CnmServiceType,
  type ImportStatus,
  type ProfileMetadata,
  type TimeFrame
} from './services/cnmApi';

const activeView = ref('imports');
const imports = ref<ImportStatus[]>([]);
const profiles = ref<ProfileMetadata[]>([]);
const selectedFiles = ref<File[]>([]);
const serviceType = ref<CnmServiceType>('CGM');
const timeFrame = ref<TimeFrame>('DAY_AHEAD');
const importMessage = ref('');
const busy = ref(false);
const message = ref('');
const fileInput = ref<HTMLInputElement>();
const retryImportId = ref('');
const selectedImport = ref<ImportStatus>();
const profileFilters = ref({ profileType: '', tso: '', businessDay: '', businessTime: '' });

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
  { key: 'state', label: 'State' },
  { key: 'profileType', label: 'Profile type' },
  { key: 'profileFamily', label: 'Profile family' },
  { key: 'tsoName', label: 'TSO' },
  { key: 'businessDay', label: 'Business day' },
  { key: 'businessTime', label: 'Business time' },
  { key: 'timeFrame', label: 'Timeframe' },
  { key: 'version', label: 'Version' },
  { key: 'fileName', label: 'File' }
];

const rows = computed(() => imports.value.map((item) => ({
  importId: item.importId,
  serviceType: item.serviceType,
  timeFrame: displayTimeFrame(item.timeFrame),
  state: item.state,
  fileCount: item.files?.length ?? 0,
  createdAt: formatDateTime(item.createdAt),
  message: item.message
})));
const fileRows = computed(() => (selectedImport.value?.files ?? []).map((file) => ({
  ...file,
  modelTimeFrame: displayModelTimeFrame(file.modelTimeFrame),
  uploadedAt: formatDateTime(file.uploadedAt)
})));
const profileRows = computed(() => profiles.value.map((profile) => ({ ...profile })));

onMounted(refresh);
watch(activeView, (view) => {
  if (view === 'profiles') {
    void refreshProfiles();
  }
});

function selectFiles(event: Event) {
  selectedFiles.value = Array.from((event.target as HTMLInputElement).files ?? []);
  if (retryImportId.value && selectedFiles.value.length > 0) {
    void upload(retryImportId.value);
  }
}

function chooseRetry(importId: string) {
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
  message.value = '';
  try {
    selectedImport.value = await getImport(importId);
    activeView.value = 'import-files';
  } catch (error) {
    message.value = error instanceof Error ? error.message : 'Unable to load import files';
  } finally {
    busy.value = false;
  }
}

function closeImportFiles() {
  selectedImport.value = undefined;
  activeView.value = 'imports';
}

async function refresh() {
  busy.value = true;
  message.value = '';
  try {
    imports.value = (await listImports()).items;
  } catch (error) {
    message.value = error instanceof Error ? error.message : 'Unable to load imports';
  } finally {
    busy.value = false;
  }
}

async function refreshProfiles() {
  busy.value = true;
  message.value = '';
  try {
    profiles.value = (await listProfiles(profileFilters.value)).items;
  } catch (error) {
    message.value = error instanceof Error ? error.message : 'Unable to load profiles';
  } finally {
    busy.value = false;
  }
}

async function upload(importId?: string) {
  if (selectedFiles.value.length === 0) {
    return;
  }
  busy.value = true;
  message.value = '';
  try {
    const imported = await uploadImport(
      selectedFiles.value,
      serviceType.value,
      timeFrame.value,
      importMessage.value,
      importId
    );
    imports.value = [imported, ...imports.value.filter((item) => item.importId !== imported.importId)];
    message.value = imported.state === 'FAILED'
      ? imported.message
      : `Import created with ${imported.files.length} model file${imported.files.length === 1 ? '' : 's'}`;
    importMessage.value = '';
  } catch (error) {
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

function displayModelTimeFrame(value: string) {
  return value === '1D' ? 'DAY AHEAD' : value;
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
</script>
