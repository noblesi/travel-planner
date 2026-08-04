<script setup>
defineProps({
  type: { type: String, default: 'empty' },
  title: { type: String, required: true },
  description: { type: String, default: '' },
  actionLabel: { type: String, default: '' },
})
defineEmits(['action'])
</script>

<template>
  <div class="state" :role="type === 'error' ? 'alert' : 'status'" :aria-busy="type === 'loading' || undefined">
    <span v-if="type === 'loading'" class="state-spinner" aria-hidden="true" />
    <span v-else class="state-icon" aria-hidden="true">{{ type === 'error' ? '!' : '⌕' }}</span>
    <strong>{{ title }}</strong><p v-if="description">{{ description }}</p>
    <button v-if="actionLabel && type !== 'loading'" type="button" @click="$emit('action')">{{ actionLabel }}</button>
  </div>
</template>

<style scoped>
.state { display: grid; min-height: 190px; place-items: center; align-content: center; gap: 8px; color: var(--admin-muted); text-align: center; }
.state strong { color: var(--admin-text); font-size: 14px; }.state p { margin: 0; font-size: 12px; }
.state-icon { display: grid; width: 38px; height: 38px; border-radius: 50%; place-items: center; background: var(--admin-orange-soft); color: var(--admin-orange); font-weight: 900; }
.state-spinner { width: 30px; height: 30px; border: 3px solid var(--admin-orange-soft); border-top-color: var(--admin-orange); border-radius: 50%; animation: spin .7s linear infinite; }
.state button { margin-top: 5px; padding: 8px 13px; border: 1px solid var(--admin-orange); border-radius: 6px; background: #fff; color: var(--admin-orange); font-weight: 800; cursor: pointer; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
