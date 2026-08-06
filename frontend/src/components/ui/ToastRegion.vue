<script setup>
import { storeToRefs } from 'pinia'

import { useToastStore } from '@/stores/toast'

const toastStore = useToastStore()
const { toasts } = storeToRefs(toastStore)

const toastIcon = {
  success: '✓',
  error: '!',
  info: 'i',
}

async function runAction(toast) {
  if (!toast.action) return
  try {
    await toast.action()
    toastStore.dismiss(toast.id)
  } catch {
    toastStore.error('요청한 작업을 완료하지 못했습니다.')
  }
}
</script>

<template>
  <TransitionGroup name="toast" tag="div" class="toast-region" aria-label="알림">
    <article
      v-for="toast in toasts"
      :key="toast.id"
      :class="['toast', `toast--${toast.type}`]"
      :role="toast.type === 'error' ? 'alert' : 'status'"
      :aria-live="toast.type === 'error' ? 'assertive' : 'polite'"
    >
      <span class="toast__icon" aria-hidden="true">{{ toastIcon[toast.type] }}</span>
      <p>{{ toast.message }}</p>
      <button
        v-if="toast.action && toast.actionLabel"
        class="toast__action"
        type="button"
        @click="runAction(toast)"
      >
        {{ toast.actionLabel }}
      </button>
      <button type="button" aria-label="알림 닫기" @click="toastStore.dismiss(toast.id)">×</button>
    </article>
  </TransitionGroup>
</template>

<style scoped>
.toast-region {
  position: fixed;
  z-index: 1200;
  top: calc(var(--layout-header-height) + 16px);
  right: 20px;
  display: grid;
  width: min(380px, calc(100vw - 40px));
  gap: 10px;
  pointer-events: none;
}

.toast {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  gap: 12px;
  align-items: center;
  padding: 14px 14px 14px 16px;
  border: 1px solid var(--color-border);
  border-left: 4px solid var(--color-info);
  border-radius: 12px;
  background: var(--color-surface);
  color: var(--color-text);
  box-shadow: 0 16px 44px rgb(15 23 42 / 16%);
  pointer-events: auto;
}

.toast--success {
  border-left-color: var(--color-success);
}

.toast--error {
  border-left-color: var(--color-danger);
}

.toast__icon {
  display: grid;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  place-items: center;
  background: var(--color-info-soft);
  color: var(--color-info);
  font-size: 13px;
  font-weight: 850;
}

.toast--success .toast__icon {
  background: var(--color-success-soft);
  color: #047857;
}

.toast--error .toast__icon {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.toast p {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
}

.toast button {
  display: grid;
  width: 30px;
  height: 30px;
  padding: 0;
  border: 0;
  border-radius: 8px;
  place-items: center;
  background: transparent;
  color: var(--color-text-muted);
  font-size: 20px;
  cursor: pointer;
}

.toast button:hover {
  background: var(--color-surface-muted);
}
.toast .toast__action { width: auto; min-width: 64px; padding: 0 10px; color: #e8443a; border: 1px solid #ffc2bd; font-size: 11px; font-weight: 800; }

.toast-enter-active,
.toast-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 520px) {
  .toast-region {
    top: 76px;
    right: 14px;
    left: 14px;
    width: auto;
  }
}

@media (prefers-reduced-motion: reduce) {
  .toast-enter-active,
  .toast-leave-active {
    transition: none;
  }
}
</style>
