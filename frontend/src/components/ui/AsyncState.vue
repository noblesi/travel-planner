<script setup>
import { computed } from 'vue'

import BaseButton from '@/components/ui/BaseButton.vue'

const props = defineProps({
  variant: {
    type: String,
    required: true,
    validator: (value) => ['loading', 'empty', 'error'].includes(value),
  },
  title: {
    type: String,
    required: true,
  },
  message: {
    type: String,
    default: '',
  },
  actionLabel: {
    type: String,
    default: '',
  },
})

defineEmits(['action'])

const stateRole = computed(() => (props.variant === 'error' ? 'alert' : 'status'))
const liveMode = computed(() => (props.variant === 'error' ? 'assertive' : 'polite'))
</script>

<template>
  <section
    :class="['async-state', `async-state--${variant}`]"
    :role="stateRole"
    :aria-live="liveMode"
    :aria-busy="variant === 'loading' ? 'true' : undefined"
  >
    <span v-if="variant === 'loading'" class="async-state__spinner" aria-hidden="true" />
    <span v-else class="async-state__icon" aria-hidden="true">
      {{ variant === 'error' ? '!' : '○' }}
    </span>
    <strong>{{ title }}</strong>
    <p v-if="message">{{ message }}</p>
    <BaseButton
      v-if="actionLabel && variant !== 'loading'"
      :variant="variant === 'error' ? 'primary' : 'secondary'"
      size="sm"
      @click="$emit('action')"
    >
      {{ actionLabel }}
    </BaseButton>
  </section>
</template>

<style scoped>
.async-state {
  display: grid;
  min-height: 220px;
  gap: 12px;
  place-items: center;
  align-content: center;
  padding: 28px;
  border: 1px dashed var(--color-border);
  border-radius: 16px;
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
  text-align: center;
}

.async-state--error {
  border-color: #fecaca;
  background: var(--color-danger-soft);
}

.async-state strong {
  color: var(--color-text);
  line-height: 1.5;
}

.async-state p {
  margin: -4px 0 0;
  font-size: 13px;
  line-height: 1.6;
}

.async-state__icon {
  display: grid;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  place-items: center;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  font-size: 22px;
  font-weight: 800;
}

.async-state--error .async-state__icon {
  background: #fee2e2;
  color: var(--color-danger);
}

.async-state__spinner {
  width: 34px;
  height: 34px;
  border: 3px solid var(--color-brand-border);
  border-top-color: var(--color-brand);
  border-radius: 50%;
  animation: state-spin 800ms linear infinite;
}

@keyframes state-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .async-state__spinner {
    animation: none;
  }
}
</style>
