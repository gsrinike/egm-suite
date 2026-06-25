<template>
  <nav class="common-menu">
    <button
      v-for="item in items"
      :key="item.id"
      class="common-menu__item"
      :class="{ 'common-menu__item--active': item.id === activeId }"
      type="button"
      @click="emit('select', item.id)"
    >
      {{ item.label }}
    </button>
  </nav>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{ id: string; label: string }>;
  activeId: string;
}>();

const emit = defineEmits<{
  select: [id: string];
}>();
</script>
