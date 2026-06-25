<template>
  <section class="common-table-shell">
    <div class="common-table-toolbar">
      <input v-model="search" type="search" placeholder="Search" />
      <span>{{ filteredRows.length }} rows</span>
    </div>
    <div class="common-table-scroll">
      <table class="common-table">
        <thead>
          <tr>
            <th v-for="column in columns" :key="column.key" @click="sort(column.key)">
              {{ column.label }}
              <span v-if="sortKey === column.key">{{ ascending ? 'ASC' : 'DESC' }}</span>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in pagedRows" :key="rowKey(row)">
            <td v-for="column in columns" :key="column.key">
              <slot :name="`cell-${column.key}`" :row="row" :value="cell(row, column.key)">
                {{ cell(row, column.key) }}
              </slot>
            </td>
          </tr>
          <tr v-if="pagedRows.length === 0">
            <td :colspan="columns.length">No rows</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="common-pagination">
      <button type="button" :disabled="page === 0" @click="page--">Previous</button>
      <span>Page {{ page + 1 }} / {{ totalPages }}</span>
      <button type="button" :disabled="page >= totalPages - 1" @click="page++">Next</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';

type Row = Record<string, unknown>;

const props = withDefaults(defineProps<{
  columns: Array<{ key: string; label: string }>;
  rows: Row[];
  pageSize?: number;
  idKey?: string;
}>(), {
  pageSize: 10,
  idKey: 'id'
});

const search = ref('');
const sortKey = ref('');
const ascending = ref(true);
const page = ref(0);

const filteredRows = computed(() => {
  const query = search.value.toLowerCase();
  const values = query
    ? props.rows.filter((row) => Object.values(row).some((value) => String(value ?? '').toLowerCase().includes(query)))
    : props.rows;
  if (!sortKey.value) {
    return values;
  }
  return [...values].sort((left, right) => {
    const a = String(left[sortKey.value] ?? '');
    const b = String(right[sortKey.value] ?? '');
    return ascending.value ? a.localeCompare(b) : b.localeCompare(a);
  });
});

const totalPages = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / props.pageSize)));
const pagedRows = computed(() => filteredRows.value.slice(page.value * props.pageSize, (page.value + 1) * props.pageSize));

watch(search, () => {
  page.value = 0;
});

function sort(key: string) {
  if (sortKey.value === key) {
    ascending.value = !ascending.value;
  } else {
    sortKey.value = key;
    ascending.value = true;
  }
}

function rowKey(row: Row) {
  return String(row[props.idKey] ?? JSON.stringify(row));
}

function cell(row: Row, key: string) {
  return row[key] ?? '';
}
</script>
