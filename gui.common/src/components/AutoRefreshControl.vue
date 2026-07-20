<template>
  <div class="common-auto-refresh-control" :class="{ 'common-auto-refresh-control--disabled': disabled }">
    <span class="common-auto-refresh-control__gear" title="Refresh options" aria-hidden="true">⚙</span>
    <select v-model="selectedInterval" aria-label="Refresh interval" :disabled="disabled" @change="applySelection">
      <option value="5000">5 seconds</option>
      <option value="10000">10 seconds</option>
      <option value="30000">30 seconds</option>
      <option value="manual">Manual</option>
    </select>
    <RefreshButton
      v-if="selectedInterval === 'manual' && !disabled"
      label="Refresh"
      title="Refresh"
      :reload="false"
      @refresh="emit('refresh')"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import RefreshButton from './RefreshButton.vue';

const props = withDefaults(defineProps<{
  storageKey?: string;
  defaultInterval?: number | null;
  disabled?: boolean;
}>(), {
  storageKey: 'egm.auto-refresh.interval',
  defaultInterval: null,
  disabled: false
});

const emit = defineEmits<{
  intervalChange: [intervalMs: number | null];
  refresh: [];
}>();

const selectedInterval = ref(readStoredInterval());

onMounted(() => {
  emitSelectedInterval();
});
watch(() => props.disabled, () => {
  emitSelectedInterval();
});

function applySelection() {
  if (props.disabled) {
    emit('intervalChange', null);
    return;
  }
  writeStoredInterval(selectedInterval.value);
  emitSelectedInterval();
}

function emitSelectedInterval() {
  emit('intervalChange', props.disabled ? null : selectedValue());
}

function selectedValue(): number | null {
  return selectedInterval.value === 'manual' ? null : Number(selectedInterval.value);
}

function readStoredInterval(): string {
  try {
    const storedValue = window.localStorage.getItem(props.storageKey);
    if (storedValue && ['5000', '10000', '30000', 'manual'].includes(storedValue)) {
      return storedValue;
    }
  } catch {
    // Fall back to the component default when browser storage is unavailable.
  }
  return props.defaultInterval == null ? 'manual' : String(props.defaultInterval);
}

function writeStoredInterval(value: string) {
  try {
    window.localStorage.setItem(props.storageKey, value);
  } catch {
    // The selected value still applies for the active page.
  }
}
</script>
