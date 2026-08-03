<script setup>
defineProps({
  modelValue: { type: [String, Number], default: '' },
  type: { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  ariaLabel: { type: String, required: true },
  options: { type: Array, default: () => [] },
})
defineEmits(['update:modelValue'])
</script>

<template>
  <select v-if="type === 'select'" class="admin-control" :value="modelValue" :aria-label="ariaLabel" @change="$emit('update:modelValue', $event.target.value)">
    <option v-for="option in options" :key="option.value" :value="option.value">{{ option.label }}</option>
  </select>
  <input v-else class="admin-control" :type="type" :value="modelValue" :placeholder="placeholder" :aria-label="ariaLabel" @input="$emit('update:modelValue', $event.target.value)" />
</template>

<style scoped>
.admin-control { width: 100%; height: 40px; padding: 0 13px; border: 1px solid var(--admin-border); border-radius: 6px; outline: none; background: var(--admin-surface); color: var(--admin-text); font: inherit; font-size: 13px; }
.admin-control:focus { border-color: var(--admin-orange); box-shadow: 0 0 0 3px rgb(243 136 59 / 14%); }
</style>
