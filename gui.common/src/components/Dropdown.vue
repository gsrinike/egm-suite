<template>
  <label class="common-field">
    <span>{{ label }}</span>
    <select :value="modelValue" @change="emit('update:modelValue', ($event.target as HTMLSelectElement).value)">
      <option v-for="option in options" :key="option.value" :value="option.value">
        {{ option.label }}
      </option>
    </select>
  </label>
</template>

<script setup lang="ts">
defineProps<{
  label: string;
  modelValue: string;
  options: Array<{ label: string; value: string }>;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();
</script>
