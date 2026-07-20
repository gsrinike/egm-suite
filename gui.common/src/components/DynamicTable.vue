<template>
  <section class="dynamic-table-shell">
    <div class="dynamic-table-tabs" v-if="tables.length > 1">
      <button
        v-for="table in tables"
        :key="table.tableId"
        type="button"
        :class="{ active: selectedTableId === table.tableId }"
        @click="selectTable(table.tableId)"
      >
        {{ table.label }} ({{ table.totalRows }})
      </button>
    </div>
    <p v-if="loading" class="dynamic-table-state">Loading profile data...</p>
    <p v-else-if="error" class="dynamic-table-state">{{ error }}</p>
    <p v-else-if="!activeTable" class="dynamic-table-state">No profile data available</p>
    <DataTable
      v-else
      :columns="activeTable.columns"
      :rows="rows"
      :page-size="pageSize"
      id-key="rowId"
      :hide-pagination="serverSide"
      :hide-search="serverSide"
    />
    <div v-if="serverSide && activeTable" class="common-pagination">
      <button type="button" :disabled="currentPage <= 0 || loading" @click="emitPage(currentPage - 1)">
        Previous
      </button>
      <span>Page {{ currentPage + 1 }} / {{ serverTotalPages }}</span>
      <button type="button" :disabled="currentPage >= serverTotalPages - 1 || loading" @click="emitPage(currentPage + 1)">
        Next
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import DataTable from './DataTable.vue';

export interface DynamicTableColumn {
  key: string;
  label: string;
  type: string;
  sortable: boolean;
  searchable: boolean;
  unit: string;
}

export interface DynamicTableRow {
  rowId: string;
  values: Record<string, unknown>;
}

export interface DynamicTableDefinition {
  tableId: string;
  label: string;
  columns: DynamicTableColumn[];
  rows: DynamicTableRow[];
  totalRows: number;
  defaultSort: string;
}

const props = withDefaults(defineProps<{
  tables: DynamicTableDefinition[];
  loading?: boolean;
  error?: string;
  pageSize?: number;
  serverSide?: boolean;
  currentPage?: number;
}>(), {
  loading: false,
  error: '',
  pageSize: 25,
  serverSide: false,
  currentPage: 0
});

const emit = defineEmits<{
  tableSelected: [tableId: string];
  pageChange: [page: number];
}>();

const selectedTableId = ref('');
const activeTable = computed(() => props.tables.find((table) => table.tableId === selectedTableId.value) ?? props.tables[0]);
const rows = computed(() => (activeTable.value?.rows ?? []).map((row) => ({ rowId: row.rowId, ...row.values })));
const currentPage = computed(() => Math.max(props.currentPage, 0));
const serverTotalPages = computed(() => Math.max(1, Math.ceil((activeTable.value?.totalRows ?? 0) / props.pageSize)));

watch(() => props.tables, (tables) => {
  const nextTableId = tables.find((table) => table.tableId === selectedTableId.value)?.tableId ?? tables[0]?.tableId ?? '';
  selectedTableId.value = nextTableId;
}, { immediate: true });

function selectTable(tableId: string) {
  selectedTableId.value = tableId;
  emit('tableSelected', tableId);
}

function emitPage(page: number) {
  emit('pageChange', page);
}
</script>
