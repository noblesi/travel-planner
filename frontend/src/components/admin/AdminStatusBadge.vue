<script setup>
defineProps({ tone: { type: String, default: 'neutral' } })
</script>

<template><span :class="['admin-badge', `admin-badge--${tone}`]"><slot /></span></template>

<style scoped>
.admin-badge { display: inline-flex; min-height: 24px; align-items: center; justify-content: center; padding: 0 9px; border-radius: 20px; background: #f0f1f3; color: #747b84; font-size: 11px; font-weight: 800; white-space: nowrap; }
.admin-badge--success { background: #e3f7eb; color: #2d9863; }
.admin-badge--info { background: #e5f3fc; color: #3f8ac0; }
.admin-badge--warning { background: var(--admin-orange-soft); color: var(--admin-orange); }
.admin-badge--danger { background: #ffe7e5; color: #e2625c; }
</style>
