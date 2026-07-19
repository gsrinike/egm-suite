<template>
  <section class="dynamic-table-shell">
    <div class="dynamic-table-tabs" v-if="tables.length > 1">
      <button
        v-for="table in tables"
        :key="table.tableId"
        type="button"
        :class="{ active: selectedTableId === table.tableId }"
        @click="selectedTableId = table.tableId"
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
    />
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
}>(), {
  loading: false,
  error: '',
  pageSize: 25
});

const selectedTableId = ref('');
const activeTable = computed(() => props.tables.find((table) => table.tableId === selectedTableId.value) ?? props.tables[0]);
const rows = computed(() => (activeTable.value?.rows ?? []).map((row) => ({ rowId: row.rowId, ...row.values })));

watch(() => props.tables, (tables) => {
  selectedTableId.value = tables[0]?.tableId ?? '';
}, { immediate: true });
</script>
