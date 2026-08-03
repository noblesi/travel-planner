<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'button',
    validator: (value) => ['button', 'submit', 'reset'].includes(value),
  },
  variant: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'secondary', 'ghost', 'danger'].includes(value),
  },
  size: {
    type: String,
    default: 'md',
    validator: (value) => ['sm', 'md', 'lg'].includes(value),
  },
  disabled: Boolean,
  loading: Boolean,
  block: Boolean,
})

const isDisabled = computed(() => props.disabled || props.loading)
</script>

<template>
  <button
    :class="[
      'base-button',
      `base-button--${variant}`,
      `base-button--${size}`,
      { 'base-button--block': block },
    ]"
    :type="type"
    :disabled="isDisabled"
    :aria-busy="loading ? 'true' : undefined"
  >
    <span v-if="loading" class="base-button__spinner" aria-hidden="true" />
    <span class="base-button__content"><slot /></span>
  </button>
</template>

<style scoped>
.base-button {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  border-radius: 10px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  transition: background-color 150ms ease, border-color 150ms ease, color 150ms ease;
}

.base-button--sm {
  min-height: 36px;
  padding: 0 13px;
  font-size: 13px;
}

.base-button--md {
  min-height: 44px;
  padding: 0 18px;
  font-size: 14px;
}

.base-button--lg {
  min-height: 50px;
  padding: 0 22px;
  font-size: 15px;
}

.base-button--block {
  width: 100%;
}

.base-button--primary {
  border-color: var(--color-brand);
  background: var(--color-brand);
  color: var(--color-brand-on);
}

.base-button--primary:hover:not(:disabled) {
  border-color: var(--color-brand-hover);
  background: var(--color-brand-hover);
}

.base-button--secondary {
  border-color: var(--color-brand-border);
  background: var(--color-surface);
  color: var(--color-brand);
}

.base-button--secondary:hover:not(:disabled) {
  border-color: var(--color-brand-accent);
  background: var(--color-brand-soft);
}

.base-button--ghost {
  background: transparent;
  color: var(--color-text-muted);
}

.base-button--ghost:hover:not(:disabled) {
  background: var(--color-surface-muted);
  color: var(--color-text);
}

.base-button--danger {
  border-color: var(--color-danger);
  background: var(--color-danger);
  color: #ffffff;
}

.base-button--danger:hover:not(:disabled) {
  border-color: #991b1b;
  background: #991b1b;
}

.base-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.base-button__spinner {
  width: 16px;
  height: 16px;
  flex: 0 0 auto;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: button-spin 700ms linear infinite;
}

@keyframes button-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .base-button,
  .base-button__spinner {
    transition: none;
    animation: none;
  }
}
</style>
