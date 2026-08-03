<script setup>
defineProps({
  variant: { type: String, default: 'secondary' },
  loading: Boolean,
  disabled: Boolean,
})
</script>

<template>
  <button
    :class="['admin-button', `admin-button--${variant}`]"
    type="button"
    :disabled="disabled || loading"
    :aria-busy="loading || undefined"
  >
    <span v-if="loading" class="spinner" aria-hidden="true" />
    <slot />
  </button>
</template>

<style scoped>
.admin-button { display: inline-flex; min-height: 38px; align-items: center; justify-content: center; gap: 7px; padding: 0 15px; border: 1px solid var(--admin-border); border-radius: 6px; background: var(--admin-surface); color: var(--admin-text); font: inherit; font-size: 12px; font-weight: 800; cursor: pointer; }
.admin-button:hover:not(:disabled) { background: var(--admin-orange-soft); }
.admin-button:focus-visible { outline: 3px solid rgb(243 136 59 / 25%); outline-offset: 2px; }
.admin-button:disabled { cursor: not-allowed; opacity: .55; }
.admin-button--primary { border-color: var(--admin-orange); background: var(--admin-orange); color: #fff; }
.admin-button--primary:hover:not(:disabled) { background: var(--admin-orange-hover); }
.admin-button--danger { border-color: #ef857d; color: #dd5c55; }
.admin-button--ghost { border-color: transparent; background: transparent; color: var(--admin-muted); }
.spinner { width: 14px; height: 14px; border: 2px solid currentColor; border-right-color: transparent; border-radius: 50%; animation: spin .7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
