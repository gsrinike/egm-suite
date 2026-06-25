<template>
  <a class="common-link" :href="href" @click="handleClick">
    <slot />
  </a>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{
  href?: string;
}>(), {
  href: '#'
});

const emit = defineEmits<{
  click: [event: MouseEvent];
}>();

function handleClick(event: MouseEvent) {
  if (props.href === '#') {
    event.preventDefault();
  }
  emit('click', event);
}
</script>
