<template>
  <button
    class="common-refresh-button"
    type="button"
    :title="title"
    :aria-label="label"
    @click="refresh"
  >
    ⟳
  </button>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{
  label?: string;
  title?: string;
  reload?: boolean;
}>(), {
  label: 'Reload page',
  title: 'Reload page',
  reload: true
});

const emit = defineEmits<{
  refresh: [];
}>();

function refresh() {
  emit('refresh');
  if (props.reload) {
    window.location.reload();
  }
}
</script>
