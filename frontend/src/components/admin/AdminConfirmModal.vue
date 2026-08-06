<script setup>
import { onBeforeUnmount, onMounted } from 'vue'

import AdminButton from './AdminButton.vue'

defineProps({
  title: { type: String, required: true },
  message: { type: String, required: true },
  confirmLabel: { type: String, default: '확인' },
  danger: Boolean,
})
const emit = defineEmits(['confirm', 'cancel'])
const onKeydown = (event) => { if (event.key === 'Escape') emit('cancel') }
onMounted(() => { document.body.style.overflow = 'hidden'; window.addEventListener('keydown', onKeydown) })
onBeforeUnmount(() => { document.body.style.overflow = ''; window.removeEventListener('keydown', onKeydown) })
</script>

<template>
  <div class="modal-overlay" role="presentation" @click.self="$emit('cancel')">
    <section class="confirm-modal" role="alertdialog" aria-modal="true" aria-labelledby="confirm-title" aria-describedby="confirm-message">
      <div class="modal-icon" aria-hidden="true">!</div>
      <h2 id="confirm-title">{{ title }}</h2>
      <p id="confirm-message">{{ message }}</p>
      <div class="modal-actions">
        <AdminButton @click="$emit('cancel')">취소</AdminButton>
        <AdminButton :variant="danger ? 'danger' : 'primary'" autofocus @click="$emit('confirm')">{{ confirmLabel }}</AdminButton>
      </div>
    </section>
  </div>
</template>

<style scoped>
.modal-overlay { position: fixed; z-index: 1500; inset: 0; display: grid; padding: 24px; place-items: center; background: rgb(37 42 49 / 48%); }
.confirm-modal { width: min(420px, 100%); padding: 28px; border: 1px solid var(--admin-border); border-radius: 16px; background: var(--admin-surface); box-shadow: 0 24px 70px rgb(37 42 49 / 20%); text-align: center; }
.modal-icon { display: grid; width: 42px; height: 42px; margin: 0 auto 16px; border-radius: 50%; place-items: center; background: var(--admin-orange-soft); color: var(--admin-orange); font-size: 20px; font-weight: 900; }
.confirm-modal h2 { margin: 0; font-size: 20px; }.confirm-modal p { margin: 12px 0 24px; color: var(--admin-muted); font-size: 13px; line-height: 1.65; }
.modal-actions { display: flex; justify-content: center; gap: 10px; }
</style>
